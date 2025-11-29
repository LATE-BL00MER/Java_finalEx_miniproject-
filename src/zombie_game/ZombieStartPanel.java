package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

public class ZombieStartPanel extends JPanel {

    private final ZombieFrame frame;
    private Image backgroundImage;   // 시작 화면 배경

    private final JTextField nameField;

    public ZombieStartPanel(ZombieFrame frame) {
        this.frame = frame;

        // 배경 이미지 먼저 로딩
        loadBackgroundImage();

        // 레이아웃 / 투명 설정
        setLayout(new BorderLayout());
        setOpaque(false); // 우리가 직접 배경을 그릴 거라서

        // ---------- 상단 타이틀 ----------
        JLabel titleLabel = new JLabel("Typing Zombie FPS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 10, 10, 10));
        titleLabel.setOpaque(false);
        add(titleLabel, BorderLayout.NORTH);

        // ---------- 중앙: 이름 + 버튼들 ----------
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 이름 입력
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel("플레이어 이름 : ");
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);

        nameField = new JTextField(12);
        nameField.setFont(new Font("맑은 고딕", Font.PLAIN, 18));

        namePanel.add(nameLabel);
        namePanel.add(nameField);

        centerPanel.add(namePanel);
        centerPanel.add(Box.createVerticalStrut(20));

        // 버튼 공통 스타일
        Dimension btnSize = new Dimension(220, 40);
        Font btnFont = new Font("맑은 고딕", Font.BOLD, 18);

        JButton startBtn     = new JButton("게임 시작");
        JButton wordSaveBtn  = new JButton("단어 저장");
        JButton wordListBtn  = new JButton("저장된 단어 보기");
        JButton rankBtn      = new JButton("랭킹 보기");
        JButton exitBtn      = new JButton("게임 종료");

        for (JButton b : new JButton[]{startBtn, wordSaveBtn, wordListBtn, rankBtn, exitBtn}) {
            b.setPreferredSize(btnSize);
            b.setFont(btnFont);
        }

        centerPanel.add(wrapButton(startBtn));
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(wrapButton(wordSaveBtn));
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(wrapButton(wordListBtn));
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(wrapButton(rankBtn));
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(wrapButton(exitBtn));

        add(centerPanel, BorderLayout.CENTER);

        // ---------- 버튼 이벤트 ----------

        // 게임 시작
        startBtn.addActionListener((ActionEvent e) -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Player";
            frame.showGamePanel(name);        // 기존에 쓰던 메서드 그대로 사용
        });

        // 단어 저장(이미 구현해 둔 다이얼로그/기능 연결)
        wordSaveBtn.addActionListener(e -> frame.showWordSaveDialog());

        // 저장된 단어 보기
        wordListBtn.addActionListener(e -> frame.showWordListDialog());

        // 랭킹 보기
        rankBtn.addActionListener(e -> frame.showRankingPanel());

        // 게임 종료
        exitBtn.addActionListener(e -> System.exit(0));
    }

    /** 버튼 하나를 가운데 정렬해서 감싸는 패널 */
    private JPanel wrapButton(JButton button) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setOpaque(false);
        p.add(button);
        return p;
    }

    /** StartPanel_background.png 불러오기 */
    private void loadBackgroundImage() {
        try {
            // 파일 위치: src/zombie_game/images/StartPanel_background.png
            URL url = getClass().getResource("images/StartPanel_background.png");
            if (url != null) {
                backgroundImage = new ImageIcon(url).getImage();
            } else {
                System.err.println("StartPanel_background.png 로드 실패");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 1) 배경 먼저 그린다
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // 2) 그 위에 버튼/텍스트 등 컴포넌트 그리기
        super.paintComponent(g);
    }
}
