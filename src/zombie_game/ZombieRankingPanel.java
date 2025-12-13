package zombie_game;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ZombieRankingPanel extends JPanel {

    private final ZombieFrame frame;

    // 중앙 랭킹 리스트를 담는 패널
    private final JPanel listPanel;

    public ZombieRankingPanel(ZombieFrame frame) {
        this.frame = frame;

        setLayout(new BorderLayout());
        setBackground(new Color(10, 10, 18));

        // ===================== 상단 타이틀 =====================
        JLabel title = new JLabel("※ 명예의 전당 ※", SwingConstants.CENTER);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 44));
        title.setForeground(new Color(255, 215, 0));
        title.setBorder(new EmptyBorder(30, 10, 20, 10));
        add(title, BorderLayout.NORTH);

        // ===================== 중앙 영역(헤더 + 리스트) =====================
        JPanel centerWrap = new JPanel();
        centerWrap.setOpaque(false);
        centerWrap.setLayout(new BorderLayout());
        centerWrap.setBorder(new EmptyBorder(10, 120, 20, 120)); // 좌우 여백

        // (1) 헤더 (순위/이름/점수 크게)
        JPanel header = createHeaderRow();
        centerWrap.add(header, BorderLayout.NORTH);

        // (2) 랭킹 리스트
        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(18, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        // 스크롤 배경 투명 처리
        JViewport vp = scroll.getViewport();
        vp.setOpaque(false);

        centerWrap.add(scroll, BorderLayout.CENTER);
        add(centerWrap, BorderLayout.CENTER);

        // ===================== 하단 버튼 =====================
        JButton backBtn = new JButton("◀ 메인으로");
        backBtn.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        backBtn.setPreferredSize(new Dimension(220, 55));
        backBtn.addActionListener(e -> frame.showStartPanel());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 10, 25, 10));
        bottom.add(backBtn);

        add(bottom, BorderLayout.SOUTH);

        // 처음 한 번 갱신
        refreshTable();
    }

    // ===================== 랭킹 갱신 =====================
    public void refreshTable() {
        listPanel.removeAll();

        List<ScoreManager.ScoreEntry> list =
                ScoreManager.getInstance().getTopScores(10);

        if (list == null || list.isEmpty()) {
            JLabel empty = new JLabel("아직 기록이 없습니다. 첫 기록의 주인공이 되어보세요!");
            empty.setFont(new Font("맑은 고딕", Font.BOLD, 20));
            empty.setForeground(new Color(220, 220, 220));
            empty.setBorder(new EmptyBorder(40, 0, 0, 0));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);

            revalidate();
            repaint();
            return;
        }

        int rank = 1;
        for (ScoreManager.ScoreEntry e : list) {
            JPanel row = createScoreRow(rank, e.name, e.score);
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(10));
            rank++;
        }

        revalidate();
        repaint();
    }

    // ===================== UI 컴포넌트 생성 =====================
    private JPanel createHeaderRow() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(true);
        p.setBackground(new Color(20, 20, 35));
        p.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel a = headerLabel("순위");
        JLabel b = headerLabel("이름");
        JLabel c = headerLabel("점수");

        p.add(a);
        p.add(b);
        p.add(c);

        // 살짝 둥글게 보이는 느낌(진짜 라운드는 아니지만 깔끔)
        p.setMaximumSize(new Dimension(900, 60));
        return p;
    }

    private JLabel headerLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("맑은 고딕", Font.BOLD, 26)); // ✅ 크게
        l.setForeground(new Color(245, 245, 245));
        return l;
    }

    private JPanel createScoreRow(int rank, String name, int score) {
        JPanel card = new JPanel(new GridLayout(1, 3, 10, 0));
        card.setOpaque(true);

        // 기본 카드 색
        Color base = new Color(28, 28, 48);
        card.setBackground(base);
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        card.setMaximumSize(new Dimension(900, 58));

        // 상위 3등 강조(촌스럽지 않게 “글씨색”만 살짝)
        Color rankColor = new Color(235, 235, 235);
        if (rank == 1) rankColor = new Color(255, 215, 0);      // Gold
        else if (rank == 2) rankColor = new Color(200, 200, 210); // Silver
        else if (rank == 3) rankColor = new Color(205, 127, 50);  // Bronze

        JLabel a = rowLabel(String.valueOf(rank), rankColor, true);
        JLabel b = rowLabel(name, new Color(240, 240, 240), false);
        JLabel c = rowLabel(score + " 점", new Color(240, 240, 240), false);

        card.add(a);
        card.add(b);
        card.add(c);

        // 마우스 올리면 살짝 밝아지는 느낌(선택사항이지만 UI 퀄리티 올라감)
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(36, 36, 60));
                card.repaint();
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(base);
                card.repaint();
            }
        });

        return card;
    }

    private JLabel rowLabel(String text, Color color, boolean bold) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("맑은 고딕", bold ? Font.BOLD : Font.PLAIN, 22)); // ✅ 본문도 크게
        l.setForeground(color);
        return l;
    }
}
