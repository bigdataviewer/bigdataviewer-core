package bdv.tools.links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.JsonUtils.TypedList;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerState;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

class BdvPropertiesV0
{
	private final AffineTransform3D transform;
	private final int timepoint;
	private final DisplayMode displaymode;
	private final Interpolation interpolation;
	private final TypedList< ResourceSpec< ? > > sourceSpecs;
	private final TypedList< ResourceConfig > sourceConfigs;
	private final List< SourceConverterConfig > converterConfigs;
	private final int currentSourceIndex;
	private final int[] activeSourceIndices;
	private final long[] panelsize;
	private final long[] mousepos;

	private final Anchor anchor;

	public enum Anchor
	{
		CENTER( "center" ),
		MOUSE( "mouse" );

		private final String label;

		Anchor( final String label )
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}

		public static Anchor fromString( final String string )
		{
			for ( final Anchor value : values() )
				if ( value.toString().equals( string ) )
					return value;
			return null;
		}
	}

	public BdvPropertiesV0()
	{
		transform = new AffineTransform3D();
		timepoint = 0;
		displaymode = DisplayMode.SINGLE;
		interpolation = Interpolation.NLINEAR;
		sourceSpecs = new TypedList<>();
		sourceConfigs = new TypedList<>();
		converterConfigs = new ArrayList<>();
		currentSourceIndex = -1;
		activeSourceIndices = new int[ 0 ];
		panelsize = new long[ 2 ];
		mousepos = new long[ 2 ];
		anchor = Anchor.CENTER;
	}

	public BdvPropertiesV0(
			final ViewerState state,
			final ConverterSetups converterSetups,
			final Dimensions panelsize,
			final Point mousepos,
			final ResourceManager resources )
	{
		this.transform = state.getViewerTransform();
		this.timepoint = state.getCurrentTimepoint();
		this.displaymode = state.getDisplayMode();
		this.interpolation = state.getInterpolation();
		this.sourceSpecs = getSourceSpecs( state.getSources(), resources );
		this.sourceConfigs = getSourceConfigs( sourceSpecs.list(), resources );
		this.converterConfigs = getSourceConverterConfigs( state.getSources(), converterSetups );
		this.currentSourceIndex = getCurrentSourceIndex( state );
		this.activeSourceIndices = getActiveSourceIndices( state );
		this.panelsize = panelsize.dimensionsAsLongArray();
		this.mousepos = mousepos.positionAsLongArray();
		this.anchor = Anchor.CENTER;
	}

	private static int getCurrentSourceIndex( final ViewerState state )
	{
		return state.getSources().indexOf( state.getCurrentSource() );
	}

	private static int[] getActiveSourceIndices( final ViewerState state )
	{
		final List< SourceAndConverter< ? > > sources = state.getSources();
		return IntStream.range( 0, sources.size() ).filter( i -> state.isSourceActive( sources.get( i ) ) ).toArray();
	}

	private static List< SourceConverterConfig > getSourceConverterConfigs( final List< SourceAndConverter< ? > > sources, final ConverterSetups converterSetups )
	{
		List< SourceConverterConfig > configs = new ArrayList<>();
		for ( SourceAndConverter< ? > soc : sources )
		{
			final ConverterSetup setup = converterSetups.getConverterSetup( soc );
			final boolean hasColor = setup.supportsColor();
			final int color = hasColor ? setup.getColor().get() : 0;
			final double min = setup.getDisplayRangeMin();
			final double max = setup.getDisplayRangeMax();
			final Bounds bounds = converterSetups.getBounds().getBounds( setup );
			final double minBound = bounds.getMinBound();
			final double maxBound = bounds.getMaxBound();
			configs.add( new SourceConverterConfig( hasColor, color, min, max, minBound, maxBound ) );
		}
		return configs;
	}

	private static TypedList< ResourceSpec< ? > > getSourceSpecs( final List< SourceAndConverter< ? > > sources, final ResourceManager resources )
	{
		List< ResourceSpec< ? > > sourceSpecs = new ArrayList<>();
		for ( final SourceAndConverter< ? > soc : sources )
		{
			sourceSpecs.add( resources.getResourceSpec( soc ) );
		}
		return new TypedList<>( sourceSpecs );
	}

	private static TypedList< ResourceConfig > getSourceConfigs( final List< ResourceSpec< ? > > sourceSpecs, final ResourceManager resources )
	{
		final List< ResourceConfig > sourceConfigs = new ArrayList<>();
		for ( ResourceSpec< ? > spec : sourceSpecs ) {
			sourceConfigs.add( spec.getConfig( resources ) );
		}
		return new TypedList<>( sourceConfigs );
	}

	public AffineTransform3D transform()
	{
		return transform;
	}

	public int timepoint()
	{
		return timepoint;
	}

	public Dimensions panelsize()
	{
		return FinalDimensions.wrap( panelsize );
	}

	public Point mousepos()
	{
		return Point.wrap( mousepos );
	}

	public Anchor getAnchor()
	{
		return anchor;
	}

	public DisplayMode displaymode()
	{
		return displaymode;
	}

	public Interpolation interpolation()
	{
		return interpolation;
	}

	public List< ResourceSpec< ? > > sourceSpecs()
	{
		return sourceSpecs.list();
	}

	public List< ResourceConfig > sourceConfigs()
	{
		return sourceConfigs.list();
	}

	public  List< SourceConverterConfig > converterConfigs()
	{
		return converterConfigs;
	}

	public int currentSourceIndex()
	{
		return currentSourceIndex;
	}

	public int[] activeSourceIndices()
	{
		return activeSourceIndices;
	}

	@Override
	public String toString()
	{
		return "BdvPropertiesV0{" +
				"transform=" + transform +
				", timepoint=" + timepoint +
				", displaymode=" + displaymode +
				", interpolation=" + interpolation +
				", sourceSpecs =" + sourceSpecs +
				", sourceConfigs =" + sourceConfigs +
				", converterConfigs=" + converterConfigs +
				", currentSourceIndex=" + currentSourceIndex +
				", activeSourceIndices=" + Arrays.toString( activeSourceIndices ) +
				", panelsize=" + Arrays.toString( panelsize ) +
				", mousepos=" + Arrays.toString( mousepos ) +
				", anchor=" + anchor +
				'}';
	}

	public static class SourceConverterConfig
	{
		final boolean hasColor;
		final int color;
		final double min;
		final double max;
		final double minBound;
		final double maxBound;

		public SourceConverterConfig( final boolean hasColor, final int color, final double min, final double max, final double minBound, final double maxBound )
		{
			this.hasColor = hasColor;
			this.color = color;
			this.min = min;
			this.max = max;
			this.minBound = minBound;
			this.maxBound = maxBound;
		}

		public double rangeMin() {
			return min;
		}

		public double rangeMax() {
			return max;
		}

		public ARGBType color() {
			return new ARGBType( color );
		}

		public Bounds bounds() {
			return new Bounds( minBound, maxBound );
		}

		@Override
		public String toString()
		{
			return "SourceConverterConfig{" +
					"hasColor=" + hasColor +
					", color=" + String.format("#%08x", color) +
					", min=" + min +
					", max=" + max +
					", minBound=" + minBound +
					", maxBound=" + maxBound +
					'}';
		}
	}
}
