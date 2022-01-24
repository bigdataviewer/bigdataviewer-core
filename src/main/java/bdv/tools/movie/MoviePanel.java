package bdv.tools.movie;

import javax.swing.*;
import java.awt.*;

public class MoviePanel extends JPanel {

    public MoviePanel() {
        super();
        setSize(800,200);
        setPreferredSize(new Dimension(800,200));
    }

    public static void main(String[] args) {
        JFrame mainFrame = new JFrame();

        mainFrame.setSize(800,200);
        mainFrame.add(new MoviePanel());
        mainFrame.setVisible(true);
    }
}
