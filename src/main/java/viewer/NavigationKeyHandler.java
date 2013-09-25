package viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import viewer.SpimViewer.AlignPlane;

public class NavigationKeyHandler
{

	private static final Properties DEFAULT_KEYBINGS = new Properties();
	static
	{
		DEFAULT_KEYBINGS.setProperty( "I", "toggle interpolation" );
		DEFAULT_KEYBINGS.setProperty( "F", "toggle fused mode" );
		DEFAULT_KEYBINGS.setProperty( "G", "toggle grouping" );

		final String[] numkeys = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };
		for ( int i = 0; i < numkeys.length; ++i )
		{
			DEFAULT_KEYBINGS.setProperty( numkeys[ i ], "set current source " + i );
			DEFAULT_KEYBINGS.setProperty( "shift " + numkeys[ i ], "toggle source visibility " + i );
		}

		DEFAULT_KEYBINGS.setProperty( "shift Z", "align XY plane" );
		DEFAULT_KEYBINGS.setProperty( "shift X", "align ZY plane" );
		DEFAULT_KEYBINGS.setProperty( "shift Y", "align XZ plane" );
		DEFAULT_KEYBINGS.setProperty( "shift A", "align XZ plane" );

		DEFAULT_KEYBINGS.setProperty( "CLOSE_BRACKET", "next timepoint" );
		DEFAULT_KEYBINGS.setProperty( "M", "next timepoint" );
		DEFAULT_KEYBINGS.setProperty( "OPEN_BRACKET", "previous timepoint" );
		DEFAULT_KEYBINGS.setProperty( "N", "previous timepoint" );
	}

	private final SpimViewer viewer;

	public NavigationKeyHandler( final SpimViewer viewer )
	{
		this.viewer = viewer;
		viewer.getKeybindings().addActionMap( "navigation", createActionMap() );
		viewer.getKeybindings().addInputMap( "navigation", readPropertyFile() );
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
		}

		return map;
	}

	protected ActionMap createActionMap()
	{
		final ActionMap map = new ActionMap();

		map.put( "toggle interpolation", NavigationActions.getToggleInterpolationAction( viewer ) );
		map.put( "toggle fused mode", NavigationActions.getToggleFusedModeAction( viewer ) );
		map.put( "toggle grouping", NavigationActions.getToggleGroupingAction( viewer ) );

		for ( int i = 0; i < 10; ++i )
		{
			map.put( "set current source " + i, NavigationActions.getSetCurrentSource( viewer, i ) );
			map.put( "toggle source visibility " + i, NavigationActions.getToggleSourceVisibilityAction( viewer, i ) );
		}

		map.put( "align XY plane", NavigationActions.getAlignPlaneAction( viewer, AlignPlane.XY ) );
		map.put( "align ZY plane", NavigationActions.getAlignPlaneAction( viewer, AlignPlane.ZY ) );
		map.put( "align XZ plane", NavigationActions.getAlignPlaneAction( viewer, AlignPlane.XZ ) );

		map.put( "next timepoint", NavigationActions.getNextTimePointAction( viewer ) );
		map.put( "previous timepoint", NavigationActions.getPreviousTimePointAction( viewer ) );

		return map;
	}
}
