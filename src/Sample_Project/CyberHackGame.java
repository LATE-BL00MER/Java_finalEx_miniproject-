package Sample_Project;// package Project_PingPong;  // 패키지 쓰면 여기에 맞게 수정

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class CyberHackGame extends JFrame {

    public CyberHackGame() {
        setTitle("⚡ Netrunner: Cyber Typing Hack ⚡");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CyberHackGame::new);
    }

    // ==================== 게임 패널 ====================
    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        private final Timer timer;
        private final Random random = new Random();

        // 떨어지는(흐르는) 단어
        private final String[] wordPool = {
                "PROTOCOL", "FIREWALL", "OVERRIDE", "NEURAL", "MATRIX",
                "PACKET", "CYBER", "GLITCH", "BACKDOOR", "ENCRYPT", "DECRYPT",
                "ZEROCOOL", "NETRUN", "GHOST", "VECTOR", "CYBERPUNK"
        };
        private String currentWord;
        private float wordX;
        private float wordY;
        private float wordSpeed;

        // 플레이어 타이핑
        private String typed = "";

        // 이펙트
        private boolean showSuccess = false;
        private boolean showFail = false;
        private int effectTimer = 0; // 프레임 카운트

        public GamePanel() {
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(this);

            // 초기 단어 설정
            resetWord();

            // 60FPS 근처 (1000/16 ≈ 62.5)
            timer = new Timer(16, this);
            timer.start();
        }

        // 단어 새로 뽑기
        private void resetWord() {
            currentWord = wordPool[random.nextInt(wordPool.length)];
            wordSpeed = 2.5f + random.nextFloat() * 2.0f; // 속도 2.5~4.5
            // 오른쪽 바깥에서 왼쪽으로 흘러오게
            wordX = getWidth() <= 0 ? 900 : getWidth();
            wordY = getHeight() <= 0 ? 250 : getHeight() / 2f;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // 안티앨리어싱
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // ---------------- 배경 연출 (사이버펑크 네온 느낌) ----------------
            // 어두운 배경 + 그리드 + 네온 라인
            Color bgTop = new Color(5, 5, 20);
            Color bgBottom = new Color(10, 0, 40);
            GradientPaint gp = new GradientPaint(0, 0, bgTop, 0, h, bgBottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // 그리드
            g2.setColor(new Color(0, 255, 255, 40));
            for (int x = 0; x < w; x += 40) {
                g2.drawLine(x, 0, x, h);
            }
            for (int y = 0; y < h; y += 40) {
                g2.drawLine(0, y, w, y);
            }

            // 가운데 네온 파이프
            g2.setColor(new Color(255, 0, 255, 120));
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(0, h / 3, w, h / 3);
            g2.drawLine(0, 2 * h / 3, w, 2 * h / 3);

            // ---------------- 상단 HUD ----------------
            g2.setFont(new Font("Consolas", Font.BOLD, 22));
            g2.setColor(new Color(0, 255, 180));
            g2.drawString(">> N E T R U N N E R  //  CYBER TYPING HACK <<", 20, 35);

            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            g2.setColor(new Color(100, 255, 200));
            g2.drawString("TYPE THE DATA PACKET TO HACK // ENTER to confirm", 20, 60);

            // ---------------- 흐르는 단어(데이터 패킷) ----------------
            g2.setFont(new Font("Consolas", Font.BOLD, 42));
            FontMetrics fm = g2.getFontMetrics();
            int wordWidth = fm.stringWidth(currentWord);
            int wordHeight = fm.getAscent();

            // 네온 효과용 살짝 겹치는 라인
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(currentWord, (int) wordX + 2, (int) wordY + 2);

            g2.setColor(new Color(0, 255, 255));
            g2.drawString(currentWord, (int) wordX, (int) wordY);

            // 데이터 패킷 테두리 박스
            g2.setColor(new Color(0, 255, 255, 120));
            int paddingX = 20;
            int paddingY = 15;
            g2.drawRoundRect(
                    (int) wordX - paddingX,
                    (int) wordY - wordHeight - paddingY / 2,
                    wordWidth + paddingX * 2,
                    wordHeight + paddingY,
                    20, 20
            );

            // ---------------- 하단 타이핑 영역 ----------------
            int inputBoxY = h - 130;
            int inputBoxH = 70;
            g2.setColor(new Color(10, 10, 10, 220));
            g2.fillRoundRect(40, inputBoxY, w - 80, inputBoxH, 25, 25);

            g2.setColor(new Color(0, 255, 180));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(40, inputBoxY, w - 80, inputBoxH, 25, 25);

            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            g2.setColor(new Color(80, 200, 255));
            g2.drawString("INPUT> " + typed, 60, inputBoxY + 40);

            // ---------------- 성공 / 실패 이펙트 ----------------
            if (showSuccess) {
                g2.setFont(new Font("Consolas", Font.BOLD, 40));
                g2.setColor(new Color(0, 255, 150, 200));
                drawCenterString(g2, "HACK SUCCESS // ACCESS GRANTED", w, h / 2 + 120);
            } else if (showFail) {
                g2.setFont(new Font("Consolas", Font.BOLD, 40));
                g2.setColor(new Color(255, 80, 80, 220));
                drawCenterString(g2, "FIREWALL COUNTERATTACK // ACCESS DENIED", w, h / 2 + 120);
            }
        }

        private void drawCenterString(Graphics2D g2, String text, int panelWidth, int centerY) {
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, (panelWidth - textWidth) / 2, centerY);
        }

        // ---------------- 게임 루프 (타이머) ----------------
        @Override
        public void actionPerformed(ActionEvent e) {
            // 단어 왼쪽으로 이동
            wordX -= wordSpeed;

            // 화면 밖으로 나가면 자동 실패 처리
            if (wordX + 10 < 0) {
                triggerFail();
            }

            // 이펙트 시간 관리 (약 0.5초 유지)
            if (showSuccess || showFail) {
                effectTimer++;
                if (effectTimer > 30) { // 30프레임 정도
                    showSuccess = false;
                    showFail = false;
                    effectTimer = 0;
                }
            }

            repaint();
        }

        // ---------------- 성공 / 실패 처리 ----------------
        private void triggerSuccess() {
            showSuccess = true;
            showFail = false;
            effectTimer = 0;
            typed = "";
            resetWord();
        }

        private void triggerFail() {
            showFail = true;
            showSuccess = false;
            effectTimer = 0;
            typed = "";
            resetWord();
        }

        // ---------------- 키보드 입력 ----------------
        @Override
        public void keyTyped(KeyEvent e) {
            // 사용 안 함
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();

            if (code == KeyEvent.VK_BACK_SPACE) {
                if (!typed.isEmpty()) {
                    typed = typed.substring(0, typed.length() - 1);
                }
            } else if (code == KeyEvent.VK_ENTER) {
                // 엔터로 판정
                if (typed.equalsIgnoreCase(currentWord)) {
                    triggerSuccess();
                } else {
                    triggerFail();
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
        public void keyReleased(KeyEvent e) {
            // 사용 안 함
        }
    }
}
