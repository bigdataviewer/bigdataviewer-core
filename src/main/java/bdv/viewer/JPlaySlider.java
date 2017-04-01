package bdv.viewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

public class JPlaySlider extends JPanel
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final JSlider playSlider = new JSlider();
	
	private final JPanel sliderLabelsPanel = new JPanel();
	private final JLabel[] sliderLabels = {
			new JLabel("<<"),
			new JLabel("<"),
			new JLabel("||"),
			new JLabel(">"),
			new JLabel(">>")
	};

	public JPlaySlider()
	{
		setLayout( new BorderLayout() );
		
		add(playSlider, BorderLayout.NORTH);
		add(sliderLabelsPanel, BorderLayout.SOUTH);
		
		playSlider.putClientProperty( "JSlider.isFilled", Boolean.FALSE );

		playSlider.setOpaque( false );
		playSlider.setFocusable( false );

		playSlider.setMaximum( 8 );
		playSlider.setMinimum( -8 );
		playSlider.setValue( 0 );

		playSlider.setSnapToTicks( true );

		playSlider.setPaintTicks( true );
		playSlider.setMinorTickSpacing( 1 );
		
		sliderLabelsPanel.setLayout( new GridLayout( 1, 5 ) );
		
		for (JLabel anySliderLabel : sliderLabels) {
			anySliderLabel.setFont( anySliderLabel.getFont().deriveFont( Font.BOLD ) );
			anySliderLabel.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
			anySliderLabel.setOpaque( true );
			
			sliderLabelsPanel.add( anySliderLabel );
		}

		sliderLabels[0].addMouseListener( onClickDo(() -> setValue(-8)) );				// <<
		sliderLabels[1].addMouseListener( onClickDo(() -> setValue(getValue() - 1)) );	// <
		sliderLabels[2].addMouseListener( onClickDo(() -> setValue(0)) );				// ||
		sliderLabels[3].addMouseListener( onClickDo(() -> setValue(getValue() + 1)) );	// >
		sliderLabels[4].addMouseListener( onClickDo(() -> setValue(8)) );				// >>

		sliderLabels[0].setHorizontalAlignment( JLabel.LEFT );
		sliderLabels[1].setHorizontalAlignment( JLabel.CENTER );
		sliderLabels[2].setHorizontalAlignment( JLabel.CENTER );
		sliderLabels[3].setHorizontalAlignment( JLabel.CENTER );
		sliderLabels[4].setHorizontalAlignment( JLabel.RIGHT );

		/*
		sliderLabels[0].setBackground( Color.BLUE );
		sliderLabels[1].setBackground( Color.RED );
		sliderLabels[2].setBackground( Color.BLUE );
		sliderLabels[3].setBackground( Color.RED );
		sliderLabels[4].setBackground( Color.BLUE );
		*/
	}
	
	private MouseAdapter onClickDo(Runnable action) {
		return new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				action.run();
				
				for (ChangeListener anyChangeListener : playSlider.getChangeListeners()) {
					SwingUtilities.invokeLater( () -> anyChangeListener.stateChanged( null ) );
				}
			}
		};
	}
	
	public void setValue(int value) {
		playSlider.setValue( value );
	}
	
	public int getValue() {
		return playSlider.getValue();
	}
	
	public void addChangeListener(ChangeListener listener) {
		playSlider.addChangeListener( listener );
	}
	
	/**
	 * Returns the {@code trackRect} of {@link BasicSliderUI} to determine the
	 * correct position of the slider thumb.
	 * 
	 * <p>
	 * If the selected LookAndFeel doesn't inherit from {@link BasicSliderUI}, a
	 * fallback implementation is used instead.
	 * </p>
	 * 
	 * @return Rectangle of track part - returns never {@code null}.
	 */
	private Rectangle getSliderTrackRect() {
		final SliderUI sliderUI = playSlider.getUI();

		final boolean fallbackNeeded = (sliderUI instanceof BasicSliderUI == false);
		if (fallbackNeeded) {
			return playSlider.getVisibleRect();
		}

		final BasicSliderUI basicSliderUI = (BasicSliderUI) sliderUI;
		final Class<? extends BasicSliderUI> uiClazz = BasicSliderUI.class;

		try {
			final Field trackRectField = uiClazz.getDeclaredField("trackRect");

			trackRectField.setAccessible(true);

			final Rectangle result = (Rectangle) trackRectField.get(basicSliderUI);

			if (null == result) {
				return playSlider.getVisibleRect();
			}

			return result;

		} catch (Exception ex) {
			return playSlider.getVisibleRect();
		}
	}
	
}
