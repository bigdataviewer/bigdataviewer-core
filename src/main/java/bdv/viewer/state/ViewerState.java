/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
import bdv.viewer.BasicViewerState;
import bdv.viewer.SynchronizedViewerState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * @author Tobias Pietzsch
 */
@Deprecated
public class ViewerState
{
	public SynchronizedViewerState getState()
	{
		return state;
	}

	final SynchronizedViewerState state;

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
		state = new SynchronizedViewerState( new BasicViewerState() );
		state.addSources( sources );
		state.setSourcesActive( sources, true );
		sourceGroups.forEach( sourceGroup -> {
			final bdv.viewer.SourceGroup handle = new bdv.viewer.SourceGroup();
			state.addGroup( handle );
			state.setGroupName( handle, sourceGroup.getName() );
			state.setGroupActive( handle, sourceGroup.isActive() );
			sourceGroup.getSourceIds().forEach( i -> state.addSourceToGroup( sources.get( i ), handle ) );
		} );
		state.setNumTimepoints( numTimePoints );
		state.setViewerTransform( new AffineTransform3D() );
		state.setInterpolation( NEARESTNEIGHBOR );
		state.setDisplayMode( SINGLE );
		state.setCurrentSource( sources.isEmpty() ? null : sources.get( 0 ) );
		state.setCurrentGroup( state.getGroups().isEmpty() ? null : state.getGroups().get( 0 ) );
		state.setCurrentTimepoint( 0 );
	}

	public ViewerState( final SynchronizedViewerState state )
	{
		this.state = state;
	}

	/**
	 * copy constructor
	 *
	 * @param s
	 */
	protected ViewerState( final ViewerState s )
	{
		synchronized ( s.state )
		{
			state = new SynchronizedViewerState( new BasicViewerState( s.state ) );
		}
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
	public void getViewerTransform( final AffineTransform3D t )
	{
		state.getViewerTransform( t );
	}

	/**
	 * Set the viewer transform.
	 *
	 * @param t
	 * 		transform parameters.
	 */
	public void setViewerTransform( final AffineTransform3D t )
	{
		state.setViewerTransform( t );
	}

	/**
	 * Get the index of the current source.
	 */
	public int getCurrentSource()
	{
		synchronized ( state )
		{
			return state.getSources().indexOf( state.getCurrentSource() );
		}
	}

	/**
	 * Make the source with the given index current.
	 */
	public void setCurrentSource( final int index )
	{
		synchronized ( state )
		{
			state.setCurrentSource( state.getSources().get( index ) );
		}
	}

	/**
	 * Make the given source current.
	 */
	public void setCurrentSource( final Source< ? > source )
	{
		state.setCurrentSource( soc( source ) );
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
	public int getCurrentGroup()
	{
		synchronized ( state )
		{
			return state.getGroups().indexOf( state.getCurrentGroup() );
		}
	}

	/**
	 * Make the group with the given index current.
	 */
	public void setCurrentGroup( final int index )
	{
		synchronized ( state )
		{
			state.setCurrentGroup( state.getGroups().get( index ) );
		}
	}

	/**
	 * Make the given group current.
	 */
	public void setCurrentGroup( final SourceGroup group )
	{
		synchronized ( state )
		{
			state.setCurrentGroup( getHandle( group ) );
		}
	}

	private bdv.viewer.SourceGroup getHandle( final SourceGroup group )
	{
		for ( bdv.viewer.SourceGroup handle : state.getGroups() )
		{
			SourceGroup g = new SourceGroup( state.getGroupName( handle ) );
			state.getSourcesInGroup( handle ).forEach( s -> g.addSource( state.getSources().indexOf( s ) ) );
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
	public Interpolation getInterpolation()
	{
		return state.getInterpolation();
	}

	/**
	 * Set the interpolation method.
	 *
	 * @param method
	 * 		interpolation method.
	 */
	public void setInterpolation( final Interpolation method )
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
	public boolean isSingleSourceMode()
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
	public void setSingleSourceMode( final boolean singleSourceMode )
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
	public void setDisplayMode( final DisplayMode mode )
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
	public DisplayMode getDisplayMode()
	{
		return state.getDisplayMode();
	}

	/**
	 * Get the timepoint index that is currently displayed.
	 *
	 * @return current timepoint index
	 */
	public int getCurrentTimepoint()
	{
		return state.getCurrentTimepoint();
	}

	/**
	 * Set the current timepoint index.
	 *
	 * @param timepoint
	 * 		timepoint index.
	 */
	public void setCurrentTimepoint( final int timepoint )
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
		synchronized ( state )
		{
			List< SourceState< ? > > sourceStates = new ArrayList<>();
			for ( SourceAndConverter< ? > source : state.getSources() )
			{
				final SourceState< ? > ss = new SourceState<>( source, this );
				sourceStates.add( ss );
			}
			return sourceStates;
		}
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

	final Map< bdv.viewer.SourceGroup, SourceGroup > handleToSourceGroup = new HashMap<>();

	/**
	 * Returns a list of all source groups.
	 *
	 * @return list of all source groups.
	 */
	public List< SourceGroup > getSourceGroups()
	{
		synchronized ( state )
		{
			List< SourceGroup > sourceGroups = new ArrayList<>();
			for ( bdv.viewer.SourceGroup handle : state.getGroups() )
			{
				sourceGroups.add( handleToSourceGroup.computeIfAbsent( handle, h -> {
					SourceGroup g = new SourceGroup( state, handle );
					return g;
				} ) );
			}
			return sourceGroups;
		}
	}

	/**
	 * Returns the number of source groups.
	 *
	 * @return number of source groups.
	 */
	public int numSourceGroups()
	{
		synchronized ( state )
		{
			return state.getGroups().size();
		}
	}

	public void addSource( final SourceAndConverter< ? > source )
	{
		synchronized ( state )
		{
			state.addSource( source );
			state.setSourceActive( source, true );
		}
	}

	public void removeSource( final Source< ? > source )
	{
		synchronized ( state )
		{
			state.removeSource( soc ( source ) );
		}
	}

	protected void removeSource( final int index )
	{
		synchronized ( state )
		{
			state.removeSource( state.getSources().get( index ) );
		}
	}

	public void addGroup( final SourceGroup group )
	{
		synchronized ( state )
		{
			final bdv.viewer.SourceGroup handle = new bdv.viewer.SourceGroup();
			state.addGroup( handle );
			state.setGroupName( handle, group.getName() );
			state.setGroupActive( handle, group.isActive() );
			group.getSourceIds().forEach( i -> state.addSourceToGroup( state.getSources().get( i ), handle ) );
		}
	}

	public void removeGroup( final SourceGroup group )
	{
		synchronized ( state )
		{
			state.removeGroup( getHandle( group ) );
		}
	}

	protected void removeGroup( final int index )
	{
		state.removeGroup( state.getGroups().get( index ) );
	}

	public boolean isSourceVisible( final int index )
	{
		synchronized ( state )
		{
			return state.isSourceVisibleAndPresent( state.getSources().get( index ) );
		}
	}

	/**
	 * Returns a list of the indices of all currently visible sources.
	 *
	 * @return indices of all currently visible sources.
	 */
	public List< Integer > getVisibleSourceIndices()
	{
		synchronized ( state )
		{
			final List< SourceAndConverter< ? > > sources = new ArrayList<>( state.getVisibleAndPresentSources() );
			sources.sort( state.sourceOrder() );
			return sources.stream().map( state.getSources()::indexOf ).collect( Collectors.toList() );
		}
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
	public int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final int sourceIndex )
	{
		synchronized ( state )
		{
			return getBestMipMapLevel( screenScaleTransform, state.getSources().get( sourceIndex ).getSpimSource() );
		}
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 * 		screen scale, transforms screen coordinates to viewer coordinates.
	 *
	 * @return mipmap level
	 */
	public int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final Source< ? > source )
	{
		synchronized ( state )
		{
			final AffineTransform3D screenTransform = new AffineTransform3D();
			getViewerTransform( screenTransform );
			screenTransform.preConcatenate( screenScaleTransform );

			return MipmapTransforms.getBestMipMapLevel( screenTransform, source, state.getCurrentTimepoint() );
		}
	}

	/**
	 * Get the number of timepoints.
	 *
	 * @return the number of timepoints.
	 */
	public int getNumTimepoints()
	{
		return state.getNumTimepoints();
	}

	/**
	 * Set the number of timepoints.
	 *
	 * @param numTimepoints
	 * 		the number of timepoints.
	 */
	public void setNumTimepoints( final int numTimepoints )
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
		state.clearGroups();
		state.clearSources();
	}
}
