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

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import bdv.util.MipmapTransforms;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Description of everything required to render the current image, such as the
 * current timepoint, the visible and current sources and groups respectively,
 * the viewer transformation, etc.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class ViewerState
{
	private final ArrayList< SourceState< ? > > sources;

	/**
	 * read-only view of {@link #sources}.
	 */
	private final List< SourceState< ? > > unmodifiableSources;

	private final ArrayList< SourceGroup > groups;

	/**
	 * read-only view of {@link #groups}.
	 */
	private final List< SourceGroup > unmodifiableGroups;

	/**
	 * number of available timepoints.
	 */
	private int numTimepoints;

	/**
	 * Transformation set by the interactive viewer. Transforms from global
	 * coordinate system to viewer coordinate system.
	 */
	private final AffineTransform3D viewerTransform;

	/**
	 * Which interpolation method is currently used to render the display.
	 */
	private Interpolation interpolation;

	/**
	 * Is the display mode <em>single-source</em>? In <em>single-source</em>
	 * mode, only the current source (SPIM angle). Otherwise, in <em>fused</em>
	 * mode, all active sources are blended.
	 */
//	protected boolean singleSourceMode;
	/**
	 * TODO
	 */
	private DisplayMode displayMode;

	/**
	 * The index of the current source.
	 * (In single-source mode only the current source is shown.)
	 */
	private int currentSource;

	/**
	 * The index of the current group.
	 * (In single-group mode only the sources in the current group are shown.)
	 */
	private int currentGroup;

	/**
	 * which timepoint is currently shown.
	 */
	private int currentTimepoint;

	public ViewerState( final List< SourceAndConverter< ? > > sources, final int numTimePoints )
	{
		this( sources, null, numTimePoints );
	}

	/**
	 *
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 */
	public ViewerState( final List< SourceAndConverter< ? > > sources, final List< SourceGroup > sourceGroups, final int numTimePoints )
	{
		this.sources = new ArrayList<>( sources.size() );
		for ( final SourceAndConverter< ? > source : sources )
			this.sources.add( SourceState.create( source, this ) );
		unmodifiableSources = Collections.unmodifiableList( this.sources );
		groups = ( sourceGroups == null ) ? new ArrayList<>() : new ArrayList<>( sourceGroups );
		unmodifiableGroups = Collections.unmodifiableList( this.groups );
		this.numTimepoints = numTimePoints;

		viewerTransform = new AffineTransform3D();
		interpolation = NEARESTNEIGHBOR;
		displayMode = SINGLE;
		currentSource = sources.isEmpty() ? -1 : 0;
		currentGroup = groups.isEmpty() ? -1 : 0;
		currentTimepoint = 0;
	}

	/**
	 * copy constructor
	 * @param s
	 */
	protected ViewerState( final ViewerState s )
	{
		sources = new ArrayList<>( s.sources.size() );
		for ( final SourceState< ? > source : s.sources )
			this.sources.add( source.copy( this ) );
		unmodifiableSources = Collections.unmodifiableList( sources );
		groups = new ArrayList<>( s.groups.size() );
		for ( final SourceGroup group : s.groups )
			groups.add( group.copy() );
		unmodifiableGroups = Collections.unmodifiableList( groups );
		numTimepoints = s.numTimepoints;
		viewerTransform = s.viewerTransform.copy();
		interpolation = s.interpolation;
		displayMode = s.displayMode;
		currentSource = s.currentSource;
		currentGroup = s.currentGroup;
		currentTimepoint = s.currentTimepoint;
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
	 * @param t is set to the viewer transform.
	 */
	public synchronized void getViewerTransform(final AffineTransform3D t)
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
		final int minIndex = sources.isEmpty() ? -1 : 0;
		if ( index >= minIndex && index < sources.size() )
		{
			sources.get( currentSource ).setCurrent( false );
			currentSource = index;
			sources.get( currentSource ).setCurrent( true );
		}
	}

	/**
	 * Make the given source current.
	 */
	public synchronized void setCurrentSource( final Source< ? > source )
	{
		final int i = getSourceIndex( source );
		if ( i >= 0 )
			setCurrentSource( i );
	}

	/**
	 * Get the index of the current group.
	 */
	public synchronized int getCurrentGroup()
	{
		return currentGroup;
	}

	/**
	 * Make the group with the given index current.
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
	 * Make the given group current.
	 */
	public synchronized void setCurrentGroup( final SourceGroup group )
	{
		final int i = getGroupIndex( group );
		if ( i >= 0 )
			setCurrentGroup( i );
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
	 *
	 * @deprecated replaced by {@link #getDisplayMode()}
	 */
	@Deprecated
	public synchronized boolean isSingleSourceMode()
	{
		return displayMode == SINGLE;
	}

	/**
	 * Set the display mode to <em>single-source</em> (true) or <em>fused</em>
	 * (false). In <em>single-source</em> mode, only the current source (SPIM
	 * angle) is shown. In <em>fused</em> mode, all active sources are blended.
	 *
	 * @param singleSourceMode
	 *            If true, set <em>single-source</em> mode. If false, set
	 *            <em>fused</em> mode.
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
	 *            the display mode
	 */
	public synchronized void setDisplayMode( final DisplayMode mode )
	{
		displayMode = mode;
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
		if ( currentSource < 0 )
			currentSource = 0;
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
		if ( sources.isEmpty() )
			currentSource = -1;
		else if ( currentSource == index )
			currentSource = 0;
		else if ( currentSource > index )
			--currentSource;
		for( final SourceGroup group : groups )
		{
			final SortedSet< Integer > ids = group.getSourceIds();
			final ArrayList< Integer > oldids = new ArrayList<>( ids );
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

	public synchronized void addGroup( final SourceGroup group )
	{
		if ( !groups.contains( group ) )
		{
			groups.add( group );
			if ( currentGroup < 0 )
				currentGroup = 0;
		}
	}

	public synchronized void removeGroup( final SourceGroup group )
	{
		final int i = groups.indexOf( group );
		if ( i >= 0 )
			removeGroup( i );
	}

	protected void removeGroup( final int index )
	{
		groups.remove( index );
		if ( groups.isEmpty() )
			currentGroup = -1;
		else if ( currentGroup == index )
			currentGroup = 0;
		else if ( currentGroup > index )
			--currentGroup;
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
		final ArrayList< Integer > visible = new ArrayList<>();
		switch ( displayMode )
		{
		case SINGLE:
			if ( currentSource >= 0 && isPresent( currentSource ) )
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
			final TreeSet< Integer > gactive = new TreeSet<>();
			for ( final SourceGroup group : groups )
				if ( group.isActive() )
					gactive.addAll( group.getSourceIds() );
			for ( final int sourceId : new ArrayList<>( gactive ) )
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
	public synchronized int getBestMipMapLevel(final AffineTransform3D screenScaleTransform, final int sourceIndex)
	{
		return getBestMipMapLevel( screenScaleTransform, sources.get( sourceIndex ).getSpimSource() );
	}

	/**
	 * Get the mipmap level that best matches the given screen scale for the given source.
	 *
	 * @param screenScaleTransform
	 *            screen scale, transforms screen coordinates to viewer coordinates.
	 * @return mipmap level
	 */
	public synchronized int getBestMipMapLevel( final AffineTransform3D screenScaleTransform, final Source< ? > source )
	{
		final AffineTransform3D screenTransform = new AffineTransform3D();
		getViewerTransform( screenTransform );
		screenTransform.preConcatenate( screenScaleTransform );

		return MipmapTransforms.getBestMipMapLevel( screenTransform, source, currentTimepoint );
	}

	/**
	 * Get the number of timepoints.
	 *
	 * @return the number of timepoints.
	 */
	public synchronized int getNumTimepoints()
	{
		return numTimepoints;
	}

	/**
	 * Set the number of timepoints.
	 *
	 * @param numTimepoints
	 *            the number of timepoints.
	 */
	public synchronized void setNumTimepoints( final int numTimepoints )
	{
		this.numTimepoints = numTimepoints;
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
		sources.clear();
		groups.clear();
	}

	/**
	 * Get index of (first) {@link SourceState} that matches the given
	 * {@link Source} or {@code -1} if not found.
	 */
	private int getSourceIndex( final Source< ? > source )
	{
		for ( int i = 0; i < sources.size(); ++i )
		{
			final SourceState< ? > s = sources.get( i );
			if ( s.getSpimSource() == source )
				return i;
		}
		return -1;
	}

	/**
	 * Get index of (first) {@link SourceGroup} that matches the given
	 * {@code group} or {@code -1} if not found.
	 */
	private int getGroupIndex( final SourceGroup group )
	{
		return groups.indexOf( group );
	}
}
