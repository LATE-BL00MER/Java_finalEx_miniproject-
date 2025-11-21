package Sample_Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class TypingBasketballGame extends JFrame {

    public TypingBasketballGame() {
        setTitle("🏀 Typing Basketball Shot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        add(new CourtPanel());

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TypingBasketballGame::new);
    }

    // ------------------ 코트 패널 ------------------
    static class CourtPanel extends JPanel implements ActionListener, KeyListener {

        private final Timer timer = new Timer(16, this);
        private final Random random = new Random();

        // 단어 풀
        private final String[] wordPool = {
                "PIVOT", "DRIBBLE", "REBOUND", "ASSIST", "DUNK",
                "SCREEN", "TRIPLE", "LAYUP", "CROSSOVER", "STEAL",
                "CENTER", "FORWARD", "GUARD", "SHOTCLOCK", "FASTBREAK"
        };

        private String currentWord = "";
        private String typed = "";

        // 점수 & 연속 성공
        private int score = 0;
        private int streak = 0;
        private boolean showHotStreak = false;
        private int hotTimer = 0;

        // 농구공 좌표 / 속도
        private double ballX = 450;
        private double ballY = 430;
        private double ballVX = 0;
        private double ballVY = 0;
        private final int ballSize = 26;

        // 슛 상태
        private boolean isShooting = false;
        private boolean shotSuccess = false;
        private boolean shotStarted = false; // 처음 한 번만 속도 세팅용

        // 골대 위치
        private final int hoopX = 450;
        private final int hoopY = 170;

        public CourtPanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            pickNewWord();
            timer.start();
        }

        private void pickNewWord() {
            currentWord = wordPool[random.nextInt(wordPool.length)];
            typed = "";
        }

        private void resetBall() {
            ballX = 450;
            ballY = 430;
            ballVX = 0;
            ballVY = 0;
            isShooting = false;
            shotStarted = false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // ----- 배경 (코트 느낌) -----
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(30, 20, 10),
                    0, h, new Color(10, 5, 0)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // 코트 바닥
            g2.setColor(new Color(90, 55, 25));
            g2.fillRect(0, h - 140, w, 140);

            // 중앙 원
            g2.setColor(new Color(200, 160, 80, 160));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(w / 2 - 80, h - 180, 160, 160);

            // ----- 골대 / 백보드 / 림 -----
            // 백보드 기둥
            g2.setColor(new Color(190, 190, 190));
            g2.fillRect(hoopX - 5, hoopY + 10, 10, 120);

            // 백보드
            g2.setColor(new Color(235, 235, 235));
            g2.fillRect(hoopX - 70, hoopY - 40, 140, 80);
            g2.setColor(new Color(200, 60, 60));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(hoopX - 30, hoopY - 20, 60, 40); // 슛 박스

            // 림
            int rimRadius = 40;
            g2.setColor(new Color(255, 120, 60));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(hoopX - rimRadius, hoopY + 15, rimRadius * 2, 10);

            // 그물
            g2.setColor(new Color(240, 240, 240, 180));
            for (int i = -rimRadius; i <= rimRadius; i += 10) {
                g2.drawLine(hoopX + i, hoopY + 20,
                        hoopX + i / 2, hoopY + 60);
            }

            // ----- 농구공 -----
            g2.setColor(new Color(255, 150, 40));
            g2.fillOval((int) (ballX - ballSize / 2.0),
                    (int) (ballY - ballSize / 2.0),
                    ballSize, ballSize);
            g2.setColor(new Color(150, 60, 20));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval((int) (ballX - ballSize / 2.0),
                    (int) (ballY - ballSize / 2.0),
                    ballSize, ballSize);

            // 공 줄무늬
            g2.drawLine((int) ballX,
                    (int) (ballY - ballSize / 2.0),
                    (int) ballX,
                    (int) (ballY + ballSize / 2.0));
            g2.drawArc((int) (ballX - ballSize / 2.0),
                    (int) ballY - ballSize / 2,
                    ballSize, ballSize, 30, 120);
            g2.drawArc((int) (ballX - ballSize / 2.0),
                    (int) ballY - ballSize / 2,
                    ballSize, ballSize, 210, 120);

            // ----- HUD -----
            g2.setFont(new Font("Consolas", Font.BOLD, 26));
            g2.setColor(Color.WHITE);
            g2.drawString("SCORE : " + score, 30, 40);

            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            g2.setColor(new Color(230, 230, 230));
            g2.drawString("STREAK : " + streak, 30, 70);
            g2.drawString("TYPE WORD & ENTER TO SHOOT", 30, 100);

            // 단어
            g2.setFont(new Font("Consolas", Font.BOLD, 28));
            g2.setColor(new Color(255, 230, 160));
            g2.drawString("WORD : " + currentWord, 30, 150);

            // 입력 중인 텍스트
            g2.setFont(new Font("Consolas", Font.PLAIN, 22));
            g2.setColor(new Color(200, 240, 255));
            g2.drawString("INPUT> " + typed, 30, 190);

            // HOT STREAK 이펙트
            if (showHotStreak) {
                g2.setFont(new Font("Consolas", Font.BOLD, 40));
                g2.setColor(new Color(255, 200, 50, 230));
                String msg = "HOT STREAK!!";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(msg);
                g2.drawString(msg, (w - tw) / 2, 80);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // 슛 애니메이션
            if (isShooting) {
                if (!shotStarted) {
                    // 처음 엔터 친 순간: 속도 설정
                    if (shotSuccess) {
                        // 정면으로 예쁜 포물선
                        ballX = 450;
                        ballY = 430;
                        ballVX = 0;
                        ballVY = -13;
                    } else {
                        // 약간 옆으로 빗나가는 포물선
                        ballX = 450;
                        ballY = 430;
                        ballVX = random.nextBoolean() ? -6 : 6;
                        ballVY = -12;
                    }
                    shotStarted = true;
                }

                // 중력
                ballX += ballVX;
                ballY += ballVY;
                ballVY += 0.6;

                // 화면 아래로 떨어지면 리셋
                if (ballY > getHeight()) {
                    resetBall();
                    pickNewWord();
                }
            }

            // HOT STREAK 유지 시간
            if (showHotStreak) {
                hotTimer++;
                if (hotTimer > 60) { // 약 1초
                    showHotStreak = false;
                }
            }

            repaint();
        }

        // --------- 키 입력 ---------
        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_BACK_SPACE) {
                if (!typed.isEmpty()) {
                    typed = typed.substring(0, typed.length() - 1);
                }
            } else if (code == KeyEvent.VK_ENTER) {
                if (isShooting) return; // 이미 슛 중이면 무시

                if (typed.equalsIgnoreCase(currentWord)) {
                    // 성공 슛
                    shotSuccess = true;
                    isShooting = true;
                    streak++;
                    score += 2;
                    if (streak >= 3) {
                        showHotStreak = true;
                        hotTimer = 0;
                    }
                } else {
                    // 실패 슛
                    shotSuccess = false;
                    isShooting = true;
                    streak = 0;
                    showHotStreak = false;
                }
            } else {
                char c = e.getKeyChar();
                if (Character.isLetter(c)) {
                    typed += Character.toUpperCase(c);
                }
            }

            repaint();
        }

        @Override
        public void keyTyped(KeyEvent e) { }

        @Override
        public void keyReleased(KeyEvent e) { }
    }
}
