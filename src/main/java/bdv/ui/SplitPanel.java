package bdv.ui;

import bdv.viewer.ViewerPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class SplitPanel extends JSplitPane
{

	private final ViewerPanel viewerPanel;

	public SplitPanel( final ViewerPanel viewerPanel, final CardPanel cardPanel )
	{
		super( JSplitPane.HORIZONTAL_SPLIT );

		this.viewerPanel = viewerPanel;

		configureSplitPane();

		final JScrollPane scrollPane = new JScrollPane( cardPanel );
		scrollPane.setPreferredSize( new Dimension( cardPanel.getPreferredSize().width + 20, viewerPanel.getPreferredSize().height ) );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 20 );
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		this.setLeftComponent( viewerPanel );
		this.setRightComponent( scrollPane );
		this.getLeftComponent().setMinimumSize( new Dimension( 200, 200 ) );
		this.getLeftComponent().setPreferredSize( viewerPanel.getPreferredSize() );
		this.setDividerLocation( viewerPanel.getOptionValues().getWidth() );

		try
		{
			final SplitPaneOneTouchExpandAnimator oneTouchExpandAnimator = new SplitPaneOneTouchExpandAnimator( this );
			viewerPanel.getDisplay().addMouseMotionListener( oneTouchExpandAnimator );
			viewerPanel.getDisplay().addMouseListener( oneTouchExpandAnimator );
			viewerPanel.addOverlayAnimator( oneTouchExpandAnimator );
		} catch ( IOException e ) {
			System.out.println("Couldn't load split-pane one touch expand resources.");
		}
	}

	private void configureSplitPane()
	{
		this.setBorder( null );
		this.setUI( new BasicSplitPaneUI()
		{
			public BasicSplitPaneDivider createDefaultDivider()
			{
				return new BasicSplitPaneDivider( this )
				{
					private static final long serialVersionUID = 1L;

					@Override
					public void paint( Graphics g )
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

		setDividerSize();

		this.setForeground( Color.white );
		this.setBackground( Color.white );
		this.setDividerLocation( viewerPanel.getPreferredSize().width );
		this.setResizeWeight( 1.0 );
	}

	private void setDividerSize()
	{
		this.setDividerSize( 3 );
	}

	/**
	 * Collapse the UI-Panel.
	 */
	public void collapseUI()
	{
		if ( this.getDividerLocation() < this.getMaximumDividerLocation() )
		{
			this.setDividerSize( 0 );
			this.setDividerLocation( 1.0d );
		}
		else
		{
			this.setDividerLocation( this.getLastDividerLocation() );
			setDividerSize();
		}
	}

	public ViewerPanel getViewerPanel()
	{
		return viewerPanel;
	}
}
