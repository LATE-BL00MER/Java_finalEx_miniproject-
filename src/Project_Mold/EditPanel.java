package Project_Mold;

import javax.swing.*;
import java.awt.*;

public class EditPanel extends JPanel {
    private JTextField inputField = new JTextField(10);
    private JButton saveBtn = new JButton("Save");

    public EditPanel() {
        this.setBackground(Color.CYAN);
        add(inputField);
        add(saveBtn);
    }
}
