package Sample_Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TypingConstellationGame extends JFrame {

    public TypingConstellationGame() {
        setTitle("✨ Typing Constellation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        add(new StarPanel());

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TypingConstellationGame::new);
    }

    // ------------------ 별자리 패널 ------------------
    static class StarPanel extends JPanel implements ActionListener, KeyListener {

        private final Timer timer = new Timer(30, this);
        private final Random random = new Random();

        // 단어 풀
        private final String[] wordPool = {
                "ORION", "LYRA", "DRACO", "CYGNUS", "AQUILA",
                "PEGASUS", "TAURUS", "LEO", "GEMINI", "ARIES",
                "PISCES", "SCORPIUS", "CASSIOPEIA", "ANDROMEDA", "PHOENIX"
        };

        private String currentWord = "";
        private String typed = "";

        // 현재 떨어지는 별
        private double starX;
        private double starY;
        private double starSpeed = 1.5;

        // 고정된 별자리 포인트
        private final List<Point> constellationPoints = new ArrayList<>();

        // 상태
        private int stage = 1;
        private boolean showComplete = false;
        private int completeTimer = 0;

        public StarPanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            spawnNewStar();
            timer.start();
        }

        private void spawnNewStar() {
            currentWord = wordPool[random.nextInt(wordPool.length)];
            typed = "";
            starX = 120 + random.nextInt(getWidth() > 0 ? getWidth() - 240 : 600);
            starY = 60;
            starSpeed = 1.0 + random.nextDouble() * 1.5;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // ----- 밤하늘 그라디언트 -----
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(5, 5, 25),
                    0, h, new Color(2, 0, 10)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // 배경 별들 (랜덤 점)
            g2.setColor(new Color(255, 255, 255, 120));
            for (int i = 0; i < 120; i++) {
                int sx = (i * 73) % w;
                int sy = (i * 127) % h;
                g2.fillOval(sx, sy, 2, 2);
            }

            // ----- 고정된 별자리 선/포인트 -----
            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(120, 200, 255, 200));
            for (int i = 0; i + 1 < constellationPoints.size(); i++) {
                Point a = constellationPoints.get(i);
                Point b = constellationPoints.get(i + 1);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            // 고정된 별
            g2.setColor(new Color(180, 220, 255));
            for (Point p : constellationPoints) {
                g2.fillOval(p.x - 5, p.y - 5, 10, 10);
            }

            // ----- 떨어지는 현재 별 -----
            // 별 외곽 후광
            RadialGradientPaint starPaint = new RadialGradientPaint(
                    new Point((int) starX, (int) starY),
                    20,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 255, 255, 230),
                            new Color(255, 255, 255, 0)}
            );
            g2.setPaint(starPaint);
            g2.fillOval((int) starX - 20, (int) starY - 20, 40, 40);

            // 별 본체
            g2.setColor(Color.WHITE);
            g2.fillOval((int) starX - 4, (int) starY - 4, 8, 8);

            // 별 옆 단어
            g2.setFont(new Font("Consolas", Font.BOLD, 24));
            g2.setColor(new Color(220, 240, 255));
            g2.drawString(currentWord, (int) starX + 20, (int) starY + 8);

            // ----- HUD -----
            g2.setFont(new Font("Consolas", Font.BOLD, 26));
            g2.setColor(new Color(230, 230, 255));
            g2.drawString("STAGE : " + stage, 30, 40);

            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            g2.setColor(new Color(210, 210, 240));
            g2.drawString("TYPE THE FALLING STAR'S NAME & PRESS ENTER", 30, 70);

            // 입력 박스
            int boxY = h - 120;
            g2.setColor(new Color(10, 10, 25, 220));
            g2.fillRoundRect(40, boxY, w - 80, 70, 20, 20);
            g2.setColor(new Color(130, 170, 255));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(40, boxY, w - 80, 70, 20, 20);

            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            g2.setColor(new Color(220, 235, 255));
            g2.drawString("INPUT> " + typed, 60, boxY + 42);

            // 별자리 완성 이펙트
            if (showComplete) {
                g2.setFont(new Font("Consolas", Font.BOLD, 34));
                g2.setColor(new Color(255, 230, 180, 240));
                String msg = "CONSTELLATION COMPLETE ✨";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(msg);
                g2.drawString(msg, (w - tw) / 2, h / 2);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // 별 떨어짐
            starY += starSpeed;

            // 화면 아래로 사라지면 그냥 새 별 (힐링 게임이라 실패 패널티 X)
            if (starY > getHeight() + 40) {
                spawnNewStar();
            }

            // 완성 이펙트 유지 시간
            if (showComplete) {
                completeTimer++;
                if (completeTimer > 80) { // 좀 오래 보여주기
                    showComplete = false;
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
                if (typed.equalsIgnoreCase(currentWord)) {
                    // 별 고정 → 별자리에 추가
                    constellationPoints.add(new Point((int) starX, (int) starY));

                    // 별자리 하나가 어느 정도 길어지면 "완성"
                    if (constellationPoints.size() >= 6) {
                        showComplete = true;
                        completeTimer = 0;
                        stage++;
                        constellationPoints.clear();
                    }

                    spawnNewStar();
                } else {
                    // 틀려도 그냥 새로운 별 (힐링 분위기)
                    spawnNewStar();
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
