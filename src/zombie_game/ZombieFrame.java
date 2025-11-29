package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 메인 프레임
 * - START / GAME / RANK 3개 화면을 CardLayout으로 전환
 */
public class ZombieFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel   = new JPanel(cardLayout);

    private final ZombieStartPanel   startPanel;
    private final ZombieGamePanel    gamePanel;
    private final ZombieRankingPanel rankingPanel;

    public ZombieFrame() {
        setTitle("타이핑 좀비 FPS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 패널 생성
        startPanel   = new ZombieStartPanel(this);
        gamePanel    = new ZombieGamePanel(this);
        rankingPanel = new ZombieRankingPanel(this);

        // 카드 레이아웃에 등록
        cardPanel.add(startPanel,   "START");
        cardPanel.add(gamePanel,    "GAME");
        cardPanel.add(rankingPanel, "RANK");

        setContentPane(cardPanel);

        // 처음 화면은 START
        showStartPanel();

        setVisible(true);
    }

    /** 시작 화면으로 전환 */
    public void showStartPanel() {
        cardLayout.show(cardPanel, "START");
    }

    /** 게임 화면으로 전환 + 새 게임 시작 */
    public void showGamePanel(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Player";
        }

        // ★ 더 이상 setPlayerName() 호출 안 함
        gamePanel.startNewGame(playerName);

        cardLayout.show(cardPanel, "GAME");
    }

    /** 랭킹 화면으로 전환 */
    public void showRankingPanel() {
        rankingPanel.refreshTable();           // 전환 전에 목록 갱신
        cardLayout.show(cardPanel, "RANK");
    }

    // ====================================================
    //                단어 저장 / 불러오기
    // ====================================================

    /** word.txt 에 단어 추가 (StartPanel에서 호출) */
    public void showWordSaveDialog() {
        WordManager wm = WordManager.getInstance();

        while (true) {
            String input = JOptionPane.showInputDialog(
                    this,
                    "추가할 단어를 입력하세요 (취소: 종료)",
                    "단어 저장",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (input == null) {      // 취소
                break;
            }

            input = input.trim();
            if (input.isEmpty()) {
                continue;
            }

            wm.addWord(input);
        }

        JOptionPane.showMessageDialog(
                this,
                "단어가 저장되었습니다.",
                "알림",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /** word.txt 전체 목록 보기 */
    public void showWordListDialog() {
        WordManager wm = WordManager.getInstance();
        List<String> list = wm.getAllWords();

        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "등록된 단어가 없습니다.",
                    "단어 목록",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String w : list) {
            sb.append(w).append('\n');
        }

        JTextArea area = new JTextArea(sb.toString(), 20, 30);
        area.setEditable(false);
        area.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

        JScrollPane scroll = new JScrollPane(area);

        JOptionPane.showMessageDialog(
                this,
                scroll,
                "단어 목록",
                JOptionPane.PLAIN_MESSAGE
        );
    }
}
