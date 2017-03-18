package bdv.viewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.metal.MetalSliderUI;

public class JPlaySlider extends JSlider{

	public class CustomSliderUI extends MetalSliderUI {

	    private BasicStroke stroke = new BasicStroke(1f, BasicStroke.CAP_ROUND, 
	            BasicStroke.JOIN_ROUND, 0f, new float[]{1f, 2f}, 0f);

	    public CustomSliderUI(JSlider b) {
	        super();

	    }
	    


	    
	    

	    /*
	    @Override
	    public void paint(Graphics g, JComponent c) {
	        Graphics2D g2d = (Graphics2D) g;
	        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
	                RenderingHints.VALUE_ANTIALIAS_ON);
	        super.paint(g, c);
	    }

	    /*
	    @Override
	    protected Dimension getThumbSize() {
	        return new Dimension(12, 16);
	    }
	    */

	    /*
	    @Override
	    public void paintTrack(Graphics g) {
	        Graphics2D g2d = (Graphics2D) g;
	        Stroke old = g2d.getStroke();
	        g2d.setStroke(stroke);
	        g2d.setPaint(Color.BLACK);
	        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
	            g2d.drawLine(trackRect.x, trackRect.y + trackRect.height / 2, 
	                    trackRect.x + trackRect.width, trackRect.y + trackRect.height / 2);
	        } else {
	            g2d.drawLine(trackRect.x + trackRect.width / 2, trackRect.y, 
	                    trackRect.x + trackRect.width / 2, trackRect.y + trackRect.height);
	        }
	        g2d.setStroke(old);
	    }
	    */

	    /*
	    @Override
	    public void paintThumb(Graphics g) {
	        Graphics2D g2d = (Graphics2D) g;
	        int x1 = thumbRect.x + 2;
	        int x2 = thumbRect.x + thumbRect.width - 2;
	        int width = thumbRect.width - 4;
	        int topY = thumbRect.y + thumbRect.height / 2 - thumbRect.width / 3;
	        GeneralPath shape = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	        shape.moveTo(x1, topY);
	        shape.lineTo(x2, topY);
	        shape.lineTo((x1 + x2) / 2, topY + width);
	        shape.closePath();
	        g2d.setPaint(new Color(81, 83, 186));
	        g2d.fill(shape);
	        Stroke old = g2d.getStroke();
	        g2d.setStroke(new BasicStroke(2f));
	        g2d.setPaint(new Color(131, 127, 211));
	        g2d.draw(shape);
	        g2d.setStroke(old);
	    }
	    */
	}
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public JPlaySlider() {
        setOpaque(false);
        
        putClientProperty("JSlider.isFilled", Boolean.FALSE);
        
        /*
        ImageIcon icon = new ImageIcon("images/middle.gif", "a pretty but meaningless splat");
        putClientProperty("Slider.horizontalThumbIcon", icon);
       */
        
        //horizThumbIcon 
        
        setUI(new CustomSliderUI(this));
    }

	/*
    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        super.paint(g2d);
        g2d.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // We need this because we've taken over the painting of the component
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();

        super.paintComponent(g);
    }
    
    */


}
