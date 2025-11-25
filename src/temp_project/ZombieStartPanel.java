package temp_project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ZombieStartPanel extends JPanel {

    private final ZombieFrame frame;
    private final JTextField nameField;

    public ZombieStartPanel(ZombieFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(new Color(15, 15, 25));

        // 전체 여백
        setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 타이틀
        JLabel title = new JLabel("좀비게임FPS", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 60));
        title.setForeground(Color.WHITE);

        // 가운데 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 이름 입력
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel("이름: ");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        nameLabel.setForeground(Color.WHITE);
        nameField = new JTextField(15);
        nameField.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        namePanel.add(nameLabel);
        namePanel.add(nameField);

        // 버튼들
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 15, 15));
        buttonPanel.setOpaque(false);
        JButton startBtn    = createStyledButton("게임 시작");
        JButton rankBtn     = createStyledButton("명예의 전당");
        JButton wordBtn     = createStyledButton("단어 저장");
        JButton wordListBtn = createStyledButton("저장된 단어");
        JButton exitBtn     = createStyledButton("종료");

        buttonPanel.add(startBtn);
        buttonPanel.add(rankBtn);
        buttonPanel.add(wordBtn);
        buttonPanel.add(wordListBtn);
        buttonPanel.add(exitBtn);

        // 버튼 컨테이너
        JPanel btnContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnContainer.setOpaque(false);
        btnContainer.add(buttonPanel);

        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(namePanel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(btnContainer);

        add(title, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // ── 이벤트 리스너 ──

        // 게임 시작
        startBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "이름을 입력해주세요!");
                return;
            }
            frame.startGame(name);
        });

        // 명예의 전당 -> 패널 전환
        rankBtn.addActionListener(e -> frame.showRankingPanel());

        // 단어 추가
        wordBtn.addActionListener(e -> {
            String newWord = JOptionPane.showInputDialog(this, "추가할 단어:");
            if (newWord != null && !newWord.trim().isEmpty()) {
                WordManager.getInstance().addWord(newWord.toUpperCase());
                JOptionPane.showMessageDialog(this, "단어 저장 완료!");
            }
        });

        // 저장된 단어 목록 보기
        wordListBtn.addActionListener(e -> showWordListDialog());

        // 종료
        exitBtn.addActionListener(e -> System.exit(0));

        // 엔터 키로 게임 시작
        nameField.addActionListener(e -> startBtn.doClick());
    }

    /** 저장된 단어들을 한 눈에 볼 수 있는 팝업 다이얼로그 */
    private void showWordListDialog() {
        java.util.List<String> words = WordManager.getInstance().getAllWords();

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "저장된 단어",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String w : words) {
            model.addElement(w);
        }
        JList<String> list = new JList<>(model);
        list.setFont(new Font("맑은 고딕", Font.PLAIN, 18));

        JScrollPane scrollPane = new JScrollPane(list);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel("현재 저장된 단어 목록", SwingConstants.CENTER);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        mainPanel.add(label, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("닫기");
        closeBtn.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.add(closeBtn);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        btn.setPreferredSize(new Dimension(250, 60));
        btn.setFocusPainted(true);

        // 탭 이동 후 엔터키 지원
        btn.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btn.doClick();
                }
            }
        });
        return btn;
    }
}
