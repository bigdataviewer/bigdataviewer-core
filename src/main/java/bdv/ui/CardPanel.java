package bdv.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * CardPanel handles components in named {@link Card}s which can be opened or closed.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 */
public class CardPanel extends JPanel
{

	/**
	 * Color scheme.
	 */
	private final static Color BACKGROUND_TAB_PANEL = Color.white;

	private final static Color HEADER_COLOR = new Color( 238, 238, 238 );

	private final static Color FONT_COLOR = Color.darkGray;

	private final Map< String, Card > cards;

	/**
	 * Empty card panel.
	 */
	public CardPanel()
	{
		super();

		this.setLayout( new MigLayout( "fillx, ins 2", "[grow]", "[]" ) );
		this.setBackground( BACKGROUND_TAB_PANEL );

		this.cards = new HashMap<>();
	}

	/**
	 * Add a new {@link JComponent} to the card panel.
	 *
	 * @param name
	 * 		of the new card. If a card with this name already exists, no new card will be added.
	 * @param component
	 * 		to add to the panel
	 * @param open
	 * 		state of the card
	 *
	 * @return if a new card was successfully added
	 */
	public boolean addCard( final String name, final JComponent component, final boolean open )
	{
		if ( !cards.keySet().contains( name ) )
		{
			final Card card = new Card( name, component, open );
			cards.put( name, card );
			this.add( card, "growx, wrap" );
			return true;
		}
		return false;
	}

	/**
	 * Remove a card. If the card does not exist, nothing happens.
	 *
	 * @param name
	 * 		of the card
	 */
	public void removeCard( final String name )
	{
		if ( cards.keySet().contains( name ) )
		{
			final Card card = cards.get( name );
			this.remove( card );
			cards.remove( name );
		}
	}

	/**
	 * Get open state of a card.
	 *
	 * @param name
	 * 		of the card
	 *
	 * @return open state
	 */
	public boolean isCardOpen( final String name )
	{
		if ( cards.keySet().contains( name ) )
		{
			return cards.get( name ).isOpen();
		}
		else
		{
			return false;
		}
	}

	/**
	 * Set open state of a card
	 *
	 * @param name
	 * 		of the card
	 * @param open
	 * 		new state
	 */
	public void setCardOpen( final String name, final boolean open )
	{
		if ( cards.keySet().contains( name ) )
		{
			cards.get( name ).setCardOpen( open );
		}
	}

	private class Card extends JPanel
	{
		private final JPanel componentPanel;

		/**
		 * Open card icon.
		 */
		private final ImageIcon downIcon;

		/**
		 * Close card icon.
		 */
		private final ImageIcon upIcon;

		/**
		 * Card name.
		 */
		private final String name;

		public Card( final String name, final JComponent component, final boolean open )
		{
			super();
			this.name = name;
			downIcon = new ImageIcon( CardPanel.class.getResource( "downbutton.png" ), "Open Dialog." );
			upIcon = new ImageIcon( CardPanel.class.getResource( "upbutton.png" ), "Close Dialog." );

			this.setLayout( new MigLayout( "fillx, ins 4, hidemode 3", "[grow]", "[]0lp![]" ) );
			this.setBackground( CardPanel.BACKGROUND_TAB_PANEL );

			componentPanel = new JPanel( new MigLayout( "fillx, ins 8, hidemode 3", "[grow]", "[]0lp![]" ) );
			componentPanel.setBackground( CardPanel.BACKGROUND_TAB_PANEL );
			componentPanel.add( component, "growx" );

			final JComponent header = createHeader( name, componentPanel, open );
			this.add( header, "growx, wrap" );
			this.add( componentPanel, "growx" );
			this.setCardOpen( open );
		}

		/**
		 * Create clickable header.
		 */
		private JComponent createHeader( final String name, final JComponent component, final boolean visible )
		{
			final JPanel header = new JPanel( new MigLayout( "fillx, aligny center, ins 0 0 0 4", "[grow][]", "" ) );
			header.setPreferredSize( new Dimension( 30, 30 ) );
			header.setBackground( CardPanel.HEADER_COLOR );

			// Holds the name with insets.
			final JPanel labelPanel = new JPanel( new MigLayout( "fillx, ins 4", "[grow]", "" ) );
			labelPanel.setBackground( CardPanel.HEADER_COLOR );
			final JLabel label = new JLabel( name );
			label.setForeground( CardPanel.FONT_COLOR );
			labelPanel.add( label );

			final JLabel icon = new JLabel();
			icon.setBackground( Color.WHITE );
			icon.setIcon( visible ? upIcon : downIcon );

			componentPanel.addComponentListener( new ComponentListener()
			{
				@Override
				public void componentResized( final ComponentEvent e )
				{
					// nothing
				}

				@Override
				public void componentMoved( final ComponentEvent e )
				{
					// nothing
				}

				@Override
				public void componentShown( final ComponentEvent e )
				{

					icon.setIcon( upIcon );
				}

				@Override
				public void componentHidden( final ComponentEvent e )
				{
					icon.setIcon( downIcon );
				}
			} );

			// By default closed.
			header.addMouseListener( new MouseListener()
			{

				@Override
				public void mouseReleased( MouseEvent e )
				{
					// nothing
				}

				@Override
				public void mousePressed( MouseEvent e )
				{
					// nothing
				}

				@Override
				public void mouseExited( MouseEvent e )
				{
					// nothing
				}

				@Override
				public void mouseEntered( MouseEvent e )
				{
					// nothing
				}

				@Override
				public void mouseClicked( MouseEvent e )
				{
					final boolean state = componentPanel.isVisible();
					componentPanel.setVisible( !state );
					componentPanel.revalidate();
				}
			} );

			header.add( labelPanel, "growx" );
			header.add( icon );

			return header;
		}

		public boolean isOpen()
		{
			return componentPanel.isVisible();
		}

		public void setCardOpen( final boolean open )
		{
			componentPanel.setVisible( open );
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj instanceof Card )
			{
				return this.name.equals( ( ( Card ) obj ).name );
			}
			else
			{
				return false;
			}
		}

		@Override
		public int hashCode()
		{
			return this.name.hashCode();
		}
	}

}
