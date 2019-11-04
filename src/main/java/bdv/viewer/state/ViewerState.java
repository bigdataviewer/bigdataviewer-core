/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.state;

import bdv.util.MipmapTransforms;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.r.DefaultViewerState;
import bdv.viewer.state.r.IViewerState;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.imglib2.realtransform.AffineTransform3D;

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

/**
 * Description of everything required to render the current image, such as the
 * current timepoint, the visible and current sources and groups respectively,
 * the viewer transformation, etc.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
@Deprecated
public class ViewerState
{
	final IViewerState state;

	public ViewerState( final List< SourceAndConverter< ? > > sources, final int numTimePoints )
	{
		this( sources, new ArrayList<>(), numTimePoints );
	}

	/**
	 * @param sources
	 * 		the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 * 		number of available timepoints.
	 */
	public ViewerState( final List< SourceAndConverter< ? > > sources, final List< SourceGroup > sourceGroups, final int numTimePoints )
	{
		state = new DefaultViewerState();
		state.getSources().addAll( sources );
		state.getSources().getActive().addAll( sources );
		sourceGroups.forEach( sourceGroup -> {
			final bdv.viewer.state.r.SourceGroup handle = new bdv.viewer.state.r.SourceGroup();
			state.getGroups().add( handle );
			state.getGroups().setName( handle, sourceGroup.getName() );
			sourceGroup.getSourceIds().forEach( i -> state.getGroups().addSourceToGroup( sources.get( i ), handle ) );
		} );
		state.setNumTimepoints( numTimePoints );
		state.setViewerTransform( new AffineTransform3D() );
		state.setInterpolation( NEARESTNEIGHBOR );
		state.setDisplayMode( SINGLE );
		state.getSources().makeCurrent( sources.isEmpty() ? null : sources.get( 0 ) );
		state.getGroups().makeCurrent( state.getGroups().isEmpty() ? null : state.getGroups().get( 0 ) );
		state.setCurrentTimepoint( 0 );
	}

	/**
	 * copy constructor
	 *
	 * @param s
	 */
	protected ViewerState( final ViewerState s )
	{
		state = new DefaultViewerState( ( DefaultViewerState ) s.state );
	}

	public synchronized ViewerState copy()
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
	 * @param t
	 * 		is set to the viewer transform.
	 */
	public synchronized void getViewerTransform( final AffineTransform3D t )
	{
		state.getViewerTransform( t );
	}

	/**
	 * Set the viewer transform.
	 *
	 * @param t
	 * 		transform parameters.
	 */
	public synchronized void setViewerTransform( final AffineTransform3D t )
	{
		state.setViewerTransform( t );
	}

	/**
	 * Get the index of the current source.
	 */
	public synchronized int getCurrentSource()
	{
		return state.getSources().indexOf( state.getSources().getCurrent() );
	}

	/**
	 * Make the source with the given index current.
	 */
	public synchronized void setCurrentSource( final int index )
	{
		state.getSources().makeCurrent( state.getSources().get( index ) );
	}

	/**
	 * Make the given source current.
	 */
	public synchronized void setCurrentSource( final Source< ? > source )
	{
		state.getSources().makeCurrent( soc( source ) );
	}

	private SourceAndConverter< ? > soc( Source< ? > source )
	{
		for ( SourceAndConverter< ? > soc : state.getSources() )
			if ( soc.getSpimSource() == source )
				return soc;
		return null;
	}

	/**
	 * Get the index of the current group.
	 */
	public synchronized int getCurrentGroup()
	{
		return state.getGroups().indexOf( state.getGroups().getCurrent() );
	}

	/**
	 * Make the group with the given index current.
	 */
	public synchronized void setCurrentGroup( final int index )
	{
		state.getGroups().makeCurrent( state.getGroups().get( index ) );
	}

	/**
	 * Make the given group current.
	 */
	public synchronized void setCurrentGroup( final SourceGroup group )
	{
		state.getGroups().makeCurrent( getHandle( group ) );
	}

	private bdv.viewer.state.r.SourceGroup getHandle( final SourceGroup group )
	{
		for ( bdv.viewer.state.r.SourceGroup handle : state.getGroups() )
		{
			SourceGroup g = new SourceGroup( state.getGroups().getName( handle ) );
			state.getGroups().getSourcesIn( handle ).forEach( s -> g.addSource( state.getSources().indexOf( s ) ) );
			if ( g.equals( group ) )
				return handle;
		}
		return null;
	}

	/**
	 * Get the interpolation method.
	 *
	 * @return interpolation method.
	 */
	public synchronized Interpolation getInterpolation()
	{
		return state.getInterpolation();
	}

	/**
	 * Set the interpolation method.
	 *
	 * @param method
	 * 		interpolation method.
	 */
	public synchronized void setInterpolation( final Interpolation method )
	{
		state.setInterpolation( method );
	}

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 *
	 * @return whether the display mode is <em>single-source</em>.
	 *
	 * @deprecated replaced by {@link #getDisplayMode()}
	 */
	@Deprecated
	public synchronized boolean isSingleSourceMode()
	{
		return state.getDisplayMode() == SINGLE;
	}

	/**
	 * Set the display mode to <em>single-source</em> (true) or <em>fused</em>
	 * (false). In <em>single-source</em> mode, only the current source (SPIM
	 * angle) is shown. In <em>fused</em> mode, all active sources are blended.
	 *
	 * @param singleSourceMode
	 * 		If true, set <em>single-source</em> mode. If false, set
	 * 		<em>fused</em> mode.
	 *
	 * @deprecated replaced by {@link #setDisplayMode(DisplayMode)}
	 */
	@Deprecated
	public synchronized void setSingleSourceMode( final boolean singleSourceMode )
	{
		if ( singleSourceMode )
			setDisplayMode( SINGLE );
		else
			setDisplayMode( FUSED );
	}

	/**
	 * Set the {@link DisplayMode}.
	 *
	 * <ul>
	 * <li>In <em>single-source</em> mode, only the current source (SPIM angle) is shown.</li>
	 * <li>In <em>fused</em> mode, all active sources are blended.</li>
	 * <li>In <em>single-group</em> mode, all sources of the current group are blended.</li>
	 * <li>In <em>fused group</em> mode, all sources of all active groups are blended.</li>
	 * </ul>
	 *
	 * @param mode
	 * 		the display mode
	 */
	public synchronized void setDisplayMode( final DisplayMode mode )
	{
		state.setDisplayMode( mode );
	}

	/**
	 * Get the current {@link DisplayMode}.
	 *
	 * <ul>
	 * <li>In <em>single-source</em> mode, only the current source (SPIM angle) is shown.</li>
	 * <li>In <em>fused</em> mode, all active sources are blended.</li>
	 * <li>In <em>single-group</em> mode, all sources of the current group are blended.</li>
	 * <li>In <em>fused group</em> mode, all sources of all active groups are blended.</li>
	 * </ul>
	 *
	 * @return the current display mode
	 */
	public synchronized DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	/**
	 * Get the timepoint index that is currently displayed.
	 *
	 * @return current timepoint index
	 */
	public synchronized int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	/**
	 * Set the current timepoint index.
	 *
	 * @param timepoint
	 * 		timepoint index.
	 */
	public synchronized void setCurrentTimepoint( final int timepoint )
	{
		state.setCurrentTimepoint( timepoint );
	}

	/**
	 * Returns a list of all sources.
	 *
	 * @return list of all sources.
	 */
	public List< SourceState< ? > > getSources()
	{
		List< SourceState< ? > > sourceStates = new ArrayList<>();
		for ( SourceAndConverter< ? > source : state.getSources() )
		{
			final SourceState< ? > ss = new SourceState<>( source, this );
			sourceStates.add( ss );
		}
		return sourceStates;
	}

	/**
	 * Returns the number of sources.
	 *
	 * @return number of sources.
	 */
	public int numSources()
	{
		return state.getSources().size();
	}

	/**
	 * Returns a list of all source groups.
	 *
	 * @return list of all source groups.
	 */
	public List< SourceGroup > getSourceGroups()
	{
		List< SourceGroup > sourceGroups = new ArrayList<>();
		for ( bdv.viewer.state.r.SourceGroup handle : state.getGroups() )
		{
			SourceGroup g = new SourceGroup( state.getGroups().getName( handle ) );
			state.getGroups().getSourcesIn( handle ).forEach( s -> g.addSource( state.getSources().indexOf( s ) ) );
			sourceGroups.add( g );
		}
		return sourceGroups;
	}

	/**
	 * Returns the number of source groups.
	 *
	 * @return number of source groups.
	 */
	public int numSourceGroups()
	{
		return state.getGroups().size();
	}

	public synchronized void addSource( final SourceAndConverter< ? > source )
	{
		state.getSources().add( source );
		state.getSources().setActive( source, true );
	}

	public synchronized void removeSource( final Source< ? > source )
	{
		state.getSources().remove( source );
	}

	protected void removeSource( final int index )
	{
		state.getSources().remove( state.getSources().get( index ) );
	}

	public synchronized void addGroup( final SourceGroup group )
	{
		final bdv.viewer.state.r.SourceGroup handle = new bdv.viewer.state.r.SourceGroup();
		state.getGroups().add( handle );
		state.getGroups().setName( handle, group.getName() );
		group.getSourceIds().forEach( i -> state.getGroups().addSourceToGroup( state.getSources().get( i ), handle ) );
	}

	public synchronized void removeGroup( final SourceGroup group )
	{
		state.getGroups().remove( getHandle( group ) );
	}

	protected void removeGroup( final int index )
	{
		state.getGroups().remove( state.getGroups().get( index ) );
	}

	public synchronized boolean isSourceVisible( final int index )
	{
		return state.getSources().isVisible( state.getSources().get( index ) );
	}

	/**
	 * Returns a list of the indices of all currently visible sources.
	 *
	 * @return indices of all currently visible sources.
	 */
	public synchronized List< Integer > getVisibleSourceIndices()
	{
		final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getSources().getVisible() );
		sources.sort( state.getSources().order() );
		return sources.stream().map( state.getSources()::indexOf ).collect( Collectors.toList() );
	}

	/*
	 * Utility methods.
	 */

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 * 		screen scale, transforms screen coordinates to viewer coordinates.
	 *
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final int sourceIndex )
	{
		return getBestMipMapLevel( screenScaleTransform, state.getSources().get( sourceIndex ).getSpimSource() );
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 * 		screen scale, transforms screen coordinates to viewer coordinates.
	 *
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final Source< ? > source )
	{
		final AffineTransform3D screenTransform = new AffineTransform3D();
		getViewerTransform( screenTransform );
		screenTransform.preConcatenate( screenScaleTransform );

		return MipmapTransforms.getBestMipMapLevel( screenTransform, source, state.getCurrentTimepoint() );
	}

	/**
	 * Get the number of timepoints.
	 *
	 * @return the number of timepoints.
	 */
	public synchronized int getNumTimepoints()
	{
		return state.getNumTimepoints();
	}

	/**
	 * Set the number of timepoints.
	 *
	 * @param numTimepoints
	 * 		the number of timepoints.
	 */
	public synchronized void setNumTimepoints( final int numTimepoints )
	{
		state.setNumTimepoints( numTimepoints );
	}

	/**
	 * DON'T USE THIS.
	 * <p>
	 * This is a work around for JDK bug
	 * https://bugs.openjdk.java.net/browse/JDK-8029147 which leads to
	 * ViewerPanel not being garbage-collected when ViewerFrame is closed. So
	 * instead we need to manually let go of resources...
	 */
	public void kill()
	{
		( ( DefaultViewerState ) state ).kill();
	}
}
