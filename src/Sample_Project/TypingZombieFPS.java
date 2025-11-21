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
        private int livesUsed = 0;   // 3이 되면 Game Over
        private boolean gameOver = false;

        // 좀비 생성 관련
        private int spawnCounter = 0;
        private int spawnDelay = 90; // 프레임 단위 (대략 1.5초 정도 간격)
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
            double t;          // 0.0 ~ 1.0 (진행률)
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
            double speed = 0.015 + random.nextDouble() * 0.01;     // 프레임당 거리 감소
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

            // ---------- 배경: 황폐한 도시 / 하늘 ----------
            GradientPaint sky = new GradientPaint(
                    0, 0, new Color(10, 10, 25),
                    0, h, new Color(30, 10, 5)
            );
            g2.setPaint(sky);
            g2.fillRect(0, 0, w, h);

            // 달
            g2.setColor(new Color(240, 240, 220, 230));
            g2.fillOval(w - 170, 40, 80, 80);

            // 건물 실루엣 (원근감 없이 레이어 느낌)
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

            // ---------- 좀비들 (멀리 있는 순서대로 그림) ----------
            zombies.sort((a, b) -> Double.compare(b.distance, a.distance)); // 먼 것부터 그림

            for (Zombie z : zombies) {
                drawZombieFPS(g2, z);
            }

            // ---------- 총알 (레이저/탄환 느낌) ----------
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(255, 240, 180));
            for (Bullet b : bullets) {
                if (!b.active || b.target == null) continue;
                // 시작점: 총구
                int gunX = centerX;
                int gunY = groundY - 40;

                // 타겟 좀비의 화면 좌표 얻기
                Point tp = getZombieScreenCenter(b.target);
                double bx = gunX + (tp.x - gunX) * b.t;
                double by = gunY + (tp.y - gunY) * b.t;

                g2.drawLine(gunX, gunY, (int) bx, (int) by);
            }

            // ---------- HUD (점수/라이프/타겟 안내) ----------
            g2.setFont(new Font("Consolas", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            g2.drawString("SCORE : " + score, 20, 40);

            int livesLeft = 3 - livesUsed;
            g2.setColor(livesLeft <= 1 ? new Color(255, 80, 80) : new Color(200, 240, 200));
            g2.drawString("LIVES : " + livesLeft, 20, 70);

            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            g2.setColor(new Color(220, 220, 230));
            g2.drawString("TYPE WORD ABOVE FRONT ZOMBIE & PRESS ENTER", 20, 100);

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

            Zombie target = getFrontZombie();
            if (!gameOver && target != null && target.alive && !target.reachedPlayer) {
                g2.setFont(new Font("Consolas", Font.PLAIN, 16));
                g2.setColor(new Color(255, 220, 180));
                g2.drawString("TARGET : " + target.word, 20, 130);
            }

            // ---------- 총 (1인칭) ----------
            drawGun(g2);

            // 총구 번쩍
            if (muzzleFlash && !gameOver) {
                g2.setColor(new Color(255, 240, 200, 200));
                int gunX = centerX;
                int gunY = groundY - 40;
                g2.fillOval(gunX - 18, gunY - 18, 36, 36);
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
            // 거리에 따라 크기 스케일
            double t = (maxDist - z.distance) / (maxDist - minDist); // 0~1
            t = Math.max(0, Math.min(1, t));

            // 기본 높이 범위
            double hFar = 60;
            double hNear = 220;
            double zombieHeight = hFar + t * (hNear - hFar);

            // 화면에서 발은 항상 groundY에 닿게
            int yCenter = (int) (groundY - zombieHeight / 2.0);

            // laneOffset은 -1~1 정도를 기준으로 좌우 이동
            double maxLaneOffsetPixels = 200;
            int xCenter = (int) (centerX + z.laneOffset * maxLaneOffsetPixels);

            return new Point(xCenter, yCenter);
        }

        // FPS 시점 좀비 그리기
        private void drawZombieFPS(Graphics2D g2, Zombie z) {
            if (z == null || z.distance <= 0) return;

            // 죽었고 피격 잔상도 없으면 그릴 필요 없음
            if (!z.alive && z.hitFlash <= 0) return;

            // 거리 기반 스케일 계산
            double t = (maxDist - z.distance) / (maxDist - minDist); // 0~1
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

            // 몸체 색 (피격 시 붉게)
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

            // 단어 (머리 위에 띄우기)
            g2.setFont(new Font("Consolas", Font.BOLD, 18 + (int) (t * 6)));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(z.word);

            int labelY = y - bodyHeight - headSize - 25;
            g2.setColor(new Color(10, 10, 10, 180));
            g2.fillRoundRect(x - tw / 2 - 6, labelY - 18, tw + 12, 22, 8, 8);
            g2.setColor(new Color(255, 240, 180));
            g2.drawString(z.word, x - tw / 2, labelY);

            // 플레이어 바로 앞까지 온 경우 화면 붉게
            if (z.reachedPlayer) {
                g2.setColor(new Color(180, 0, 0, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        // FPS에서 총(손에 들고 있는 총) 그리기
        private void drawGun(Graphics2D g2) {
            int gunW = 120;
            int gunH = 80;
            int gunX = centerX - gunW / 2;
            int gunY = groundY - gunH + 10;

            // 총 본체
            g2.setColor(new Color(50, 50, 60));
            g2.fillRoundRect(gunX, gunY, gunW, gunH, 12, 12);

            // 총열
            g2.setColor(new Color(80, 80, 90));
            g2.fillRect(gunX + gunW / 2 - 10, gunY - 25, 20, 30);

            // 손잡이
            g2.setColor(new Color(40, 40, 50));
            g2.fillRoundRect(gunX + gunW - 35, gunY + 25, 26, 40, 8, 8);

            // 라인
            g2.setColor(new Color(130, 130, 140));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(gunX, gunY, gunW, gunH, 12, 12);
        }

        // ======================= 게임 루프 =======================
        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameOver) {
                // Game Over 상태에서도 배경/잔상만 보여주기
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

                if (z.distance <= minDist && !z.reachedPlayer) {
                    // 플레이어 근접 → 라이프 감소
                    z.reachedPlayer = true;
                    livesUsed++;
                    // TODO: 좀비 공격 사운드 재생 가능
                    if (livesUsed >= 3) {
                        gameOver = true;
                    }
                }
            }

            // 총알 이동 및 충돌 체크
            for (Bullet b : bullets) {
                if (!b.active || b.target == null) continue;

                b.t += b.speed;
                if (b.t >= 1.0) {
                    // 타겟에 도달
                    if (b.target.alive && !b.target.reachedPlayer) {
                        b.target.alive = false;
                        b.target.hitFlash = 12;
                        score += 10;
                        // TODO: 피격 사운드 재생 가능
                    }
                    b.active = false;
                }
            }

            // 비활성/죽은 좀비 정리
            zombies.removeIf(z -> (!z.alive && z.hitFlash <= 0) || z.distance <= 0.1);
            bullets.removeIf(b -> !b.active);

            // 총구 이펙트 시간
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
                // 성공/실패 상관 없이 입력은 초기화
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
            gameOver = false;
        }

        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyReleased(KeyEvent e) { }
    }
}
