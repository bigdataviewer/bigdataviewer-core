/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bdv.ui.CardPanel;
import bdv.ui.UIUtils;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.ViewerPanel;
import bdv.viewer.location.LocationPanel;
import bdv.viewer.location.SourceInfoToolBar;

import static bdv.ui.BdvDefaultCards.*;

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

	private final SplitPaneOneTouchExpandAnimator oneTouchExpandAnimator;

	public SplitPanel( final AbstractViewerPanel viewerPanel, final CardPanel cardPanel )
	{
		super( JSplitPane.HORIZONTAL_SPLIT );

		final double uiScale = UIUtils.getUIScaleFactor( this );

		configureSplitPane();

		final JComponent cardPanelComponent = cardPanel.getComponent();
		scrollPane = new JScrollPane( cardPanelComponent );
		scrollPane.setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setPreferredSize(
				new Dimension(
						( int ) Math.floor( 800 * uiScale ),
						( int ) Math.floor( 200 * uiScale ) ) );

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
					setLastDividerLocation( Math.max( w / 2, w - Math.max( ( int ) Math.floor( 200 * uiScale ), cardPanelComponent.getPreferredSize().width ) ) );
				}
				width = w;
			}
		} );

		// add hook to expand card panel and locations card when edit button in source info toolbar is clicked
		if (viewerPanel instanceof ViewerPanel) {
			final ViewerPanel viewer = (ViewerPanel) viewerPanel;
			final LocationPanel locationPanel = viewer.getLocationPanel();
			final SourceInfoToolBar sourceInfoToolBar = viewer.getSourceInfoToolBar();
			sourceInfoToolBar.setEditActionListener(e -> {
				// expand card panel and location card
				this.setCollapsed(false);
				cardPanel.setCardExpanded(DEFAULT_SOURCES_CARD, false);
				cardPanel.setCardExpanded(DEFAULT_SOURCEGROUPS_CARD, false);
				cardPanel.setCardExpanded(DEFAULT_LOCATIONS_CARD, true);
				locationPanel.requestFocusOnFirstComponent();
			});
		}

	}

	private void configureSplitPane()
	{
		this.setResizeWeight( 1.0 );
		this.setContinuousLayout( true );
	}

	// divider size set externally
	private int dividerSizeWhenVisible = -1;

	@Override
	public void setDividerSize( final int newSize )
	{
		dividerSizeWhenVisible = newSize;
		updateDividerSize();
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
			setDividerLocation( 1.0d );
		}
		else
		{
			setRightComponent( scrollPane );
			final int dl = getLastDividerLocation();
			final int w = getWidth();
			final double uiScale = UIUtils.getUIScaleFactor( this );
			setDividerLocation( Math.max( Math.min( w / 2, ( int ) Math.floor( 50 * uiScale ) ), Math.min( w - ( int ) Math.floor( 50 * uiScale ), dl ) ) );
		}
		updateDividerSize();
	}

	private void updateDividerSize()
	{
		final int s;
		if ( isCollapsed() )
		{
			s = 0;
		}
		else if ( dividerSizeWhenVisible >= 0 )
		{
			// divider size was set externally
			s = dividerSizeWhenVisible;
		}
		else
		{
			final double uiScale = UIUtils.getUIScaleFactor( this );
			s = ( int ) Math.round( uiScale * DEFAULT_DIVIDER_SIZE );
		}
		super.setDividerSize( s );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		if ( scrollPane != null )
		{
			final double uiScale = UIUtils.getUIScaleFactor( this );
			scrollPane.setPreferredSize(
					new Dimension(
							( int ) Math.floor( 800 * uiScale ),
							( int ) Math.floor( 200 * uiScale ) ) );
			if ( getRightComponent() == null )
			{
				// scrollPane is currently not a child component, therefore update it "manually"
				SwingUtilities.updateComponentTreeUI( scrollPane );
			}
			updateDividerSize();
		}
	}

	public boolean isCollapsed()
	{
		return getRightComponent() == null;
	}
}
