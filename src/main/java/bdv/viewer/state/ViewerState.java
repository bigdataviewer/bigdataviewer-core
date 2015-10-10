/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.util.MipmapTransforms;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

/**
 * Description of everything required to render the current image, such as the
 * current timepoint, the visible and current sources and groups respectively,
 * the viewer transformation, etc.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ViewerState
{
	protected final ArrayList< SourceState< ? > > sources;

	/**
	 * read-only view of {@link #sources}.
	 */
	private final List< SourceState< ? > > unmodifiableSources;

	protected final ArrayList< SourceGroup > groups;

	/**
	 * read-only view of {@link #groups}.
	 */
	private final List< SourceGroup > unmodifiableGroups;

	/**
	 * number of available timepoints.
	 */
	protected final int numTimePoints;

	/**
	 * Transformation set by the interactive viewer. Transforms from global
	 * coordinate system to viewer coordinate system.
	 */
	protected final AffineTransform3D viewerTransform;

	/**
	 * Which interpolation method is currently used to render the display.
	 */
	protected Interpolation interpolation;

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 */
//	protected boolean singleSourceMode;
	/**
	 * TODO
	 */
	protected DisplayMode displayMode;

	/**
	 * The index of the current source.
	 * (In single-source mode only the current source is shown.)
	 */
	protected int currentSource;

	/**
	 * The index of the current group.
	 * (In single-group mode only the sources in the current group are shown.)
	 */
	protected int currentGroup;

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
	public ViewerState( final List< SourceAndConverter< ? > > sources, final List< SourceGroup > sourceGroups, final int numTimePoints )
	{
		this.sources = new ArrayList< SourceState< ? > >( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( SourceState.create( source, this ) );
		unmodifiableSources = Collections.unmodifiableList( this.sources );
		groups = new ArrayList< SourceGroup >( sourceGroups.size() );
		for ( final SourceGroup g : sourceGroups )
			groups.add( g.copy( this ) );
		unmodifiableGroups = Collections.unmodifiableList( this.groups );
		this.numTimePoints = numTimePoints;

		viewerTransform = new AffineTransform3D();
		interpolation = NEARESTNEIGHBOR;
		displayMode = SINGLE;
		currentSource = 0;
		currentGroup = 0;
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
			this.sources.add( source.copy( this ) );
		unmodifiableSources = Collections.unmodifiableList( sources );
		groups = new ArrayList< SourceGroup >( s.groups.size() );
		for ( final SourceGroup group : s.groups )
			this.groups.add( group.copy( this ) );
		unmodifiableGroups = Collections.unmodifiableList( groups );
		numTimePoints = s.numTimePoints;
		viewerTransform = s.viewerTransform.copy();
		interpolation = s.interpolation;
		displayMode = s.displayMode;
		currentSource = s.currentSource;
		currentGroup = s.currentGroup;
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
	 * Get the index of the current source.
	 */
	public synchronized int getCurrentSource()
	{
		return currentSource;
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
	public synchronized int getCurrentGroup()
	{
		return currentGroup;
	}

	/**
	 * Make the source with the given index current.
	 */
	public synchronized void setCurrentGroup( final int index )
	{
		if ( index >= 0 && index < groups.size() )
		{
			groups.get( currentGroup ).setCurrent( false );
			currentGroup = index;
			groups.get( currentGroup ).setCurrent( true );
		}
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

	// TODO: replace by getDisplayMode()
	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 *
	 * @return whether the display mode is <em>single-source</em>.
	 */
	public synchronized boolean isSingleSourceMode()
	{
		return displayMode == SINGLE;
	}

	// TODO: replace by setDisplayMode();
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
		if ( singleSourceMode )
			setDisplayMode( SINGLE );
		else
			setDisplayMode( FUSED );
	}

	/**
	 * TODO
	 * @param mode
	 */
	public synchronized void setDisplayMode( final DisplayMode mode )
	{
		displayMode = mode;
	}

	public synchronized DisplayMode getDisplayMode()
	{
		return displayMode;
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
	public List< SourceState< ? > > getSources()
	{
		return unmodifiableSources;
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
	 * Returns a list of all source groups.
	 *
	 * @return list of all source groups.
	 */
	public List< SourceGroup > getSourceGroups()
	{
		return unmodifiableGroups;
	}

	/**
	 * Returns the number of source groups.
	 *
	 * @return number of source groups.
	 */
	public int numSourceGroups()
	{
		return groups.size();
	}

	public synchronized void addSource( final SourceAndConverter< ? > source )
	{
		sources.add( SourceState.create( source, this ) );
	}

	public synchronized void removeSource( final Source< ? > source )
	{
		for ( int i = 0; i < sources.size(); )
		{
			final SourceState< ? > s = sources.get( i );
			if ( s.getSpimSource() == source )
				removeSource( i );
			else
				i++;
		}
	}

	protected void removeSource( final int index )
	{
		sources.remove( index );
		if ( currentSource == index )
			currentSource = 0;
		else if ( currentSource > index )
			--currentSource;
		for( final SourceGroup group : groups )
		{
			final SortedSet< Integer > ids = group.getSourceIds();
			final ArrayList< Integer > oldids = new ArrayList< Integer >( ids );
			ids.clear();
			for ( final int id : oldids )
			{
				if ( id < index )
					ids.add( id );
				else if ( id > index )
					ids.add( id - 1 );
			}
		}
	}

	public synchronized boolean isSourceVisible( final int index )
	{
		switch ( displayMode )
		{
		case SINGLE:
			return ( index == currentSource ) && isPresent( index );
		case GROUP:
			return groups.get( currentGroup ).getSourceIds().contains( index ) && isPresent( index );
		case FUSED:
			return sources.get( index ).isActive() && isPresent( index );
		case FUSEDGROUP:
		default:
			for ( final SourceGroup group : groups )
				if ( group.isActive() && group.getSourceIds().contains( index ) && isPresent( index ) )
					return true;
			return false;
		}
	}

	private boolean isPresent( final int sourceId )
	{
		return sources.get( sourceId ).getSpimSource().isPresent( currentTimepoint );
	}

	/**
	 * Returns a list of the indices of all currently visible sources.
	 *
	 * @return indices of all currently visible sources.
	 */
	public synchronized List< Integer > getVisibleSourceIndices()
	{
		final ArrayList< Integer > visible = new ArrayList< Integer >();
		switch ( displayMode )
		{
		case SINGLE:
			if ( isPresent( currentSource ) )
				visible.add( currentSource );
			break;
		case GROUP:
			for ( final int sourceId : groups.get( currentGroup ).getSourceIds() )
				if ( isPresent( sourceId ) )
					visible.add( sourceId );
			break;
		case FUSED:
			for ( int i = 0; i < sources.size(); ++i )
				if ( sources.get( i ).isActive() && isPresent( i ) )
					visible.add( i );
			break;
		case FUSEDGROUP:
			final TreeSet< Integer > gactive = new TreeSet< Integer >();
			for ( final SourceGroup group : groups )
				if ( group.isActive() )
					gactive.addAll( group.getSourceIds() );
			for ( final int sourceId : new ArrayList< Integer >( gactive ) )
				if ( isPresent( sourceId ) )
					visible.add( sourceId );
			break;
		}
		return visible;
	}

	/*
	 * Utility methods.
	 */

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
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

		final Source< ? > source = sources.get( sourceIndex ).getSpimSource();

		return MipmapTransforms.getBestMipMapLevel( screenTransform, source, currentTimepoint );
	}

	public int getNumTimePoints()
	{
		return numTimePoints;
	}
}
