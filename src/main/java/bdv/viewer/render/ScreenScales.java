package bdv.viewer.render;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class ScreenScales
{
	/**
	 * Target rendering time in nanoseconds. The rendering time for the coarsest
	 * rendered scale should be below this threshold. After the coarsest scale,
	 * increasingly finer scales are rendered, but these render passes may be
	 * canceled (while the coarsest may not).
	 */
	private final double targetRenderNanos;

	private final List< ScreenScale > screenScales;

	private int screenW = 0;

	private int screenH = 0;

	/**
	 * @param screenScaleFactors
	 *     Scale factors from the viewer canvas to screen images of different
	 *     resolutions. A scale factor of 1 means 1 pixel in the screen image is
	 *     displayed as 1 pixel on the canvas, a scale factor of 0.5 means 1
	 *     pixel in the screen image is displayed as 2 pixel on the canvas, etc.
	 * @param targetRenderNanos
	 *     Target rendering time in nanoseconds. The rendering time for the
	 *     coarsest rendered scale should be below this threshold.
	 */
	public ScreenScales( final double[] screenScaleFactors, final double targetRenderNanos )
	{
		this.targetRenderNanos = targetRenderNanos;
		screenScales = new ArrayList<>();
		for ( final double scale : screenScaleFactors )
			screenScales.add( new ScreenScale( scale ) );
	}

	/**
	 * Check whether the screen size was changed and revise {@link #screenScales} accordingly.
	 *
	 * @return whether the size was changed.
	 */
	public boolean checkResize( final int newScreenW, final int newScreenH )
	{
		if ( newScreenW != screenW || newScreenH != screenH )
		{
			screenW = newScreenW;
			screenH = newScreenH;
			screenScales.forEach( s -> s.resize( screenW, screenH ) );
			return true;
		}
		return false;
	}

	/**
	 * @return the screen scale at {@code index}
	 */
	public ScreenScale get( final int index )
	{
		return screenScales.get( index );
	}

	/**
	 * @return number of screen scales.
	 */
	public int size()
	{
		return screenScales.size();
	}

	public int suggestScreenScale( final double renderNanosPerPixel )
	{
		for ( int i = 0; i < screenScales.size() - 1; i++ )
		{
			final double renderTime = screenScales.get( i ).estimateRenderNanos( renderNanosPerPixel );
			if ( renderTime <= targetRenderNanos )
				return i;
		}
		return screenScales.size() - 1;
	}

	public int suggestIntervalScreenScale( final double renderNanosPerPixel, final int minScreenScaleIndex )
	{
		for ( int i = minScreenScaleIndex; i < screenScales.size() - 1; i++ )
		{
			final double renderTime = screenScales.get( i ).estimateIntervalRenderNanos( renderNanosPerPixel );
			if ( renderTime <= targetRenderNanos )
				return i;
		}
		return screenScales.size() - 1;
	}

	public void requestInterval( final Interval screenInterval )
	{
		screenScales.forEach( s -> s.requestInterval( screenInterval ) );
	}

	public void clearRequestedIntervals()
	{
		screenScales.forEach( ScreenScale::pullScreenInterval );
	}

	public static class ScreenScale
	{
		/**
		 * Scale factor from the viewer coordinates to target image of this screen scale.
		 */
		private final double scale;

		/**
		 * The width of the target image at this ScreenScale.
		 */
		private int w = 0;

		/**
		 * The height of the target image at this ScreenScale.
		 */
		private int h = 0;

		/**
		 * The transformation from viewer to target image coordinates at this ScreenScale.
		 */
		private final AffineTransform3D scaleTransform = new AffineTransform3D();

		/**
		 * Pending interval request.
		 * This is in viewer coordinates.
		 * To transform to target coordinates of this scale, use {@link #scaleScreenInterval}.
		 */
		private Interval requestedScreenInterval = null;

		/**
		 * @param scale
		 * 		Scale factor from the viewer coordinates to target image of this screen scale/
		 */
		ScreenScale( final double scale )
		{
			this.scale = scale;
		}

		void resize( final int screenW, final int screenH )
		{
			w = ( int ) Math.ceil( scale * screenW );
			h = ( int ) Math.ceil( scale * screenH );

			scaleTransform.set( scale, 0, 0 );
			scaleTransform.set( scale, 1, 1 );
			scaleTransform.set( 0.5 * scale - 0.5, 0, 3 );
			scaleTransform.set( 0.5 * scale - 0.5, 1, 3 );

			requestedScreenInterval = null;
		}

		double estimateRenderNanos( final double renderNanosPerPixel )
		{
			return renderNanosPerPixel * w * h;
		}

		public void requestInterval( final Interval screenInterval )
		{
			requestedScreenInterval = requestedScreenInterval == null
					? screenInterval
					: Intervals.union( requestedScreenInterval, screenInterval );
		}

		public Interval pullScreenInterval()
		{
			final Interval interval = requestedScreenInterval;
			requestedScreenInterval = null;
			return interval;
		}

		public double estimateIntervalRenderNanos( final double renderNanosPerPixel )
		{
			return renderNanosPerPixel * Intervals.numElements( scaleScreenInterval( requestedScreenInterval ) );
		}

		public Interval scaleScreenInterval( final Interval requestedScreenInterval )
		{
			// This is equivalent to
			// Intervals.intersect( new FinalInterval( w, h ), Intervals.smallestContainingInterval( Intervals.scale( requestedScreenInterval, screenToViewerScale ) ) );
			return Intervals.createMinMax(
					Math.max( 0, ( int ) Math.floor( requestedScreenInterval.min( 0 ) * scale ) ),
					Math.max( 0, ( int ) Math.floor( requestedScreenInterval.min( 1 ) * scale ) ),
					Math.min( w - 1, ( int ) Math.ceil( requestedScreenInterval.max( 0 ) * scale ) ),
					Math.min( h - 1, ( int ) Math.ceil( requestedScreenInterval.max( 1 ) * scale ) )
			);
		}

		public int width()
		{
			return w;
		}

		public int height()
		{
			return h;
		}

		public double scale()
		{
			return scale;
		}

		public AffineTransform3D scaleTransform()
		{
			return scaleTransform;
		}
	}
}
