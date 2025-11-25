package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZombieGamePanel extends JPanel {

    private final ZombieFrame frame;
    private final RoundManager roundManager;

    // HUD
    private final JLabel infoLabel;
    private final JLabel scoreLabel;
    private final JPanel heartPanel;

    // 입력
    private final JTextField inputField;

    // 게임 화면
    private final JPanel viewPanel;

    // 게임 상태
    private String playerName = "Player";
    private int hp = 5;
    private int score = 0;
    private boolean isRoundAnimating = false;

    // 라운드/틱
    private int tickCount = 0;
    private GameLoopThread gameThread;

    // 좀비 / 총알
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private int zombieIdSeq = 1;

    // 이미지
    private Image gunImage;
    private Image backgroundImage;
    private final Image[] zombieImages = new Image[4];

    // 총 그려진 위치(총알 출발점 계산용)
    private int gunDrawX, gunDrawY, gunDrawW, gunDrawH;

    // 피격 연출
    private int damageEffectFrames = 0;
    private int gunShakeFrames = 0;
    private static final int DAMAGE_DISTANCE_THRESHOLD = 5;

    // ---------------- 내부 클래스 ----------------

    private static class Zombie {
        String word;
        int distance;   // 0에 가까울수록 플레이어 근처
        int id;
        int xPos;       // 화면상 x 좌표(중앙 기준)
        int spriteIndex;

        Zombie(int id, String word, int distance, int xPos, int spriteIndex) {
            this.id = id;
            this.word = word;
            this.distance = distance;
            this.xPos = xPos;
            this.spriteIndex = spriteIndex;
        }
    }

    private static class Bullet {
        double x, y;
        final double startX, startY;
        final double targetX, targetY;
        final Zombie target;
        boolean finished = false;

        Bullet(double startX, double startY, double targetX, double targetY, Zombie target) {
            this.startX = startX;
            this.startY = startY;
            this.x = startX;
            this.y = startY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.target = target;
        }
    }

    /** 게임 루프 담당 쓰레드 (좀비 생성/이동) */
    private class GameLoopThread extends Thread {
        private volatile boolean running = true;

        public void requestStop() {
            running = false;
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                int delay = getCurrentDelayByRound();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
                if (!running) break;

                SwingUtilities.invokeLater(() -> gameTick());
            }
        }
    }

    /** 총알 애니메이션 쓰레드 */
    private class BulletThread extends Thread {
        private final Bullet bullet;

        BulletThread(Bullet bullet) {
            this.bullet = bullet;
        }

        @Override
        public void run() {
            int steps = 22;          // 총알 이동 단계
            int sleepMs = 18;
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                final double nx = bullet.startX + (bullet.targetX - bullet.startX) * t;
                final double ny = bullet.startY + (bullet.targetY - bullet.startY) * t;

                SwingUtilities.invokeLater(() -> {
                    bullet.x = nx;
                    bullet.y = ny;
                    viewPanel.repaint();
                });

                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    return;
                }
            }

            SwingUtilities.invokeLater(() -> {
                bullet.finished = true;
                applyBulletHit(bullet.target);
                bullets.remove(bullet);
                viewPanel.repaint();
            });
        }
    }

    // ---------------- 생성자 ----------------

    public ZombieGamePanel(ZombieFrame frame) {
        this.frame = frame;
        this.roundManager = new RoundManager();

        setLayout(new BorderLayout());

        // 상단 HUD
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        heartPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        infoLabel = new JLabel("플레이어: -  |  라운드: 1");
        infoLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        leftPanel.add(heartPanel);
        leftPanel.add(infoLabel);

        scoreLabel = new JLabel("점수: 0", SwingConstants.RIGHT);
        scoreLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(scoreLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 이미지 로딩
        loadImages();

        // 중앙 게임 화면
        viewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGameScreen((Graphics2D) g);
            }
        };
        viewPanel.setBackground(Color.BLACK);
        add(viewPanel, BorderLayout.CENTER);

        // 하단 입력
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JLabel inputLabel = new JLabel(" 입력: ");
        inputLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        inputField = new JTextField();
        inputField.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        bottomPanel.add(inputLabel, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // 입력 이벤트
        inputField.addActionListener(e -> {
            if (isRoundAnimating || gameThread == null) return;
            String text = inputField.getText().trim();
            inputField.setText("");
            if (!text.isEmpty()) {
                handleShot(text);
            }
        });

        // ESC: 일시정지
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    togglePause();
                }
            }
        });

        // TAB: 언제든지 입력창 포커스
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "focusInputField");
        getActionMap().put("focusInputField", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        updateHearts();
        updateHud();
    }

    // ---------------- 게임 시작 / 라운드 ----------------

    public void startNewGame(String name) {
        this.playerName = name;
        this.hp = 5;
        this.score = 0;
        this.zombies.clear();
        this.bullets.clear();
        this.zombieIdSeq = 1;
        this.tickCount = 0;

        roundManager.reset();

        updateHud();
        updateHearts();

        inputField.setText("");
        inputField.requestFocus();

        startRoundEffect();
    }

    /** 라운드 시작 연출 + 스레드 시작 */
    private void startRoundEffect() {
        isRoundAnimating = true;
        stopGameThread();
        viewPanel.repaint();

        Thread effectThread = new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(() -> {
                isRoundAnimating = false;
                if (hp > 0 && roundManager.getRound() <= 3) {
                    startGameThread();
                }
                viewPanel.repaint();
            });
        });
        effectThread.start();
    }

    private void startGameThread() {
        stopGameThread();
        gameThread = new GameLoopThread();
        gameThread.start();
    }

    private void stopGameThread() {
        if (gameThread != null) {
            gameThread.requestStop();
            gameThread = null;
        }
    }

    /** 현재 라운드에 따라 게임 틱 딜레이(ms) */
    private int getCurrentDelayByRound() {
        int r = roundManager.getRound();
        if (r <= 1) return 700;
        if (r == 2) return 500;
        return 350;
    }

    // ---------------- 메인 게임 틱 ----------------

    private void gameTick() {
        if (hp <= 0) return;
        if (isRoundAnimating) return;

        tickCount++;

        int round = roundManager.getRound();
        int spawnInterval;
        if (round <= 1) spawnInterval = 3;
        else if (round == 2) spawnInterval = 2;
        else spawnInterval = 1;

        if (tickCount % spawnInterval == 0) {
            spawnZombie();
        }

        boolean damaged = false;
        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            z.distance -= roundManager.getZombieSpeed();

            if (z.distance <= DAMAGE_DISTANCE_THRESHOLD) {
                it.remove();
                hp--;
                damaged = true;
            }
        }

        if (damaged) {
            updateHearts();
            triggerDamageEffect();
            if (hp <= 0) {
                gameOver();
                return;
            }
        }

        if (damageEffectFrames > 0) damageEffectFrames--;
        if (gunShakeFrames > 0) gunShakeFrames--;

        viewPanel.repaint();
    }

    private void triggerDamageEffect() {
        damageEffectFrames = 10;   // 화면 붉게
        gunShakeFrames = 10;       // 총 흔들림
    }

    private void spawnZombie() {
        String w = WordManager.getInstance().getRandomWord();
        int xPos = (int) (viewPanel.getWidth() * (0.15 + Math.random() * 0.7));

        int spriteIndex = 0;
        if (zombieImages.length > 0) {
            spriteIndex = (int) (Math.random() * zombieImages.length);
            if (spriteIndex < 0) spriteIndex = 0;
            if (spriteIndex >= zombieImages.length) spriteIndex = zombieImages.length - 1;
        }

        zombies.add(new Zombie(zombieIdSeq++, w, 100, xPos, spriteIndex));
    }

    // ---------------- 그리기 ----------------

    private void drawGameScreen(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(20, 20, 40),
                    0, getHeight(), new Color(40, 40, 60)
            );
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // 좀비
        drawZombies(g2d);

        // 총알
        drawBullets(g2d);

        // 총
        drawGun(g2d);

        // 피격 시 붉은 플래시
        if (damageEffectFrames > 0) {
            drawDamageOverlay(g2d);
        }

        // 조준선
        drawCrosshair(g2d);

        // 라운드 시작 연출
        if (isRoundAnimating) {
            drawRoundEffect(g2d);
        }
    }

    private void drawDamageOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(255, 0, 0, 80));
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawGun(Graphics2D g2d) {
        if (gunImage == null) return;

        int w = gunImage.getWidth(this);
        int h = gunImage.getHeight(this);

        double fixedScale = 0.85;
        int drawW = (int) (w * fixedScale);
        int drawH = (int) (h * fixedScale);

        double autoScale = 1.0;
        if (drawW > getWidth() * 0.55) {
            autoScale = (getWidth() * 0.55) / drawW;
        }
        if (drawH > getHeight() * 0.60) {
            autoScale = Math.min(autoScale, (getHeight() * 0.60) / drawH);
        }
        if (autoScale < 1.0) {
            drawW = (int) (drawW * autoScale);
            drawH = (int) (drawH * autoScale);
        }

        int marginX = 60;
        int marginY = 45;
        int x = getWidth() - drawW - marginX;
        int y = getHeight() - drawH - marginY;

        if (gunShakeFrames > 0) {
            int sx = (int) (Math.sin(gunShakeFrames * 0.7) * 6);
            int sy = (int) (Math.cos(gunShakeFrames * 0.7) * 4);
            x += sx;
            y += sy;
        }

        g2d.drawImage(gunImage, x, y, drawW, drawH, this);

        gunDrawX = x;
        gunDrawY = y;
        gunDrawW = drawW;
        gunDrawH = drawH;
    }

    private void drawBullets(Graphics2D g2d) {
        g2d.setColor(new Color(255, 230, 80));
        int r = 6;
        for (Bullet b : bullets) {
            if (b.finished) continue;
            g2d.fillOval((int) b.x - r, (int) b.y - r, r * 2, r * 2);
        }
    }

    private void drawCrosshair(Graphics2D g2d) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(cx - 12, cy, cx + 12, cy);
        g2d.drawLine(cx, cy - 12, cx, cy + 12);
    }

    private void drawZombies(Graphics2D g2d) {
        int groundY = getHeight() * 2 / 3;

        boolean anySprite = false;
        for (Image img : zombieImages) {
            if (img != null) {
                anySprite = true;
                break;
            }
        }

        for (Zombie z : zombies) {
            if (anySprite &&
                    z.spriteIndex >= 0 && z.spriteIndex < zombieImages.length &&
                    zombieImages[z.spriteIndex] != null) {

                Image img = zombieImages[z.spriteIndex];
                int iw = img.getWidth(this);
                int ih = img.getHeight(this);

                float depthScale = 1.0f - (z.distance / 120.0f);
                if (depthScale < 0.3f) depthScale = 0.3f;
                if (depthScale > 1.0f) depthScale = 1.0f;

                float spriteScale = 0.25f + 0.55f * depthScale;

                int drawW = (int) (iw * spriteScale);
                int drawH = (int) (ih * spriteScale);

                // 흔들림/들썩임(생동감)
                double t = (tickCount + z.id * 5) * 0.15;
                int sway = (int) (Math.cos(t) * 3 * depthScale);
                int bob = (int) (Math.sin(t) * 5 * depthScale);

                int xPos = z.xPos - drawW / 2 + sway;
                int yPos = groundY - drawH + bob;

                // 그림자
                int shadowW = (int) (drawW * 0.7);
                int shadowH = (int) (drawH * 0.15);
                int shadowX = z.xPos - shadowW / 2 + sway;
                int shadowY = groundY - shadowH / 2;
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.fillOval(shadowX, shadowY, shadowW, shadowH);

                g2d.drawImage(img, xPos, yPos, drawW, drawH, this);

                int fontSize = Math.max(14, (int) (24 * spriteScale));
                g2d.setFont(new Font("맑은 고딕", Font.BOLD, fontSize));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(z.word);

                int textX = xPos + drawW / 2 - textWidth / 2;
                int textY = yPos - 10;

                g2d.setColor(Color.BLACK);
                g2d.drawString(z.word, textX + 1, textY + 1);
                g2d.drawString(z.word, textX - 1, textY + 1);

                g2d.setColor(Color.RED);
                g2d.drawString(z.word, textX, textY);
            } else {
                // (백업) 이미지 없을 때 원형으로 표현
                float scale = 1.0f - (z.distance / 120.0f);
                if (scale < 0.25f) scale = 0.25f;
                int size = (int) (80 * scale);
                int yPos = groundY - size;
                g2d.setColor(new Color(0, 120, 0, 180));
                g2d.fillOval(z.xPos - size / 2, yPos, size, size);
                g2d.setColor(Color.WHITE);
                g2d.drawString(z.word, z.xPos - size / 2, yPos - 5);
            }
        }
    }

    private void drawRoundEffect(Graphics2D g2d) {
        String msg = "ROUND " + roundManager.getRound();
        g2d.setFont(new Font("Verdana", Font.BOLD, 100));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(msg);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.BLACK);
        g2d.drawString(msg, cx - textW / 2 + 5, cy + 5);

        g2d.setColor(Color.YELLOW);
        g2d.drawString(msg, cx - textW / 2, cy);
    }

    // ---------------- 입력/사격 처리 ----------------

    private void handleShot(String text) {
        String typed = text.trim();
        if (typed.isEmpty()) return;

        Zombie target = null;
        for (Zombie z : zombies) {
            if (z.word.equals(typed)) {
                if (target == null || z.distance < target.distance) {
                    target = z;
                }
            }
        }
        if (target == null) return;

        int startX, startY;
        if (gunDrawW > 0 && gunDrawH > 0) {
            startX = gunDrawX + (int) (gunDrawW * 0.75);
            startY = gunDrawY + (int) (gunDrawH * 0.35);
        } else {
            startX = getWidth() - 100;
            startY = getHeight() - 100;
        }

        Point p = computeZombieCenter(target);
        Bullet bullet = new Bullet(startX, startY, p.x, p.y, target);
        bullets.add(bullet);

        new BulletThread(bullet).start();
    }

    private Point computeZombieCenter(Zombie z) {
        int groundY = getHeight() * 2 / 3;

        Image img = null;
        if (z.spriteIndex >= 0 && z.spriteIndex < zombieImages.length) {
            img = zombieImages[z.spriteIndex];
        }
        if (img == null) {
            int y = groundY - 60;
            return new Point(z.xPos, y);
        }

        int iw = img.getWidth(this);
        int ih = img.getHeight(this);

        float depthScale = 1.0f - (z.distance / 120.0f);
        if (depthScale < 0.3f) depthScale = 0.3f;
        if (depthScale > 1.0f) depthScale = 1.0f;
        float spriteScale = 0.25f + 0.55f * depthScale;

        int drawW = (int) (iw * spriteScale);
        int drawH = (int) (ih * spriteScale);

        int xPos = z.xPos - drawW / 2;
        int yPos = groundY - drawH;

        int cx = xPos + drawW / 2;
        int cy = yPos + drawH / 3;
        return new Point(cx, cy);
    }

    private void applyBulletHit(Zombie target) {
        if (!zombies.contains(target)) return;

        zombies.remove(target);
        score++;

        if (roundManager.checkLevelUp(score)) {
            if (roundManager.getRound() > 3) {
                gameClear();
                return;
            } else {
                startRoundEffect();
            }
        }
        updateHud();
    }

    // ---------------- 일시정지 / 게임오버 ----------------

    private void togglePause() {
        if (isRoundAnimating || hp <= 0) return;

        boolean running = (gameThread != null);
        if (running) {
            stopGameThread();

            String[] options = {"계속하기", "메인으로"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "게임 일시정지",
                    "PAUSE",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (choice == 1) {
                frame.showStartPanel();
            } else {
                startGameThread();
                inputField.requestFocus();
            }
        }
    }

    private void gameOver() {
        stopGameThread();
        ScoreManager.getInstance().addScore(playerName, score);

        String[] options = {"다시하기", "메인으로"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "GAME OVER\n최종 점수: " + score + "\n라운드: " + roundManager.getRound(),
                "게임 종료",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            startNewGame(playerName);
        } else {
            frame.showStartPanel();
        }
    }

    private void gameClear() {
        stopGameThread();
        ScoreManager.getInstance().addScore(playerName, score);

        String[] options = {"다시하기", "메인으로"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "미션 클리어!\n최종 점수: " + score + "\n라운드: " + roundManager.getRound(),
                "게임 클리어",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            startNewGame(playerName);
        } else {
            frame.showStartPanel();
        }
    }

    // ---------------- HUD / 하트 ----------------

    private void updateHud() {
        infoLabel.setText("플레이어: " + playerName + "  |  라운드: " + roundManager.getRound());
        scoreLabel.setText("점수: " + score);
    }

    private void updateHearts() {
        heartPanel.removeAll();
        for (int i = 0; i < 5; i++) {
            JLabel heart = new JLabel(i < hp ? "❤️" : "🖤");
            heart.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            heartPanel.add(heart);
        }
        heartPanel.revalidate();
        heartPanel.repaint();
    }

    // ---------------- 이미지 로딩 ----------------

    private void loadImages() {
        // 총
        try {
            URL gunUrl = getClass().getResource("images/gun.png");
            if (gunUrl != null) {
                gunImage = new ImageIcon(gunUrl).getImage();
            } else {
                System.err.println("gun.png 로드 실패");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 좀비 4종
        try {
            zombieImages[0] = new ImageIcon(getClass().getResource("images/zombie_1.png")).getImage();
        } catch (Exception ex) { System.err.println("zombie_1.png 로드 실패"); }
        try {
            zombieImages[1] = new ImageIcon(getClass().getResource("images/zombie_2.png")).getImage();
        } catch (Exception ex) { System.err.println("zombie_2.png 로드 실패"); }
        try {
            zombieImages[2] = new ImageIcon(getClass().getResource("images/zombie_3.png")).getImage();
        } catch (Exception ex) { System.err.println("zombie_3.png 로드 실패"); }
        try {
            zombieImages[3] = new ImageIcon(getClass().getResource("images/zombie_4.png")).getImage();
        } catch (Exception ex) { System.err.println("zombie_4.png 로드 실패"); }

        // 배경
        try {
            String direct = "src/temp_project/images/ZombieBackground.jpg";
            ImageIcon bg = new ImageIcon(direct);
            if (bg.getImageLoadStatus() == MediaTracker.COMPLETE) {
                backgroundImage = bg.getImage();
            } else {
                URL bgUrl = getClass().getResource("images/ZombieBackground.jpg");
                if (bgUrl != null) {
                    backgroundImage = new ImageIcon(bgUrl).getImage();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
