package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 랭킹 화면
 */
public class ZombieRankingPanel extends JPanel {

    private final ZombieFrame frame;
    private final JTextArea rankingArea;

    public ZombieRankingPanel(ZombieFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 30));   // 어두운 배경

        // ── 타이틀 ──
        JLabel title = new JLabel("🧟 명예의 전당 🧟", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        title.setForeground(Color.YELLOW);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        // ── 랭킹 영역 ──
        rankingArea = new JTextArea();
        rankingArea.setEditable(false);
        rankingArea.setFont(new Font("맑은 고딕", Font.PLAIN, 18));
        rankingArea.setBackground(new Color(40, 40, 60));
        rankingArea.setForeground(Color.WHITE);
        rankingArea.setMargin(new Insets(10, 20, 10, 20));

        JScrollPane scrollPane = new JScrollPane(rankingArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        add(scrollPane, BorderLayout.CENTER);

        // ── 하단 버튼 ──
        JButton backBtn = new JButton("◀ 메인으로");
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        backBtn.addActionListener(e -> frame.showStartPanel());

        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(20, 20, 30));
        bottom.add(backBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    /** ScoreManager 에서 점수 읽어서 텍스트 갱신 */
    public void refreshTable() {
        List<ScoreManager.ScoreEntry> list =
                ScoreManager.getInstance().getAllScores();

        StringBuilder sb = new StringBuilder();

        if (list.isEmpty()) {
            sb.append("\n\n        아직 등록된 기록이 없습니다.\n");
        } else {
            int rank = 1;
            sb.append(String.format("%-6s  %-15s  %s\n", "순위", "이름", "점수"));
            sb.append("----------------------------------------\n");
            for (ScoreManager.ScoreEntry entry : list) {
                sb.append(String.format(" %-6d  %-15s  %5d점\n",
                        rank++, entry.name, entry.score));
            }
        }

        rankingArea.setText(sb.toString());
        rankingArea.setCaretPosition(0);
    }
}
