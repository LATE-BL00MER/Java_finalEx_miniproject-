package zombie_game;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 메인 프레임
 * - START / GAME / RANK 3개 화면을 CardLayout으로 전환
 * - BGM은 Frame에서 단일 관리 (Start/Game 공통)
 */
public class ZombieFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private final ZombieStartPanel startPanel;
    private final ZombieGamePanel gamePanel;
    private final ZombieRankingPanel rankingPanel;

    // ================= BGM (Start/Game 공통) =================
    private final AudioPlayer bgmPlayer;
    private boolean bgmMuted = false;

    public ZombieFrame() {
        setTitle("타이핑 좀비 FPS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 패널 생성
        startPanel   = new ZombieStartPanel(this);
        gamePanel    = new ZombieGamePanel(this);
        rankingPanel = new ZombieRankingPanel(this);

        // 카드 레이아웃 등록
        cardPanel.add(startPanel, "START");
        cardPanel.add(gamePanel, "GAME");
        cardPanel.add(rankingPanel, "RANK");
        setContentPane(cardPanel);

        // --------- BGM: 여기서 1번만 시작 ---------
        bgmPlayer = new AudioPlayer("bgm.wav");
        bgmPlayer.playLoop();            // 루프는 1번만 걸어두고
        if (bgmMuted) bgmPlayer.pause();  // muted면 바로 pause

        showStartPanel();
        setVisible(true);
    }

    /** 시작 화면으로 전환 */
    public void showStartPanel() {
        // Start로 돌아올 때, muted가 아니면 재개
        if (!bgmMuted && bgmPlayer != null) bgmPlayer.resume();

        // StartPanel 아이콘 상태 동기화
        startPanel.syncSoundIcon();
        startPanel.resetFields();

        cardLayout.show(cardPanel, "START");
    }

    /** 게임 화면으로 전환 + 새 게임 시작 */
    public void showGamePanel(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Player";
        }

        gamePanel.startNewGame(playerName);
        cardLayout.show(cardPanel, "GAME");
    }

    /** 랭킹 화면으로 전환 */
    public void showRankingPanel() {
        rankingPanel.refreshTable();
        cardLayout.show(cardPanel, "RANK");
    }

    // ====================================================
    //                단어 저장 / 불러오기
    // ====================================================

    /** word.txt 에 단어 추가 (StartPanel에서 호출) */
    public void showWordSaveDialog() {
        while (true) {
            JTextField field = new JTextField(16);

            JButton okBtn = new JButton("저장");
            JButton cancelBtn = new JButton("취소");

            JOptionPane pane = new JOptionPane(
                    new Object[]{"추가할 단어를 입력하세요:", field},
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION,
                    null,
                    new Object[]{okBtn, cancelBtn},
                    okBtn
            );

            JDialog dialog = pane.createDialog(this, "단어 저장");
            dialog.getRootPane().setDefaultButton(okBtn); // ✅ Enter = 저장

            okBtn.addActionListener(e -> { pane.setValue(okBtn); dialog.dispose(); });
            cancelBtn.addActionListener(e -> { pane.setValue(cancelBtn); dialog.dispose(); });

            dialog.setVisible(true);

            Object value = pane.getValue();
            if (value != okBtn) {
                // ✅ 취소면 즉시 종료 (저장 알림 안 뜸)
                return;
            }

            String input = field.getText();
            if (input == null) continue;
            input = input.trim();

            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "빈 단어는 저장할 수 없습니다.",
                        "알림",
                        JOptionPane.WARNING_MESSAGE
                );
                // ✅ 다시 입력 받기(루프 계속)
                continue;
            }

            // ✅ 저장
            WordManager.getInstance().addWord(input);

            // ✅ 저장 즉시 알림
            JOptionPane.showMessageDialog(
                    this,
                    "단어가 저장되었습니다: " + input,
                    "알림",
                    JOptionPane.INFORMATION_MESSAGE
            );

            return; // ✅ 한 번 저장하면 종료 (원하면 continue로 바꾸면 연속 입력 가능)
        }
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
        for (String w : list) sb.append(w).append('\n');

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

    // ====================================================
    //                    BGM 제어
    // ====================================================

    public boolean isBgmMuted() {
        return bgmMuted;
    }

    /** StartPanel에서 아이콘 클릭하면 이거 호출 */
    public void toggleBgmMute() {
        setBgmMuted(!bgmMuted);
    }

    public void setBgmMuted(boolean muted) {
        this.bgmMuted = muted;
        if (bgmPlayer == null) return;

        if (bgmMuted) bgmPlayer.pause();
        else bgmPlayer.resume();

        // StartPanel 아이콘 동기화
        startPanel.syncSoundIcon();
    }

    /** (GamePanel ESC 일시정지 메뉴에서 필요하면 사용) */
    public void pauseBgmForPauseMenu() {
        if (!bgmMuted && bgmPlayer != null) bgmPlayer.pause();
    }

    /** (GamePanel ESC 일시정지 메뉴에서 필요하면 사용) */
    public void resumeBgmForPauseMenu() {
        if (!bgmMuted && bgmPlayer != null) bgmPlayer.resume();
    }

    public AudioPlayer getBgmPlayer() {
        return bgmPlayer;
    }
}
