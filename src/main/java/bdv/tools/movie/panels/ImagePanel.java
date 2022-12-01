package bdv.tools.movie.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {
    private final BufferedImage image;

    public ImagePanel(BufferedImage image) {
        super();
        setPreferredSize(new Dimension(90, 90));
        this.image = scale(image, 90, 90);
    }

    public static ImagePanel snapshotOf(JPanel panel) {
        return new ImagePanel(takeSnapShot(panel));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
    }

    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int x, y;
        int ww = src.getWidth();
        int hh = src.getHeight();
        int[] ys = new int[h];
        for (y = 0; y < h; y++)
            ys[y] = y * hh / h;
        for (x = 0; x < w; x++) {
            int newX = x * ww / w;
            for (y = 0; y < h; y++) {
                int col = src.getRGB(newX, ys[y]);
                img.setRGB(x, y, col);
            }
        }
        return img;
    }

    public static BufferedImage takeSnapShot(JPanel panel){
        BufferedImage bufImage = new BufferedImage(panel.getSize().width, panel.getSize().height,BufferedImage.TYPE_INT_RGB);
        panel.paint(bufImage.createGraphics());
        return bufImage;
    }
}
