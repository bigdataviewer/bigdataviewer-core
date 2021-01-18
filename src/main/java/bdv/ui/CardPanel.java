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
package bdv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

/**
 * CardPanel handles components in named {@code Card}s which can be expanded or collapsed.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 * @author Tobias Pietzsch
 */
public class CardPanel
{
	/**
	 * Color scheme.
	 */
	private final static Color DEFAULT_CARD_BACKGROUND = UIManager.getColor( "Panel.background" ); // Color.white;

	private final static Color DEFAULT_HEADER_BACKGROUND = UIManager.getColor( "InternalFrame.inactiveTitleBackground" ); // Color( 0xcccccc );

	private final static Color DEFAULT_HEADER_FOREGROUND = UIManager.getColor( "InternalFrame.inactiveTitleForeground" ); // new Color( 0x202020 );

	private Color headerBackground = DEFAULT_HEADER_BACKGROUND;

	private Color headerForeground = DEFAULT_HEADER_FOREGROUND;

	private final Map< Object, Card > cards = new HashMap<>();

	private final List< Card > cardList = new ArrayList<>();

	private final Container container;

	/**
	 * Empty card panel.
	 */
	public CardPanel()
	{
		container = new Container( new MigLayout( "fillx, ins 0", "[grow]", "[]0[]" ) );
		container.setBackground( DEFAULT_CARD_BACKGROUND );
	}

	public JComponent getComponent()
	{
		return container;
	}

	/**
	 * Add a new {@link JComponent} to the card panel.
	 *
	 * @param title
	 * 		title of the new card. Is shown in the card header.
	 * 		This is also used as the key of the new card.
	 * 		If a card with this key already exists, no new card will be added!
	 * @param component
	 * 		body of the new card
	 * @param expanded
	 * 		state of the card
	 * @param insets
	 * 		insets of card body
	 *
	 * @return if a new card was successfully added
	 */
	public boolean addCard( final String title, final JComponent component, final boolean expanded, final Insets insets )
	{
		return addCard( title, title, component, expanded, insets );
	}

	/**
	 * Add a new {@link JComponent} to the card panel.
	 *
	 * @param key
	 * 		key of the new card.
	 * 		If a card with this key already exists, no new card will be added!
	 * @param title
	 * 		title of the new card. Is shown in the card header.
	 * @param component
	 * 		body of the new card
	 * @param expanded
	 * 		state of the card
	 * @param insets
	 * 		insets of card body
	 *
	 * @return if a new card was successfully added
	 */
	public synchronized boolean addCard( final Object key, final String title, final JComponent component, final boolean expanded, final Insets insets )
	{
		if ( key == null || title == null || component == null )
			throw new NullPointerException();

		if ( cards.containsKey( key ) )
			return false;

		final Card card = new Card( key, title, component, expanded, insets );
		cards.put( key, card );
		if ( !cardList.isEmpty() )
			cardList.get( cardList.size() - 1 ).setIsLastCard( false );
		cardList.add( card );
		container.add( card, "growx, wrap" );
		container.revalidate();
		return true;
	}

	public boolean addCard( final String title, final JComponent component, final boolean expanded )
	{
		return addCard( title, title, component, expanded, null );
	}

	public boolean addCard( final Object key, final String title, final JComponent component, final boolean expanded )
	{
		return addCard( key, title, component, expanded, null );
	}

	/**
	 * Remove a card. If the card does not exist, nothing happens.
	 *
	 * @param key
	 * 		of the card
	 */
	public synchronized void removeCard( final Object key )
	{
		final Card card = cards.remove( key );
		if ( card != null )
		{
			cardList.remove( card );
			final Card lastCard = cardList.get( cardList.size() - 1 );
			lastCard.setIsLastCard( true );
			container.remove( card );
			container.revalidate();
		}
	}

	/**
	 * TODO
	 *
	 * @param key
	 *
	 * @return
	 */
	public synchronized int indexOf( final Object key )
	{
		if ( cards.containsKey( key ) )
			for ( int i = 0; i < cardList.size(); i++ )
				if ( cardList.get( i ).getKey().equals( key ) )
					return i;
		return -1;
	}

	/**
	 * Get expanded state of a card.
	 *
	 * @param key
	 * 		of the card
	 *
	 * @return open state
	 */
	public synchronized boolean isCardExpanded( final Object key )
	{
		final Card card = cards.get( key );
		return card != null && card.isExpanded();
	}

	/**
	 * Set open state of a card
	 *
	 * @param key
	 * 		of the card
	 * @param expanded
	 * 		new state
	 */
	public synchronized void setCardExpanded( final Object key, final boolean expanded )
	{
		final Card card = cards.get( key );
		if ( card != null && card.isExpanded() != expanded )
			card.setExpanded( expanded );
	}

	/**
	 * Set the card background color
	 */
	public void setCardBackground( final Color bg )
	{
		container.setBackground( bg );
		for ( final Card card : cardList )
		{
			card.setBackground( bg );
			card.componentPanel.setBackground( bg );
		}
	}

	public Color getCardBackground()
	{
		return container.getBackground();
	}

	/**
	 * Set the header background color
	 */
	public void setHeaderBackground( final Color bg )
	{
		headerBackground = bg;
		for ( final Card card : cardList )
		{
			card.headerPanel.setBackground( bg );
			card.terminalResizePanel.setBackground( bg );
		}
	}

	public Color getHeaderBackground()
	{
		return headerBackground;
	}

	/**
	 * Set the header foreground color
	 */
	public void setHeaderForeground( final Color fg )
	{
		headerForeground = fg;
		for ( final Card card : cardList )
			card.headerPanel.setForeground( fg );
	}

	public Color getHeaderForeground()
	{
		return headerForeground;
	}

	// =================================================================

	private static final Icon collapsedIcon = new CardCollapseIcon( 10, 10, true, false );
	private static final Icon collapsedMouseOverIcon = new CardCollapseIcon( 10, 10, true, true );
	private static final Icon expandedIcon = new CardCollapseIcon( 10, 10, false, false );
	private static final Icon expandedMouseOverIcon = new CardCollapseIcon( 10, 10, false, true );

	private static final int RESIZE_HANDLE_HEIGHT = 10;

	private static class TerminalResizePanel extends JPanel
	{
		public TerminalResizePanel( final JComponent component )
		{
			UIUtils.setMinimumHeight( this, 5 );
			UIUtils.setPreferredHeight( this, 5 );

			final ResizeMouseHandler resizeHandler = new ResizeMouseHandler( this, new Resizable()
			{
				@Override
				public boolean showResizeHandle()
				{
					return true;
				}

				@Override
				public JComponent getComponent()
				{
					return component;
				}
			} );
			addMouseListener( resizeHandler );
			addMouseMotionListener( resizeHandler );
		}
	}

	private class Card extends JPanel
	{
		private final JPanel componentPanel;

		private final HeaderPanel headerPanel;

		private final TerminalResizePanel terminalResizePanel;

		/**
		 * Card key.
		 */
		private final Object key;

		private boolean isLastCard = true;

		public Card( final Object key, final String title, final JComponent component, final boolean open, final Insets insets )
		{
			this.key = key;

			this.setLayout( new MigLayout( "fillx, ins 0, hidemode 3", "[grow]", "[]0lp![]" ) );
			this.setBackground( getCardBackground() );

			final String ins = insets == null ? "ins 4 4 4 0" : String.format( "ins %d %d %d %d", insets.top, insets.left, insets.bottom, insets.right );
			componentPanel = new JPanel( new MigLayout( "fillx, hidemode 3, " + ins, "[grow]", "[grow]0lp![]" ) );
			componentPanel.setBackground( getCardBackground() );
			componentPanel.add( component, "grow, wrap" );

			terminalResizePanel = new TerminalResizePanel( componentPanel );
			terminalResizePanel.setBackground( getHeaderBackground() );

			headerPanel = new HeaderPanel( title );
			headerPanel.setBackground( getHeaderBackground() );
			headerPanel.setForeground( getHeaderForeground() );

			this.add( headerPanel, "growx, wrap" );
			this.add( componentPanel, "growx, wrap" );
			this.add( terminalResizePanel, "growx" );
			this.setExpanded( open );
		}

		private class HeaderPanel extends JPanel
		{
			private final JPanel labelPanel;

			private final JLabel label;

			@Override
			public void setBackground( final Color bg )
			{
				super.setBackground( bg );
				if ( labelPanel != null )
					labelPanel.setBackground( bg );
			}

			@Override
			public void setForeground( final Color fg )
			{
				super.setForeground( fg );
				if ( label != null )
					label.setForeground( fg );
			}

			public HeaderPanel( final String title )
			{
				super( new MigLayout( "fillx, aligny center, ins 0 0 0 0", "[][grow]", "" ) );
				UIUtils.setPreferredWidth( this, 100 );

				// Holds the name with insets.
				labelPanel = new JPanel( new MigLayout( "fillx, ins 0 4 0 4", "[grow]", "" ) );
				label = new JLabel( title );
				labelPanel.add( label );

				final JToggleButton collapseButton = new JToggleButton();
				collapseButton.setIcon( expandedIcon );
				collapseButton.setRolloverIcon( expandedMouseOverIcon );
				collapseButton.setSelectedIcon( collapsedIcon );
				collapseButton.setRolloverSelectedIcon( collapsedMouseOverIcon );
				collapseButton.setFocusable( false );
				collapseButton.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
				collapseButton.setBorderPainted( false );
				collapseButton.setFocusPainted( false );
				collapseButton.setContentAreaFilled( false );

				collapseButton.addActionListener( e -> setExpanded( !collapseButton.isSelected() ) );

				componentPanel.addComponentListener( new ComponentAdapter()
				{
					@Override
					public void componentShown( final ComponentEvent e )
					{
						collapseButton.setSelected( false );
					}

					@Override
					public void componentHidden( final ComponentEvent e )
					{
						collapseButton.setSelected( true );
					}
				} );

				final ResizeMouseHandler resizeHandler = new ResizeMouseHandler( this, new Resizable()
				{
					@Override
					public boolean showResizeHandle()
					{
						return isCardAboveExpanded();
					}

					@Override
					public JComponent getComponent()
					{
						return getContentOfCardAbove();
					}
				} );
				addMouseListener( resizeHandler );
				addMouseMotionListener( resizeHandler );

				add( collapseButton );
				add( labelPanel, "growx" );
			}
		}

		/**
		 * Get the key of this card. Keys are unique within one CardPanel.
		 */
		public Object getKey()
		{
			return key;
		}

		public boolean isExpanded()
		{
			return componentPanel.isVisible();
		}

		public void setExpanded( final boolean open )
		{
			componentPanel.setVisible( open );
			terminalResizePanel.setVisible( open && isLastCard );
			componentPanel.revalidate();
		}

		public void setIsLastCard( final boolean isLastCard )
		{
			this.isLastCard = isLastCard;
			terminalResizePanel.setVisible( componentPanel.isVisible() && isLastCard );
			terminalResizePanel.revalidate();
		}

		@Override
		public boolean equals( final Object obj )
		{
			return obj instanceof Card && this.key.equals( ( ( Card ) obj ).key );
		}

		@Override
		public int hashCode()
		{
			return key.hashCode();
		}

		private JComponent getContentOfCardAbove()
		{
			synchronized ( CardPanel.this )
			{
				final int i = cardList.indexOf( this );
				if ( i <= 0 )
					return null;

				final JPanel component = cardList.get( i - 1 ).componentPanel;
				return component.isVisible() ? component : null;
			}
		}

		private boolean isCardAboveExpanded()
		{
			synchronized ( CardPanel.this )
			{
				final int i = cardList.indexOf( this );
				if ( i <= 0 )
					return false;

				return cardList.get( i - 1 ).isExpanded();
			}
		}
	}

	// =================================================================

	private interface Resizable
	{
		boolean showResizeHandle();

		JComponent getComponent();
	}

	private static class ResizeMouseHandler extends MouseAdapter
	{
		private final JComponent attachedComponent;

		private final Resizable resizable;

		private int oy;

		private int oheight;

		private int minheight;

		private int maxheight;

		private JComponent resizeComponent;

		public ResizeMouseHandler( final JComponent attachedComponent, final Resizable resizable )
		{
			this.attachedComponent = attachedComponent;
			this.resizable = resizable;
		}

		@Override
		public void mousePressed( final MouseEvent e )
		{
			if ( e.getY() < RESIZE_HANDLE_HEIGHT )
				resizeComponent = resizable.getComponent();
			else
				resizeComponent = null;

			if ( resizeComponent != null )
			{
				oheight = resizeComponent.getHeight();
				minheight = resizeComponent.getMinimumSize().height;
				maxheight = resizeComponent.getMaximumSize().height;
				UIUtils.setPreferredHeight( resizeComponent, oheight );
				oy = e.getYOnScreen();
			}
		}

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			if ( e.getY() < RESIZE_HANDLE_HEIGHT && resizable.showResizeHandle() )
				attachedComponent.setCursor( Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR ) );
			else
				attachedComponent.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}

		@Override
		public void mouseExited( final MouseEvent e )
		{
			attachedComponent.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			if ( resizeComponent != null )
			{
				final int height = oheight + e.getYOnScreen() - oy;
				UIUtils.setPreferredHeight( resizeComponent, Math.min( maxheight, Math.max( minheight, height ) ) );
				resizeComponent.revalidate();
			}
		}
	}

	// =================================================================

	/**
	 * Scroll-savvy JPanel that tracks viewport width
	 */
	private static class Container extends JPanel implements Scrollable
	{
		public Container( final LayoutManager layout )
		{
			super( layout );
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement( final Rectangle visibleRect, final int orientation, final int direction )
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement( final Rectangle visibleRect, final int orientation, final int direction )
		{
			return 10;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}

	// =================================================================

	private static class CardCollapseIcon implements Icon
	{
		private static final Color mouseOverColor = new Color(0x606060 );

		private static final Color color = new Color( 0x808080 );

		private final int width;

		private final int height;

		private final boolean collapsed;

		private final boolean mouseOver;

		public CardCollapseIcon( final int width, final int height, final boolean collapsed, final boolean mouseOver )
		{
			this.width = width;
			this.height = height;
			this.collapsed = collapsed;
			this.mouseOver = mouseOver;
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			final Path2D.Double path = new Path2D.Double();
			if ( collapsed )
			{
				path.moveTo( 0.05, 0.05 );
				path.lineTo( 0.85, 0.5 );
				path.lineTo( 0.05, 0.95 );
			}
			else
			{
				path.moveTo( 0.05, 0.05 );
				path.lineTo( 0.5, 0.85 );
				path.lineTo( 0.95, 0.05 );
			}
			path.transform( AffineTransform.getScaleInstance( width, height ) );
			path.transform( AffineTransform.getTranslateInstance( x, y ) );

			g2d.setColor( mouseOver ? mouseOverColor : color );
			g2d.fill( path );
		}

		@Override
		public int getIconWidth()
		{
			return width;
		}

		@Override
		public int getIconHeight()
		{
			return height;
		}
	}
}
