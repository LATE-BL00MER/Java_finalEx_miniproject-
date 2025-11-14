import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class GameFrame  extends JFrame{
    private TextStore tStore = new TextStore();
    private JMenuItem startItem = new JMenuItem("Start");
    private JButton startBtn = new JButton("Start");
    private ScorePanel scorePanel = new ScorePanel();
    private EditPanel editPanel = new EditPanel();
    private GamePanel gamePanel = new GamePanel(scorePanel, tStore);

    //이미지 로딩
    ImageIcon normalIcon = new ImageIcon("images/normal.png");
    ImageIcon pressedIcon = new ImageIcon("images/pressed.png");
    ImageIcon rolloverIcon = new ImageIcon("images/rollover.png");

    public GameFrame() {
        super("게임");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        makeMenu();
        makeToolBar();
        makeSplit();
        setVisible(true);
    }

    private void makeMenu() {
        JMenuBar mBar = new JMenuBar();
        this.setJMenuBar(mBar);

        JMenu fileMenu = new JMenu("File");
        mBar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        mBar.add(editMenu);

        JMenuItem openItem = new JMenuItem("Open");
        fileMenu.add(openItem);
        fileMenu.add("Save");
        fileMenu.addSeparator();
        fileMenu.add(startItem);

        startItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });
    }

    private void start() {
        gamePanel.start();
    }

    private void makeToolBar() {
        JToolBar tBar = new JToolBar();
        tBar.add(startBtn);
        tBar.setFloatable(false);
        getContentPane().add(tBar, BorderLayout.NORTH);
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });

        JButton b = new JButton(normalIcon);
        b.setPressedIcon(pressedIcon);
        b.setRolloverIcon(rolloverIcon);
        tBar.add(b);
    }


    private void makeSplit() {
        JSplitPane hPane = new JSplitPane();
        hPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        hPane.setDividerLocation(500);
        hPane.setEnabled(false);
        getContentPane().add(hPane, BorderLayout.CENTER);

        JSplitPane vPane = new JSplitPane();
        vPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        vPane.setDividerLocation(250);
        vPane.setEnabled(false);
        vPane.setTopComponent(scorePanel);
        vPane.setBottomComponent(editPanel);
        hPane.setRightComponent(vPane);
        hPane.setLeftComponent(gamePanel);
    }
}
