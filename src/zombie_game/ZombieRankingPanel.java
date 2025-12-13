package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ZombieRankingPanel extends JPanel {

    private final ZombieFrame frame;
    private final JPanel listPanel;

    public ZombieRankingPanel(ZombieFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 30));

        // ===== 상단 타이틀 =====
        JLabel title = new JLabel("🏆 명예의 전당 🏆", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 36));
        title.setForeground(new Color(255, 215, 0));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // ===== 리스트 패널 =====
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(scroll);

        add(centerWrap, BorderLayout.CENTER);

        // ===== 하단 버튼 =====
        JButton backBtn = new JButton("◀ 메인으로");
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        backBtn.setFocusPainted(false);
        backBtn.addActionListener(e -> frame.showStartPanel());

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        bottom.add(backBtn);

        add(bottom, BorderLayout.SOUTH);
    }

    /** 랭킹 갱신 */
    public void refreshTable() {
        listPanel.removeAll();

        // ✅ ScoreManager 구조에 맞춤: 상위 10개 가져오기
        List<ScoreManager.ScoreEntry> list = ScoreManager.getInstance().getTopScores(10);

        // 헤더
        listPanel.add(createRow("순위", "이름", "점수", true));

        int rank = 1;
        for (ScoreManager.ScoreEntry e : list) {
            listPanel.add(createRow(
                    String.valueOf(rank),
                    e.name,
                    e.score + " 점",
                    false
            ));
            rank++;
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /** 한 줄(row) */
    private JPanel createRow(String rank, String name, String score, boolean header) {
        JPanel row = new JPanel(new GridLayout(1, 3));
        row.setMaximumSize(new Dimension(600, 45));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        Font font = header
                ? new Font("맑은 고딕", Font.BOLD, 18)
                : new Font("맑은 고딕", Font.PLAIN, 17);

        Color color = header ? Color.LIGHT_GRAY : Color.WHITE;

        // Top 3 강조
        if (!header) {
            int r = Integer.parseInt(rank);
            if (r == 1) color = new Color(255, 215, 0);      // gold
            else if (r == 2) color = new Color(192, 192, 192); // silver
            else if (r == 3) color = new Color(205, 127, 50);  // bronze
        }

        row.add(makeLabel(rank, font, color, SwingConstants.CENTER));
        row.add(makeLabel(name, font, color, SwingConstants.CENTER));
        row.add(makeLabel(score, font, color, SwingConstants.CENTER));

        return row;
    }

    private JLabel makeLabel(String text, Font font, Color color, int align) {
        JLabel label = new JLabel(text, align);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
}
