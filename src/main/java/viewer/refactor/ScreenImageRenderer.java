package viewer.refactor;

import java.util.ArrayList;

import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.sampler.special.ConstantRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import viewer.SpimSource;
import viewer.display.AccumulateARGB;
import viewer.display.InterruptibleRenderer;

public class ScreenImageRenderer
{
	/**
	 *
	 * @param screenImage
	 * 			  render target.
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @param mipmapIndex
	 *            mipmap level.
	 */
	public static InterruptibleRenderer< ?, ARGBType > createProjector( final SpimViewerState viewerState, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		synchronized( viewerState )
		{
			final ArrayList< SpimSourceState< ? > > visibleSources = viewerState.getVisibleSources();
			if ( visibleSources.isEmpty() )
				return new InterruptibleRenderer< ARGBType, ARGBType >( new ConstantRandomAccessible< ARGBType >( argbtype, 2 ), new TypeIdentity< ARGBType >() );
			else if ( visibleSources.size() == 1 )
				return createSingleSourceProjector( viewerState, visibleSources.get( 0 ), screenScaleTransform, mipmapIndex );
			else
			{
				final ArrayList< RandomAccessible< ARGBType > > accessibles = new ArrayList< RandomAccessible< ARGBType > >( visibleSources.size() );
				for ( final SpimSourceState< ? > source : visibleSources )
					accessibles.add( getConvertedTransformedSource( viewerState, source, screenScaleTransform, mipmapIndex ) );
				return new InterruptibleRenderer< ARGBType, ARGBType >( new AccumulateARGB( accessibles ), new TypeIdentity< ARGBType >() );
			}
		}
	}

	private final static ARGBType argbtype = new ARGBType();

	private static < T extends NumericType< T > > RandomAccessible< T > getTransformedSource( final SpimViewerState viewerState, final SpimSource< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		final int timepoint = viewerState.getCurrentTimepoint();
		final Interpolation interpolation = viewerState.getInterpolation();
		final RealRandomAccessible< T > img = source.getInterpolatedSource( timepoint, mipmapIndex, interpolation );

		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		viewerState.getViewerTransform( sourceToScreen );
		sourceToScreen.concatenate( source.getSourceTransform( timepoint, mipmapIndex ) );
		sourceToScreen.preConcatenate( screenScaleTransform );

		return RealViews.constantAffine( img, sourceToScreen );
	}

	private static < T extends NumericType< T > > RandomAccessible< ARGBType > getConvertedTransformedSource( final SpimViewerState viewerState, final SpimSourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		return Converters.convert( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter(), argbtype );
	}

	private static < T extends NumericType< T > > InterruptibleRenderer< T, ARGBType > createSingleSourceProjector( final SpimViewerState viewerState, final SpimSourceState< T > source, final AffineTransform3D screenScaleTransform, final int mipmapIndex )
	{
		return new InterruptibleRenderer< T, ARGBType >( getTransformedSource( viewerState, source.getSpimSource(), screenScaleTransform, mipmapIndex ), source.getConverter() );
	}
}
