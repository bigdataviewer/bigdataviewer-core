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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import bdv.tools.links.PasteSettings;

/**
 * De/serialize {@link LinkSettings} to/from YAML file
 */
public class LinkSettingsIO
{
	private static final Logger LOG = LoggerFactory.getLogger( LinkSettingsIO.class );

	public static LinkSettings load( final String filename ) throws IOException
	{
		final FileReader input = new FileReader( filename );
		final LoaderOptions loaderOptions = new LoaderOptions();
		final Yaml yaml = new Yaml( new LinkSettingsConstructor( loaderOptions ) );
		final Iterable< Object > objs = yaml.loadAll( input );
		final List< Object > list = new ArrayList<>();
		objs.forEach( list::add );
		if ( list.size() != 1 )
			throw new IllegalArgumentException( "unexpected input in yaml file" );
		return ( LinkSettings ) list.get( 0 );
	}

	public static void save( final LinkSettings settings, final String filename ) throws IOException
	{
		new File( filename ).getParentFile().mkdirs();
		final FileWriter output = new FileWriter( filename );
		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );
		final Yaml yaml = new Yaml( new LinkSettingsRepresenter( dumperOptions ), dumperOptions );
		final ArrayList< Object > objects = new ArrayList<>();
		objects.add( settings );
		yaml.dumpAll( objects.iterator(), output );
		output.close();
	}

	static final Tag LINKSETTINGS_TAG = new Tag( "!linksettings" );

	static class LinkSettingsRepresenter extends Representer
	{
		public LinkSettingsRepresenter( final DumperOptions dumperOptions )
		{
			super( dumperOptions );
			this.representers.put( LinkSettings.class, new RepresentLinkSettings() );
		}

		private class RepresentLinkSettings implements Represent
		{
			@Override
			public Node representData( final Object data )
			{
				final LinkSettings s = ( LinkSettings ) data;
				final Map< String, Object > mapping = new LinkedHashMap<>();
				mapping.put( "pasteDisplayMode", s.pasteDisplayMode() );
				mapping.put( "pasteViewerTransform", s.pasteViewerTransform() );
				mapping.put( "recenterMethod", s.recenterMethod().name() );
				mapping.put( "rescaleMethod", s.rescaleMethod().name() );
				mapping.put( "pasteCurrentTimepoint", s.pasteCurrentTimepoint() );
				mapping.put( "pasteSourceVisibility", s.pasteSourceVisibility() );
				mapping.put( "pasteSourceConverterConfigs", s.pasteSourceConverterConfigs() );
				mapping.put( "pasteSourceConfigs", s.pasteSourceConfigs() );
				mapping.put( "sourceMatchingMethod", s.sourceMatchingMethod().name() );
				mapping.put( "showLinkSettingsCard", s.showLinkSettingsCard() );
				return representMapping( LINKSETTINGS_TAG, mapping, getDefaultFlowStyle() );
			}
		}
	}

	static class LinkSettingsConstructor extends Constructor
	{
		public LinkSettingsConstructor( final LoaderOptions loaderOptions )
		{
			super( loaderOptions );
			this.yamlConstructors.put( LINKSETTINGS_TAG, new ConstructLinkSettings() );
		}

		private class ConstructLinkSettings extends AbstractConstruct
		{
			@Override
			public Object construct( final Node node )
			{
				try
				{
					final Map< Object, Object > mapping = constructMapping( ( MappingNode ) node );
					final LinkSettings s = new LinkSettings();
					s.setPasteDisplayMode( ( Boolean ) mapping.get( "pasteDisplayMode" ) );
					s.setPasteViewerTransform( ( Boolean ) mapping.get( "pasteViewerTransform" ) );
					s.setRecenterMethod( PasteSettings.RecenterMethod.valueOf( ( String ) mapping.get( "recenterMethod" ) ) );
					s.setRescaleMethod( PasteSettings.RescaleMethod.valueOf( ( String ) mapping.get( "rescaleMethod" ) ) );
					s.setPasteCurrentTimepoint( ( Boolean ) mapping.get( "pasteCurrentTimepoint" ) );
					s.setPasteSourceVisibility( ( Boolean ) mapping.get( "pasteSourceVisibility" ) );
					s.setPasteSourceConverterConfigs( ( Boolean ) mapping.get( "pasteSourceConverterConfigs" ) );
					s.setPasteSourceConfigs( ( Boolean ) mapping.get( "pasteSourceConfigs" ) );
					s.setSourceMatchingMethod( PasteSettings.SourceMatchingMethod.valueOf( ( String ) mapping.get( "sourceMatchingMethod" ) ) );
					s.setShowLinkSettingsCard( ( Boolean ) mapping.get( "showLinkSettingsCard" ) );
					return s;
				}
				catch( final Exception e )
				{
					LOG.info( "Error constructing LinkSettings", e );
				}
				return null;
			}
		}
	}
}
