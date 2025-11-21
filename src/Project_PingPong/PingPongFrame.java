package Project_PingPong;

import javax.swing.*;

public class PingPongFrame extends JFrame {

    public PingPongFrame() {
        setTitle("★내일은 탁구왕!★");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        add(new PingPongPanel());   // 탁구대 패널 붙이기

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PingPongFrame::new);
    }
}
