package bdv.ui.appearance;

import bdv.util.Prefs.OverlayPosition;
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
			public Node representData( final Object data )
			{
				return representScalar( Tag.INT, String.format( "0x%08x", ( ( Hex ) data ).value ) );
			}
		}

		private class RepresentLookAndFeelInfo implements Represent
		{
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

		private static int hexColor( Object rgba )
		{
			return ( ( Number ) rgba ).intValue();
		}

		private static OverlayPosition overlayPosition( Object str )
		{
			return OverlayPosition.valueOf( ( String ) str );
		}

		private class ConstructLookAndFeelInfo extends AbstractConstruct
		{
			public Object construct( Node node )
			{
				final String name = constructScalar( ( ScalarNode ) node );
				return Appearance.lookAndFeelInfoForName( name );
			}
		}
	}
}
