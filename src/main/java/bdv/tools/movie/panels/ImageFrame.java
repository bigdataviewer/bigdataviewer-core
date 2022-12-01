package bdv.tools.movie.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageFrame extends JFrame {
    private BufferedImage image;

    public ImageFrame() throws HeadlessException {
        super();
        setSize(new Dimension(420, 420));
    }

    @Override
    public void paint(Graphics g) {
        if (image != null)
            g.drawImage(image, 10, 10, this);
        else
            super.paint(g);
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    public void setImage(BufferedImage image) {
        this.image = resize(image, 400, 400);
        if (!isVisible())
            setVisible(true);
        SwingUtilities.updateComponentTreeUI(this);
    }

    public static void main(String[] args) {
        new ImageFrame();
    }
}
