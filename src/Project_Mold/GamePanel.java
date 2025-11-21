package Project_Mold;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel {
    private JLabel fallingLabel = new JLabel("");
    private GroundPanel groundPanel = new GroundPanel();
    private ScorePanel scorePanel = null;
    private TextStore tStore = null;
    private FallingThread fThread = new FallingThread();

    public GamePanel(ScorePanel scorePanel, TextStore tStore) {
        this.scorePanel = scorePanel;
        this.tStore = tStore;
        setLayout(new BorderLayout());
        add(new InputPanel(), BorderLayout.SOUTH);
        add(groundPanel, BorderLayout.CENTER);
    }

    // start() ~~
    public void start() {
        fallingLabel.setVisible(true);
        String text = tStore.get(); // 새 단어 받기
        fallingLabel.setText(text);
        fThread.start();
    }

    class FallingThread extends Thread {
        @Override
        public void run() {
            while(true) {
                try {
                    sleep(300);
                    int x = fallingLabel.getX();
                    int y = fallingLabel.getY();

                    fallingLabel.setLocation(x, y + 10);

                } catch (InterruptedException e) { //종료시키려는 사건이 발생할때
                    e.printStackTrace();
                }
            }
        }
    }

    class GroundPanel extends JPanel {
        public GroundPanel() {
            this.setBackground(Color.WHITE);
            this.setLayout(null);

            fallingLabel.setSize(100, 20);
            fallingLabel.setLocation(100, 100);
            fallingLabel.setVisible(false);
            add(fallingLabel);
        }
    }

    class InputPanel extends JPanel {
        private JTextField inputField = new JTextField(10);

        public InputPanel() {
            this.setBackground(Color.GRAY);
            add(inputField);
            inputField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JTextField tf = (JTextField) (e.getSource());
                    String userText = tf.getText();
                    if (userText.equals(fallingLabel.getText())) {
                        scorePanel.increase();
                        String text = tStore.get(); // 새 단어 받기
                        fallingLabel.setText(text);
                        fallingLabel.setLocation(100, 50);
                        tf.setText("");
                    }
                }
            });
        }
    }
}
