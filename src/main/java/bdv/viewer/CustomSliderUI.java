package bdv.viewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicSliderUI;

public class CustomSliderUI extends BasicSliderUI  {

	private BasicStroke stroke = new BasicStroke(1f, BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 0f, new float[]{1f, 2f}, 0f);
	
	private int keyframePosition;
	
	public CustomSliderUI(JSlider slider) {
		super(slider);
		
		JPopupMenu menu = new  JPopupMenu();
		JMenuItem menuItem = new JMenuItem("test");
		menu.add(menuItem);
        
		slider.setComponentPopupMenu(menu);
		
		/*
		BasicSliderUI.TrackListener tl = this.new TrackListener() {
            @Override public void mouseClicked(MouseEvent e) {
            
            	Point p = e.getPoint();
                int value = valueForXPosition(p.x);
            	
            	if(SwingUtilities.isLeftMouseButton(e)){
            		slider.setValue(value);
            	}
            	else if(SwingUtilities.isRightMouseButton(e)){
            		
            		if(p.x >= keyframePosition && p.x <= keyframePosition+10)
            			slider.getComponentPopupMenu().show(slider, p.x, p.y);
            	}
            }
        };
        slider.addMouseListener(tl);
        */
	}

	@Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = c.getWidth();
        int keyframeTime = 100;
        double keyframePercentage  = (double)keyframeTime / (double)width;
        keyframePosition =(int) ( width * keyframePercentage);
        
        g.setColor(Color.RED);
        
        g.fillRect(keyframePosition, 0, 10, c.getHeight());
        
        super.paint(g, c);
    }
	
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
	 

	
}
