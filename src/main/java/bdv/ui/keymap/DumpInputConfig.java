/*-
 * #%L
 * Mastodon
 * %%
 * Copyright (C) 2014 - 2021 Tobias Pietzsch, Jean-Yves Tinevez
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

import bdv.BigDataViewerActions;
import bdv.KeyConfigContexts;
import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;
import bdv.viewer.NavigationActions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.scijava.Context;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

class DumpInputConfig
{
	public static void writeToYaml( final String fileName, final InputTriggerConfig config ) throws IOException
	{
		mkdirs( fileName );
		final List< InputTriggerDescription > descriptions = new InputTriggerDescriptionsBuilder( config ).getDescriptions();
		YamlConfigIO.write( descriptions, fileName );
	}

	public static void writeDefaultConfigToYaml( final String fileName, final Context context ) throws IOException
	{
		mkdirs( fileName );
		final List< InputTriggerDescription > descriptions = new InputTriggerDescriptionsBuilder( buildCommandDescriptions( context ).createDefaultKeyconfig() ).getDescriptions();
		YamlConfigIO.write( descriptions, fileName );
	}

	private static boolean mkdirs( final String fileName )
	{
		final File dir = new File( fileName ).getParentFile();
		return dir != null && dir.mkdirs();
	}

	static CommandDescriptions buildCommandDescriptions( final Context context )
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		context.inject( builder );

		builder.addManually( new BigDataViewerActions.Descriptions(), KeyConfigContexts.BIGDATAVIEWER );
		builder.addManually( new NavigationActions.Descriptions(), KeyConfigContexts.BIGDATAVIEWER );
		builder.addManually( new TransformEventHandler3D.Descriptions(), KeyConfigContexts.BIGDATAVIEWER );
		builder.addManually( new TransformEventHandler2D.Descriptions(), KeyConfigContexts.BIGDATAVIEWER );

		builder.verifyManuallyAdded(); // TODO: It should be possible to filter by Scope here

		return builder.build();
	}

	public static void main( String[] args ) throws IOException
	{
		final String target = KeymapManager.class.getResource( "default.yaml" ).getFile();
		final File resource = new File( target.replaceAll( "target/classes", "src/main/resources" ) );
		System.out.println( "resource = " + resource );
		writeDefaultConfigToYaml( resource.getAbsolutePath(), new Context( PluginService.class ) );
	}
}
