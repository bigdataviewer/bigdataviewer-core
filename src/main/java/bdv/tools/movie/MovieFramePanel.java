package bdv.tools.movie;

import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class MovieFramePanel extends JPanel {
    final private AffineTransform3D transform;
    final private ImagePanel image;
    final private int position;

    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        super();
        Border blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder title = BorderFactory.createTitledBorder( blackline, String.valueOf(position));
        title.setTitleJustification(TitledBorder.CENTER);
        setBorder(title);
//        setBackground(Color.magenta);
//        setSize(new Dimension(100,180));
        setPreferredSize(new Dimension(100,150));
        add(image);
        this.transform = transform;
        this.image = image;
        this.position = position;
    }

    public AffineTransform3D getTransform() {
        return transform;
    }

    public ImagePanel getImage() {
        return image;
    }

    public int getPosition() {
        return position;
    }
}
