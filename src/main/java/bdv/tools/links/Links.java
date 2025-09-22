package bdv.tools.links;

import static bdv.BigDataViewer.createConverterSetup;
import static bdv.tools.links.PasteSettings.RecenterMethod.MOUSE_POS;
import static bdv.tools.links.PasteSettings.RecenterMethod.PANEL_CENTER;
import static bdv.tools.links.PasteSettings.SourceMatchingMethod.BY_INDEX;
import static bdv.tools.links.PasteSettings.SourceMatchingMethod.BY_SPEC_LOAD_MISSING;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.tools.JsonUtils;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.links.BdvPropertiesV0.SourceConverterConfig;
import bdv.tools.links.PasteSettings.RecenterMethod;
import bdv.tools.links.PasteSettings.RescaleMethod;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Cast;

class Links
{
	private static final Logger LOG = LoggerFactory.getLogger( Links.class );

	static JsonElement copyJson(
			final AbstractViewerPanel panel,
			final ConverterSetups converterSetups,
			final ResourceManager resources )
	{
		final BdvPropertiesV0 properties = copyV0( panel, converterSetups, resources );
		final Gson gson = JsonUtils.gson();
		final VersionAndProperties versionAndProperties = new VersionAndProperties( 0, gson.toJsonTree( properties ) );
		return gson.toJsonTree( versionAndProperties );
	}

	static void paste(
			final JsonElement json,
			final AbstractViewerPanel panel,
			final ConverterSetups converterSetups,
			final PasteSettings pasteSettings,
			final ResourceManager resources ) throws JsonParseException
	{
		final Gson gson = JsonUtils.gson();
		final VersionAndProperties versionAndProperties = gson.fromJson( json, VersionAndProperties.class );
		if ( versionAndProperties.version() == 0 )
		{
			final BdvPropertiesV0 properties = gson.fromJson( versionAndProperties.properties(), BdvPropertiesV0.class );
			pasteV0( properties, panel, converterSetups, resources, pasteSettings );
			panel.requestRepaint();
		}
		else
		{
			throw new JsonParseException( "Unsupported version: " + versionAndProperties.version() );
		}
	}

	private static BdvPropertiesV0 copyV0(
			final AbstractViewerPanel panel,
			final ConverterSetups converterSetups,
			final ResourceManager resources )
	{
		final ViewerState state = panel.state();
		synchronized ( state )
		{
			final Dimensions panelsize = new FinalDimensions(
					panel.getDisplayComponent().getWidth(),
					panel.getDisplayComponent().getHeight() );
			final Point mouse = new Point( 2 );
			panel.getMouseCoordinates( mouse );
			return new BdvPropertiesV0( state, converterSetups, panelsize, mouse, resources );
		}
	}

	private static void pasteV0(
			final BdvPropertiesV0 properties,
			final AbstractViewerPanel panel,
			final ConverterSetups converterSetups,
			final ResourceManager resources,
			final PasteSettings pasteSettings )
	{
		final ViewerState state = panel.state();
		synchronized ( state )
		{
			if ( pasteSettings.pasteViewerTransform() )
			{
				final int panelWidth = panel.getDisplayComponent().getWidth();
				final int panelHeight = panel.getDisplayComponent().getHeight();
				final AffineTransform3D transform = adjustedViewerTransform(
						properties, panelWidth, panelHeight,
						pasteSettings.recenterMethod(),
						pasteSettings.rescaleMethod() );
				state.setViewerTransform( transform );
			}

			if ( pasteSettings.pasteCurrentTimepoint() )
			{
				state.setCurrentTimepoint( properties.timepoint() );
			}

			final int[] sourceMapping = getSourceMapping(
					pasteSettings.sourceMatchingMethod(),
					properties.sourceSpecs(),
					state, converterSetups, resources );

			if ( pasteSettings.pasteSourceConfigs() )
			{
				setSourceConfigs( sourceMapping, properties.sourceConfigs(), state.getSources(), resources );
			}

			if ( pasteSettings.pasteDisplayMode() )
			{
				state.setDisplayMode( properties.displaymode() );
			}

			if ( pasteSettings.pasteInterpolation() )
			{
				state.setInterpolation( properties.interpolation() );
			}
			if ( pasteSettings.pasteConverterConfigs() )
			{
				setConverterConfigs( sourceMapping, properties.converterConfigs(), state.getSources(), converterSetups );
			}
			if ( pasteSettings.pasteSourceVisibility() )
			{
				setSourceVisibility( sourceMapping, properties.currentSourceIndex(), properties.activeSourceIndices(), state );
			}
		}
	}

	private static int[] getSourceMapping(
			final PasteSettings.SourceMatchingMethod method,
			final List< ResourceSpec< ? > > specs,
			final ViewerState state,
			final ConverterSetups converterSetups,
			final ResourceManager resources )
	{
		final int[] sourceMapping = matchSources( method, specs, state.getSources(), resources );
		if ( method == BY_SPEC_LOAD_MISSING )
			loadUnmatchedSources( sourceMapping, specs, state, converterSetups, resources );
		return sourceMapping;
	}

	// return a mapping from index i in specs to index j in sources
	// (j==-1 if no source matches the spec at i)
	private static int[] matchSources(
			final PasteSettings.SourceMatchingMethod method,
			final List< ResourceSpec< ? > > specs,
			final List< SourceAndConverter< ? > > sources,
			final ResourceManager resources )
	{
		final int[] matches = new int[ specs.size() ];
		if ( method == BY_INDEX )
		{
			final int numSources = sources.size();
			Arrays.setAll( matches, i -> i < numSources ? i : -1 );
		}
		else
		{
			for ( int i = 0; i < specs.size(); i++ )
			{
				final SourceAndConverter< ? > soc = Cast.unchecked( resources.getResource( specs.get( i ) ) );
				matches[ i ] = sources.indexOf( soc );
			}
		}
		return matches;
	}

	private static void loadUnmatchedSources(
			final int[] sourceMapping,
			final List< ResourceSpec< ? > > specs,
			final ViewerState state,
			final ConverterSetups converterSetups,
			final ResourceManager resources )
	{
		for ( int i = 0; i < sourceMapping.length; i++ )
		{
			if ( sourceMapping[ i ] < 0 )
			{
				final ResourceSpec< SourceAndConverter< ? > > spec = Cast.unchecked( specs.get( i ) );
				try
				{
					final SourceAndConverter< ? > soc = resources.
							getOrCreateResource( spec );
					final ConverterSetup setup = createConverterSetup( soc, 0 );
					converterSetups.put( soc, setup );
					state.addSource( soc );
					sourceMapping[ i ] = state.getSources().indexOf( soc );
				}
				catch ( final ResourceCreationException e )
				{
					LOG.debug( "Couldn't load resource.", e );
				}
			}
		}
	}

	private static void setSourceConfigs(
			final int[] sourceMapping,
			final List< ResourceConfig > configs,
			final List< SourceAndConverter< ? > > sources,
			final ResourceManager resources )
	{
		for ( int i = 0; i < sourceMapping.length; i++ )
		{
			if ( sourceMapping[ i ] >= 0 )
			{
				final SourceAndConverter< ? > soc = sources.get( sourceMapping[ i ] );
				final ResourceSpec< ? > spec = resources.getResourceSpec( soc );
				configs.get( i ).apply( spec, resources );
			}
		}
	}

	// apply display range and color settings to matched sources
	// sourceMapping[i]==j --> converterConfigs[i] corresponds to sources[j]
	private static void setConverterConfigs(
			final int[] sourceMapping,
			final List< SourceConverterConfig > converterConfigs,
			final List< SourceAndConverter< ? > > sources,
			final ConverterSetups converterSetups )
	{
		for ( int i = 0; i < sourceMapping.length; i++ )
		{
			if ( sourceMapping[ i ] >= 0 )
			{
				final SourceConverterConfig config = converterConfigs.get( i );
				if ( config != null )
				{
					final SourceAndConverter< ? > soc = sources.get( sourceMapping[ i ] );
					final ConverterSetup setup = converterSetups.getConverterSetup( soc );
					if ( setup != null )
					{
						converterSetups.getBounds().setBounds( setup, config.bounds() );
						setup.setDisplayRange( config.rangeMin(), config.rangeMax() );
						setup.setColor( config.color() );
					}
				}
			}
		}
	}

	private static void setSourceVisibility(
			final int[] sourceMapping,
			final int currentSourceIndex,
			final int[] activeSourceIndices,
			final ViewerState state )
	{
		final List< SourceAndConverter< ? > > sources = state.getSources();
		for ( int i = 0; i < sourceMapping.length; i++ )
		{
			if ( sourceMapping[ i ] >= 0 )
			{
				final SourceAndConverter< ? > soc = sources.get( sourceMapping[ i ] );
				state.setSourceActive( soc, contains( activeSourceIndices, i ) );
				if ( currentSourceIndex == i )
					state.setCurrentSource( soc );
			}
		}
	}

	private static boolean contains( int[] elements, int element )
	{
		for ( int e : elements )
			if ( e == element )
				return true;
		return false;
	}

	private static AffineTransform3D adjustedViewerTransform(
			final BdvPropertiesV0 properties,
			final int panelWidth,
			final int panelHeight,
			final RecenterMethod recenterMethod,
			final RescaleMethod rescaleMethod )
	{
		// take the transform from properties
		AffineTransform3D t = properties.transform();

		if ( recenterMethod == PANEL_CENTER )
		{
			// shift it to the center of the panel that it was copied from
			final long sx = properties.panelsize().dimension( 0 ) / 2;
			final long sy = properties.panelsize().dimension( 1 ) / 2;
			t = shift( t, sx, sy );
		}
		else if ( recenterMethod == MOUSE_POS )
		{
			// shift it to the mouse position when it was copied
			final long sx = properties.mousepos().getLongPosition( 0 );
			final long sy = properties.mousepos().getLongPosition( 1 );
			t = shift( t, sx, sy );
		}

		if ( rescaleMethod != RescaleMethod.NONE )
		{
			// scale factors to fit panel width and height
			final double scaleX = ( double ) panelWidth / properties.panelsize().dimension( 0 );
			final double scaleY = ( double ) panelHeight / properties.panelsize().dimension( 1 );
			final double scale = ( rescaleMethod == RescaleMethod.FIT_PANEL )
					? Math.min( scaleX, scaleY )
					: Math.max( scaleX, scaleY );
			t.scale( scale, scale, 1 );
		}

		if ( recenterMethod != RecenterMethod.NONE )
		{
			// shift it back to the top-left corner of the panel we want to paste it into
			t = shift( t, -panelWidth / 2.0, -panelHeight / 2.0 );
		}

		return t;
	}

	/**
	 * Shift world-to-screen transform by (sx,sy) in screen coordinates.
	 * <p>
	 * For example, with (sx,sy) = (windowWidth/2, windowHeight/2), this make
	 * the viewer transform obtained from {@code ViewerState} relative to the
	 * window center instead of relative to the top-left corner.
	 */
	private static AffineTransform3D shift( final AffineTransform3D transform, final double sx, final double sy )
	{
		final AffineTransform3D t = new AffineTransform3D();
		t.set( transform );
		t.set( t.get( 0, 3 ) - sx, 0, 3 );
		t.set( t.get( 1, 3 ) - sy, 1, 3 );
		return t;
	}

	static class VersionAndProperties
	{
		private final int version;

		private final JsonElement properties;

		VersionAndProperties( int version, JsonElement properties )
		{
			this.version = version;
			this.properties = properties;
		}

		public int version()
		{
			return version;
		}

		public JsonElement properties()
		{
			return properties;
		}

		@JsonUtils.JsonIo( jsonType = "VersionAndProperties", type = VersionAndProperties.class )
		public static class Adapter implements JsonDeserializer< VersionAndProperties >, JsonSerializer< VersionAndProperties >
		{
			@Override
			public VersionAndProperties deserialize(
					final JsonElement json,
					final Type typeOfT,
					final JsonDeserializationContext context ) throws JsonParseException
			{
				if ( !json.isJsonObject() )
					throw new JsonParseException( "expected object. (got \"" + json + "\")" );

				final JsonElement bdv = json.getAsJsonObject().get( "bdv" );
				if ( bdv == null || !bdv.isJsonObject() )
					throw new JsonParseException( "expected a \"bdv\" object attribute. (got \"" + json + "\")" );

				final int version;
				try {
					version = bdv.getAsJsonObject().get( "version" ).getAsInt();
				} catch ( final Exception e ) {
					throw new JsonParseException( "expected a \"version\" integer attribute. (got \"" + bdv + "\")" );
				}

				final JsonElement properties = bdv.getAsJsonObject().get( "properties" );
				if ( properties == null || !properties.isJsonObject() )
					throw new JsonParseException( "expected a \"properties\" object attribute. (got \"" + bdv + "\")" );

				return new VersionAndProperties( version, properties );
			}

			@Override
			public JsonElement serialize(
					final VersionAndProperties src,
					final Type typeOfSrc,
					final JsonSerializationContext context )
			{
				final JsonObject bdv = new JsonObject();
				bdv.addProperty( "version", src.version );
				bdv.add( "properties", src.properties );
				final JsonObject obj = new JsonObject();
				obj.add( "bdv", bdv );
				return obj;
			}
		}
	}
}

