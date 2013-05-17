package viewer.render;

import static viewer.render.Interpolation.NEARESTNEIGHBOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;

public class ViewerState
{
	final protected ArrayList< SourceState< ? > > sources;

	/**
	 * number of available timepoints.
	 */
	final protected int numTimePoints;

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
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 */
	public ViewerState( final Collection< SourceAndConverter< ? > > sources, final int numTimePoints )
	{
		this.sources = new ArrayList< SourceState< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( SourceState.create( source ) );
		this.numTimePoints = numTimePoints;

		viewerTransform = new AffineTransform3D();
		interpolation = NEARESTNEIGHBOR;
		singleSourceMode = true;
		currentSource = 0;
		currentTimepoint = 0;
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected ViewerState( final ViewerState s )
	{
		sources = new ArrayList< SourceState< ? > >( s.sources.size() );
		for ( final SourceState< ? > source : s.sources )
			this.sources.add( source.copy() );
		numTimePoints = s.numTimePoints;
		viewerTransform = s.viewerTransform.copy();
		interpolation = s.interpolation;
		singleSourceMode = s.singleSourceMode;
		currentSource = s.currentSource;
		currentTimepoint = s.currentTimepoint;
	}

	public ViewerState copy()
	{
		return new ViewerState( this );
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
	public synchronized List< SourceState< ? > > getSources()
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
	protected synchronized ArrayList< SourceState< ? > > getVisibleSources()
	{
		final ArrayList< SourceState< ? > > visibleSources = new ArrayList< SourceState< ? > >();
		for ( final SourceState< ? > source : sources )
			if ( source.isVisible( singleSourceMode ) )
				visibleSources.add( source );
		return visibleSources;
	}

	/**
	 * Returns a list of the indices of all currently visible sources.
	 *
	 * @return indices of all currently visible sources.
	 */
	public synchronized ArrayList< Integer > getVisibleSourceIndices()
	{
		final ArrayList< Integer > visibleSources = new ArrayList< Integer >();
		for ( int i = 0; i < sources.size(); ++i )
			if ( sources.get( i ).isVisible( singleSourceMode ) )
				visibleSources.add( i );
		return visibleSources;
	}

	/*
	 * Utility methods.
	 */

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 * If the source is invisible, returns the coarsest mipmap level.
	 *
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final int sourceIndex )
	{
		final AffineTransform3D screenTransform = new AffineTransform3D();
		getViewerTransform( screenTransform );
		screenTransform.preConcatenate( screenScaleTransform );

		final SourceState< ? > source = sources.get( sourceIndex );
		int targetLevel = source.getSpimSource().getNumMipmapLevels() - 1;
		if ( source.isVisible( singleSourceMode ) )
		{
			for ( int level = targetLevel - 1; level >= 0; level-- )
			{
//				System.out.println( "source.getVoxelScreenSize( level = " + level + " ) = " + source.getVoxelScreenSize( screenTransform, currentTimepoint, level ) );
				if ( source.getVoxelScreenSize( screenTransform, currentTimepoint, level ) >= 0.99 /*1.0*/ )
					targetLevel = level;
				else
					break;
			}
		}
		return targetLevel;
	}
}
