package Project_PingPong;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PingPongPanel extends JPanel implements ActionListener, KeyListener {

    // 애니메이션 타이머 (대략 60fps)
    private final Timer timer = new Timer(16, this);

    // 탁구대 영역
    private Rectangle tableRect;

    // 공
    private double ballX, ballY;
    private double ballVX = 4;   // 속도 X
    private double ballVY = -4;  // 속도 Y
    private final int ballSize = 18;

    // 플레이어 패들 (아래)
    private double paddleX;
    private final int paddleWidth = 100;
    private final int paddleHeight = 14;

    // 상대 패들 (위) – 간단 AI (볼 따라감)
    private double enemyPaddleX;

    // 상태
    private int score = 0;
    private int missCount = 0;
    private boolean showMissEffect = false;
    private int missEffectTimer = 0;

    public PingPongPanel() {
        setBackground(new Color(0, 40, 110)); // 탁구대 느낌 나는 파란/초록톤 배경
        setFocusable(true);
        addKeyListener(this);
        timer.start();
    }

    // 레이아웃/크기 변경 시 탁구대 계산
    private void updateTableRect() {
        int w = getWidth();
        int h = getHeight();

        // 화면 가운데 세로형 탁구대
        int tableW = (int) (w * 0.55);
        int tableH = (int) (h * 0.85);
        int tableX = (w - tableW) / 2;
        int tableY = (h - tableH) / 2;

        tableRect = new Rectangle(tableX, tableY, tableW, tableH);

        // 공과 패들 초기 위치
        ballX = tableRect.getCenterX();
        ballY = tableRect.getCenterY();

        paddleX = tableRect.getCenterX() - paddleWidth / 2.0;
        enemyPaddleX = paddleX;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (tableRect == null) {
            updateTableRect();
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // --------- 배경 그라디언트 (살짝 고급 느낌) ---------
        Color top = new Color(10, 30, 80);
        Color bottom = new Color(0, 10, 40);
        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRect(0, 0, w, h);

        // --------- 탁구대 (실제 느낌) ---------
        // 탁구대 안쪽 색
        Color tableColor = new Color(18, 120, 60);
        g2.setColor(tableColor);
        g2.fillRoundRect(tableRect.x, tableRect.y, tableRect.width, tableRect.height, 20, 20);

        // 테두리 라인
        g2.setStroke(new BasicStroke(4));
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(tableRect.x, tableRect.y, tableRect.width, tableRect.height, 20, 20);

        // 중앙 세로선
        int centerX = tableRect.x + tableRect.width / 2;
        g2.drawLine(centerX, tableRect.y, centerX, tableRect.y + tableRect.height);

        // 네트선 (가운데 가로)
        int netY = tableRect.y + tableRect.height / 2;
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(tableRect.x, netY, tableRect.x + tableRect.width, netY);

        // 네트 격자 느낌 (간단)
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(255, 255, 255, 120));
        int netSpacing = 10;
        for (int x = tableRect.x; x <= tableRect.x + tableRect.width; x += netSpacing) {
            g2.drawLine(x, netY - 12, x, netY + 12);
        }

        // --------- 패들 (라켓) ---------
        // 플레이어 패들 (아래쪽)
        int playerPaddleY = tableRect.y + tableRect.height - 30;
        g2.setColor(new Color(250, 230, 80));
        g2.fillRoundRect((int) paddleX, playerPaddleY, paddleWidth, paddleHeight, 10, 10);
        g2.setColor(new Color(180, 150, 40));
        g2.drawRoundRect((int) paddleX, playerPaddleY, paddleWidth, paddleHeight, 10, 10);

        // 상대 패들 (윗쪽)
        int enemyPaddleY = tableRect.y + 15;
        g2.setColor(new Color(255, 120, 120));
        g2.fillRoundRect((int) enemyPaddleX, enemyPaddleY, paddleWidth, paddleHeight, 10, 10);
        g2.setColor(new Color(160, 60, 60));
        g2.drawRoundRect((int) enemyPaddleX, enemyPaddleY, paddleWidth, paddleHeight, 10, 10);

        // --------- 공 ---------
        g2.setColor(Color.WHITE);
        g2.fillOval((int) (ballX - ballSize / 2.0),
                (int) (ballY - ballSize / 2.0),
                ballSize, ballSize);

        // 공에 살짝 그림자
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawOval((int) (ballX - ballSize / 2.0),
                (int) (ballY - ballSize / 2.0),
                ballSize, ballSize);

        // --------- 점수 / 상태 HUD ---------
        g2.setFont(new Font("Consolas", Font.BOLD, 22));
        g2.setColor(new Color(240, 240, 240));
        g2.drawString("SCORE : " + score, 30, 40);

        g2.setFont(new Font("Consolas", Font.PLAIN, 16));
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("MISS : " + missCount, 30, 65);
        g2.drawString("← → 방향키로 패들 이동", 30, 90);

        if (showMissEffect) {
            g2.setFont(new Font("Consolas", Font.BOLD, 36));
            g2.setColor(new Color(255, 80, 80, 220));
            String msg = "MISS!";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            g2.drawString(msg, (w - tw) / 2, tableRect.y - 20);
        }
    }

    // ---------- 게임 루프 ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (tableRect == null || tableRect.width == 0) {
            updateTableRect();
        }

        // 공 이동
        ballX += ballVX;
        ballY += ballVY;

        // 벽(좌우) 충돌
        if (ballX - ballSize / 2.0 <= tableRect.x ||
                ballX + ballSize / 2.0 >= tableRect.x + tableRect.width) {
            ballVX *= -1;
            ballX = Math.max(ballX, tableRect.x + ballSize / 2.0);
            ballX = Math.min(ballX, tableRect.x + tableRect.width - ballSize / 2.0);
        }

        // 위쪽(상대 패들 근처) 충돌
        int enemyPaddleY = tableRect.y + 15 + paddleHeight;
        if (ballVY < 0 && ballY - ballSize / 2.0 <= enemyPaddleY) {
            if (ballX >= enemyPaddleX &&
                    ballX <= enemyPaddleX + paddleWidth) {
                ballVY *= -1;
                ballY = enemyPaddleY + ballSize / 2.0;
            } else if (ballY - ballSize / 2.0 <= tableRect.y) {
                // 그냥 벽에 부딪히는 경우
                ballVY *= -1;
                ballY = tableRect.y + ballSize / 2.0;
            }
        }

        // 아래쪽(플레이어 패들) 충돌
        int playerPaddleY = tableRect.y + tableRect.height - 30;
        if (ballVY > 0 && ballY + ballSize / 2.0 >= playerPaddleY) {
            if (ballX >= paddleX &&
                    ballX <= paddleX + paddleWidth) {
                // 패들에 맞으면 튕겨 올라감 + 점수
                ballVY *= -1;
                ballY = playerPaddleY - ballSize / 2.0;
                score++;

                // 맞는 위치에 따라 X 방향 약간 변화 (조금 더 자연스럽게)
                double hitPos = (ballX - (paddleX + paddleWidth / 2.0)) / (paddleWidth / 2.0);
                ballVX += hitPos * 1.5;
            } else if (ballY + ballSize / 2.0 >= tableRect.y + tableRect.height) {
                // 바닥에 떨어지면 MISS
                missCount++;
                showMissEffect = true;
                missEffectTimer = 0;
                resetBall();
            }
        }

        // 상대 패들은 공을 따라가는 간단 AI
        double targetX = ballX - paddleWidth / 2.0;
        double speed = 3.0;
        if (enemyPaddleX < targetX) {
            enemyPaddleX += speed;
        } else if (enemyPaddleX > targetX) {
            enemyPaddleX -= speed;
        }
        // 테이블 안으로 제한
        enemyPaddleX = Math.max(enemyPaddleX, tableRect.x);
        enemyPaddleX = Math.min(enemyPaddleX, tableRect.x + tableRect.width - paddleWidth);

        // MISS 이펙트 시간 제어
        if (showMissEffect) {
            missEffectTimer++;
            if (missEffectTimer > 40) {
                showMissEffect = false;
            }
        }

        repaint();
    }

    private void resetBall() {
        ballX = tableRect.getCenterX();
        ballY = tableRect.getCenterY();
        ballVX = (Math.random() < 0.5 ? -1 : 1) * (3 + Math.random() * 2);
        ballVY = -4;
    }

    // ---------- 키 입력 (플레이어 패들 이동) ----------
    @Override
    public void keyPressed(KeyEvent e) {
        if (tableRect == null) return;

        int code = e.getKeyCode();
        int move = 20;

        if (code == KeyEvent.VK_LEFT) {
            paddleX -= move;
        } else if (code == KeyEvent.VK_RIGHT) {
            paddleX += move;
        }

        // 탁구대 안으로만
        paddleX = Math.max(paddleX, tableRect.x);
        paddleX = Math.min(paddleX, tableRect.x + tableRect.width - paddleWidth);

        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) { }
}
