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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

public final class KeyProperties
{
	private final HashSet< KeyStroke > allKeys;

	private final HashMap< String, HashSet< KeyStroke > > actionToKeysMap;

	private static final HashSet< KeyStroke > emptySet = new HashSet< KeyStroke >();

	public KeyProperties( final Properties properties )
	{
		allKeys = new HashSet< KeyStroke >();
		actionToKeysMap = new HashMap< String, HashSet<KeyStroke> >();
		if ( properties != null )
		{
			final Set< Entry< Object, Object > > entries = properties.entrySet();
			for ( final Entry< Object, Object > entry : entries )
			{
				final String keyname = ( String ) entry.getKey();
				final String action = ( String ) entry.getValue();
				final KeyStroke key = KeyStroke.getKeyStroke( keyname );
				if ( key == null )
					System.err.println( "key name formatted incorrectly: \"" + keyname + "\"" );
				else
				{
					allKeys.add( key );
					HashSet< KeyStroke > keys = actionToKeysMap.get( action );
					if ( keys == null )
					{
						keys = new HashSet< KeyStroke >();
						actionToKeysMap.put( action, keys );
					}
					keys.add( key );
				}
			}
		}
	}

	public static KeyProperties readPropertyFile()
	{
		final File file = new File( "bigdataviewer.keys.properties" );
		return readPropertyFile( file );
	}

	public static KeyProperties readPropertyFile( final File file )
	{
		try
		{
			final InputStream stream = new FileInputStream( file );
			final Properties config = new Properties();
			config.load( stream );
			return new KeyProperties( config );
		}
		catch ( final IOException e )
		{
			System.out.println( "Cannot find the config file :" + file + ". Using default key bindings." );
			System.out.println( e.getMessage() );
		}
		return new KeyProperties( null );
	}

	public Set< KeyStroke > getKeyStrokes( final String actionName )
	{
		final HashSet< KeyStroke > keys = actionToKeysMap.get( actionName );
		return keys == null ? emptySet : keys;
	}

	public Set< KeyStroke > getAllKeyStrokes()
	{
		return allKeys;
	}

	public KeyStrokeAdder adder( final InputMap map )
	{
		return new KeyStrokeAdder( map, this );
	}

	public static class KeyStrokeAdder
	{
		private final InputMap map;

		private final KeyProperties config;

		public KeyStrokeAdder( final InputMap map, final KeyProperties config )
		{
			this.map = map;
			this.config = config;
		}

		public void put( final String actionName, final KeyStroke ... defaultKeyStrokes )
		{
			final Set< KeyStroke > keys = config.getKeyStrokes( actionName );
			for ( final KeyStroke key : keys )
				map.put( key, actionName );

			final Set< KeyStroke > overriddenKeys = config.getAllKeyStrokes();
			for ( final KeyStroke key : defaultKeyStrokes )
				if ( ! overriddenKeys.contains( key ) )
					map.put( key, actionName );
		}

		public void put( final String actionName, final String ... defaultKeyStrokes )
		{
			final KeyStroke[] keys = new KeyStroke[ defaultKeyStrokes.length ];
			int i = 0;
			for ( final String s : defaultKeyStrokes )
				keys[ i++ ] = KeyStroke.getKeyStroke( s );
			put( actionName, keys );
		}
	}

	public static Properties createPropertiesFromInputMap( final InputMap inputMap )
	{
		final Properties properties = new Properties();
		final KeyStroke[] keys = inputMap.allKeys();
		for ( final KeyStroke key : keys )
		{
			final Object actionName = inputMap.get( key );
			properties.put( key.toString(), actionName.toString() );
		}
		return properties;
	}
}
