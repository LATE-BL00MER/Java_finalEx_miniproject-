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
        setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 타이틀
        JLabel title = new JLabel("좀비게임FPS", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 60));

        // 가운데 패널
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 이름 입력
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel nameLabel = new JLabel("이름: ");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        nameField = new JTextField(15);
        nameField.setFont(new Font("맑은 고딕", Font.PLAIN, 20));
        namePanel.add(nameLabel);
        namePanel.add(nameField);

        // 버튼들
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 15, 15));
        JButton startBtn = createStyledButton("게임 시작");
        JButton rankBtn  = createStyledButton("명예의 전당");
        JButton wordBtn  = createStyledButton("단어 저장");
        JButton exitBtn  = createStyledButton("종료");

        buttonPanel.add(startBtn);
        buttonPanel.add(rankBtn);
        buttonPanel.add(wordBtn);
        buttonPanel.add(exitBtn);

        // 버튼 컨테이너
        JPanel btnContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnContainer.add(buttonPanel);

        centerPanel.add(Box.createVerticalStrut(50));
        centerPanel.add(namePanel);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(btnContainer);

        add(title, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // ── 이벤트 리스너 ──

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

        wordBtn.addActionListener(e -> {
            String newWord = JOptionPane.showInputDialog(this, "추가할 단어 (영문):");
            if (newWord != null && !newWord.trim().isEmpty()) {
                WordManager.getInstance().addWord(newWord.toUpperCase());
                JOptionPane.showMessageDialog(this, "단어 저장 완료!");
            }
        });

        exitBtn.addActionListener(e -> System.exit(0));

        // 엔터 키로 게임 시작
        nameField.addActionListener(e -> startBtn.doClick());
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