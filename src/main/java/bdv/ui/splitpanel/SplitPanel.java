package bdv.ui.splitpanel;

import bdv.ui.CardPanel;
import bdv.viewer.ViewerPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

/**
 * A {@code JSplitPane} with a {@code ViewerPanel} on the left and a
 * {@code CardPanel} on the right. Animated arrows are added to the
 * {@code ViewerPanel}, such that the right ({@code CardPanel}) pane can be
 * fully collapsed or exanded. The {@code CardPanel} can be also
 * programmatically collapsed or exanded using {@link #setCollapsed(boolean)}.
 *
 * @author Tim-Oliver Buchholz
 * @author Tobias Pietzsch
 */
public class SplitPanel extends JSplitPane
{
	private static final int DEFAULT_DIVIDER_SIZE = 3;

	private static final String FOCUS_VIEWER_PANEL = "focus viewer panel";
	private static final String HIDE_CARD_PANEL = "hide card panel";

	private final JScrollPane scrollPane;

	private int width;

	public SplitPanel( final ViewerPanel viewerPanel, final CardPanel cardPanel )
	{
		super( JSplitPane.HORIZONTAL_SPLIT );

		configureSplitPane();

		final JComponent cardPanelComponent = cardPanel.getComponent();
		scrollPane = new JScrollPane( cardPanelComponent );
		scrollPane.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setPreferredSize( new Dimension( 800, 200 ) );

		final InputMap inputMap = scrollPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		inputMap.put( KeyStroke.getKeyStroke( "F6" ), "none" );

		final InputTriggerConfig inputTriggerConfig = viewerPanel.getOptionValues().getInputTriggerConfig();
		final Actions actions = new Actions( inputMap, scrollPane.getActionMap(), inputTriggerConfig, "bdv" );
		actions.runnableAction( viewerPanel::requestFocusInWindow, FOCUS_VIEWER_PANEL, "ESCAPE" );
		actions.runnableAction( () -> {
			setCollapsed( true );
			viewerPanel.requestFocusInWindow();
		}, HIDE_CARD_PANEL, "shift ESCAPE" );

		setLeftComponent( viewerPanel );
		setRightComponent( null );
		setBorder( null );
		setPreferredSize( viewerPanel.getPreferredSize() );

		super.setDividerSize( 0 );

		final SplitPaneOneTouchExpandAnimator oneTouchExpandAnimator = new SplitPaneOneTouchExpandAnimator( this::isCollapsed );
		viewerPanel.addOverlayAnimator( oneTouchExpandAnimator );

		final SplitPaneOneTouchExpandTrigger oneTouchExpandTrigger = new SplitPaneOneTouchExpandTrigger( oneTouchExpandAnimator, this, viewerPanel );
		viewerPanel.getDisplay().addMouseMotionListener( oneTouchExpandTrigger );
		viewerPanel.getDisplay().addMouseListener( oneTouchExpandTrigger );

		setDividerSize( DEFAULT_DIVIDER_SIZE );

		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				final int w = getWidth();
				if ( width > 0 )
				{
					final int dl = getLastDividerLocation() + w - width;
					setLastDividerLocation( Math.max( 50, dl ) );
				}
				else
				{
					// When the component is first made visible, set LastDividerLocation to a reasonable value
					setDividerLocation( w );
					setLastDividerLocation( Math.max( w / 2, w - Math.max( 200, cardPanelComponent.getPreferredSize().width ) ) );
				}
				width = w;
			}
		} );
	}

	private void configureSplitPane()
	{
		this.setUI( new BasicSplitPaneUI()
		{
			@Override
			public BasicSplitPaneDivider createDefaultDivider()
			{
				return new BasicSplitPaneDivider( this )
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void paint( final Graphics g )
					{
						g.setColor( Color.white );
						g.fillRect( 0, 0, getSize().width, getSize().height );
						super.paint( g );
					}

					@Override
					public void setBorder( final Border border )
					{
						super.setBorder( null );
					}
				};
			}
		} );
		this.setForeground( Color.white );
		this.setBackground( Color.white );
		this.setResizeWeight( 1.0 );
		this.setContinuousLayout( true );
	}

	// divider size set externally
	private int dividerSizeWhenVisible = DEFAULT_DIVIDER_SIZE;

	@Override
	public void setDividerSize( final int newSize )
	{
		dividerSizeWhenVisible = newSize;
	}

	/**
	 * Un/collapse the UI-Panel.
	 */
	public void setCollapsed( final boolean collapsed )
	{
		if ( isCollapsed() == collapsed )
			return;

		if ( collapsed )
		{
			setRightComponent( null );
			super.setDividerSize( 0 );
			setDividerLocation( 1.0d );
		}
		else
		{
			setRightComponent( scrollPane );
			super.setDividerSize( dividerSizeWhenVisible );
			final int dl = getLastDividerLocation();
			final int w = getWidth();
			setDividerLocation( Math.max( Math.min ( w / 2, 50 ), Math.min( w - 50, dl ) ) );
		}
	}

	public boolean isCollapsed()
	{
		return getRightComponent() == null;
	}
}
