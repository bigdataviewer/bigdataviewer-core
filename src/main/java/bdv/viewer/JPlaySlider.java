package bdv.viewer;

import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;

public class JPlaySlider extends JSlider{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public JPlaySlider() {
        putClientProperty("JSlider.isFilled", Boolean.FALSE);
        
        setOpaque(false);
        
        setMaximum(8);
		setMinimum(-8);
    	setValue(0);
    	
		setSnapToTicks(true);
		
		setPaintTicks(true);
		setPaintLabels(true);
		setMinorTickSpacing(1);
		setMajorTickSpacing(4);
		
		Hashtable<Integer, JComponent> labels = new Hashtable<>();
		labels.put(getMinimum(), new JLabel ( "<<" ));
		labels.put(getMinimum()/2, new JLabel ( "<" ));
		labels.put(0, new JLabel ( "||" ));
		labels.put(getMaximum()/2, new JLabel ( ">" ));
		labels.put(getMaximum(), new JLabel ( ">>" ));
		setLabelTable(labels);
    }
}
