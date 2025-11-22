package bdv.viewer.render;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.VolatileBlockSupplier;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.interpolation.Interpolant;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.util.Cast;

class SourceRenderInfo
{
	private final SourceAndConverter< ? > source;

	private MipmapOrdering.MipmapHints mipmapHints;

	private SourceAndConverter< ? extends Volatile< ? > > volatileSource;

	SourceRenderInfo( final SourceAndConverter< ? > source )
	{
		this.source = source;
	}

	/**
	 * Returns the {@code SourceAndConverter} to which this RenderInfo is
	 * associated.
	 */
	public SourceAndConverter< ? > source()
	{
		return source;
	}

	/**
	 * Returns {@code true} if this source should be rendered as volatile (that
	 * means, using {@code VolatileHierarchyProjector}.
	 * <p>
	 * If {@code renderVolatile()==true},
	 * then {@link #getVolatileSource()} can be used to retrieve the volatile
	 * version of {@link #source()}, and {@link #getRenderSource()} will also
	 * return this volatile version,
	 */
	public boolean renderVolatile()
	{
		return volatileSource != null;
	}

	public MipmapOrdering.MipmapHints getMipmapHints()
	{
		return mipmapHints;
	}

	public void setMipmapHints( final MipmapOrdering.MipmapHints mipmapHints )
	{
		this.mipmapHints = mipmapHints;
	}

	public SourceAndConverter< ? extends Volatile< ? > > getVolatileSource()
	{
		return volatileSource;
	}

	public void setVolatileSource( final SourceAndConverter< ? extends Volatile< ? > > volatileSource )
	{
		this.volatileSource = volatileSource;
	}

	public SourceAndConverter< ? > getRenderSource()
	{
		return renderVolatile() ? volatileSource : source;
	}


















	private BlockSupplierInfo< ? >[] blockSupplierInfos;

	public BlockSupplierInfo< ? > getBlockSupplierInfo( final int mipmapLevel )
	{
		return blockSupplierInfos[ mipmapLevel ];
	}

	public boolean supportsBlockSuppliers()
	{
		return blockSupplierInfos != null;
	}

	static class BlockSupplierInfo< T extends NativeType< T > >
	{
		final BlockSupplier< T > blockSupplier;
		final Transform.Interpolation interpolation;

		BlockSupplierInfo(
				final BlockSupplier< T > blockSupplier,
				final Transform.Interpolation interpolation )
		{
			this.blockSupplier = blockSupplier;
			this.interpolation = interpolation;
		}
	}

	public void setupBlockSuppliers(
			final int timepoint,
			final Interpolation method )
	{
		final Source< ? extends Volatile< ? > > spimSource = getVolatileSource().getSpimSource();
		final BlockSupplierInfo< ? >[] infos = new BlockSupplierInfo[ spimSource.getNumMipmapLevels() ];
		if ( renderVolatile() )
		{
			for ( final MipmapOrdering.Level level : getMipmapHints().getLevels() )
			{
				final int mipmapLevel = level.getMipmapLevel();
				final RealRandomAccessible< ? extends Volatile< ? > > interpolated = spimSource.getInterpolatedSource( timepoint, mipmapLevel, method );
				final BlockSupplierInfo< ? > supplierInfo = tryGetVolatileBlockSupplier( interpolated );
				if ( supplierInfo == null )
					return;
				infos[ mipmapLevel ] = supplierInfo;
			}
			blockSupplierInfos = infos;
		}
		else
		{
			// TODO extract BlockSuppliers for non-volatile rendering
			throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
		}
	}

	private static < T extends Volatile< ? > & NativeType< T > > BlockSupplierInfo< T > tryGetVolatileBlockSupplier(
			final RealRandomAccessible< ? extends Volatile< ? > > rra )
	{
		if ( !( rra.getType() instanceof NativeType ) )
			return null;
		final RealRandomAccessible< T > s1 = Cast.unchecked( rra );

		if ( !( s1 instanceof Interpolant ) )
			return null;
		final Interpolant< T, ? > s2 = ( Interpolant< T, ? > ) s1;

		final InterpolatorFactory< T, ? > f = s2.getInterpolatorFactory();
		final Transform.Interpolation interpolation;
		if ( f instanceof ClampingNLinearInterpolatorFactory )
			interpolation = Transform.Interpolation.NLINEAR;
		else if ( f instanceof NearestNeighborInterpolatorFactory )
			interpolation = Transform.Interpolation.NEARESTNEIGHBOR;
		else
			return null;
		final RandomAccessible< T > s3 = Cast.unchecked( s2.getSource() );

		try
		{
			final BlockSupplier< T > blockSupplier = VolatileBlockSupplier.of( s3 );
			return new BlockSupplierInfo<>( blockSupplier, interpolation );
		}
		catch ( IllegalArgumentException e )
		{
			return null;
		}
	}


}
