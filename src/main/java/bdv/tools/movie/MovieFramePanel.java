package bdv.tools.movie;

import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class MovieFramePanel extends JPanel {
    final private MovieFrame movieFrame;
    final private ImagePanel image;
    private JTextField framesField;
    private JTextField accelField;


    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        super();
        this.movieFrame = new MovieFrame(position, transform);
        this.image = image;
        initView();
    }

    public MovieFramePanel(MovieFrame movieFrame, ImagePanel image) {
        super();
        this.movieFrame = movieFrame;
        this.image = image;
        initView();
    }


    private void initView() {
        Border blackline = BorderFactory.createLineBorder(Color.lightGray);
        TitledBorder title = BorderFactory.createTitledBorder(blackline, String.valueOf(movieFrame.getPosition()));
        title.setTitleJustification(TitledBorder.CENTER);
        setBorder(title);
        setPreferredSize(new Dimension(120, 180));
        if (image != null)
            add(image);
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2));
        framesField = new JTextField(String.valueOf(movieFrame.getFrames()));
        framesField.setFont(new Font("Serif", Font.PLAIN, 9));
        accelField = new JTextField(String.valueOf(movieFrame.getAccel()));
        accelField.setFont(new Font("Serif", Font.PLAIN, 9));
        JLabel framesLabel = new JLabel("Frames: ");
        framesLabel.setFont(new Font("Serif", Font.PLAIN, 8));
        JLabel accelLabel = new JLabel("Accel: ");
        accelLabel.setFont(new Font("Serif", Font.PLAIN, 8));
        fieldsPanel.add(framesLabel);
        fieldsPanel.add(framesField);
        fieldsPanel.add(accelLabel);
        fieldsPanel.add(accelField);
        add(fieldsPanel);
    }

    public ImagePanel getImage() {
        return image;
    }

    public MovieFramePanel updateFields() {
        try {
            int accel = Integer.valueOf(accelField.getText());
            movieFrame.setAccel(accel);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Invalid value :" + accelField.getText());
        }
        try {
            int frames = Integer.valueOf(framesField.getText());
            movieFrame.setFrames(frames);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Invalid value :" + framesField.getText());
        }
        return this;
    }

    public int getAccel() {
        updateFields();
        return movieFrame.getAccel();
    }

    public int getFrames() {
        updateFields();
        return movieFrame.getFrames();
    }

    public MovieFrame getMovieFrame() {
        return movieFrame;
    }
}
