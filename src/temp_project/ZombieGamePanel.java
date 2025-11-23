package temp_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ZombieGamePanel extends JPanel implements ActionListener {

    private final ZombieFrame frame;
    private final RoundManager roundManager;

    // HUD
    private final JLabel infoLabel;
    private final JLabel scoreLabel;
    private final JPanel heartPanel;

    // 입력
    private final JTextField inputField;

    // 게임 상태
    private String playerName = "Player";
    private int hp = 5;
    private int score = 0;

    private boolean isRoundAnimating = false;

    // 좀비 리스트
    private final List<Zombie> zombies = new ArrayList<>();

    // 타이머
    private Timer gameTimer;
    private int tickCount = 0;

    // 1인칭 화면 리소스
    private final JPanel viewPanel;
    private Image gunImage;
    private Image backgroundImage; // ★ 배경 이미지 변수 추가

    private static class Zombie {
        String word;
        int distance;
        int id;
        int xPos;

        Zombie(int id, String word, int distance, int xPos) {
            this.id = id;
            this.word = word;
            this.distance = distance;
            this.xPos = xPos;
        }
    }

    private int zombieIdSeq = 1;

    public ZombieGamePanel(ZombieFrame frame) {
        this.frame = frame;
        this.roundManager = new RoundManager();

        setLayout(new BorderLayout());

        // ── 상단 HUD ──
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

        // ★ 이미지 로딩 (배경 + 총)
        loadImages();

        // ── 중앙 화면 ──
        viewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGameScreen((Graphics2D) g);
            }
        };
        viewPanel.setBackground(Color.BLACK);

        // ── 하단 입력 ──
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        bottomPanel.add(new JLabel(" 입력: "), BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(viewPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // ── 이벤트 및 타이머 ──
        inputField.addActionListener(e -> {
            if (isRoundAnimating || !gameTimer.isRunning()) return;

            String text = inputField.getText().trim().toUpperCase();
            inputField.setText("");
            if (text.isEmpty()) return;

            handleShot(text);
            viewPanel.repaint();
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    togglePause();
                }
            }
        });

        gameTimer = new Timer(600, this);
    }

    /** 게임 화면 그리기 */
    private void drawGameScreen(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ★ 1. 배경 그리기
        if (backgroundImage != null) {
            // 이미지가 있으면 화면에 꽉 차게 그림
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // 이미지가 없으면 기존 그라데이션 + 지평선
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(20, 20, 40),
                    0, getHeight(), new Color(40, 40, 60)
            );
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(new Color(50, 50, 70));
            g2d.fillRect(0, getHeight() * 2 / 3, getWidth(), getHeight() / 3);
        }

        // 2. 좀비들
        drawZombies(g2d);

        // 3. 총
        if (gunImage != null) {
            int w = gunImage.getWidth(this);
            int h = gunImage.getHeight(this);
            int x = getWidth() - w - 20;
            int y = getHeight() - h - 10;
            g2d.drawImage(gunImage, x, y, this);
        } else {
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect(getWidth() - 200, getHeight() - 100, 150, 50);
        }

        // 4. 조준선
        drawCrosshair(g2d);

        // 5. 라운드 텍스트
        if (isRoundAnimating) {
            String msg = "ROUND " + roundManager.getRound() + "!";
            g2d.setFont(new Font("Verdana", Font.BOLD, 100));
            FontMetrics fm = g2d.getFontMetrics();
            int textW = fm.stringWidth(msg);
            int textH = fm.getAscent();

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(Color.BLACK);
            g2d.drawString(msg, cx - textW / 2 + 5, cy + 5);

            g2d.setColor(Color.YELLOW);
            g2d.drawString(msg, cx - textW / 2, cy);
        }
    }

    private void drawZombies(Graphics2D g2d) {
        int panelHeight = getHeight();
        for (Zombie z : zombies) {
            float scale = 1.0f - (z.distance / 120.0f);
            if (scale < 0.25f) scale = 0.25f;

            int zombieSize = (int)(100 * scale);
            int yPos = panelHeight * 2 / 3 - zombieSize - 30; // 발 위치 조정

            // 배경이 생기면 좀비가 떠 보일 수 있으니 y좌표를 살짝 내려서 바닥에 붙임
            if (backgroundImage != null) {
                yPos += 50;
            }

            int xPos = z.xPos - zombieSize / 2;

            g2d.setColor(new Color(0, 100, 0, 180));
            g2d.fillOval(xPos, yPos, zombieSize, zombieSize);
            g2d.setColor(new Color(0, 255, 0));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(xPos, yPos, zombieSize, zombieSize);

            // 단어 그리기 (이전 수정사항 유지)
            int fontSize = Math.max(14, (int)(32 * scale));
            g2d.setFont(new Font("맑은 고딕", Font.BOLD, fontSize));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(z.word);

            int textX = xPos + zombieSize / 2 - textWidth / 2;
            int textY = yPos - 10;

            g2d.setColor(Color.BLACK);
            g2d.drawString(z.word, textX + 1, textY + 1);
            g2d.drawString(z.word, textX - 1, textY + 1);

            g2d.setColor(Color.RED);
            g2d.drawString(z.word, textX, textY);
        }
    }

    private void drawCrosshair(Graphics2D g2d) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        g2d.setColor(new Color(255, 50, 50, 200));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(cx - 15, cy, cx + 15, cy);
        g2d.drawLine(cx, cy - 15, cx, cy + 15);
    }

    public void startNewGame(String name) {
        this.playerName = name;
        this.hp = 5;
        this.score = 0;
        this.zombies.clear();
        this.zombieIdSeq = 1;
        this.tickCount = 0;

        roundManager.reset();

        updateHud();
        updateHearts();

        inputField.setText("");
        inputField.requestFocus();
        startRoundEffect();
    }

    private void startRoundEffect() {
        isRoundAnimating = true;
        gameTimer.stop();
        viewPanel.repaint();

        Timer delayTimer = new Timer(2000, e -> {
            isRoundAnimating = false;
            gameTimer.start();
            int baseDelay = 600;
            int newDelay = Math.max(200, baseDelay - (roundManager.getRound() - 1) * 60);
            gameTimer.setDelay(newDelay);
            viewPanel.repaint();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    private void handleShot(String text) {
        Zombie target = null;
        for (Zombie z : zombies) {
            if (z.word.equals(text)) {
                if (target == null || z.distance < target.distance) {
                    target = z;
                }
            }
        }
        if (target == null) return;

        zombies.remove(target);
        score += 1;

        if (roundManager.checkLevelUp(score)) {
            startRoundEffect();
        }
        updateHud();
    }

    private void togglePause() {
        if (isRoundAnimating || hp <= 0) return;

        boolean wasRunning = gameTimer.isRunning();
        if (wasRunning) {
            gameTimer.stop();
            String[] options = {"계속하기", "메인으로"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "게임 일시정지 (설정)",
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
                gameTimer.start();
                inputField.requestFocus();
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tickCount++;
        if (tickCount % 2 == 0) {
            String w = WordManager.getInstance().getRandomWord();
            int xPos = (int)(viewPanel.getWidth() * (0.15 + Math.random() * 0.7));
            zombies.add(new Zombie(zombieIdSeq++, w, 100, xPos));
        }

        boolean damaged = false;
        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            z.distance -= roundManager.getZombieSpeed();

            if (z.distance <= 0) {
                it.remove();
                hp--;
                damaged = true;
            }
        }

        if (damaged) {
            updateHearts();
            if (hp <= 0) gameOver();
        }
        viewPanel.repaint();
    }

    private void gameOver() {
        gameTimer.stop();
        ScoreManager.getInstance().addScore(playerName, score);

        String[] options = {"다시하기", "메인으로"};
        int choice = JOptionPane.showOptionDialog(this,
                "GAME OVER\n최종 점수: " + score + "\n라운드: " + roundManager.getRound(),
                "게임 종료",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);

        if (choice == 0) startNewGame(playerName);
        else frame.showStartPanel();
    }

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

    // ★ 이미지 로딩 메서드 (수정됨: 강제 로딩 방식 적용)
    private void loadImages() {
        // 1. 총 이미지 (기존 방식이 잘 되므로 유지)
        try {
            URL gunUrl = getClass().getResource("images/gun.png");
            if (gunUrl != null) {
                gunImage = new ImageIcon(gunUrl).getImage().getScaledInstance(350, 200, Image.SCALE_SMOOTH);
            }
        } catch (Exception ex) { ex.printStackTrace(); }

        // 2. 배경 이미지 (해결책: src 폴더에서 직접 읽어오기)
        try {
            // 인텔리제이가 파일을 못 찾을 때 사용하는 '절대 무적' 경로입니다.
            // 프로젝트 폴더(src)부터 시작해서 파일을 직접 가리킵니다.
            String directPath = "src/temp_project/images/ZombieBackground.jpg";
            ImageIcon bgIcon = new ImageIcon(directPath);

            // 이미지 로드 상태 확인
            if (bgIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                backgroundImage = bgIcon.getImage();
                System.out.println("배경 이미지 로드 성공! (소스 경로)");
            } else {
                System.err.println("여전히 이미지를 못 찾았습니다. 다음 경로를 확인해보세요: " + directPath);

                // 혹시 모르니 기존 방식(Resource)도 예비로 한 번 더 시도
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