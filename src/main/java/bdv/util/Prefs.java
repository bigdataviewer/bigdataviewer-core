/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

public class Prefs
{
	public static boolean showScaleBar()
	{
		return getInstance().showScaleBar;
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

	private static Prefs instance;

	public static Prefs getInstance()
	{
		if ( instance == null )
		{
			instance = readPropertyFile();
		}
		return instance;
	}

	private static final String SHOW_SCALE_BAR = "show-scale-bar";
	private static final String SHOW_SCALE_BAR_IN_MOVIE = "show-scale-bar-in-movie";
	private static final String SCALE_BAR_COLOR = "scale-bar-color";
	private static final String SCALE_BAR_BG_COLOR = "scale-bar-bg-color";

	private final boolean showScaleBar;
	private final boolean showScaleBarInMovie;
	private final int scaleBarColor;
	private final int scaleBarBgColor;

	private Prefs( final Properties p )
	{
		showScaleBar = getBoolean( p, SHOW_SCALE_BAR, false );
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

	private static Prefs readPropertyFile()
	{
		final File file = new File( "bigdataviewer.properties" );
		return readPropertyFile( file );
	}

	private static Prefs readPropertyFile( final File file )
	{
		try
		{
			final InputStream stream = new FileInputStream( file );
			final Properties config = new Properties();
			config.load( stream );
			return new Prefs( config );
		}
		catch ( final IOException e )
		{
			System.out.println( "Cannot find the config file :" + file + ". Using default settings." );
			System.out.println( e.getMessage() );
		}
		return new Prefs( null );
	}

	public static Properties getDefaultProperties()
	{
		final Properties properties = new Properties();
		final Prefs prefs = new Prefs( null );
		properties.put( SHOW_SCALE_BAR, "" + prefs.showScaleBar );
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
