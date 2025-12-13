package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class ZombieStartPanel extends JPanel {

    private final ZombieFrame frame;

    // 사운드 아이콘
    private JLabel soundLabel;
    private ImageIcon speakerIcon;
    private ImageIcon muteIcon;

    // 배경
    private Image backgroundImage;

    // 이름 입력
    private final JTextField nameField;

    // ====== 여기서 아이콘 크기만 바꾸면 됨 ======
    private static final int SOUND_ICON_SIZE = 32; // 24~40 추천

    public ZombieStartPanel(ZombieFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setOpaque(false);

        // ===== 이미지 로딩(파일 경로 방식) =====
        loadImagesFromFileSystem();

        // ================= 상단 바 =================
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("Typing Zombie FPS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);

        soundLabel = new JLabel();
        soundLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        soundLabel.setPreferredSize(new Dimension(SOUND_ICON_SIZE + 8, SOUND_ICON_SIZE + 8));
        soundLabel.setHorizontalAlignment(SwingConstants.CENTER);
        soundLabel.setVerticalAlignment(SwingConstants.CENTER);

        syncSoundIcon();

        JPanel soundWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        soundWrap.setOpaque(false);
        soundWrap.add(soundLabel);

        topBar.add(titleLabel, BorderLayout.CENTER);
        topBar.add(soundWrap, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ===== 사운드 토글: mouseReleased(클릭 뗄 때) =====
        soundLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 눌림 느낌(선택)
                soundLabel.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80)));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                soundLabel.setBorder(null);

                frame.toggleBgmMute();
                syncSoundIcon();
            }
        });

        // ================= 중앙: 이름 + 버튼 =================
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

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

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(namePanel, gbc);

        // 버튼
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

        // 1행: 시작 / 단어 저장
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        centerPanel.add(startBtn, gbc);

        gbc.gridx = 1;
        centerPanel.add(wordSaveBtn, gbc);

        // 2행: 단어 보기 / 랭킹
        gbc.gridy = 2;
        gbc.gridx = 0;
        centerPanel.add(wordListBtn, gbc);

        gbc.gridx = 1;
        centerPanel.add(rankBtn, gbc);

        // 3행: 종료 (가운데)
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerPanel.add(exitBtn, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // ================= 버튼 이벤트 =================
        startBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "이름을 입력해야 게임을 시작할 수 있습니다.",
                        "알림",
                        JOptionPane.WARNING_MESSAGE
                );
                nameField.requestFocusInWindow();
                return;
            }
            frame.showGamePanel(name);
        });

        wordSaveBtn.addActionListener(e -> frame.showWordSaveDialog());
        wordListBtn.addActionListener(e -> frame.showWordListDialog());
        rankBtn.addActionListener(e -> frame.showRankingPanel());
        exitBtn.addActionListener(e -> System.exit(0));
    }

    // =========================================================
    //  파일 경로 기반 이미지 로딩 + 아이콘 리사이즈
    // =========================================================
    private void loadImagesFromFileSystem() {
        // 경로 후보(당신 프로젝트에서 나올만한 경우들)
        String speakerPath = findFirstExistingPath(
                "src/zombie_game/images/speaker.png",
                "src/images/speaker.png",
                "images/speaker.png"
        );
        String mutePath = findFirstExistingPath(
                "src/zombie_game/images/mute.png",
                "src/images/mute.png",
                "images/mute.png"
        );
        String bgPath = findFirstExistingPath(
                "src/zombie_game/images/StartPanel_background.png",
                "src/images/StartPanel_background.png",
                "images/StartPanel_background.png"
        );

        speakerIcon = loadAndResizeIcon(speakerPath, SOUND_ICON_SIZE);
        muteIcon    = loadAndResizeIcon(mutePath, SOUND_ICON_SIZE);

        backgroundImage = loadImage(bgPath);
    }

    private String findFirstExistingPath(String... candidates) {
        String base = System.getProperty("user.dir");

        for (String rel : candidates) {
            File f1 = new File(rel);
            if (f1.exists() && f1.isFile()) return f1.getAbsolutePath();

            File f2 = new File(base, rel);
            if (f2.exists() && f2.isFile()) return f2.getAbsolutePath();
        }
        // 없으면 첫 후보를 그냥 반환(로그용). 실제 로딩에서 실패 처리됨.
        return candidates.length > 0 ? candidates[0] : "";
    }

    private ImageIcon loadAndResizeIcon(String absoluteOrRelPath, int size) {
        if (absoluteOrRelPath == null || absoluteOrRelPath.isEmpty()) {
            System.err.println("❌ 아이콘 경로가 비어있음");
            return new ImageIcon();
        }

        File f = new File(absoluteOrRelPath);
        if (!f.exists()) {
            System.err.println("❌ 아이콘 파일 없음: " + absoluteOrRelPath);
            return new ImageIcon();
        }

        ImageIcon origin = new ImageIcon(f.getAbsolutePath());
        if (origin.getIconWidth() <= 0) {
            System.err.println("❌ 아이콘 읽기 실패(파일은 존재): " + f.getAbsolutePath());
            return new ImageIcon();
        }

        Image scaled = origin.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private Image loadImage(String absoluteOrRelPath) {
        if (absoluteOrRelPath == null || absoluteOrRelPath.isEmpty()) {
            System.err.println("❌ 배경 경로가 비어있음");
            return null;
        }

        File f = new File(absoluteOrRelPath);
        if (!f.exists()) {
            System.err.println("❌ 배경 파일 없음: " + absoluteOrRelPath);
            return null;
        }

        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        if (icon.getIconWidth() <= 0) {
            System.err.println("❌ 배경 읽기 실패(파일은 존재): " + f.getAbsolutePath());
            return null;
        }
        return icon.getImage();
    }

    // =========================================================
    //  BGM 아이콘 동기화 (ZombieFrame에서 Start로 돌아올 때도 호출 가능)
    // =========================================================
    public void syncSoundIcon() {
        if (soundLabel == null) return;
        soundLabel.setIcon(frame.isBgmMuted() ? muteIcon : speakerIcon);
    }

    // =========================================================
    //  페인팅
    // =========================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
