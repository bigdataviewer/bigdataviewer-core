/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static bdv.util.Prefs.OverlayPosition.TOP_CENTER;

public class Prefs
{
	public static boolean showScaleBar()
	{
		return getInstance().showScaleBar;
	}

	public static boolean showMultibox()
	{
		return getInstance().showMultibox;
	}

	public static boolean showTextOverlay()
	{
		return getInstance().showTextOverlay;
	}

	public static OverlayPosition sourceNameOverlayPosition()
	{
		return getInstance().sourceNameOverlayPosition;
	}

	public static boolean showScaleBarInMovie()
	{
		return getInstance().showScaleBarInMovie;
	}

	public static int scaleBarColor()
	{
		return getInstance().scaleBarColor;
	}

	public static int scaleBarBgColor()
	{
		return getInstance().scaleBarBgColor;
	}

	public static void showScaleBar( final boolean show )
	{
		getInstance().showScaleBar = show;
	}

	public static void showMultibox( final boolean show )
	{
		getInstance().showMultibox = show;
	}

	public static void showTextOverlay( final boolean show )
	{
		getInstance().showTextOverlay = show;
	}

	public static void sourceNameOverlayPosition( final OverlayPosition position )
	{
		getInstance().sourceNameOverlayPosition = position;
	}

	public static void showScaleBarInMovie( final boolean show )
	{
		getInstance().showScaleBarInMovie = show;
	}

	public static void scaleBarColor( final int color )
	{
		getInstance().scaleBarColor = color;
	}

	public static void scaleBarBgColor( final int color )
	{
		getInstance().scaleBarBgColor = color;
	}

	private static Prefs instance;

	public static Prefs getInstance()
	{
		if ( instance == null )
		{
			instance = readPropertyFile();
		}
		return instance;
	}

	public enum OverlayPosition
	{
		TOP_CENTER, // centered at the top. default
		TOP_RIGHT   // aligned with the position and time point overlays
	}

	private static final String SHOW_SCALE_BAR = "show-scale-bar";
	private static final String SHOW_MULTIBOX_OVERLAY = "show-multibox-overlay";
	private static final String SHOW_TEXT_OVERLAY = "show-text-overlay";
	private static final String SOURCE_NAME_OVERLAY_POSITION = "source-name-overlay-position";
	private static final String SHOW_SCALE_BAR_IN_MOVIE = "show-scale-bar-in-movie";
	private static final String SCALE_BAR_COLOR = "scale-bar-color";
	private static final String SCALE_BAR_BG_COLOR = "scale-bar-bg-color";

	private boolean showScaleBar;
	private boolean showMultibox;
	private boolean showTextOverlay;
	private OverlayPosition sourceNameOverlayPosition;
	private boolean showScaleBarInMovie;
	private int scaleBarColor;
	private int scaleBarBgColor;

	private Prefs( final Properties p )
	{
		showScaleBar = getBoolean( p, SHOW_SCALE_BAR, false );
		showMultibox = getBoolean( p, SHOW_MULTIBOX_OVERLAY, true );
		showTextOverlay = getBoolean( p, SHOW_TEXT_OVERLAY, true );
		sourceNameOverlayPosition = getOverlayPosition( p, SOURCE_NAME_OVERLAY_POSITION, TOP_CENTER );
		showScaleBarInMovie = getBoolean( p, SHOW_SCALE_BAR_IN_MOVIE, false );
		scaleBarColor = getInt( p, SCALE_BAR_COLOR, 0xffffffff );
		scaleBarBgColor = getInt( p, SCALE_BAR_BG_COLOR, 0x88000000 );
	}

	private boolean getBoolean( final Properties p, final String key, final boolean defaultValue )
	{
		final String property = ( p != null ) ? p.getProperty( key ) : null;
		return ( property != null ) ? Boolean.parseBoolean( property ) : defaultValue;
	}

	private int getInt( final Properties p, final String key, final int defaultValue )
	{
		try
		{
			final String property = ( p != null ) ? p.getProperty( key ) : null;
			return ( property != null ) ? ( int ) ( Long.decode( property ).longValue() & 0xffffffff ) : defaultValue;
		}
		catch ( final NumberFormatException e )
		{
			e.printStackTrace();
			return defaultValue;
		}
	}

	private double getDouble( final Properties p, final String key, final double defaultValue )
	{
		try
		{
			final String property = ( p != null ) ? p.getProperty( key ) : null;
			return ( property != null ) ? Double.parseDouble( property ) : defaultValue;
		}
		catch ( final NumberFormatException e )
		{
			e.printStackTrace();
			return defaultValue;
		}
	}

	private OverlayPosition getOverlayPosition( final Properties p, final String key, final OverlayPosition defaultValue )
	{
		try
		{
			final String property = ( p != null ) ? p.getProperty( key ) : null;
			return ( property != null ) ? OverlayPosition.valueOf( property ) : defaultValue;
		}
		catch ( final IllegalArgumentException e )
		{
			e.printStackTrace();
			return defaultValue;
		}
	}

	private static Prefs readPropertyFile()
	{
		// try "bdvkeyconfig.yaml" in current directory
		// then "~/.bdv/bdvkeyconfig.yaml"
		final File[] files = new File[] {
				new File( "bigdataviewer.properties" ),
				new File( System.getProperty( "user.home" ) + "/.bdv/bigdataviewer.properties" ) };
		for ( final File file : files )
		{
			if ( file.isFile() )
			{
				try
				{
					return readPropertyFile( file );
				}
				catch ( final IOException e )
				{
					System.err.println( "Cannot read config file :" + file );
					e.printStackTrace();
				}
			}
		}
		return new Prefs( null );
	}

	private static Prefs readPropertyFile( final File file ) throws IOException
	{
		final InputStream stream = new FileInputStream( file );
		final Properties config = new Properties();
		config.load( stream );
		return new Prefs( config );
	}

	public static Properties getDefaultProperties()
	{
		final Properties properties = new Properties();
		final Prefs prefs = new Prefs( null );
		properties.put( SHOW_SCALE_BAR, "" + prefs.showScaleBar );
		properties.put( SHOW_MULTIBOX_OVERLAY, "" + prefs.showMultibox );
		properties.put( SHOW_TEXT_OVERLAY, "" + prefs.showTextOverlay );
		properties.put( SOURCE_NAME_OVERLAY_POSITION, "" + prefs.sourceNameOverlayPosition );
		properties.put( SHOW_SCALE_BAR_IN_MOVIE, "" + prefs.showScaleBarInMovie );
		properties.put( SCALE_BAR_COLOR, "" + prefs.scaleBarColor );
		properties.put( SCALE_BAR_BG_COLOR, "" + prefs.scaleBarBgColor );
		return properties;
	}

	public static void printDefaultProperties() throws IOException
	{
		getDefaultProperties().store( System.out, "default properties" );
	}
}
