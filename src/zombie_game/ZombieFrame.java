package zombie_game;

import javax.swing.*;
import java.awt.*;

public class ZombieFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private final ZombieStartPanel startPanel;
    private final ZombieGamePanel gamePanel;
    private final ZombieRankingPanel rankingPanel; // 추가됨

    public ZombieFrame() {
        setTitle("좀비게임FPS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 패널 생성
        startPanel = new ZombieStartPanel(this);
        gamePanel = new ZombieGamePanel(this);
        rankingPanel = new ZombieRankingPanel(this); // 추가됨

        // 카드 레이아웃에 패널들 추가
        cardPanel.add(startPanel, "START");
        cardPanel.add(gamePanel, "GAME");
        cardPanel.add(rankingPanel, "RANK"); // 추가됨

        setContentPane(cardPanel);
        setVisible(true);
    }

    public void showStartPanel() {
        cardLayout.show(cardPanel, "START");
    }

    public void startGame(String playerName) {
        gamePanel.startNewGame(playerName);
        cardLayout.show(cardPanel, "GAME");
    }

    // 명예의 전당 화면 보여주기
    public void showRankingPanel() {
        rankingPanel.updateRanking(); // 데이터 갱신 후 표시
        cardLayout.show(cardPanel, "RANK");
    }
}