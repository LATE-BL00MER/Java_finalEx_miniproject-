package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

public class ZombieStartPanel extends JPanel {

    private final ZombieFrame frame;
    private Image backgroundImage;   // 시작 화면 배경

    private final JTextField nameField;
    private final JButton bgmBtn;

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
        JPanel centerPanel = new JPanel(new GridBagLayout()); // ★ 그리드배치
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        // 이름 입력 (가운데 정렬)
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel("플레이어 이름 : ");
        nameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);

        nameField = new JTextField(12);
        nameField.setFont(new Font("맑은 고딕", Font.PLAIN, 18));

        namePanel.add(nameLabel);
        namePanel.add(nameField);

        // 이름 패널은 두 칸(0,0 / 1,0)을 가로로 합쳐서 중앙에
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(namePanel, gbc);

        // 버튼 공통 스타일
        Dimension btnSize = new Dimension(220, 40);
        Font btnFont = new Font("맑은 고딕", Font.BOLD, 18);

        JButton startBtn     = new JButton("게임 시작");        // 1번
        JButton wordSaveBtn  = new JButton("단어 저장");        // 2번
        JButton wordListBtn  = new JButton("저장된 단어 보기"); // 3번
        JButton rankBtn      = new JButton("랭킹 보기");        // 4번
        JButton exitBtn      = new JButton("게임 종료");        // 5번

        // BGM 토글 버튼 (StartPanel에서도 음악 On/Off)
        bgmBtn = new JButton(frame.isBgmMuted() ? "🔇 음악 켜기" : "🔊 음악 끄기");
        bgmBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        bgmBtn.addActionListener(e -> {
            frame.toggleBgmMute();
            syncBgmButton();
        });

        for (JButton b : new JButton[]{startBtn, wordSaveBtn, wordListBtn, rankBtn, exitBtn}) {
            b.setPreferredSize(btnSize);
            b.setFont(btnFont);
        }

        // 1행: 1 2  (게임 시작 / 단어 저장)
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        centerPanel.add(startBtn, gbc);      // 1번

        gbc.gridx = 1;
        centerPanel.add(wordSaveBtn, gbc);   // 2번

        // 2행: 3 4  (저장된 단어 보기 / 랭킹 보기)
        gbc.gridy = 2;
        gbc.gridx = 0;
        centerPanel.add(wordListBtn, gbc);   // 3번

        gbc.gridx = 1;
        centerPanel.add(rankBtn, gbc);       // 4번

        // 3행:   5   (게임 종료, 가운데)
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerPanel.add(exitBtn, gbc);       // 5번

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(bgmBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // ---------- 버튼 이벤트 ----------

        // 게임 시작
        startBtn.addActionListener((ActionEvent e) -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "이름을 입력해야 게임을 시작할 수 있습니다.",
                        "알림",
                        JOptionPane.WARNING_MESSAGE
                );
                nameField.requestFocus();
                return;
            }
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

    /** StartPanel BGM 버튼 텍스트 동기화 */
    public void syncBgmButton() {
        if (bgmBtn != null) {
            bgmBtn.setText(frame.isBgmMuted() ? "🔇 음악 켜기" : "🔊 음악 끄기");
        }
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
