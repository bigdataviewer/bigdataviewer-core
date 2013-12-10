package viewer;

import static viewer.NavigationActions.ALIGN_PLANE;
import static viewer.NavigationActions.NEXT_TIMEPOINT;
import static viewer.NavigationActions.PREVIOUS_TIMEPOINT;
import static viewer.NavigationActions.SET_CURRENT_SOURCE;
import static viewer.NavigationActions.TOGGLE_FUSED_MODE;
import static viewer.NavigationActions.TOGGLE_GROUPING;
import static viewer.NavigationActions.TOGGLE_INTERPOLATION;
import static viewer.NavigationActions.TOGGLE_SOURCE_VISIBILITY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import viewer.ViewerPanel.AlignPlane;
import viewer.gui.InputActionBindings;

public class NavigationKeyHandler
{
	private static final Properties DEFAULT_KEYBINGS = new Properties();
	static
	{
		DEFAULT_KEYBINGS.setProperty( "I", TOGGLE_INTERPOLATION );
		DEFAULT_KEYBINGS.setProperty( "F", TOGGLE_FUSED_MODE );
		DEFAULT_KEYBINGS.setProperty( "G", TOGGLE_GROUPING );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for ( int i = 0; i < numkeys.length; ++i )
		{
			DEFAULT_KEYBINGS.setProperty( numkeys[ i ], String.format( SET_CURRENT_SOURCE, i ) );
			DEFAULT_KEYBINGS.setProperty( "shift " + numkeys[ i ], String.format( TOGGLE_SOURCE_VISIBILITY, i ) );
		}

		DEFAULT_KEYBINGS.setProperty( "shift Z", String.format( ALIGN_PLANE, AlignPlane.XY ) );
		DEFAULT_KEYBINGS.setProperty( "shift X", String.format( ALIGN_PLANE, AlignPlane.ZY ) );
		DEFAULT_KEYBINGS.setProperty( "shift Y", String.format( ALIGN_PLANE, AlignPlane.XZ ) );
		DEFAULT_KEYBINGS.setProperty( "shift A", String.format( ALIGN_PLANE, AlignPlane.XZ ) );

		DEFAULT_KEYBINGS.setProperty( "CLOSE_BRACKET", NEXT_TIMEPOINT );
		DEFAULT_KEYBINGS.setProperty( "M", NEXT_TIMEPOINT );
		DEFAULT_KEYBINGS.setProperty( "OPEN_BRACKET", PREVIOUS_TIMEPOINT );
		DEFAULT_KEYBINGS.setProperty( "N", PREVIOUS_TIMEPOINT );
	}

	private final ViewerPanel viewer;

	public NavigationKeyHandler( final InputActionBindings inputActionBindings, final ViewerPanel viewer )
	{
		this.viewer = viewer;
		inputActionBindings.addActionMap( "navigation", createActionMap() );
		inputActionBindings.addInputMap( "navigation", readPropertyFile() );
	}

	/**
	 * Unused, yet.
	 *
	 * @return
	 */
	protected InputMap readPropertyFile()
	{
		Properties config;
		final File file = new File( "spimviewer.properties" );
		try
		{
			final InputStream stream = new FileInputStream( file );
			config = new Properties();
			config.load( stream );
		}
		catch ( final IOException e )
		{
			System.out.println( "Cannot find the config file :" + file + ". Using default key bindings." );
			System.out.println( e.getMessage() );
			config = DEFAULT_KEYBINGS;
		}

		return generateMapFrom( config );
	}

	private InputMap generateMapFrom( final Properties config )
	{
		final InputMap map = new InputMap();
		for ( final Object obj : config.keySet() )
		{

			final String key = ( String ) obj;
			final String command = config.getProperty( key );
			map.put( KeyStroke.getKeyStroke( key ), command );
//			System.out.println( KeyStroke.getKeyStroke( key ) + " -- " + key );
		}

		return map;
	}

	protected ActionMap createActionMap()
	{
		return NavigationActions.createActionMap( viewer, 10 );
	}
}
