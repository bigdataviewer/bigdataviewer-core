package bdv.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;

/**
 * CardPanel handles components in named {@link CardWrapper}s which can be opened or closed.
 *
 * @author Tim-Oliver Buchholz, MPI-CBG CSBD, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresden
 */
public class CardPanel extends JPanel
{

	private final CommandService commandService;

	/**
	 * Color scheme.
	 */
	private final static Color BACKGROUND_TAB_PANEL = Color.white;

	private final static Color HEADER_COLOR = new Color( 0xeeeeee );

	private final static Color FONT_COLOR = Color.darkGray;

	private final Map< String, CardWrapper > cards;

	/**
	 * Empty card panel.
	 */
	public CardPanel( final CommandService commandService )
	{
		this.commandService = commandService;

		this.setLayout( new MigLayout( "fillx, ins 2", "[grow]", "[]" ) );
		this.setBackground( BACKGROUND_TAB_PANEL );

		this.cards = new HashMap<>();

	}

	/**
	 * Add all {@link DiscoverableCard}s for which all inputs can be resolved to this panel.
	 *
	 * @param type
	 * 		of the cards
	 * @param parameterMap
	 * 		used to resolve inputs
	 */
	public void addAll( final Class< ? extends DiscoverableCard > type, final Map< Class< ? >, Object > parameterMap )
	{
		final List< CommandInfo > cardInfos = commandService.getCommandsOfType( type );

		cardInfos.forEach( cardInfo -> {
			try
			{
				final CommandModule cardModule = new CommandModule( cardInfo );
				boolean allInputsResolved = true;
				for ( final ModuleItem< ? > item : cardInfo.inputs() )
				{
					final Class< ? > inputKlass = item.getType();

					if ( parameterMap.containsKey( inputKlass ) )
					{
						cardModule.setInput( item.getName(), parameterMap.get( inputKlass ) );
						cardModule.resolveInput( item.getName() );
					}
					else
					{
						allInputsResolved = false;
						break;
					}
				}
				if ( allInputsResolved )
				{
					final Card card = ( ( DiscoverableCard ) cardModule.getCommand() ).getCard();
					addCard( card.getName(), card.getComponent(), card.getDefaultVisibilty() );
				}
			}
			catch ( ModuleException e )
			{
				e.printStackTrace();
			}
		} );
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
		if ( !cards.containsKey( name ) )
		{
			final CardWrapper card = new CardWrapper( name, component, open );
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
		final CardWrapper card = cards.remove( name );
		if ( card != null )
		{
			this.remove( card );
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
		final CardWrapper card = cards.get( name );
		return card != null && card.isOpen();
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
		final CardWrapper card = cards.get( name );
		if ( card != null )
		{
			card.setCardOpen( open );
		}
	}

	private static class CardWrapper extends JPanel
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
		 * CardWrapper name.
		 */
		private final String name;

		public CardWrapper( final String name, final JComponent component, final boolean open )
		{
			this.name = name;
			downIcon = new ImageIcon( CardPanel.class.getResource( "downbutton.png" ), "Open Dialog." );
			upIcon = new ImageIcon( CardPanel.class.getResource( "upbutton.png" ), "Close Dialog." );

			this.setLayout( new MigLayout( "fillx, ins 4, hidemode 3", "[grow]", "[]0lp![]" ) );
			this.setBackground( CardPanel.BACKGROUND_TAB_PANEL );

			componentPanel = new JPanel( new MigLayout( "fillx, ins 8, hidemode 3", "[grow]", "[]0lp![]" ) );
			componentPanel.setBackground( CardPanel.BACKGROUND_TAB_PANEL );
			componentPanel.add( component, "growx" );

			final JComponent header = createHeader( name, open );
			this.add( header, "growx, wrap" );
			this.add( componentPanel, "growx" );
			this.setCardOpen( open );
		}

		/**
		 * Create clickable header.
		 */
		private JComponent createHeader( final String name, final boolean visible )
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

			componentPanel.addComponentListener( new ComponentAdapter()
			{
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
			header.addMouseListener( new MouseAdapter()
			{
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
			if ( obj instanceof CardWrapper )
			{
				return this.name.equals( ( ( CardWrapper ) obj ).name );
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
