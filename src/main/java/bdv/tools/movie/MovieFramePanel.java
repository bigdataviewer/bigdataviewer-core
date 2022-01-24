package bdv.tools.movie;

import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import java.awt.*;

public class MovieFramePanel extends JPanel {
    final private AffineTransform3D transform;
    final private ImagePanel image;
    final private int position;

    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        super();
        setBackground(Color.magenta);
//        setSize(new Dimension(100,180));
        setPreferredSize(new Dimension(100,180));
        add(image);
        this.transform = transform;
        this.image = image;
        this.position = position;
    }
}
