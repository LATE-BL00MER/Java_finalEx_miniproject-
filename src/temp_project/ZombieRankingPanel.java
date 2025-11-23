package temp_project;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ZombieRankingPanel extends JPanel {
    private final ZombieFrame frame;
    private final JTextArea rankingArea;

    public ZombieRankingPanel(ZombieFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(20, 20, 30)); // 어두운 배경

        // ── 타이틀 ──
        JLabel title = new JLabel("🏆 명예의 전당 🏆", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 48));
        title.setForeground(Color.YELLOW);
        title.setBorder(BorderFactory.createEmptyBorder(50, 0, 30, 0));
        add(title, BorderLayout.NORTH);

        // ── 랭킹 리스트 영역 ──
        rankingArea = new JTextArea();
        rankingArea.setEditable(false);
        rankingArea.setFont(new Font("Monospaced", Font.BOLD, 24));
        rankingArea.setBackground(new Color(40, 40, 50));
        rankingArea.setForeground(Color.WHITE);
        rankingArea.setMargin(new Insets(30, 100, 30, 100)); // 여백

        // 스크롤바 커스터마이징은 생략하고 기본 사용
        JScrollPane scrollPane = new JScrollPane(rankingArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        // ── 하단 뒤로가기 버튼 ──
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 50, 0));

        JButton backBtn = new JButton("뒤로가기");
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        backBtn.setPreferredSize(new Dimension(200, 60));
        backBtn.setFocusPainted(false);

        // 버튼 클릭 시 시작 화면으로
        backBtn.addActionListener(e -> frame.showStartPanel());

        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** 화면이 열릴 때마다 랭킹 정보를 갱신해서 보여줌 */
    public void updateRanking() {
        List<ScoreManager.ScoreEntry> list = ScoreManager.getInstance().getTopScores();
        StringBuilder sb = new StringBuilder();

        if (list.isEmpty()) {
            sb.append("\n\n        아직 등록된 기록이 없습니다.\n");
        } else {
            int rank = 1;
            sb.append(String.format("%-6s  %-15s  %s\n", "순위", "이름", "점수"));
            sb.append("----------------------------------------\n");
            for (ScoreManager.ScoreEntry entry : list) {
                sb.append(String.format(" %-6d  %-15s  %5d점\n", rank++, entry.name, entry.score));
            }
        }
        rankingArea.setText(sb.toString());
    }
}