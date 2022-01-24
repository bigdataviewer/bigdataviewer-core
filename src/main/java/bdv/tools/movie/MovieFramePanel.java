package bdv.tools.movie;

import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;

public class MovieFramePanel extends JPanel {
    final private AffineTransform3D transform;
    final private ImagePanel image;
    final private int position;

    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        this.transform = transform;
        this.image = image;
        this.position = position;
    }
}
