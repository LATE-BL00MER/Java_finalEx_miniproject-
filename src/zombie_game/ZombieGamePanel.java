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

    // 중앙 게임 화면
    private final JPanel viewPanel;

    // 게임 상태
    private String playerName = "Player";
    private int hp = 5;
    private int score = 0;

    private boolean isRoundAnimating = false;
    private boolean isPaused = false;
    private boolean isCountingDown = false;
    private int countdownValue = 0;

    // 틱 & 스레드
    private long tickCount = 0;
    private GameLoopThread gameThread;
    private static final int LOOP_DELAY_MS = 40; // 약 25fps

    // 좀비 / 총알
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private int zombieIdSeq = 1;

    // 이미지들
    private Image gunImage;
    private Image backgroundImage;
    private final Image[] zombieImages = new Image[4];
    private Image bossImage;

    // 총 위치 (총알 출발점 계산)
    private int gunDrawX, gunDrawY, gunDrawW, gunDrawH;

    // 피격 연출 & 쿨타임
    private int damageEffectFrames = 0;
    private int gunShakeFrames = 0;
    private int damageCooldownTicks = 0;              // 맞은 후 잠깐 무적
    private static final double DAMAGE_DISTANCE_THRESHOLD = 0.0;
    private static final int DAMAGE_COOLDOWN_MAX = 15;

    // 근접 경고(닿기 직전 표현)
    private static final double DANGER_DISTANCE = 20.0;
    private boolean dangerNear = false;
    private int dangerPulseTick = 0;

    private String damageText = null;
    private int damageTextFrames = 0;

    // 보스
    private BossZombie bossZombie = null;
    private int bossSpawnCountThisRound = 0;
    // index : 라운드 번호, 값 : 해당 라운드에서 최대 보스 스폰 횟수
    private static final int[] BOSS_SPAWN_LIMIT = {0, 2, 3, 5};

    // ---------------- 내부 클래스 ----------------

    private static class Zombie {
        String word;
        double distance;  // 0에 가까울수록 플레이어 근처
        int id;
        int xPos;         // 화면 X 위치(중앙 기준)
        int spriteIndex;

        Zombie(int id, String word, double distance, int xPos, int spriteIndex) {
            this.id = id;
            this.word = word;
            this.distance = distance;
            this.xPos = xPos;
            this.spriteIndex = spriteIndex;
        }
    }

    /** 3개의 단어를 순서대로 맞춰야 죽는 보스 */
    private static class BossZombie extends Zombie {
        String[] words;
        int index;

        BossZombie(int id, String[] words, double distance, int xPos) {
            super(id, words[0], distance, xPos, -1);
            this.words = words;
            this.index = 0;
        }

        /** 현재 단어와 같으면 다음 단계로, 마지막이면 true(사망) */
        boolean hit(String typed) {
            if (!words[index].equalsIgnoreCase(typed)) {
                return false;
            }
            index++;
            if (index < words.length) {
                this.word = words[index];
                return false;
            } else {
                return true;
            }
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

    /** 메인 게임 루프 스레드 */
    private class GameLoopThread extends Thread {
        private volatile boolean running = true;

        public void requestStop() {
            running = false;
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(LOOP_DELAY_MS);
                } catch (InterruptedException e) {
                    break;
                }
                if (!running) break;

                SwingUtilities.invokeLater(() -> gameTick());
            }
        }
    }

    /** 총알 애니메이션 스레드 */
    private class BulletThread extends Thread {
        private final Bullet bullet;

        BulletThread(Bullet bullet) {
            this.bullet = bullet;
        }

        @Override
        public void run() {
            int steps = 22;
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

    /** 일시정지 해제 3-2-1 카운트다운 */
    private class ResumeCountdownThread extends Thread {
        @Override
        public void run() {
            try {
                for (int i = 3; i >= 1; i--) {
                    countdownValue = i;
                    SwingUtilities.invokeLater(() -> viewPanel.repaint());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException ignored) {
            }
            SwingUtilities.invokeLater(() -> {
                countdownValue = 0;
                isPaused = false;
                isCountingDown = false;
                startGameThread();
                inputField.requestFocusInWindow();
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

        // 엔터로 발사
        inputField.addActionListener(e -> {
            if (isRoundAnimating || isPaused || isCountingDown || gameThread == null) return;
            String text = inputField.getText().trim();
            inputField.setText("");
            if (!text.isEmpty()) {
                handleShot(text);
            }
        });

        // TAB: 언제든지 입력창 포커스
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "focusInputField");
        getActionMap().put("focusInputField", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        // ESC: 일시정지
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "togglePause");
        getActionMap().put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
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
        this.bossZombie = null;
        this.zombieIdSeq = 1;
        this.tickCount = 0;
        this.damageCooldownTicks = 0;
        this.damageEffectFrames = 0;
        this.gunShakeFrames = 0;
        this.damageText = null;
        this.damageTextFrames = 0;
        this.bossSpawnCountThisRound = 0;
        this.dangerNear = false;
        this.dangerPulseTick = 0;

        roundManager.reset();

        updateHud();
        updateHearts();

        inputField.setText("");
        inputField.requestFocus();

        startRoundEffect();
    }

    /** 라운드 시작 연출 */
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

    private void resetBossForNewRound() {
        bossZombie = null;
        bossSpawnCountThisRound = 0;
    }

    // ---------------- 메인 게임 틱 ----------------

    private void gameTick() {
        if (hp <= 0) return;
        if (isRoundAnimating || isPaused || isCountingDown) return;

        tickCount++;

        // 좀비 스폰 (라운드별 간격)
        int round = roundManager.getRound();
        int spawnInterval;
        if (round <= 1) spawnInterval = 32;      // 약 1.3초
        else if (round == 2) spawnInterval = 24; // 약 1.0초
        else spawnInterval = 18;                 // 약 0.7초

        if (tickCount % spawnInterval == 0) {
            spawnZombie();
        }

        // 보스 스폰 (라운드마다 제한 횟수 다름)
        trySpawnBoss();

        // 피격 쿨타임 감소
        if (damageCooldownTicks > 0) {
            damageCooldownTicks--;
        }
        if (damageEffectFrames > 0) damageEffectFrames--;
        if (gunShakeFrames > 0) gunShakeFrames--;
        if (damageTextFrames > 0) damageTextFrames--;

        boolean damagedThisTick = false;

        // 좀비 이동 (더 느리고 부드럽게)
        double speedPerTick = roundManager.getZombieSpeed() * 0.08; // 0.12 → 0.08 로 완화

        dangerNear = false;

        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            z.distance -= speedPerTick;

            if (z.distance <= DANGER_DISTANCE) {
                dangerNear = true;
            }

            if (z.distance <= DAMAGE_DISTANCE_THRESHOLD) {
                it.remove();
                if (!damagedThisTick && damageCooldownTicks == 0) {
                    hp--;
                    damagedThisTick = true;
                }
            }
        }

        // 보스 이동
        if (bossZombie != null) {
            bossZombie.distance -= speedPerTick;

            if (bossZombie.distance <= DANGER_DISTANCE) {
                dangerNear = true;
            }

            if (bossZombie.distance <= DAMAGE_DISTANCE_THRESHOLD) {
                if (damageCooldownTicks == 0) {
                    hp -= 2;              // 보스는 2칸 데미지
                    damagedThisTick = true;
                }
                bossZombie = null;
            }
        }

        if (dangerNear) {
            dangerPulseTick++;
        } else {
            dangerPulseTick = 0;
        }

        if (damagedThisTick) {
            damageCooldownTicks = DAMAGE_COOLDOWN_MAX;
            damageEffectFrames = 12;
            gunShakeFrames = 12;
            damageText = (Math.random() < 0.5) ? "물림!" : "윽!";
            damageTextFrames = 20;

            updateHearts();
            if (hp <= 0) {
                gameOver();
                return;
            }
        }

        viewPanel.repaint();
    }

    /** 일반 좀비 하나 스폰 */
    private void spawnZombie() {
        String w = WordManager.getInstance().getRandomWord();
        int xPos = (int) (viewPanel.getWidth() * (0.15 + Math.random() * 0.7));

        int spriteIndex = (int) (Math.random() * zombieImages.length);
        if (spriteIndex < 0) spriteIndex = 0;
        if (spriteIndex >= zombieImages.length) spriteIndex = zombieImages.length - 1;

        zombies.add(new Zombie(zombieIdSeq++, w, 100.0, xPos, spriteIndex));
    }

    /** 현재 라운드, 스폰 횟수, 틱에 따라 보스를 스폰 */
    private void trySpawnBoss() {
        int round = roundManager.getRound();

        int limit;
        if (round >= 0 && round < BOSS_SPAWN_LIMIT.length) {
            limit = BOSS_SPAWN_LIMIT[round];
        } else {
            limit = BOSS_SPAWN_LIMIT[BOSS_SPAWN_LIMIT.length - 1];
        }

        if (limit == 0) return;
        if (bossZombie != null) return;
        if (bossSpawnCountThisRound >= limit) return;

        int interval;
        if (round <= 1) interval = 220;       // 1R: 최대 2번 정도
        else if (round == 2) interval = 170;  // 2R: 더 자주
        else interval = 130;                  // 3R+: 더 자주

        if (tickCount % interval == 0) {
            spawnBoss(round);
        }
    }

    private void spawnBoss(int round) {
        String[] bossWords = {"거대좀비", "악취공격", "최후심판"};
        int xPos = getWidth() / 2;
        bossZombie = new BossZombie(zombieIdSeq++, bossWords, 100.0, xPos);
        bossSpawnCountThisRound++;
    }

    // ---------------- 그리기 ----------------

    private void drawGameScreen(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 배경
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        // 좀비 & 보스 & 총알 & 총
        drawZombies(g2d);
        drawBoss(g2d);
        drawBullets(g2d);
        drawGun(g2d);

        // 조준선
        drawCrosshair(g2d);

        // 근접 경고(닿기 직전): 화면 테두리 붉게 + "위험!"
        if (dangerNear && damageEffectFrames <= 0) {
            double pulse = 0.5 + 0.5 * Math.sin(dangerPulseTick * 0.25);
            int alpha = (int) (40 + 50 * pulse);
            g2d.setColor(new Color(255, 0, 0, alpha));

            int t = 25; // 테두리 두께
            g2d.fillRect(0, 0, getWidth(), t);                     // 위
            g2d.fillRect(0, getHeight() - t, getWidth(), t);       // 아래
            g2d.fillRect(0, 0, t, getHeight());                    // 왼쪽
            g2d.fillRect(getWidth() - t, 0, t, getHeight());       // 오른쪽

            String warn = "위험!";
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 26));
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(warn);
            int x = (getWidth() - w) / 2;
            int y = getHeight() - 40;
            g2d.setColor(Color.BLACK);
            g2d.drawString(warn, x + 2, y + 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString(warn, x, y);
        }

        // 피격 오버레이 + 텍스트
        if (damageEffectFrames > 0) {
            g2d.setColor(new Color(255, 0, 0, 70));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        if (damageTextFrames > 0 && damageText != null) {
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 40));
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(damageText);
            int x = (getWidth() - w) / 2;
            int y = getHeight() / 2;
            g2d.setColor(Color.BLACK);
            g2d.drawString(damageText, x + 3, y + 3);
            g2d.setColor(Color.WHITE);
            g2d.drawString(damageText, x, y);
        }

        // 라운드 시작 연출
        if (isRoundAnimating) {
            drawRoundEffect(g2d);
        }

        // 일시정지 / 카운트다운 오버레이
        if (isPaused || isCountingDown) {
            g2d.setColor(new Color(50, 50, 50, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        if (isPaused && !isCountingDown) {
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 32));
            String msg = "일시 정지";
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(msg);
            int x = (getWidth() - w) / 2;
            int y = getHeight() / 2;
            g2d.setColor(Color.BLACK);
            g2d.drawString(msg, x + 2, y + 2);
            g2d.setColor(Color.WHITE);
            g2d.drawString(msg, x, y);
        }

        if (isCountingDown && countdownValue > 0) {
            String msg = String.valueOf(countdownValue);
            g2d.setFont(new Font("Verdana", Font.BOLD, 80));
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(msg);
            int x = (getWidth() - w) / 2;
            int y = getHeight() / 2;
            g2d.setColor(Color.BLACK);
            g2d.drawString(msg, x + 3, y + 3);
            g2d.setColor(Color.YELLOW);
            g2d.drawString(msg, x, y);
        }
    }

    private void drawRoundEffect(Graphics2D g2d) {
        String msg = "ROUND " + roundManager.getRound();
        g2d.setFont(new Font("Verdana", Font.BOLD, 80));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(msg);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.BLACK);
        g2d.drawString(msg, cx - textW / 2 + 4, cy + 4);

        g2d.setColor(Color.YELLOW);
        g2d.drawString(msg, cx - textW / 2, cy);
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

                float depthScale = (float) (1.0 - (z.distance / 120.0));
                if (depthScale < 0.3f) depthScale = 0.3f;
                if (depthScale > 1.0f) depthScale = 1.0f;

                float spriteScale = 0.25f + 0.55f * depthScale;

                int drawW = (int) (iw * spriteScale);
                int drawH = (int) (ih * spriteScale);

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
                // 백업: 원형 좀비
                float scale = (float) (1.0 - (z.distance / 120.0));
                if (scale < 0.25f) scale = 0.25f;
                if (scale > 1.0f) scale = 1.0f;

                int zombieSize = (int) (100 * scale);
                int yPos = groundY - zombieSize;
                int xPos = z.xPos - zombieSize / 2;

                g2d.setColor(new Color(0, 120, 0, 180));
                g2d.fillOval(xPos, yPos, zombieSize, zombieSize);

                g2d.setColor(Color.WHITE);
                g2d.drawString(z.word, xPos, yPos - 5);
            }
        }
    }

    private void drawBoss(Graphics2D g2d) {
        if (bossZombie == null) return;

        int groundY = getHeight() * 2 / 3;

        if (bossImage == null) {
            int size = 140;
            int yPos = groundY - size;
            int xPos = bossZombie.xPos - size / 2;

            g2d.setColor(new Color(180, 0, 0, 180));
            g2d.fillOval(xPos, yPos, size, size);
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(xPos, yPos, size, size);

            g2d.setFont(new Font("맑은 고딕", Font.BOLD, 22));
            g2d.setColor(Color.WHITE);
            g2d.drawString(bossZombie.word, xPos, yPos - 10);
            return;
        }

        int iw = bossImage.getWidth(this);
        int ih = bossImage.getHeight(this);

        float depthScale = (float) (1.0 - (bossZombie.distance / 120.0));
        if (depthScale < 0.4f) depthScale = 0.4f;
        if (depthScale > 1.1f) depthScale = 1.1f;

        int drawW = (int) (iw * depthScale);
        int drawH = (int) (ih * depthScale);

        double t = (tickCount + bossZombie.id * 5) * 0.1;
        int sway = (int) (Math.sin(t) * 4);
        int bob = (int) (Math.cos(t * 0.7) * 4);

        int xPos = bossZombie.xPos - drawW / 2 + sway;
        int yPos = groundY - drawH + bob;

        int shadowW = (int) (drawW * 0.8);
        int shadowH = (int) (drawH * 0.18);
        int shadowX = bossZombie.xPos - shadowW / 2 + sway;
        int shadowY = groundY - shadowH / 2;
        g2d.setColor(new Color(0, 0, 0, 90));
        g2d.fillOval(shadowX, shadowY, shadowW, shadowH);

        g2d.drawImage(bossImage, xPos, yPos, drawW, drawH, this);

        g2d.setFont(new Font("맑은 고딕", Font.BOLD, 28));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(bossZombie.word);

        int textX = xPos + drawW / 2 - textW / 2;
        int textY = yPos - 15;

        g2d.setColor(Color.BLACK);
        g2d.drawString(bossZombie.word, textX + 2, textY + 2);

        g2d.setColor(Color.YELLOW);
        g2d.drawString(bossZombie.word, textX, textY);
    }

    // ---------------- 입력/사격 처리 ----------------

    private void handleShot(String text) {
        String typed = text.trim();
        if (typed.isEmpty()) return;

        // 1) 보스가 있으면 보스 우선
        if (bossZombie != null) {
            boolean bossDead = bossZombie.hit(typed);
            score += 1;

            if (bossDead) {
                score += 2;   // 마지막 타 보너스
                bossZombie = null;
            }

            if (roundManager.checkLevelUp(score)) {
                if (roundManager.getRound() > 3) {
                    gameClear();
                    return;
                } else {
                    zombies.clear();
                    bullets.clear();
                    tickCount = 0;
                    damageCooldownTicks = 0;
                    resetBossForNewRound();
                    startRoundEffect();
                }
            }

            updateHud();
            viewPanel.repaint();
            return;
        }

        // 2) 일반 좀비 중 같은 단어인 것들 중 "가장 가까운" 한 마리
        Zombie target = null;
        for (Zombie z : zombies) {
            if (z.word.equalsIgnoreCase(typed)) {
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

        float depthScale = (float) (1.0 - (z.distance / 120.0));
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
                zombies.clear();
                bullets.clear();
                tickCount = 0;
                damageCooldownTicks = 0;
                resetBossForNewRound();
                startRoundEffect();
            }
        }

        updateHud();
        viewPanel.repaint();
    }

    // ---------------- 일시정지 / 게임오버 ----------------

    private void togglePause() {
        if (hp <= 0 || isRoundAnimating) return;
        if (isCountingDown) return;

        if (!isPaused) {
            // 일시정지 진입
            isPaused = true;
            stopGameThread();
            viewPanel.repaint();

            String[] options = {"계속하기", "메인으로"};
            int choice = JOptionPane.showOptionDialog(
                    frame,
                    "게임이 일시정지되었습니다.",
                    "일시정지",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 1) {
                // 메인으로
                isPaused = false;
                frame.showStartPanel();
            } else {
                // 3-2-1 카운트다운 후 재개
                isCountingDown = true;
                new ResumeCountdownThread().start();
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
                "생존!\n\n당신은 끝까지 살아남았습니다.\n최종 점수: " + score + "\n도달 라운드: " + roundManager.getRound(),
                "생존!",
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

        // 일반 좀비 4종
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

        // 보스 이미지
        try {
            URL bossUrl = getClass().getResource("images/zombie_boss.png");
            if (bossUrl != null) {
                bossImage = new ImageIcon(bossUrl).getImage();
            } else {
                System.err.println("zombie_boss.png 로드 실패");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 배경
        try {
            URL bgUrl = getClass().getResource("images/ZombieBackground.jpg");
            if (bgUrl != null) {
                backgroundImage = new ImageIcon(bgUrl).getImage();
            } else {
                System.err.println("ZombieBackground.jpg 로드 실패");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
