package bdv.tools.movie.panels;

import bdv.tools.movie.preview.MovieFrameInst;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class MovieFramePanel extends JPanel {
    final private MovieFrameInst movieFrame;
    final private ImagePanel image;
    private JTextField framesField;
    private JComboBox<String> accelField;
    private final String[] ACCELS = new String[]{"symmetric", "slow start", "slow end", "soft symmetric", "soft slow start", "soft slow end"};

    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        super();
        this.movieFrame = new MovieFrameInst(position, transform);
        this.image = image;
        initView();
    }

    public MovieFramePanel(MovieFrameInst movieFrame, ImagePanel image) {
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
        setPreferredSize(new Dimension(140, 180));
        if (image != null)
            add(image);
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 1));
        framesField = new JTextField(String.valueOf(movieFrame.getFrames()));
        framesField.setFont(new Font("Serif", Font.PLAIN, 9));
        accelField = new JComboBox<>(ACCELS);
        accelField.setSelectedIndex(movieFrame.getAccel());
        accelField.setFont(new Font("Serif", Font.PLAIN, 9));
        JLabel framesLabel = new JLabel("Frames: ");
        framesLabel.setFont(new Font("Serif", Font.PLAIN, 8));
        JPanel framePanel = new JPanel(new GridLayout(1, 2));
        framePanel.add(framesLabel);
        framePanel.add(framesField);
        fieldsPanel.add(framePanel);
        fieldsPanel.add(accelField);
        add(fieldsPanel);
    }

    public ImagePanel getImage() {
        return image;
    }

    public MovieFramePanel updateFields() {
        try {
            int accel = accelField.getSelectedIndex();
            movieFrame.setAccel(accel);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Invalid value :" + accelField.getSelectedItem());
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

    public MovieFrameInst getMovieFrame() {
        updateFields();
        return movieFrame;
    }
}
