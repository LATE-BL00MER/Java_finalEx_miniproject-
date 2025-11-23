package Sample_Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TypingZombieFPS extends JFrame {

    public TypingZombieFPS() {
        setTitle("🧟 Typing Zombie Defense - FPS View");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TypingZombieFPS::new);
    }

    // ======================= 게임 패널 =======================
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        private final Timer timer = new Timer(16, this);
        private final Random random = new Random();
        private final Rectangle muteButtonBounds = new Rectangle(940, 20, 32, 32);

        // 단어 풀
        private final String[] wordPool = {
                "INFECTED", "VIRUS", "OUTBREAK", "QUARANTINE", "SURVIVOR",
                "ZOMBIE", "BITE", "PANIC", "APOCALYPSE", "PLAGUE",
                "ANTIDOTE", "BLOOD", "FEVER", "NIGHTMARE", "RIOT",
                "RADIO", "SHELTER", "DANGER", "HORDE", "ALERT"
        };

        // 좀비, 총알 리스트
        private final List<Zombie> zombies = new ArrayList<>();
        private final List<Bullet> bullets = new ArrayList<>();

        // 입력 중인 단어
        private String typed = "";

        // 게임 상태
        private int score = 0;
        private int livesUsed = 0;      // 3이 되면 Game Over
        private int round = 1;          // 라운드
        private int scoreForNextRound = 100; // 다음 라운드까지 필요한 점수
        private boolean gameOver = false;
        private boolean paused = false;
        private boolean musicMuted = false;

        // 좀비 생성 관련
        private int spawnCounter = 0;
        private int spawnDelay = 110; // 프레임 단위 (대략 1.8초 정도 간격에서 시작)
        private final double maxDist = 4.0;
        private final double minDist = 0.8;  // 여기에 도달하면 플레이어 바로 앞

        // 화면 정보
        private int groundY;         // 바닥 y
        private int centerX;         // 화면 중앙 x

        // 총구 이펙트
        private boolean muzzleFlash = false;
        private int muzzleTimer = 0;

        public GamePanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            // 마우스로 음소거 / 일시정지 메뉴 클릭
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseClick(e.getPoint());
                }
            });

            timer.start();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
        }

        // ======================= 내부 클래스: Zombie =======================
        static class Zombie {
            double distance;   // 플레이어와의 거리 (4.0 → 0.8)
            double laneOffset; // 왼쪽/오른쪽으로 약간 치우치게
            double speed;      // 거리 감소 속도
            String word;
            boolean alive = true;
            boolean reachedPlayer = false;
            int hitFlash = 0;  // 맞았을 때 붉게 보이는 프레임 수

            Zombie(double distance, double laneOffset, double speed, String word) {
                this.distance = distance;
                this.laneOffset = laneOffset;
                this.speed = speed;
                this.word = word;
            }
        }

        // ======================= 내부 클래스: Bullet =======================
        static class Bullet {
            double t;           // 0.0 ~ 1.0 (진행률)
            final double speed; // 진행 속도
            Zombie target;
            boolean active = true;

            Bullet(Zombie target) {
                this.target = target;
                this.t = 0.0;
                this.speed = 0.18;
            }
        }

        // ======================= 유틸: 좀비 생성 =======================
        private void spawnZombie() {
            // 멀리서 출발 (distance = maxDist ~ maxDist+랜덤)
            double dist = maxDist + random.nextDouble() * 0.5;
            // 좌우 랜덤 오프셋 (FPS에서 살짝 왼/오른쪽)
            double laneOffset = (random.nextDouble() - 0.5) * 1.5; // -0.75 ~ 0.75

            // 기본 속도 설정 후, 기존 대비 1/3 수준으로 느리게 시작 + 라운드별 소폭 증가
            double baseSpeed = 0.015 + random.nextDouble() * 0.01; // 0.015 ~ 0.025
            double speed = baseSpeed / 3.0 * (1.0 + (round - 1) * 0.15);

            String word = wordPool[random.nextInt(wordPool.length)];
            zombies.add(new Zombie(dist, laneOffset, speed, word));
        }

        // 가장 "가까운" 좀비 찾기
        private Zombie getFrontZombie() {
            Zombie front = null;
            for (Zombie z : zombies) {
                if (!z.alive || z.reachedPlayer) continue;
                if (front == null || z.distance < front.distance) {
                    front = z;
                }
            }
            return front;
        }

        // ======================= 총알 발사 =======================
        private void shootAtZombie(Zombie target) {
            if (target == null) return;
            bullets.add(new Bullet(target));
            muzzleFlash = true;
            muzzleTimer = 0;
            // TODO: 총소리 넣고 싶으면 여기서 Clip 재생
        }

        // ======================= 렌더링 =======================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            groundY = h - 90;
            centerX = w / 2;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // ---------- 배경: 밤 하늘 + 도시 ----------
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(10, 10, 25),
                    0, h, new Color(30, 10, 5)
            );
            g2.setPaint(sky);
            g2.fillRect(0, 0, w, h);

            // 달
            g2.setColor(new Color(240, 240, 220, 230));
            g2.fillOval(w - 170, 40, 80, 80);

            // 건물 실루엣
            g2.setColor(new Color(20, 20, 40));
            for (int i = 0; i < w; i += 90) {
                int bh = 120 + (i * 37 % 80);
                g2.fillRect(i, h - 220 - bh, 60, bh);
            }

            // 안개
            g2.setColor(new Color(210, 210, 255, 20));
            for (int i = 0; i < 5; i++) {
                int fogY = 120 + i * 60;
                g2.fillOval(-150, fogY, w + 300, 90);
            }

            // 도로 / 땅
            g2.setColor(new Color(15, 15, 18));
            g2.fillRect(0, groundY, w, h - groundY);

            // 도로 중앙선
            g2.setColor(new Color(140, 140, 160, 130));
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(centerX, groundY, centerX, h);

            // ---------- 좀비들 (먼 것부터 그림) ----------
            zombies.sort((a, b) -> Double.compare(b.distance, a.distance));
            for (Zombie z : zombies) {
                drawZombieFPS(g2, z);
            }

            // ---------- 총알 (레이저/탄환 느낌) ----------
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(255, 240, 180));
            for (Bullet b : bullets) {
                if (!b.active || b.target == null) continue;

                int gunX = centerX;
                int gunY = groundY - 40;

                Point tp = getZombieScreenCenter(b.target);
                double bx = gunX + (tp.x - gunX) * b.t;
                double by = gunY + (tp.y - gunY) * b.t;

                g2.drawLine(gunX, gunY, (int) bx, (int) by);
            }

            // ---------- HUD (점수/라운드/라이프/타겟 안내) ----------
            g2.setFont(new Font("Consolas", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            g2.drawString("SCORE : " + score, 20, 40);
            g2.drawString("ROUND : " + round, 20, 68);

            int livesLeft = 3 - livesUsed;
            g2.setColor(livesLeft <= 1 ? new Color(255, 80, 80) : new Color(200, 240, 200));
            g2.drawString("LIVES : " + livesLeft, 20, 96);

            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            g2.setColor(new Color(220, 220, 230));
            g2.drawString("NEXT ROUND AT " + scoreForNextRound + " PTS", 20, 124);
            g2.drawString("TYPE WORD ABOVE FRONT ZOMBIE & PRESS ENTER", 20, 152);

            // 입력 박스
            int boxY = h - 80;
            g2.setColor(new Color(5, 5, 15, 230));
            g2.fillRoundRect(20, boxY, w - 40, 50, 15, 15);
            g2.setColor(new Color(120, 200, 255));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(20, boxY, w - 40, 50, 15, 15);

            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            g2.setColor(new Color(220, 235, 255));
            g2.drawString("INPUT> " + typed, 40, boxY + 32);

            // 타겟 좀비 안내
            Zombie target = getFrontZombie();
            if (!gameOver && target != null && target.alive && !target.reachedPlayer) {
                g2.setFont(new Font("Consolas", Font.PLAIN, 16));
                g2.setColor(new Color(255, 220, 180));
                g2.drawString("TARGET : " + target.word, 20, 182);
            }

            // ---------- 총 (1인칭) ----------
            drawGun(g2);

            // 음소거 버튼
            drawMuteButton(g2);

            // 총구 번쩍
            if (muzzleFlash && !gameOver) {
                g2.setColor(new Color(255, 240, 200, 200));
                int gunX = centerX;
                int gunY = groundY - 40;
                g2.fillOval(gunX - 18, gunY - 18, 36, 36);
            }

            // PAUSE 오버레이
            if (paused && !gameOver) {
                drawPauseOverlay(g2);
            }

            // GAME OVER 표시
            if (gameOver) {
                g2.setFont(new Font("Consolas", Font.BOLD, 42));
                g2.setColor(new Color(255, 80, 80, 230));
                String msg = "GAME OVER";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(msg);
                g2.drawString(msg, (w - tw) / 2, h / 2 - 10);

                g2.setFont(new Font("Consolas", Font.PLAIN, 22));
                g2.setColor(new Color(240, 240, 240));
                String msg2 = "Press R to Restart";
                int tw2 = g2.getFontMetrics().stringWidth(msg2);
                g2.drawString(msg2, (w - tw2) / 2, h / 2 + 30);
            }
        }

        // 좀비의 화면상 중심 위치 계산 (거리/오프셋 기반)
        private Point getZombieScreenCenter(Zombie z) {
            double t = (maxDist - z.distance) / (maxDist - minDist); // 0~1
            t = Math.max(0, Math.min(1, t));

            double hFar = 60;
            double hNear = 220;
            double zombieHeight = hFar + t * (hNear - hFar);

            int yCenter = (int) (groundY - zombieHeight / 2.0);

            double maxLaneOffsetPixels = 200;
            int xCenter = (int) (centerX + z.laneOffset * maxLaneOffsetPixels);

            return new Point(xCenter, yCenter);
        }

        // FPS 시점 좀비 그리기
        private void drawZombieFPS(Graphics2D g2, Zombie z) {
            if (z == null || z.distance <= 0) return;
            if (!z.alive && z.hitFlash <= 0) return;

            double t = (maxDist - z.distance) / (maxDist - minDist);
            t = Math.max(0, Math.min(1, t));

            double hFar = 60;
            double hNear = 220;
            double zombieHeight = hFar + t * (hNear - hFar);
            double zombieWidth = zombieHeight * 0.45;

            Point center = getZombieScreenCenter(z);
            int x = center.x;
            int y = center.y;

            int bodyWidth = (int) zombieWidth;
            int bodyHeight = (int) (zombieHeight * 0.65);
            int headSize = (int) (zombieHeight * 0.30);

            // 몸체 색
            Color bodyColor = new Color(60, 90, 70);
            if (z.hitFlash > 0) {
                bodyColor = new Color(200, 80, 80);
            }

            // 몸체
            g2.setColor(bodyColor);
            g2.fillRoundRect(x - bodyWidth / 2, y - bodyHeight, bodyWidth, bodyHeight, 12, 12);

            // 머리
            g2.setColor(new Color(95, 145, 95));
            g2.fillOval(x - headSize / 2, y - bodyHeight - headSize + 8, headSize, headSize);

            // 눈
            int eyeY = y - bodyHeight - headSize / 2;
            g2.setColor(new Color(250, 250, 200));
            int eyeSize = Math.max(3, headSize / 6);
            g2.fillOval(x - eyeSize - 3, eyeY, eyeSize, eyeSize);
            g2.fillOval(x + 3, eyeY, eyeSize, eyeSize);

            // 입
            g2.setColor(new Color(150, 40, 40));
            g2.drawLine(x - eyeSize, eyeY + eyeSize + 3, x + eyeSize, eyeY + eyeSize + 4);

            // 팔
            g2.setStroke(new BasicStroke(3));
            g2.setColor(bodyColor.darker());
            g2.drawLine(x - bodyWidth / 2, y - bodyHeight + 15,
                    x - bodyWidth, y - bodyHeight + 25);
            g2.drawLine(x + bodyWidth / 2, y - bodyHeight + 15,
                    x + bodyWidth, y - bodyHeight + 25);

            // 플레이어 바로 앞까지 온 경우 붉은 오버레이
            if (z.reachedPlayer) {
                g2.setColor(new Color(180, 0, 0, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            // 단어 (머리 위)
            g2.setFont(new Font("Consolas", Font.BOLD, 18 + (int) (t * 6)));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(z.word);

            int labelY = y - bodyHeight - headSize - 25;
            g2.setColor(new Color(10, 10, 10, 180));
            g2.fillRoundRect(x - tw / 2 - 6, labelY - 18, tw + 12, 22, 8, 8);
            g2.setColor(new Color(255, 240, 180));
            g2.drawString(z.word, x - tw / 2, labelY);
        }

        // FPS에서 총(손에 들고 있는 총) 그리기
        private void drawGun(Graphics2D g2) {
            int gunW = 120;
            int gunH = 80;
            int gunX = centerX - gunW / 2;
            int gunY = groundY - gunH + 10;

            g2.setColor(new Color(50, 50, 60));
            g2.fillRoundRect(gunX, gunY, gunW, gunH, 12, 12);

            g2.setColor(new Color(80, 80, 90));
            g2.fillRect(gunX + gunW / 2 - 10, gunY - 25, 20, 30);

            g2.setColor(new Color(40, 40, 50));
            g2.fillRoundRect(gunX + gunW - 35, gunY + 25, 26, 40, 8, 8);

            g2.setColor(new Color(130, 130, 140));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(gunX, gunY, gunW, gunH, 12, 12);
        }

        // 음소거 버튼 UI
        private void drawMuteButton(Graphics2D g2) {
            g2.setColor(new Color(30, 30, 35, 180));
            g2.fillRoundRect(muteButtonBounds.x - 6, muteButtonBounds.y - 6,
                    muteButtonBounds.width + 12, muteButtonBounds.height + 12, 10, 10);
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawRoundRect(muteButtonBounds.x - 6, muteButtonBounds.y - 6,
                    muteButtonBounds.width + 12, muteButtonBounds.height + 12, 10, 10);

            int x = muteButtonBounds.x;
            int y = muteButtonBounds.y;
            int w = muteButtonBounds.width;
            int h = muteButtonBounds.height;

            // 스피커 도형
            g2.setColor(Color.WHITE);
            int[] xs = {x + 6, x + 14, x + 22};
            int[] ys = {y + 10, y + 6, y + 10};
            g2.fillPolygon(xs, ys, 3);
            g2.fillRect(x + 14, y + 10, 10, 12);

            if (!musicMuted) {
                g2.drawArc(x + 20, y + 8, 12, 16, -45, 90);
                g2.drawArc(x + 24, y + 8, 14, 16, -45, 90);
            } else {
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(x + 20, y + 8, x + 30, y + 24);
                g2.drawLine(x + 30, y + 8, x + 20, y + 24);
            }
        }

        // 일시정지 화면 오버레이
        private void drawPauseOverlay(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(230, 230, 230));
            g2.setFont(new Font("Consolas", Font.BOLD, 42));
            String msg = "PAUSED";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            g2.drawString(msg, (getWidth() - tw) / 2, 180);

            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            String line1 = "ESC : Resume    |    Click speaker : Mute/Unmute";
            String line2 = "Enter : Shoot matching word    |    R : Restart";
            String line3 = "Exit Game : Click below";
            g2.drawString(line1, (getWidth() - g2.getFontMetrics().stringWidth(line1)) / 2, 230);
            g2.drawString(line2, (getWidth() - g2.getFontMetrics().stringWidth(line2)) / 2, 260);
            g2.drawString(line3, (getWidth() - g2.getFontMetrics().stringWidth(line3)) / 2, 290);

            drawPauseButton(g2, "RESUME", getWidth() / 2 - 220, 330, 150, 50);
            drawPauseButton(g2, musicMuted ? "UNMUTE" : "MUTE", getWidth() / 2 - 60, 330, 150, 50);
            drawPauseButton(g2, "EXIT", getWidth() / 2 + 100, 330, 150, 50);
        }

        private void drawPauseButton(Graphics2D g2, String label, int x, int y, int w, int h) {
            g2.setColor(new Color(30, 30, 40, 210));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(new Color(180, 180, 200));
            g2.drawRoundRect(x, y, w, h, 12, 12);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label);
            g2.drawString(label, x + (w - tw) / 2, y + h / 2 + 6);
        }

        // ======================= 게임 루프 =======================
        @Override
        public void actionPerformed(ActionEvent e) {
            // Game Over/일시정지 상태에서는 화면만 유지
            if (gameOver || paused) {
                repaint();
                return;
            }

            // 좀비 생성
            spawnCounter++;
            if (spawnCounter > spawnDelay) {
                if (zombies.size() < 7) {
                    spawnZombie();
                }
                spawnCounter = 0;
            }

            // 좀비 이동
            for (Zombie z : zombies) {
                if (!z.alive || z.reachedPlayer) {
                    if (z.hitFlash > 0) z.hitFlash--;
                    continue;
                }

                z.distance -= z.speed;

                // 플레이어 근접 → 라이프 감소
                if (z.distance <= minDist && !z.reachedPlayer) {
                    z.reachedPlayer = true;
                    livesUsed++;
                    // TODO: 좀비 공격 사운드
                    if (livesUsed >= 3) {
                        gameOver = true;
                    }
                }
            }

            // 총알 이동 및 충돌
            for (Bullet b : bullets) {
                if (!b.active || b.target == null) continue;

                b.t += b.speed;
                if (b.t >= 1.0) {
                    if (b.target.alive && !b.target.reachedPlayer) {
                        b.target.alive = false;
                        b.target.hitFlash = 12;
                        score += 10 + b.target.word.length(); // 단어 길이만큼 보너스
                        checkRoundProgression();
                        // TODO: 피격 사운드
                    }
                    b.active = false;
                }
            }

            // 죽은 좀비 / 비활성 탄 정리
            zombies.removeIf(z -> (!z.alive && z.hitFlash <= 0) || z.distance <= 0.1);
            bullets.removeIf(b -> !b.active);

            // 총구 이펙트 유지 시간
            if (muzzleFlash) {
                muzzleTimer++;
                if (muzzleTimer > 6) {
                    muzzleFlash = false;
                }
            }

            repaint();
        }

        // ======================= 키 입력 =======================
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (gameOver) {
                if (code == KeyEvent.VK_R) {
                    restartGame();
                }
                return;
            }

            // ESC : 일시정지 토글
            if (code == KeyEvent.VK_ESCAPE) {
                paused = !paused;
                repaint();
                return;
            }

            if (paused) {
                return;
            }

            if (code == KeyEvent.VK_BACK_SPACE) {
                if (!typed.isEmpty()) {
                    typed = typed.substring(0, typed.length() - 1);
                }
            } else if (code == KeyEvent.VK_ENTER) {
                Zombie target = getFrontZombie();
                if (target != null && target.alive && !target.reachedPlayer &&
                        typed.equalsIgnoreCase(target.word)) {
                    shootAtZombie(target);
                }
                typed = "";
            } else {
                char c = e.getKeyChar();
                if (Character.isLetter(c)) {
                    typed += Character.toUpperCase(c);
                }
            }

            repaint();
        }

        private void restartGame() {
            zombies.clear();
            bullets.clear();
            typed = "";
            score = 0;
            livesUsed = 0;
            round = 1;
            scoreForNextRound = 100;
            spawnDelay = 110;
            gameOver = false;
            paused = false;
        }

        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyReleased(KeyEvent e) { }

        // ======================= 마우스 처리 =======================
        private void handleMouseClick(Point p) {
            // 오른쪽 위 스피커 버튼
            if (muteButtonBounds.contains(p)) {
                toggleMusic();
                repaint();
                return;
            }

            if (paused) {
                Rectangle resume = new Rectangle(getWidth() / 2 - 220, 330, 150, 50);
                Rectangle mute = new Rectangle(getWidth() / 2 - 60, 330, 150, 50);
                Rectangle exit = new Rectangle(getWidth() / 2 + 100, 330, 150, 50);

                if (resume.contains(p)) {
                    paused = false;
                } else if (mute.contains(p)) {
                    toggleMusic();
                } else if (exit.contains(p)) {
                    System.exit(0);
                }
                repaint();
            }
        }

        private void toggleMusic() {
            musicMuted = !musicMuted;
            // 실제 배경음 추가 시 Clip 볼륨 제어를 이 부분에 연결
        }

        // 점수 기반 라운드 진행
        private void checkRoundProgression() {
            while (score >= scoreForNextRound) {
                round++;
                scoreForNextRound += 100;
                spawnDelay = Math.max(60, spawnDelay - 10); // 라운드 올라갈수록 스폰 주기 감소
            }
        }
    }
}
