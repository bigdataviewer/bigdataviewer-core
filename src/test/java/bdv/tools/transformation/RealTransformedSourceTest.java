package bdv.tools.transformation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Interpolation;
import net.imglib2.Cursor;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.iterator.LocalizingRealIntervalIterator;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;

public class RealTransformedSourceTest
{
	private static final double EPSILON = 1e-9;

	/** Identity pixel calibration. */
	@Test
	public void testAffineMatchesTransformedSource()
	{
		final RandomAccessibleIntervalSource< DoubleType > base = exampleImg( new AffineTransform3D() );
		assertSourcesMatch( base, exampleAffine() );
	}

	/** Non-identity calibration, with a different scale in each dimension. */
	@Test
	public void testAffineMatchesTransformedSourceWithCalibration()
	{
		final AffineTransform3D calibration = new AffineTransform3D();
		calibration.scale( 2.0, 3.0, 4.0 );

		final RandomAccessibleIntervalSource< DoubleType > base = exampleImg( calibration );
		assertSourcesMatch( base, exampleAffine() );
	}

	/**
	 * Asserts that a {@link RealTransformedSource} and a {@link TransformedSource}
	 * built with the same {@code affine} render the same world-space image for the
	 * given {@code base}.
	 * <p>
	 * Both interpolated sources live in their own pixel frame, so each is lifted
	 * into world coordinates with {@link RealViews#affine} using that source's
	 * {@code getSourceTransform}. The comparison walks the world-space footprint of
	 * the base image (its interval mapped through the {@link TransformedSource}'s
	 * source transform) and samples both world accessibles there.
	 */
	private static void assertSourcesMatch( final RandomAccessibleIntervalSource< DoubleType > base, final AffineTransform3D affine )
	{
		final TransformedSource< DoubleType > ts = new TransformedSource<>( base );
		ts.setFixedTransform( affine );

		// RealTransformedSource uses the inverse transform
		final RealTransformedSource< DoubleType > rts = new RealTransformedSource<>( base, "rts", affine.inverse() );

		// transform interpolated sources into world coordinates
		final AffineTransform3D tsToWorld = new AffineTransform3D();
		ts.getSourceTransform( 0, 0, tsToWorld );
		final RealRandomAccess< DoubleType > tsWorld =
				RealViews.affine( ts.getInterpolatedSource( 0, 0, Interpolation.NLINEAR ), tsToWorld ).realRandomAccess();

		final AffineTransform3D rtsToWorld = new AffineTransform3D();
		rts.getSourceTransform( 0, 0, rtsToWorld );
		final RealRandomAccess< DoubleType > rtsWorld =
				RealViews.affine( rts.getInterpolatedSource( 0, 0, Interpolation.NLINEAR ), rtsToWorld ).realRandomAccess();

		// test over the the base image's bounds in world coordinates as estimated by tsToWorld
		final FinalRealInterval worldBounds = tsToWorld.estimateBounds( base.getSource( 0, 0 ) );
		final double[] steps = { 1, 1, 1 };
		final LocalizingRealIntervalIterator it = new LocalizingRealIntervalIterator( worldBounds, steps );

		while ( it.hasNext() )
		{
			it.fwd();
			tsWorld.setPosition( it );
			rtsWorld.setPosition( it );
			final double vTs = tsWorld.get().get();
			final double vRts = rtsWorld.get().get();
			assertEquals( "world: " + it, vTs, vRts, EPSILON );
		}

	}

	private static RandomAccessibleIntervalSource< DoubleType > exampleImg( final AffineTransform3D calibration )
	{
		final long[] dims = { 5, 4, 3 };
		final ArrayImg< DoubleType, ? > img = ArrayImgs.doubles( dims );
		final Cursor< DoubleType > c = img.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			final int x = c.getIntPosition( 0 );
			final int y = c.getIntPosition( 1 );
			final int z = c.getIntPosition( 2 );
			c.get().set( 1 + x + 5 * y + 20 * z );
		}

		return new RandomAccessibleIntervalSource<>(img, new DoubleType(), calibration, "base");
	}

	private static AffineTransform3D exampleAffine()
	{
		final AffineTransform3D affine = new AffineTransform3D();
		affine.scale(  0.8 );
		affine.translate( 0.2, 0.4, 0.6 );
		return affine;
	}
}
