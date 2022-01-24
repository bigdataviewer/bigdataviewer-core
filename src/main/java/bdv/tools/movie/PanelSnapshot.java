package bdv.tools.movie;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class PanelSnapshot {
    public static BufferedImage takeSnapShot(JPanel panel){
        BufferedImage bufImage = new BufferedImage(panel.getSize().width, panel.getSize().height,BufferedImage.TYPE_INT_RGB);
        panel.paint(bufImage.createGraphics());

       return bufImage;
    }

    public static void showPanel(JPanel panel){
        JFrame frame = new JFrame();
        frame.setSize(panel.getSize());
        ImagePanel imagePanel = new ImagePanel(takeSnapShot(panel));
        frame.add(imagePanel);
        frame.setVisible(true);
    }
}
