package bdv.ui.iconcards.options;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Deborah Schmidt, CSBD/MPI-CBG, Dresden
 */
public class DefaultIconOptionsCard extends JPanel implements IconOptionsCard
{

	int iconSize = 40;

	class OptionGroup
	{
		JPanel panel;

		final Map<String, Option> options = new HashMap<>();
	}

	class Option
	{
		final Map<String, ImageIcon> choices = new HashMap<>();

		public JPopupMenu menu;

		JButton button;
	}

	private final Map<String, OptionGroup> groups = new HashMap<>();

	public DefaultIconOptionsCard()
	{
		setLayout( new MigLayout( "fillx, nogrid, ins n 0 n 0, gap 0" ) );
		setBackground( null );
		setOpaque( false );
		setForeground( Color.lightGray );
	}

	public void addGroup( String optionGroup )
	{
		final JPanel panel = new JPanel( new MigLayout( "ins 0, fillx, filly", "", "top" ) );
		panel.setBackground( null );
		panel.setOpaque( false );
		panel.add( new JLabel( optionGroup ), "span 3, growx, center, wrap" );
		OptionGroup optionSet = new OptionGroup();
		optionSet.panel = panel;
		groups.put( optionGroup, optionSet );
		add( panel );
	}

	@Override
	public void removeGroup( String optionGroup )
	{
		remove( groups.get( optionGroup ).panel );
		groups.remove( optionGroup );
	}

	@Override
	public void addOptionChoice( String optionGroupName, String optionName, String choiceName, Runnable action, URL iconURL )
	{
		if ( !groups.containsKey( optionGroupName ) )
			addGroup( optionGroupName );
		OptionGroup optionGroup = groups.get( optionGroupName );
		Option option = optionGroup.options.get( optionName );
		if ( option == null )
			option = addOption( optionName, optionGroup );
		ImageIcon icon = new ImageIcon( iconURL );
		option.choices.put( choiceName, icon );
		JMenuItem menuItem = new JMenuItem( new AbstractAction( choiceName )
		{
			@Override
			public void actionPerformed( ActionEvent actionEvent )
			{
				action.run();
			}
		} );
		Image newimg = icon.getImage().getScaledInstance( 20, 20, java.awt.Image.SCALE_SMOOTH );
		ImageIcon scaledIcon = new ImageIcon( newimg );
		menuItem.setIcon( scaledIcon );
		option.menu.add( menuItem );
		Option finalOption = option;
		menuItem.addActionListener( actionEvent -> {
			finalOption.button.setIcon( icon );
			finalOption.button.setToolTipText( createToolTipText( finalOption.choices, choiceName ) );
		} );
	}

	private Option addOption( String optionName, OptionGroup optionGroup )
	{
		Option option;
		option = new Option();
		optionGroup.options.put( optionName, option );
		JButton btn = new JButton();
		option.button = btn;
		JPopupMenu menu = new JPopupMenu( optionName );
		option.menu = menu;
		btn.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( MouseEvent mouseEvent )
			{
				menu.show( btn, mouseEvent.getX(), mouseEvent.getY() );
			}
		} );
		setLook( btn );
		optionGroup.panel.add( btn );
		return option;
	}

	@Override
	public void setSelected( String optionGroup, String optionName, String choiceName )
	{
		Option option = groups.get( optionGroup ).options.get( optionName );
		ImageIcon choice = option.choices.get( choiceName );
		option.button.setIcon( choice );
		option.button.setToolTipText( createToolTipText( option.choices, choiceName ) );
	}

	private String createToolTipText( Map<String, ImageIcon> optionChoices, String currentChoice )
	{
		return currentChoice;
	}

	private void setLook( final JButton button )
	{
		button.setMaximumSize( new Dimension( iconSize, iconSize ) );
		button.setBackground( Color.white );
		button.setBorderPainted( false );
		//		button.setFocusPainted( false );
		//		button.setContentAreaFilled( false );
	}

	@Override
	public void removeOption( String optionGroup, String optionName )
	{

	}
}

