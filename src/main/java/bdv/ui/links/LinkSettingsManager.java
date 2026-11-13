/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.ui.links;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the {@link LinkSettings} (save/load config file).
 */
public class LinkSettingsManager
{
	private static final Logger LOG = LoggerFactory.getLogger( LinkSettingsManager.class );

	private static final String CONFIG_FILE_NAME = "linksettings.yaml";

	private final String configFile;

	/**
	 * The managed LinkSettings. This will be updated with changes from the
	 * Preferences (on "Apply" or "Ok").
	 */
	private final LinkSettings settings;

	public LinkSettingsManager()
	{
		this( null );
	}

	public LinkSettingsManager( final String configDir )
	{
		configFile = configDir == null ? null : configDir + "/" + CONFIG_FILE_NAME;
		settings = new LinkSettings();
		load();
	}

	public LinkSettings linkSettings()
	{
		return settings;
	}

	void load()
	{
		load( configFile );
	}

	void load( final String filename )
	{
		try
		{
			final LinkSettings s = LinkSettingsIO.load( filename );
			settings.set( s );
		}
		catch ( final FileNotFoundException e )
		{
			LOG.info( "LinkSettings file {} not found. Using defaults.", filename, e );
		}
		catch ( final Exception e )
		{
			LOG.warn( "Error while reading LinkSettings file {}. Using defaults.", filename, e );
		}
	}

	void save()
	{
		save( configFile );
	}

	void save( final String filename )
	{
		try
		{
			LinkSettingsIO.save( settings, filename );
		}
		catch ( final Exception e )
		{
			LOG.warn( "Error while writing LinkSettings file {}", filename, e );
		}
	}
}
