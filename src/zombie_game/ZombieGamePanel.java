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

    // BGM 재생용
    private AudioPlayer bgmPlayer;
    private boolean bgmMuted = false;   // true면 음소거 상태

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

    // 라운드별 배경
    private Image backgroundImage1;   // round 1 기본 배경
    private Image backgroundImage2;   // round 2 배경
    private Image backgroundImage3;   // round 3 배경
    private Image backgroundImage;    // 현재 실제로 그릴 배경

    private final Image[] zombieImages = new Image[4];
    private Image bossImage;

    // 총 위치 (총알 출발점 계산용)
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
    private int screenShakeFrames = 0;   // 피격 시 화면 흔들림 프레임 수

    // 보스
    private BossZombie bossZombie = null;
    private int bossSpawnCountThisRound = 0;
    private static final int[] BOSS_SPAWN_LIMIT = {0, 2, 3, 5};


    // ---------------- 내부 클래스 ----------------

    private static class Zombie {
        String word;
        double distance;
        int id;
        int xPos;
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

        /** 현재 단어를 맞췄다면 true/false, 마지막 단어까지 다 맞추면 최종 true */
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
        final Zombie target;    // 현재는 판정용으로 사용하지 않음(즉시 판정)

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
        public void requestStop() { running = false; interrupt(); }
        @Override public void run() {
            while (running) {
                try { Thread.sleep(LOOP_DELAY_MS); }
                catch (InterruptedException e) { break; }
                if (!running) break;
                SwingUtilities.invokeLater(() -> gameTick());
            }
        }
    }

    /** 총알 애니메이션 스레드 (빠르게) */
    private class BulletThread extends Thread {
        private final Bullet bullet;
        BulletThread(Bullet bullet) { this.bullet = bullet; }

        @Override
        public void run() {
            int steps = 18;
            int sleepMs = 8;

            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                final double nx = bullet.startX + (bullet.targetX - bullet.startX) * t;
                final double ny = bullet.startY + (bullet.targetY - bullet.startY) * t;

                SwingUtilities.invokeLater(() -> {
                    bullet.x = nx;
                    bullet.y = ny;
                    viewPanel.repaint();
                });

                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) { return; }
            }

            // ★ 총알 이동 애니메이션 끝 → bullets 리스트에서 제거
            SwingUtilities.invokeLater(() -> bullets.remove(bullet));
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
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(() -> {
                countdownValue = 0;
                isPaused = false;
                isCountingDown = false;

                // 일시정지 해제 시, 음소거가 아니라면 BGM 재개
                if (bgmPlayer != null && !bgmMuted) {
                    bgmPlayer.resume();
                }

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
            @Override public void actionPerformed(ActionEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        // ESC: 일시정지
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "togglePause");
        getActionMap().put("togglePause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { togglePause(); }
        });

        // BGM 로딩 및 반복 재생 시작
        bgmPlayer = new AudioPlayer("bgm.wav"); // 프로젝트 루트에 bgm.wav
        if (bgmPlayer != null && !bgmMuted) {
            bgmPlayer.playLoop();
        }

        updateHearts();
        updateHud();
    }

    // 🔥 라운드 번호에 따라 배경 이미지 바꾸는 함수
    private void setupRound(int round) {
        if (round == 1) {
            backgroundImage = backgroundImage1;
        } else if (round == 2) {
            backgroundImage = (backgroundImage2 != null) ? backgroundImage2 : backgroundImage1;
        } else if (round == 3) {
            backgroundImage = (backgroundImage3 != null) ? backgroundImage3 : backgroundImage1;
        } else {
            backgroundImage = backgroundImage1;
        }
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
        this.screenShakeFrames = 0;

        roundManager.reset();

        // 🔥 1라운드로 초기화된 상태에서 배경 세팅
        setupRound(roundManager.getRound());

        updateHud();
        updateHearts();

        inputField.setText("");
        inputField.requestFocus();

        // 새 게임 시작 시 BGM도 다시 루프
        if (bgmPlayer != null && !bgmMuted) {
            bgmPlayer.playLoop();
        }

        startRoundEffect();
    }

    private void startRoundEffect() {
        isRoundAnimating = true;
        stopGameThread();
        viewPanel.repaint();

        Thread effectThread = new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
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

        int round = roundManager.getRound();
        int spawnInterval;
        if (round <= 1) spawnInterval = 30;
        else if (round == 2) spawnInterval = 22;
        else spawnInterval = 16;

        if (tickCount % spawnInterval == 0) {
            spawnZombie();
        }

        trySpawnBoss();

        if (damageCooldownTicks > 0) damageCooldownTicks--;
        if (damageEffectFrames > 0) damageEffectFrames--;
        if (gunShakeFrames > 0) gunShakeFrames--;
        if (damageTextFrames > 0) damageTextFrames--;

        boolean damagedThisTick = false;

        // 좀비 이동 속도 (라운드별로)
        double speedBase = roundManager.getZombieSpeed(); // 1,2,3...
        double speedPerTick;
        if (round == 1)      speedPerTick = speedBase * 0.15;
        else if (round == 2) speedPerTick = speedBase * 0.18;
        else                 speedPerTick = speedBase * 0.21;

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

        if (bossZombie != null) {
            bossZombie.distance -= speedPerTick;

            if (bossZombie.distance <= DANGER_DISTANCE) {
                dangerNear = true;
            }

            if (bossZombie.distance <= DAMAGE_DISTANCE_THRESHOLD) {
                if (damageCooldownTicks == 0) {
                    hp -= 2;
                    damagedThisTick = true;
                }
                bossZombie = null;
            }
        }

        if (dangerNear) dangerPulseTick++; else dangerPulseTick = 0;

        if (damagedThisTick) {
            damageCooldownTicks = DAMAGE_COOLDOWN_MAX;
            damageEffectFrames = 12;
            gunShakeFrames = 12;
            screenShakeFrames = 12;  // ★ 화면 흔들림 추가
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

    // 다른 좀비와 너무 겹쳐서 스폰되지 않도록 체크
    private boolean isSpawnTooClose(int xPos) {
        for (Zombie z : zombies) {
            if (Math.abs(z.xPos - xPos) < 90 && z.distance > 30) {
                return true;
            }
        }
        if (bossZombie != null && Math.abs(bossZombie.xPos - xPos) < 120) {
            return true;
        }
        return false;
    }

    private void spawnZombie() {
        String w = WordManager.getInstance().getRandomWord();

        int xPos;
        int attempts = 0;
        do {
            xPos = (int) (viewPanel.getWidth() * (0.15 + Math.random() * 0.7));
            attempts++;
        } while (attempts < 10 && isSpawnTooClose(xPos));

        int spriteIndex = (int) (Math.random() * zombieImages.length);
        if (spriteIndex < 0) spriteIndex = 0;
        if (spriteIndex >= zombieImages.length) spriteIndex = zombieImages.length - 1;

        zombies.add(new Zombie(zombieIdSeq++, w, 100.0, xPos, spriteIndex));
    }

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
        if (round <= 1) interval = 220;
        else if (round == 2) interval = 170;
        else interval = 130;

        if (tickCount % interval == 0) {
            spawnBoss(round);
        }
    }

    private void spawnBoss(int round) {
        // 🔥 항상 3개의 단어를 사용하는 보스
        String[] bossWords = BossWordManager.getInstance().getRandomBossWords(3);

        // 파일이 없거나 로딩 실패했을 때 대비 기본값
        if (bossWords == null || bossWords.length == 0) {
            bossWords = new String[]{"핵펀치", "좀비대군", "도시붕괴"};
        }

        int xPos = getWidth() / 2;
        bossZombie = new BossZombie(zombieIdSeq++, bossWords, 100.0, xPos);
        bossSpawnCountThisRound++;
    }

    // ---------------- 그리기 ----------------

    private void drawGameScreen(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ★ 화면 흔들림 계산
        int shakeX = 0;
        int shakeY = 0;
        if (screenShakeFrames > 0) {
            double power = 6.0; // 흔들림 강도
            shakeX = (int) ((Math.random() - 0.5) * 2 * power);
            shakeY = (int) ((Math.random() - 0.5) * 2 * power);
            screenShakeFrames--;
        }

        // ★ 전체 화면을 살짝 이동
        g2d.translate(shakeX, shakeY);

        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        drawZombies(g2d);
        drawBoss(g2d);
        drawBullets(g2d);
        drawGun(g2d);
        drawCrosshair(g2d);

        // 근접 경고
        if (dangerNear && damageEffectFrames <= 0) {
            double pulse = 0.5 + 0.5 * Math.sin(dangerPulseTick * 0.25);
            int alpha = (int) (40 + 50 * pulse);
            g2d.setColor(new Color(255, 0, 0, alpha));

            int t = 25;
            g2d.fillRect(0, 0, getWidth(), t);
            g2d.fillRect(0, getHeight() - t, getWidth(), t);
            g2d.fillRect(0, 0, t, getHeight());
            g2d.fillRect(getWidth() - t, 0, t, getHeight());

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

        if (isRoundAnimating) {
            drawRoundEffect(g2d);
        }

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

        // ★ 이동한 만큼 다시 원위치
        g2d.translate(-shakeX, -shakeY);
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

    // ----------- 총기 그리기 (화면 비율에 맞게, 손 보이게) -----------
    private void drawGun(Graphics2D g2d) {
        if (gunImage == null) return;

        int iw = gunImage.getWidth(this);
        int ih = gunImage.getHeight(this);

        double baseScale = 0.8;

        double maxByWidth  = (getWidth()  * 0.50) / iw;
        double maxByHeight = (getHeight() * 0.60) / ih;

        double scale = Math.min(baseScale, Math.min(maxByWidth, maxByHeight));

        int drawW = (int) (iw * scale);
        int drawH = (int) (ih * scale);

        int marginRight  = 40;
        int marginBottom = 35;
        int x = getWidth()  - drawW - marginRight;
        int y = getHeight() - drawH - marginBottom;

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
            if (img != null) { anySprite = true; break; }
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

        // 총알 출발 위치 (총 이미지 기준)
        int startX, startY;
        if (gunDrawW > 0 && gunDrawH > 0) {
            startX = gunDrawX + (int) (gunDrawW * 0.75);
            startY = gunDrawY + (int) (gunDrawH * 0.35);
        } else {
            startX = getWidth() - 100;
            startY = getHeight() - 100;
        }

        // 1) 먼저 일반 좀비부터 판정 (보스가 있어도 항상 가능해야 함)
        Zombie normalTarget = null;
        for (Zombie z : zombies) {
            if (z.word.equalsIgnoreCase(typed)) {
                if (normalTarget == null || z.distance < normalTarget.distance) {
                    normalTarget = z;
                }
            }
        }

        if (normalTarget != null) {
            Point p = computeZombieCenter(normalTarget);
            applyBulletHit(normalTarget); // 점수 + 레벨업 처리

            Bullet bullet = new Bullet(startX, startY, p.x, p.y, null);
            bullets.add(bullet);
            new BulletThread(bullet).start();
            return; // 이번 입력은 일반 좀비 명중으로 끝
        }

        // 2) 일반 좀비가 아니면 보스 판정
        if (bossZombie != null) {
            handleBossShot(typed, startX, startY);
        }
    }

    private void handleBossShot(String typed, int startX, int startY) {
        if (bossZombie == null) return;

        boolean bossDead = bossZombie.hit(typed);

        // 총알 애니메이션
        Point p = computeBossCenter();
        Bullet bullet = new Bullet(startX, startY, p.x, p.y, null);
        bullets.add(bullet);
        new BulletThread(bullet).start();

        if (bossDead) {
            // ★ 세 단어를 모두 맞춰서 최종적으로 쓰러뜨렸을 때만 +1점
            score += 1;

            // ★ 보스 사망 시 총알들도 바로 지워주기
            bullets.clear();

            if (roundManager.checkLevelUp(score)) {
                if (roundManager.getRound() > 3) {
                    gameClear();
                    return;
                } else {
                    zombies.clear();
                    bullets.clear();   // 라운드 넘어갈 때 한 번 더 초기화
                    tickCount = 0;
                    damageCooldownTicks = 0;

                    // 🔥 새 라운드 번호에 맞는 배경으로 교체
                    setupRound(roundManager.getRound());

                    resetBossForNewRound();
                    startRoundEffect();
                }
            }
            bossZombie = null;
        }

        updateHud();
        viewPanel.repaint();
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

    private Point computeBossCenter() {
        int groundY = getHeight() * 2 / 3;

        if (bossZombie == null) {
            return new Point(getWidth() / 2, groundY - 80);
        }

        if (bossImage == null) {
            int y = groundY - 80;
            return new Point(bossZombie.xPos, y);
        }

        int iw = bossImage.getWidth(this);
        int ih = bossImage.getHeight(this);

        float depthScale = (float) (1.0 - (bossZombie.distance / 120.0));
        if (depthScale < 0.4f) depthScale = 0.4f;
        if (depthScale > 1.1f) depthScale = 1.1f;

        int drawW = (int) (iw * depthScale);
        int drawH = (int) (ih * depthScale);

        int xPos = bossZombie.xPos - drawW / 2;
        int yPos = groundY - drawH;

        return new Point(xPos + drawW / 2, yPos + drawH / 3);
    }

    private void applyBulletHit(Zombie target) {
        // 이미 제거된 좀비면 무시
        if (!zombies.contains(target)) return;

        // 1) 좀비 제거 & 점수 증가
        zombies.remove(target);
        score++;

        // 2) ★ 화면에 남아 있을 수 있는 총알들을 싹 지워준다
        bullets.clear();

        // 3) 라운드 클리어 / 게임 클리어 체크
        if (roundManager.checkLevelUp(score)) {
            if (roundManager.getRound() > 3) {
                gameClear();
                return;
            } else {
                zombies.clear();
                bullets.clear();       // 다음 라운드 시작 전에 한 번 더 초기화
                tickCount = 0;
                damageCooldownTicks = 0;

                // 🔥 새 라운드 번호에 맞는 배경으로 교체
                setupRound(roundManager.getRound());

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
        if (isPaused) return;

        isPaused = true;
        stopGameThread();
        viewPanel.repaint();

        JButton muteBtn = new JButton(bgmMuted ? "🔇 음악 켜기" : "🔊 음악 끄기");

        muteBtn.addActionListener(e -> {
            bgmMuted = !bgmMuted;
            if (bgmPlayer != null) {
                if (bgmMuted) bgmPlayer.pause();
                else bgmPlayer.resume();
            }
            muteBtn.setText(bgmMuted ? "🔇 음악 켜기" : "🔊 음악 끄기");
        });

        Object[] message = {
                "게임이 일시정지되었습니다.",
                muteBtn
        };
        String[] options = {"계속하기", "메인으로"};

        int choice = JOptionPane.showOptionDialog(
                frame,
                message,
                "일시정지",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 1) {
            stopGameThread();
            if (bgmPlayer != null) bgmPlayer.stop();
            frame.showStartPanel();
            return;
        }

        isCountingDown = true;
        new ResumeCountdownThread().start();
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
            if (bgmPlayer != null) bgmPlayer.stop();
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
            if (bgmPlayer != null) bgmPlayer.stop();
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
            heartPanel.add(heart);
        }
        heartPanel.revalidate();
        heartPanel.repaint();
    }

    // ---------------- 이미지 로딩 ----------------

    private void loadImages() {
        try {
            URL gunUrl = getClass().getResource("images/gun.png");
            if (gunUrl != null) gunImage = new ImageIcon(gunUrl).getImage();
            else System.err.println("gun.png 로드 실패");
        } catch (Exception ex) { ex.printStackTrace(); }

        try { zombieImages[0] = new ImageIcon(getClass().getResource("images/zombie_1.png")).getImage(); }
        catch (Exception ex) { System.err.println("zombie_1.png 로드 실패"); }
        try { zombieImages[1] = new ImageIcon(getClass().getResource("images/zombie_2.png")).getImage(); }
        catch (Exception ex) { System.err.println("zombie_2.png 로드 실패"); }
        try { zombieImages[2] = new ImageIcon(getClass().getResource("images/zombie_3.png")).getImage(); }
        catch (Exception ex) { System.err.println("zombie_3.png 로드 실패"); }
        try { zombieImages[3] = new ImageIcon(getClass().getResource("images/zombie_4.png")).getImage(); }
        catch (Exception ex) { System.err.println("zombie_4.png 로드 실패"); }

        try {
            URL bossUrl = getClass().getResource("images/zombie_boss.png");
            if (bossUrl != null) bossImage = new ImageIcon(bossUrl).getImage();
            else System.err.println("zombie_boss.png 로드 실패");
        } catch (Exception ex) { ex.printStackTrace(); }

        try {
            URL bg1 = getClass().getResource("images/ZombieBackground.jpg");      // Round1
            if (bg1 != null) {
                backgroundImage1 = new ImageIcon(bg1).getImage();
            }

            URL bg2 = getClass().getResource("images/zombieBackground_2.jpg");    // Round2
            if (bg2 != null) {
                backgroundImage2 = new ImageIcon(bg2).getImage();
            }

            URL bg3 = getClass().getResource("images/zombieBackground_3.jpg");    // Round3
            if (bg3 != null) {
                backgroundImage3 = new ImageIcon(bg3).getImage();
            }

            // 게임 시작 시 Round1 배경을 기본값으로
            backgroundImage = backgroundImage1;

        } catch (Exception ignored) {}
    }
}
