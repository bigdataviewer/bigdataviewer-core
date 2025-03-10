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
package bdv.ui.keymap;

import bdv.KeyConfigScopes;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.scijava.Context;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

/**
 * Manages a collection of {@link Keymap}.
 * <p>
 * Provides de/serialization of user-defined keymaps.
 *
 * @author Tobias Pietzsch
 */
public class KeymapManager extends AbstractKeymapManager< KeymapManager >
{
	private static final String CONFIG_FILE_NAME = "keymaps/";

	private final String configFile;

	private CommandDescriptions descriptions;

	public KeymapManager( final String configDir )
	{
		this( true, configDir );
		discoverCommandDescriptions();
	}

	public KeymapManager()
	{
		this( false, null );
	}

	private KeymapManager( final boolean loadStyles, final String configDir )
	{
		configFile = configDir == null ? null : configDir + "/" + CONFIG_FILE_NAME;
		if ( loadStyles )
			loadStyles();
	}

	public synchronized void setCommandDescriptions( final CommandDescriptions descriptions )
	{
		this.descriptions = descriptions;
		final Consumer< Keymap > augmentInputTriggerConfig = k -> descriptions.augmentInputTriggerConfig( k.getConfig() );
		builtinStyles.forEach( augmentInputTriggerConfig );
		userStyles.forEach( augmentInputTriggerConfig );
	}

	public CommandDescriptions getCommandDescriptions()
	{
		return descriptions;
	}

	/**
	 * Discover all {@code CommandDescriptionProvider}s with scope {@link KeyConfigScopes#BIGDATAVIEWER},
	 * build {@code CommandDescriptions}, and set it.
	 */
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = new Context( PluginService.class );
		context.inject( builder );
		builder.discoverProviders( KeyConfigScopes.BIGDATAVIEWER );
		context.dispose();
		setCommandDescriptions( builder.build() );
	}

	@Override
	protected List< Keymap > loadBuiltinStyles()
	{
		synchronized ( KeymapManager.class )
		{
			if ( loadedBuiltinStyles == null )
				try
				{
					loadedBuiltinStyles = Arrays.asList(
							loadBuiltinStyle( "Default", "default.yaml" ) );
				}
				catch ( final IOException e )
				{
					e.printStackTrace();
					loadedBuiltinStyles = Arrays.asList( createDefaultStyle( "Default" ) );
				}
		}
		return loadedBuiltinStyles;
	}

	private static List< Keymap > loadedBuiltinStyles;

	private static Keymap loadBuiltinStyle( final String name, final String filename ) throws IOException
	{
		try ( Reader reader = new InputStreamReader( KeymapManager.class.getResourceAsStream( filename ) ) )
		{
			return new Keymap( name, new InputTriggerConfig( YamlConfigIO.read( reader ) ) );
		}
	}

	private static Keymap createDefaultStyle( final String name )
	{
		final Context context = new Context( PluginService.class );
		final InputTriggerConfig defaultKeyconfig = DumpInputConfig.buildCommandDescriptions( context ).createDefaultKeyconfig();
		return new Keymap( name, defaultKeyconfig );
	}

	public void loadStyles()
	{
		try
		{
			loadStyles( new File( configFile ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void saveStyles()
	{
		try
		{
			saveStyles( new File( configFile ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
