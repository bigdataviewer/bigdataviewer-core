package viewer.refactor;

import static viewer.refactor.Interpolation.NEARESTNEIGHBOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import viewer.SpimSource;
import viewer.SpimSourceAndConverter;

public class SpimViewerState
{
	final protected ArrayList< SpimSourceState< ? > > sources;

	/**
	 * number of available timepoints.
	 */
	final protected int numTimePoints;

	/**
	 * number of available mipmap levels.
	 */
	final protected int numMipmapLevels;



	/*
	 * Renderer state:
	 *
	 * which sources to show, which interpolation method to use, etc.
	 */

	/**
	 * Transformation set by the interactive viewer. Transforms from global
	 * coordinate system to viewer coordinate system.
	 */
	final protected AffineTransform3D viewerTransform;

	/**
	 * Which interpolation method is currently used to render the display.
	 */
	protected Interpolation interpolation;

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 */
	protected boolean singleSourceMode;

	/**
	 * The index of the current source.
	 * (In single-source mode only the current source is shown.)
	 */
	protected int currentSource;

	/**
	 * which timepoint is currently shown.
	 */
	protected int currentTimepoint;


	/**
	 *
	 * @param sources
	 *            the {@link SpimSourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param numMipmapLevels
	 *            number of available mipmap levels.
	 */
	public SpimViewerState( final Collection< SpimSourceAndConverter< ? > > sources, final int numTimePoints, final int numMipmapLevels )
	{
		this.sources = new ArrayList< SpimSourceState< ? > >( sources.size() );
		for ( final SpimSourceAndConverter< ? > source : sources )
			this.sources.add( SpimSourceState.create( source ) );
		this.numTimePoints = numTimePoints;
		this.numMipmapLevels = numMipmapLevels;

		// viewer state
		viewerTransform = new AffineTransform3D();
		interpolation = NEARESTNEIGHBOR;
		singleSourceMode = true;
		currentSource = 0;
		currentTimepoint = 0;
	}



	/*
	 * Renderer state.
	 * (which sources to show, which interpolation method to use, etc.)
	 */

	/**
	 * Get the viewer transform.
	 *
	 * @param t is set to the viewer transform.
	 */
	public synchronized void getViewerTransform( final AffineTransform3D t )
	{
		t.set( viewerTransform );
	}

	/**
	 * Set the viewer transform.
	 *
	 * @param t transform parameters.
	 */
	public synchronized void setViewerTransform( final AffineTransform3D t )
	{
		viewerTransform.set( t );
	}

	/**
	 * Make the source with the given index current.
	 */
	public synchronized void setCurrentSource( final int index )
	{
		if ( index >= 0 && index < sources.size() )
		{
			sources.get( currentSource ).setCurrent( false );
			currentSource = index;
			sources.get( currentSource ).setCurrent( true );
		}
	}

	/**
	 * Get the index of the current source.
	 */
	public synchronized int getCurrentSource()
	{
		return currentSource;
	}

	/**
	 * Get the interpolation method.
	 *
	 * @return interpolation method.
	 */
	public synchronized Interpolation getInterpolation()
	{
		return interpolation;
	}

	/**
	 * Set the interpolation method.
	 *
	 * @param method interpolation method.
	 */
	public synchronized void setInterpolation( final Interpolation method )
	{
		interpolation = method;
	}

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 *
	 * @return whether the display mode is <em>single-source</em>.
	 */
	public synchronized boolean isSingleSourceMode()
	{
		return singleSourceMode;
	}

	/**
	 * Set the display mode to <em>single-source</em> (true) or <em>fused</em>
	 * (false). In <em>single-source</em> mode, only the current source (SPIM
	 * angle) is shown. In <em>fused</em> mode, all active sources are blended.
	 *
	 * @param singleSourceMode
	 *            If true, set <em>single-source</em> mode. If false, set
	 *            <em>fused</em> mode.
	 */
	public synchronized void setSingleSourceMode( final boolean singleSourceMode )
	{
		this.singleSourceMode = singleSourceMode;
	}

	/**
	 * Get the timepoint index that is currently displayed.
	 *
	 * @return current timepoint index
	 */
	public synchronized int getCurrentTimepoint()
	{
		return currentTimepoint;
	}

	/**
	 * Set the current timepoint index.
	 *
	 * @param timepoint
	 *            timepoint index.
	 */
	public synchronized void setCurrentTimepoint( final int timepoint )
	{
		currentTimepoint = timepoint;
	}

	/**
	 * Returns a list of all sources.
	 *
	 * @return list of all sources.
	 */
	public synchronized List< SpimSourceState< ? > > getSources()
	{
		return Collections.unmodifiableList( sources );
	}

	/**
	 * Returns the number of sources.
	 *
	 * @return number of sources.
	 */
	public int numSources()
	{
		return sources.size();
	}

	/**
	 * Returns a list of all currently visible sources.
	 *
	 * @return list of all currently visible sources.
	 */
	protected synchronized ArrayList< SpimSourceState< ? > > getVisibleSources()
	{
		final ArrayList< SpimSourceState< ? > > visibleSources = new ArrayList< SpimSourceState< ? > >();
		for ( final SpimSourceState< ? > source : sources )
			if ( source.isVisible( singleSourceMode ) )
				visibleSources.add( source );
		return visibleSources;
	}


	/*
	 * Utility methods.
	 */

	/**
	 * Compute the projected voxel size at the given screen transform and mipmap
	 * level. Take a source voxel (0,0,0)-(1,1,1) at the given mipmap level and
	 * transform it to the screen image at the given screen scale. Take the
	 * maximum of the screen extends of the transformed projected voxel edges.
	 *
	 * @param source
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param timepoint
	 *            for which timepoint to query the source
	 * @param mipmapIndex
	 *            mipmap level
	 * @return pixel size
	 */
	public static double getVoxelScreenSize( final SpimSource< ? > source, final AffineTransform3D screenTransform, final int timepoint, final int mipmapIndex )
	{
		double pixelSize = 0;
		final AffineTransform3D sourceToScreen = new AffineTransform3D();
		sourceToScreen.set( screenTransform );
		sourceToScreen.concatenate( source.getSourceTransform( timepoint, mipmapIndex ) );
		final double[] zero = new double[] { 0, 0, 0 };
		final double[] tzero = new double[ 3 ];
		final double[] one = new double[ 3 ];
		final double[] tone = new double[ 3 ];
		final double[] diff = new double[ 2 ];
		sourceToScreen.apply( zero, tzero );
		for ( int i = 0; i < 3; ++i )
		{
			for ( int d = 0; d < 3; ++d )
				one[ d ] = d == i ? 1 : 0;
			sourceToScreen.apply( one, tone );
			LinAlgHelpers.subtract( tone, tzero, tone );
			diff[0] = tone[0];
			diff[1] = tone[1];
			final double l = LinAlgHelpers.length( diff );
			if ( l > pixelSize )
				pixelSize = l;
		}
		return pixelSize;
	}

	/**
	 * Compute the maximum "pixel size" at the given screen transform and mipmap
	 * level. For every source, take a source voxel (0,0,0)-(1,1,1) at the given
	 * mipmap level and transform it to the screen image at the given screen
	 * scale. Take the maximum of the screen extends of the transformed projected voxel.
	 * Do this for all visible sources and take the maximum.
	 *
	 * @param screenTransform
	 *            transforms screen coordinates to global coordinates.
	 * @param mipmapIndex
	 *            mipmap level
	 * @return pixel size
	 */
	protected double getSourceResolution( final AffineTransform3D screenTransform, final int mipmapIndex )
	{
		double pixelSize = 0;
		for ( final SpimSourceState< ? > source : sources )
			if ( source.isVisible( singleSourceMode ) )
				pixelSize = Math.max( pixelSize, getVoxelScreenSize( source.getSpimSource(), screenTransform, currentTimepoint, mipmapIndex ) );
		return pixelSize;
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for all visible sources.
	 *
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform )
	{
		final AffineTransform3D screenTransform = new AffineTransform3D();
		getViewerTransform( screenTransform );
		screenTransform.preConcatenate( screenScaleTransform );

		int targetLevel = numMipmapLevels - 1;
		for ( int level = numMipmapLevels - 2; level >= 0; level-- )
			if ( getSourceResolution( screenScaleTransform, level ) >= 1.0 )
				targetLevel = level;
			else
				break;
		return targetLevel;
	}
}
