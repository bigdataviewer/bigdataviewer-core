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
    private final JTextField framesField;
    private final JTextField accelField;
    private final static int  DEFAULT_FRAMES = 120;
    private final static int  DEFAULT_ACCEL = 0;


    public MovieFramePanel(AffineTransform3D transform, ImagePanel image, int position) {
        super();
        Border blackline = BorderFactory.createLineBorder(Color.lightGray);
        TitledBorder title = BorderFactory.createTitledBorder( blackline, String.valueOf(position));
        title.setTitleJustification(TitledBorder.CENTER);
        setBorder(title);
//        setBackground(Color.magenta);
//        setSize(new Dimension(100,180));
        setPreferredSize(new Dimension(120,180));
        add(image);
        JPanel fieldsPanel = new JPanel(new GridLayout(2,2));
        int framesValue = (position == 0 )? 0 : DEFAULT_FRAMES;
        int accelValue = (position == 0 )? 0 : DEFAULT_ACCEL;
        framesField = new JTextField(String.valueOf(framesValue));
        framesField.setFont(new Font("Serif", Font.PLAIN, 9));
        accelField = new JTextField(String.valueOf(accelValue));
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

    public int getAccel() {
        try{
            int accel = Integer.valueOf(accelField.getText());
            return accel;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Invalid value :"+accelField.getText());
        }
        return (position == 0 )? 0 : DEFAULT_ACCEL ;
    }

    public int getFrames() {
        try{
            int frames = Integer.valueOf(framesField.getText());
            return frames;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Invalid value :"+framesField.getText());
        }
        return (position == 0 )? 0 : DEFAULT_FRAMES ;
    }
}
