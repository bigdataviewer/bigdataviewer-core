/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.ui.splitpanel;

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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.ui.CardPanel;
import bdv.ui.UIUtils;
import bdv.viewer.AbstractViewerPanel;

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
	private static final double uiScale = UIUtils.getUIScaleFactor();

	private static final int DEFAULT_DIVIDER_SIZE = ( int )Math.round( 3 * uiScale );

	private static final String FOCUS_VIEWER_PANEL = "focus viewer panel";
	private static final String HIDE_CARD_PANEL = "hide card panel";

	private final JScrollPane scrollPane;

	private int width;

	private final SplitPaneOneTouchExpandAnimator oneTouchExpandAnimator;

	public SplitPanel( final AbstractViewerPanel viewerPanel, final CardPanel cardPanel )
	{
		super( JSplitPane.HORIZONTAL_SPLIT );

		configureSplitPane();

		final JComponent cardPanelComponent = cardPanel.getComponent();
		scrollPane = new JScrollPane( cardPanelComponent );
		scrollPane.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setPreferredSize(
				new Dimension(
						( int )Math.floor( 800 * uiScale ),
						( int )Math.floor( 200 * uiScale ) ) );

		final InputMap inputMap = scrollPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		inputMap.put( KeyStroke.getKeyStroke( "F6" ), "none" );

		final InputTriggerConfig inputTriggerConfig = viewerPanel.getInputTriggerConfig();
		final Actions actions = new Actions( inputMap, scrollPane.getActionMap(), inputTriggerConfig, "bdv" );
		actions.runnableAction( viewerPanel::requestFocusInWindow, FOCUS_VIEWER_PANEL, "ESCAPE" );
		actions.runnableAction( () -> {
			setCollapsed( true );
			viewerPanel.requestFocusInWindow();
		}, HIDE_CARD_PANEL, "shift ESCAPE" );

		setLeftComponent( viewerPanel );
		setRightComponent( null );
		setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		setPreferredSize( viewerPanel.getPreferredSize() );

		super.setDividerSize( 0 );

		oneTouchExpandAnimator = new SplitPaneOneTouchExpandAnimator( this::isCollapsed );
		viewerPanel.addOverlayAnimator( oneTouchExpandAnimator );

		final SplitPaneOneTouchExpandTrigger oneTouchExpandTrigger = new SplitPaneOneTouchExpandTrigger( oneTouchExpandAnimator, this, viewerPanel );
		viewerPanel.getDisplayComponent().addMouseMotionListener( oneTouchExpandTrigger );
		viewerPanel.getDisplayComponent().addMouseListener( oneTouchExpandTrigger );

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
					setLastDividerLocation( Math.max( w / 2, w - Math.max( ( int )Math.floor( 200 * uiScale ), cardPanelComponent.getPreferredSize().width ) ) );
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
						g.setColor( UIManager.getColor( "SplitPane.background" ) );
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
//		this.setForeground( Color.white );
//		this.setBackground( Color.white );
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

		oneTouchExpandAnimator.clearPaintState();
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
			setDividerLocation( Math.max( Math.min ( w / 2, ( int )Math.floor( 50 * uiScale ) ), Math.min( w - ( int )Math.floor( 50 * uiScale ), dl ) ) );
		}
	}

	@Override
	public void updateUI()
	{
		if ( getRightComponent() == null && scrollPane != null )
			// scrollPane is currently not a child component, therefore update it "manually"
			SwingUtilities.updateComponentTreeUI( scrollPane );
	}

	public boolean isCollapsed()
	{
		return getRightComponent() == null;
	}
}
