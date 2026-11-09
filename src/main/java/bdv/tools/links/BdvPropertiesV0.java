package bdv.tools.links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.gson.JsonParseException;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.JsonUtils.TypedList;
import bdv.util.Bounds;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
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
	private final List< SourceGroupConfig > groupConfigs;
	private final int currentGroupIndex;
	private final int[] activeGroupIndices;
	private final long[] panelsize;
	private final long[] mousepos;

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
		groupConfigs = new ArrayList<>();
		currentGroupIndex = -1;
		activeGroupIndices = new int[ 0 ];
		panelsize = new long[ 2 ];
		mousepos = new long[ 2 ];
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
		this.groupConfigs = getGroupConfigs( state );
		this.currentGroupIndex = getCurrentGroupIndex( state );
		this.activeGroupIndices = getActiveGroupIndices( state );
		this.panelsize = panelsize.dimensionsAsLongArray();
		this.mousepos = mousepos.positionAsLongArray();
	}

	/**
	 * Check that properties obtained from {@code gson.fromJson(...)} are
	 * complete and consistent, so that pasting them doesn't fail half-way
	 * through.
	 * <p>
	 * This is only needed after deserialization. Gson's reflective adapter
	 * first creates an instance with the no-arg constructor, then overwrites
	 * the fields for each matching JSON attribute. This bypasses the
	 * constructors, and Gson also writes {@code final} fields (via
	 * reflection). So, although all fields are {@code final} and initialized
	 * non-{@code null} by both constructors, after deserialization
	 * <ul>
	 * <li>an explicit {@code "transform": null} (etc.) leaves the field
	 * {@code null},</li>
	 * <li>arrays such as {@code panelsize} may have the wrong length, and</li>
	 * <li>the per-source lists ({@code sourceSpecs}, {@code sourceConfigs},
	 * {@code converterConfigs}) may have different lengths.</li>
	 * </ul>
	 * Attributes that are missing from the JSON keep the defaults from the
	 * no-arg constructor, and are fine.
	 * <p>
	 * Instances created with the {@code BdvPropertiesV0(ViewerState, ...)}
	 * constructor are consistent by construction and don't need this check.
	 *
	 * @throws JsonParseException
	 * 		if properties are missing or inconsistent
	 */
	void validateDeserialized() throws JsonParseException
	{
		final Object[] required = { transform, displaymode, interpolation, sourceSpecs, sourceConfigs, converterConfigs, activeSourceIndices, groupConfigs, activeGroupIndices, panelsize, mousepos };
		final String[] names = { "transform", "displaymode", "interpolation", "sourceSpecs", "sourceConfigs", "converterConfigs", "activeSourceIndices", "groupConfigs", "activeGroupIndices", "panelsize", "mousepos" };
		for ( int i = 0; i < required.length; i++ )
			if ( required[ i ] == null )
				throw new JsonParseException( "missing attribute \"" + names[ i ] + "\"" );

		if ( panelsize.length != 2 || panelsize[ 0 ] < 0 || panelsize[ 1 ] < 0 )
			throw new JsonParseException( "expected \"panelsize\" to be 2 non-negative integers (got " + Arrays.toString( panelsize ) + ")" );
		if ( mousepos.length != 2 )
			throw new JsonParseException( "expected \"mousepos\" to be 2 integers (got " + Arrays.toString( mousepos ) + ")" );

		final int numSources = sourceSpecs.list().size();
		if ( sourceConfigs.list().size() != numSources || converterConfigs.size() != numSources )
			throw new JsonParseException( "expected \"sourceSpecs\", \"sourceConfigs\", and \"converterConfigs\" to have the same length (got "
					+ numSources + ", " + sourceConfigs.list().size() + ", " + converterConfigs.size() + ")" );

		if ( groupConfigs.contains( null ) )
			throw new JsonParseException( "unexpected null in \"groupConfigs\"" );
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

	private static int getCurrentGroupIndex( final ViewerState state )
	{
		return state.getGroups().indexOf( state.getCurrentGroup() );
	}

	private static int[] getActiveGroupIndices( final ViewerState state )
	{
		final List< SourceGroup > groups = state.getGroups();
		return IntStream.range( 0, groups.size() ).filter( i -> state.isGroupActive( groups.get( i ) ) ).toArray();
	}

	private static List< SourceGroupConfig > getGroupConfigs( final ViewerState state )
	{
		final List< SourceGroupConfig > configs = new ArrayList<>();
		final List< SourceAndConverter< ? > > sources = state.getSources();
		for ( SourceGroup group : state.getGroups() )
		{
			final String name = state.getGroupName( group );
			final Set< SourceAndConverter< ? > > sourcesInGroup = state.getSourcesInGroup( group );
			final int[] indices = IntStream.range( 0, sources.size() ).filter( i -> sourcesInGroup.contains( sources.get( i ) ) ).toArray();
			final SourceGroupConfig config = new SourceGroupConfig( name, indices );
			configs.add( config );
		}
		return configs;
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

	public List< SourceConverterConfig > converterConfigs()
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

	public List< SourceGroupConfig > groupConfigs()
	{
		return groupConfigs;
	}

	public int currentGroupIndex()
	{
		return currentGroupIndex;
	}

	public int[] activeGroupIndices()
	{
		return activeGroupIndices;
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
				", groupConfigs=" + groupConfigs +
				", currentGroupIndex=" + currentGroupIndex +
				", activeGroupIndices=" + Arrays.toString( activeGroupIndices ) +
				", panelsize=" + Arrays.toString( panelsize ) +
				", mousepos=" + Arrays.toString( mousepos ) +
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

	public static class SourceGroupConfig
	{
		final String name;
		final int[] sources;

		public SourceGroupConfig( final String name, final int[] sources )
		{
			this.name = name;
			this.sources = sources;
		}

		public String name()
		{
			return name;
		}

		public int[] sources()
		{
			return sources;
		}

		@Override
		public String toString()
		{
			return "SourceGroupConfig{" +
					"name='" + name + '\'' +
					", sources=" + Arrays.toString( sources ) +
					'}';
		}
	}
}
