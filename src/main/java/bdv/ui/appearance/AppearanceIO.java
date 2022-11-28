/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.ui.appearance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIManager.LookAndFeelInfo;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import bdv.util.Prefs.OverlayPosition;

/**
 * De/serialize {@link Appearance} to/from YAML file
 */
public class AppearanceIO
{
	public static Appearance load( final String filename ) throws IOException
	{
		final FileReader input = new FileReader( filename );
		final Yaml yaml = new Yaml( new AppearanceConstructor() );
		final Iterable< Object > objs = yaml.loadAll( input );
		final List< Object > list = new ArrayList<>();
		objs.forEach( list::add );
		if ( list.size() != 1 )
			throw new IllegalArgumentException( "unexpected input in yaml file" );
		return ( Appearance ) list.get( 0 );
	}

	public static void save( final Appearance appearance, final String filename ) throws IOException
	{
		new File( filename ).getParentFile().mkdirs();
		final FileWriter output = new FileWriter( filename );
		final DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );
		final Yaml yaml = new Yaml( new AppearanceRepresenter(), dumperOptions );
		final ArrayList< Object > objects = new ArrayList<>();
		objects.add( appearance );
		yaml.dumpAll( objects.iterator(), output );
		output.close();
	}

	static final Tag APPEARANCE_TAG = new Tag( "!appearance" );
	static final Tag LOOK_AND_FEEL_TAG = new Tag( "!lookandfeel" );

	static class AppearanceRepresenter extends Representer
	{
		public AppearanceRepresenter()
		{
			this.representers.put( Appearance.class, new RepresentAppearance() );
			this.representers.put( Hex.class, new RepresentHex() );
			this.representers.put( LookAndFeelInfo.class, new RepresentLookAndFeelInfo() );
		}

		private class RepresentAppearance implements Represent
		{
			@Override
			public Node representData( final Object data )
			{
				final Appearance a = ( Appearance ) data;
				final Map< String, Object > mapping = new LinkedHashMap<>();
				mapping.put( "showScaleBar", a.showScaleBar() );
				mapping.put( "showScaleBarInMovie", a.showScaleBarInMovie() );
				mapping.put( "showMultibox", a.showMultibox() );
				mapping.put( "showTextOverlay", a.showTextOverlay() );
				mapping.put( "sourceNameOverlayPosition", a.sourceNameOverlayPosition().toString() );
				mapping.put( "scaleBarColor", hex( a.scaleBarColor() ) );
				mapping.put( "scaleBarBgColor", hex( a.scaleBarBgColor() ) );
				mapping.put( "lookAndFeel", a.lookAndFeel() );
				return representMapping( APPEARANCE_TAG, mapping, getDefaultFlowStyle() );
			}
		}

		private static Hex hex( final int i )
		{
			return new Hex( i );
		}

		private static class Hex
		{
			int value;

			Hex( final int value )
			{
				this.value = value;
			}
		}

		private class RepresentHex implements Represent
		{
			@Override
			public Node representData( final Object data )
			{
				return representScalar( Tag.INT, String.format( "0x%08x", ( ( Hex ) data ).value ) );
			}
		}

		private class RepresentLookAndFeelInfo implements Represent
		{
			@Override
			public Node representData( final Object data )
			{
				final LookAndFeelInfo info = ( LookAndFeelInfo ) data;
				final String str = info == null ? null : info.getName();
				return representScalar( LOOK_AND_FEEL_TAG, str );
			}
		}
	}

	static class AppearanceConstructor extends Constructor
	{
		public AppearanceConstructor()
		{
			this.yamlConstructors.put( APPEARANCE_TAG, new ConstructAppearance() );
			this.yamlConstructors.put( LOOK_AND_FEEL_TAG, new ConstructLookAndFeelInfo() );
		}

		private class ConstructAppearance extends AbstractConstruct
		{
			@Override
			public Object construct( final Node node )
			{
				try
				{
					final Map< Object, Object > mapping = constructMapping( ( MappingNode ) node );
					final Appearance a = new Appearance();
					a.setShowScaleBar( ( Boolean ) mapping.get( "showScaleBar" ) );
					a.setShowScaleBarInMovie( ( Boolean ) mapping.get( "showScaleBarInMovie" ) );
					a.setShowMultibox( ( Boolean ) mapping.get( "showMultibox" ) );
					a.setShowTextOverlay( ( Boolean ) mapping.get( "showTextOverlay" ) );
					a.setSourceNameOverlayPosition( overlayPosition( mapping.get( "sourceNameOverlayPosition" ) ) );
					a.setScaleBarColor( hexColor( mapping.get( "scaleBarColor" ) ) );
					a.setScaleBarBgColor( hexColor( mapping.get( "scaleBarBgColor" ) ) );
					a.setLookAndFeel( ( LookAndFeelInfo ) mapping.get( "lookAndFeel" ) );
					return a;
				}
				catch( final Exception e )
				{
					e.printStackTrace();
				}
				return null;
			}
		}

		private static int hexColor( final Object rgba )
		{
			return ( ( Number ) rgba ).intValue();
		}

		private static OverlayPosition overlayPosition( final Object str )
		{
			return OverlayPosition.valueOf( ( String ) str );
		}

		private class ConstructLookAndFeelInfo extends AbstractConstruct
		{
			@Override
			public Object construct( final Node node )
			{
				final String name = constructScalar( ( ScalarNode ) node );
				return Appearance.lookAndFeelInfoForName( name );
			}
		}
	}
}
