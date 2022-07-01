/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.viewer;

import static bdv.ui.UIUtils.TextPosition.TOP_RIGHT;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jdom2.Element;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.TransformEventHandler;
import bdv.TransformState;
import bdv.cache.CacheControl;
import bdv.ui.UIUtils;
import bdv.util.Prefs;
import bdv.viewer.animate.AbstractTransformAnimator;
import bdv.viewer.animate.MessageOverlayAnimator;
import bdv.viewer.animate.OverlayAnimator;
import bdv.viewer.animate.TextOverlayAnimator;
import bdv.viewer.animate.TextOverlayAnimator.TextPosition;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.overlay.SourceInfoOverlayRenderer;
import bdv.viewer.render.DebugTilingOverlay;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.awt.BufferedImageOverlayRenderer;
import bdv.viewer.state.XmlIoViewerState;
import net.imglib2.Interval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * A JPanel for viewing multiple of {@link Source}s. The panel contains a
 * {@link InteractiveDisplayCanvas canvas} and a time slider (if there
 * are multiple time-points). Maintains a {@link ViewerState render state}, the
 * renderer, and basic navigation help overlays. It has it's own
 * {@link PainterThread} for painting, which is started on construction (use
 * {@link #stop() to stop the PainterThread}.
 *
 * @author Tobias Pietzsch
 */
public class ViewerPanel extends AbstractViewerPanel implements OverlayRenderer, PainterThread.Paintable, ViewerStateChangeListener
{
	private static final long serialVersionUID = 1L;

	/**
	 * Currently rendered state (visible sources, transformation, timepoint, etc.)
	 */
	private final SynchronizedViewerState state;

	/**
	 * Legacy wrapper around {@code state} to support deprecated API
	 */
	private final bdv.viewer.state.ViewerState deprecatedState;

	/**
	 * Renders the current state for the {@link #display}.
	 */
	private final MultiResolutionRenderer imageRenderer;

	/**
	 * TODO
	 */
	private final BufferedImageOverlayRenderer renderTarget;

	/**
	 * Overlay navigation boxes.
	 */
	// TODO: move to specialized class
	private final MultiBoxOverlayRenderer multiBoxOverlayRenderer;

	/**
	 * Overlay current source name and current timepoint.
	 */
	// TODO: move to specialized class
	private final SourceInfoOverlayRenderer sourceInfoOverlayRenderer;

	/**
	 * Overlay scalebar for current source.
	 */
	private final ScaleBarOverlayRenderer scaleBarOverlayRenderer;

	private final TransformEventHandler transformEventHandler;

	/**
	 * Canvas used for displaying the rendered {@link #renderTarget image} and
	 * overlays.
	 */
	private final InteractiveDisplayCanvas display;

	private final JSlider sliderTime;

	private boolean blockSliderTimeEvents;

	/**
	 * A {@link ThreadGroup} for (only) the threads used by this
	 * {@link ViewerPanel}, that is, {@link #painterThread} and
	 * {@link #renderingExecutorService}.
	 */
	private ThreadGroup threadGroup;

	/**
	 * Thread that triggers repainting of the display.
	 */
	private final PainterThread painterThread;

	/**
	 * The {@link ExecutorService} used for rendereing.
	 */
	private final ForkJoinPool renderingExecutorService;

	/**
	 * Manages visibility and currentness of sources and groups, as well as
	 * grouping of sources, and display mode.
	 */
	private final VisibilityAndGrouping visibilityAndGrouping;

	/**
	 * These listeners will be notified about changes to the
	 * viewer-transform. This is done <em>before</em> calling
	 * {@link #requestRepaint()} so listeners have the chance to interfere.
	 */
	private final Listeners.List< TransformListener< AffineTransform3D > > transformListeners;

	/**
	 * These listeners will be notified about changes to the current timepoint
	 * {@link ViewerState#getCurrentTimepoint()}. This is done <em>before</em>
	 * calling {@link #requestRepaint()} so listeners have the chance to
	 * interfere.
	 */
	private final Listeners.List< TimePointListener > timePointListeners;

	private final Listeners.List< InterpolationModeListener > interpolationModeListeners;

	/**
	 * Current animator for viewer transform, or null. This is for example used
	 * to make smooth transitions when {@link #align(AlignPlane) aligning to
	 * orthogonal planes}.
	 */
	private AbstractTransformAnimator currentAnimator = null;

	/**
	 * A list of currently incomplete (see {@link OverlayAnimator#isComplete()})
	 * animators. Initially, this contains a {@link TextOverlayAnimator} showing
	 * the "press F1 for help" message.
	 */
	private final ArrayList< OverlayAnimator > overlayAnimators;

	/**
	 * Fade-out overlay of recent messages. See {@link #showMessage(String)}.
	 */
	private final MessageOverlayAnimator msgOverlay;

	private final ViewerOptions.Values options;

	public ViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimePoints, final CacheControl cacheControl )
	{
		this( sources, numTimePoints, cacheControl, ViewerOptions.options() );
	}

	/**
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimepoints
	 *            number of available timepoints.
	 * @param cacheControl
	 *            to control IO budgeting and fetcher queue.
	 * @param optional
	 *            optional parameters. See {@link ViewerOptions#options()}.
	 */
	public ViewerPanel( final List< SourceAndConverter< ? > > sources, final int numTimepoints, final CacheControl cacheControl, final ViewerOptions optional )
	{
		super( new BorderLayout(), false );

		options = optional.values;

		state = setupState( sources, numTimepoints, options.getNumSourceGroups() );
		deprecatedState = new bdv.viewer.state.ViewerState( state );

		multiBoxOverlayRenderer = new MultiBoxOverlayRenderer();
		sourceInfoOverlayRenderer = new SourceInfoOverlayRenderer();
		scaleBarOverlayRenderer = new ScaleBarOverlayRenderer();

		threadGroup = new ThreadGroup( this.toString() );
		painterThread = new PainterThread( threadGroup, this );
		painterThread.setDaemon( true );
		transformEventHandler = options.getTransformEventHandlerFactory().create(
				TransformState.from( state::getViewerTransform, state::setViewerTransform ) );
		renderTarget = new BufferedImageOverlayRenderer();
		display = new InteractiveDisplayCanvas( options.getWidth(), options.getHeight() );
		display.setTransformEventHandler( transformEventHandler );
		display.overlays().add( renderTarget );
		display.overlays().add( this );

		renderingExecutorService = new ForkJoinPool(
				options.getNumRenderingThreads() );
		imageRenderer = new MultiResolutionRenderer(
				renderTarget, painterThread,
				options.getScreenScales(),
				options.getTargetRenderNanos(),
				options.getNumRenderingThreads(),
				renderingExecutorService,
				options.isUseVolatileIfAvailable(),
				options.getAccumulateProjectorFactory(),
				cacheControl );

		display.addHandler( mouseCoordinates );

		sliderTime = new JSlider( SwingConstants.HORIZONTAL, 0, numTimepoints - 1, 0 );
		sliderTime.addChangeListener( e -> {
			if ( !blockSliderTimeEvents )
				state.setCurrentTimepoint( sliderTime.getValue() );
		} );

		add( display, BorderLayout.CENTER );
		if ( numTimepoints > 1 )
			add( sliderTime, BorderLayout.SOUTH );
		setFocusable( false );

		visibilityAndGrouping = new VisibilityAndGrouping( deprecatedState );

		transformListeners = new Listeners.SynchronizedList<>( l -> l.transformChanged( state.getViewerTransform() ) );
		timePointListeners = new Listeners.SynchronizedList<>( l -> l.timePointChanged( state.getCurrentTimepoint() ) );
		interpolationModeListeners = new Listeners.SynchronizedList<>();

		msgOverlay = options.getMsgOverlay();

		overlayAnimators = new ArrayList<>();
		overlayAnimators.add( msgOverlay );
		overlayAnimators.add( new TextOverlayAnimator( "Press <F1> for help.", 3000, TextPosition.CENTER ) );

		display.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				requestRepaint();
				display.removeComponentListener( this );
			}
		} );

		state.changeListeners().add( this );

		painterThread.start();
	}

	/**
	 * Initialize ViewerState with the given {@code sources} and {@code numTimepoints}.
	 * Set up {@code numGroups} SourceGroups named "group 1", "group 2", etc. Add the
	 * first source to the first group, the second source to the second group etc.
	 *
	 * TODO: Setting up groups like this doesn't make a lot of sense. This just
	 *   replicates legacy behaviour. The remaining thing that stands in the way of
	 *   removing it is ViewerState serialization, which assumes that there are always 10
	 *   groups ... m(
	 */
	private static SynchronizedViewerState setupState( final List< SourceAndConverter< ? > > sources, final int numTimepoints, final int numGroups )
	{
		final SynchronizedViewerState state = new SynchronizedViewerState( new BasicViewerState() );
		state.addSources( sources );
		state.setSourcesActive( sources, true );
		for ( int i = 0; i < numGroups; ++i ) {
			final SourceGroup handle = new SourceGroup();
			state.addGroup( handle );
			state.setGroupName( handle,  "group " + ( i + 1 ) );
			state.setGroupActive( handle, true );
			if ( i < sources.size() )
				state.addSourceToGroup( sources.get( i ), handle );
		}
		state.setNumTimepoints( numTimepoints );
		state.setInterpolation( NEARESTNEIGHBOR );
		state.setDisplayMode( SINGLE );
		state.setCurrentSource( sources.isEmpty() ? null : sources.get( 0 ) );
		state.setCurrentGroup( numGroups <= 0 ? null : state.getGroups().get( 0 ) );

		return state;
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void addSource( final SourceAndConverter< ? > sourceAndConverter )
	{
		state.addSource( sourceAndConverter );
		state.setSourceActive( sourceAndConverter, true );
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void addSources( final Collection< SourceAndConverter< ? > > sourceAndConverter )
	{
		state.addSources( sourceAndConverter );
	}

	// helper for deprecated methods taking Source<?>
	@Deprecated
	private SourceAndConverter< ? > soc( final Source< ? > source )
	{
		for ( final SourceAndConverter< ? > soc : state.getSources() )
			if ( soc.getSpimSource() == source )
				return soc;
		return null;
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void removeSource( final Source< ? > source )
	{
		synchronized ( state )
		{
			state.removeSource( soc( source ) );
		}
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void removeSources( final Collection< Source< ? > > sources )
	{
		synchronized ( state )
		{
			state.removeSources( sources.stream().map( this::soc ).collect( Collectors.toList() ) );
		}
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void removeAllSources()
	{
		state.clearSources();
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void addGroup( final bdv.viewer.state.SourceGroup group )
	{
		synchronized ( state )
		{
			deprecatedState.addGroup( group );
		}
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 */
	@Deprecated
	public void removeGroup( final bdv.viewer.state.SourceGroup group )
	{
		synchronized ( state )
		{
			deprecatedState.removeGroup( group );
		}
	}

	/**
	 * Set {@code gPos} to the display coordinates at gPos transformed into the
	 * global coordinate system.
	 *
	 * @param gPos
	 *            is set to the corresponding global coordinates.
	 */
	public void displayToGlobalCoordinates( final double[] gPos )
	{
		assert gPos.length >= 3;
		state.getViewerTransform().applyInverse( gPos, gPos );
	}

	/**
	 * Set {@code gPos} to the display coordinates at gPos transformed into the
	 * global coordinate system.
	 *
	 * @param gPos
	 *            is set to the corresponding global coordinates.
	 */
	public < P extends RealLocalizable & RealPositionable > void displayToGlobalCoordinates( final P gPos )
	{
		assert gPos.numDimensions() >= 3;
		state.getViewerTransform().applyInverse( gPos, gPos );
	}

	/**
	 * Set {@code gPos} to the display coordinates (x,y,0)<sup>T</sup> transformed into the
	 * global coordinate system.
	 *
	 * @param gPos
	 *            is set to the global coordinates at display (x,y,0)<sup>T</sup>.
	 */
	public void displayToGlobalCoordinates( final double x, final double y, final RealPositionable gPos )
	{
		assert gPos.numDimensions() >= 3;
		final RealPoint lPos = new RealPoint( 3 );
		lPos.setPosition( x, 0 );
		lPos.setPosition( y, 1 );
		state.getViewerTransform().applyInverse( gPos, lPos );
	}

	@Override
	public void paint()
	{
		imageRenderer.paint( state );

		display.repaint();

		synchronized ( this )
		{
			if ( currentAnimator != null )
			{
				final AffineTransform3D transform = currentAnimator.getCurrent( System.currentTimeMillis() );
				state.setViewerTransform( transform );
				if ( currentAnimator.isComplete() )
					currentAnimator = null;
				else
					requestRepaint();
			}
		}
	}

	/**
	 * Repaint as soon as possible.
	 */
	@Override
	public void requestRepaint()
	{
		imageRenderer.requestRepaint();
	}

	public void requestRepaint( final Interval screenInterval )
	{
		imageRenderer.requestRepaint( screenInterval );
	}

	@Override
	protected void onMouseMoved()
	{
		if ( Prefs.showTextOverlay() )
			// trigger repaint for showing updated mouse coordinates
			getDisplayComponent().repaint();
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		boolean requiresRepaint = false;
		if ( Prefs.showMultibox() )
		{
			multiBoxOverlayRenderer.setViewerState( state );
			multiBoxOverlayRenderer.updateVirtualScreenSize( display.getWidth(), display.getHeight() );
			multiBoxOverlayRenderer.paint( ( Graphics2D ) g );
			requiresRepaint = multiBoxOverlayRenderer.isHighlightInProgress();
		}

		if ( Prefs.showTextOverlay() )
		{
			final Font font = UIUtils.getFont( "monospaced.small.font" );
			sourceInfoOverlayRenderer.setViewerState( state );
			sourceInfoOverlayRenderer.setSourceNameOverlayPosition( Prefs.sourceNameOverlayPosition() );
			sourceInfoOverlayRenderer.paint( ( Graphics2D ) g );

			final double[] gPos = new double[ 3 ];
			getGlobalMouseCoordinates( RealPoint.wrap( gPos ) );
			final String mousePosGlobalString = String.format( Locale.ROOT, "%6.1f, %6.1f, %6.1f", gPos[ 0 ], gPos[ 1 ], gPos[ 2 ] );

			g.setFont( font );
			UIUtils.drawString( g, TOP_RIGHT, 1, mousePosGlobalString );
		}

		if ( Prefs.showScaleBar() )
		{
			scaleBarOverlayRenderer.setViewerState( state );
			scaleBarOverlayRenderer.paint( ( Graphics2D ) g );
		}

		final long currentTimeMillis = System.currentTimeMillis();
		final ArrayList< OverlayAnimator > overlayAnimatorsToRemove = new ArrayList<>();
		for ( final OverlayAnimator animator : overlayAnimators )
		{
			animator.paint( ( Graphics2D ) g, currentTimeMillis );
			requiresRepaint |= animator.requiresRepaint();
			if ( animator.isComplete() )
				overlayAnimatorsToRemove.add( animator );
		}
		overlayAnimators.removeAll( overlayAnimatorsToRemove );

		if ( requiresRepaint )
			display.repaint();
	}

	@Override
	public void viewerStateChanged( final ViewerStateChange change )
	{
		switch ( change )
		{
		case CURRENT_SOURCE_CHANGED:
			multiBoxOverlayRenderer.highlight( state.getSources().indexOf( state.getCurrentSource() ) );
			display.repaint();
			break;
		case DISPLAY_MODE_CHANGED:
			showMessage( state.getDisplayMode().getName() );
			display.repaint();
			break;
		case GROUP_NAME_CHANGED:
			display.repaint();
			break;
		case CURRENT_GROUP_CHANGED:
			// TODO multiBoxOverlayRenderer.highlight() all sources in group that became current
			break;
		case SOURCE_ACTIVITY_CHANGED:
			// TODO multiBoxOverlayRenderer.highlight() all sources that became visible
			break;
		case GROUP_ACTIVITY_CHANGED:
			// TODO multiBoxOverlayRenderer.highlight() all sources that became visible
			break;
		case VISIBILITY_CHANGED:
			requestRepaint();
			break;
//		case SOURCE_TO_GROUP_ASSIGNMENT_CHANGED:
//		case NUM_SOURCES_CHANGED:
//		case NUM_GROUPS_CHANGED:
		case INTERPOLATION_CHANGED:
			final Interpolation interpolation = state.getInterpolation();
			showMessage( interpolation.getName() );
			interpolationModeListeners.list.forEach( l -> l.interpolationModeChanged( interpolation ) );
			requestRepaint();
			break;
		case NUM_TIMEPOINTS_CHANGED:
		{
			final int numTimepoints = state.getNumTimepoints();
			final int timepoint = Math.max( 0, Math.min( state.getCurrentTimepoint(), numTimepoints - 1 ) );
			SwingUtilities.invokeLater( () -> {
				final boolean sliderVisible = Arrays.asList( getComponents() ).contains( sliderTime );
				if ( numTimepoints > 1 && !sliderVisible )
					add( sliderTime, BorderLayout.SOUTH );
				else if ( numTimepoints == 1 && sliderVisible )
					remove( sliderTime );
				sliderTime.setModel( new DefaultBoundedRangeModel( timepoint, 0, 0, numTimepoints - 1 ) );
				revalidate();
			} );
			break;
		}
		case CURRENT_TIMEPOINT_CHANGED:
		{
			final int timepoint = state.getCurrentTimepoint();
			SwingUtilities.invokeLater( () -> {
				blockSliderTimeEvents = true;
				if ( sliderTime.getValue() != timepoint )
					sliderTime.setValue( timepoint );
				blockSliderTimeEvents = false;
			} );
			timePointListeners.list.forEach( l -> l.timePointChanged( timepoint ) );
			requestRepaint();
			break;
		}
		case VIEWER_TRANSFORM_CHANGED:
			final AffineTransform3D transform = state.getViewerTransform();
			transformListeners.list.forEach( l -> l.transformChanged( transform ) );
			requestRepaint();
		}
	}

	@Override
	public synchronized void setTransformAnimator( final AbstractTransformAnimator animator )
	{
		currentAnimator = animator;
		currentAnimator.setTime( System.currentTimeMillis() );
		requestRepaint();
	}

	/**
	 * Switch to next interpolation mode. (Currently, there are two
	 * interpolation modes: nearest-neighbor and N-linear.)
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void toggleInterpolation()
	{
		NavigationActions.toggleInterpolation( state );
	}

	/**
	 * Set the {@link Interpolation} mode.
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void setInterpolation( final Interpolation mode )
	{
		state.setInterpolation( mode );
	}

	/**
	 * Set the {@link DisplayMode}.
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void setDisplayMode( final DisplayMode displayMode )
	{
		state.setDisplayMode( displayMode );
	}

	/**
	 * @deprecated Modify {@link #state()} directly ({@code state().setViewerTransform(t)})
	 */
	@Deprecated
	public void setCurrentViewerTransform( final AffineTransform3D viewerTransform )
	{
		state.setViewerTransform( viewerTransform );
	}

	/**
	 * Show the specified time-point.
	 *
	 * @param timepoint
	 *            time-point index.
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void setTimepoint( final int timepoint )
	{
		state.setCurrentTimepoint( timepoint );
	}

	/**
	 * Show the next time-point.
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void nextTimePoint()
	{
		NavigationActions.nextTimePoint( state );
	}

	/**
	 * Show the previous time-point.
	 */
	// TODO: Deprecate or leave as convenience?
	public synchronized void previousTimePoint()
	{
		NavigationActions.previousTimePoint( state );
	}

	/**
	 * Set the number of available timepoints. If {@code numTimepoints == 1}
	 * this will hide the time slider, otherwise show it. If the currently
	 * displayed timepoint would be out of range with the new number of
	 * timepoints, the current timepoint is set to {@code numTimepoints - 1}.
	 * <p>
	 * This is equivalent to {@code state().setNumTimepoints(numTimepoints}}.
	 *
	 * @param numTimepoints
	 *            number of available timepoints. Must be {@code >= 1}.
	 */
	public void setNumTimepoints( final int numTimepoints )
	{
		state.setNumTimepoints( numTimepoints );
	}

	/**
	 * @deprecated Use {@link #state()} instead.
	 *
	 * Get a copy of the current {@link ViewerState}.
	 *
	 * @return a copy of the current {@link ViewerState}.
	 */
	@Deprecated
	public bdv.viewer.state.ViewerState getState()
	{
		return deprecatedState.copy();
	}

	/**
	 * Get the ViewerState. This can be directly used for modifications, e.g.,
	 * adding/removing sources etc. See {@link SynchronizedViewerState} for
	 * thread-safety considerations.
	 */
	@Override
	public SynchronizedViewerState state()
	{
		return state;
	}

	/**
	 * Get the viewer canvas.
	 *
	 * @return the viewer canvas.
	 */
	@Override
	public InteractiveDisplayCanvas getDisplay()
	{
		return display;
	}

	/**
	 * Get the AWT {@code Component} of the viewer canvas.
	 *
	 * @return the viewer canvas.
	 */
	@Override
	public Component getDisplayComponent()
	{
		return display;
	}

	public TransformEventHandler getTransformEventHandler()
	{
		return transformEventHandler;
	}

	/**
	 * Display the specified message in a text overlay for a short time.
	 *
	 * @param msg
	 *            String to display. Should be just one line of text.
	 */
	@Override
	public void showMessage( final String msg )
	{
		msgOverlay.add( msg );
		display.repaint();
	}

	/**
	 * Add a new {@link OverlayAnimator} to the list of animators. The animation
	 * is immediately started. The new {@link OverlayAnimator} will remain in
	 * the list of animators until it {@link OverlayAnimator#isComplete()}.
	 *
	 * @param animator
	 *            animator to add.
	 */
	@Override
	public void addOverlayAnimator( final OverlayAnimator animator )
	{
		overlayAnimators.add( animator );
		display.repaint();
	}

	/**
	 * Add/remove {@link InterpolationModeListener} to notify when the interpolation
	 * mode is changed.. Listeners will be notified <em>before</em> calling
	 * {@link #requestRepaint()} so they have the chance to interfere.
	 */
	public Listeners< InterpolationModeListener > interpolationModeListeners()
	{
		return interpolationModeListeners;
	}

	/**
	 * @deprecated Use {@code interpolationModeListeners().add( listener )}.
	 */
	@Deprecated
	public void addInterpolationModeListener( final InterpolationModeListener listener )
	{
		interpolationModeListeners().add( listener );
	}

	/**
	 * @deprecated Use {@code interpolationModeListeners().remove( listener )}.
	 */
	@Deprecated
	public void removeInterpolationModeListener( final InterpolationModeListener listener )
	{
		interpolationModeListeners().remove( listener );
	}

	/**
	 * Add/remove {@code TransformListener}s to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been painted
	 * with the viewer transform used to render that image.
	 * <p>
	 * This happens immediately after that image is painted onto the screen,
	 * before any overlays are painted.
	 */
	@Override
	public Listeners< TransformListener< AffineTransform3D > > renderTransformListeners()
	{
		return renderTarget.transformListeners();
	}

	/**
	 * @deprecated Use {@code renderTransformListeners().add( listener )}.
	 */
	@Deprecated
	public void addRenderTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		renderTransformListeners().add( listener );
	}

	/**
	 * @deprecated Use {@code renderTransformListeners().add( index, listener )}.
	 */
	@Deprecated
	public void addRenderTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		renderTransformListeners().add( index, listener );
	}

	/**
	 * Add/remove {@code TransformListener}s to notify about viewer transformation
	 * changes. Listeners will be notified <em>before</em> calling
	 * {@link #requestRepaint()} so they have the chance to interfere.
	 */
	@Override
	public Listeners< TransformListener< AffineTransform3D > > transformListeners()
	{
		return transformListeners;
	}

	/**
	 * @deprecated Use {@code transformListeners().add( listener )}.
	 */
	@Deprecated
	public void addTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		transformListeners().add( listener );
	}

	/**
	 * @deprecated Use {@code transformListeners().add( index, listener )}.
	 */
	@Deprecated
	public void addTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		transformListeners().add( index, listener );
	}

	/**
	 * @deprecated Use {@code transformListeners().remove( listener )} or
	 * {@code renderTransformListeners().remove( listener )} (whichever the listener was added to).
	 */
	@Deprecated
	public void removeTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		transformListeners().remove( listener );
		renderTransformListeners().remove( listener );
	}

	/**
	 * Add/remove {@link TimePointListener} to notify about time-point
	 * changes. Listeners will be notified <em>before</em> calling
	 * {@link #requestRepaint()} so they have the chance to interfere.
	 */
	public Listeners< TimePointListener > timePointListeners()
	{
		return timePointListeners;
	}

	/**
	 * @deprecated Use {@code timePointListeners().add( listener )}.
	 */
	@Deprecated
	public void addTimePointListener( final TimePointListener listener )
	{
		timePointListeners().add( listener );
	}

	/**
	 * @deprecated Use {@code timePointListeners().add( index, listener )}.
	 */
	@Deprecated
	public void addTimePointListener( final TimePointListener listener, final int index )
	{
		timePointListeners().add( index, listener );
	}

	/**
	 * @deprecated Use {@code timePointListeners().remove( listener )}.
	 */
	@Deprecated
	public void removeTimePointListener( final TimePointListener listener )
	{
		timePointListeners().remove( listener );
	}

	public synchronized Element stateToXml()
	{
		return new XmlIoViewerState().toXml( deprecatedState );
	}

	public synchronized void stateFromXml( final Element parent )
	{
		final XmlIoViewerState io = new XmlIoViewerState();
		io.restoreFromXml( parent.getChild( io.getTagName() ), deprecatedState );
	}

	/**
	 * @deprecated Modify {@link #state()} directly
	 *
	 * Returns the {@link VisibilityAndGrouping} that can be used to modify
	 * visibility and currentness of sources and groups, as well as grouping of
	 * sources, and display mode.
	 */
	@Deprecated
	public VisibilityAndGrouping getVisibilityAndGrouping()
	{
		return visibilityAndGrouping;
	}

	public ViewerOptions.Values getOptionValues()
	{
		return options;
	}

	@Override
	public InputTriggerConfig getInputTriggerConfig()
	{
		return options.getInputTriggerConfig();
	}

	public SourceInfoOverlayRenderer getSourceInfoOverlayRenderer()
	{
		return sourceInfoOverlayRenderer;
	}

	/**
	 * Stop the {@link #painterThread} and shutdown rendering {@link ExecutorService}.
	 */
	public void stop()
	{
		painterThread.interrupt();
		try
		{
			painterThread.join( 0 );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		renderingExecutorService.shutdown();
		state.clearGroups();
		state.clearSources();
		imageRenderer.kill();
		renderTarget.kill();
	}

	protected static final AtomicInteger panelNumber = new AtomicInteger( 1 );

	protected static class RenderThreadFactory implements ThreadFactory
	{
		private final ThreadGroup threadGroup;

		private final String threadNameFormat = String.format(
				"bdv-panel-%d-thread-%%d",
				panelNumber.getAndIncrement() );

		private final AtomicInteger threadNumber = new AtomicInteger( 1 );

		protected RenderThreadFactory( final ThreadGroup threadGroup )
		{
			this.threadGroup = threadGroup;
		}

		@Override
		public Thread newThread( final Runnable r )
		{
			final Thread t = new Thread( threadGroup, r,
					String.format( threadNameFormat, threadNumber.getAndIncrement() ),
					0 );
			if ( !t.isDaemon() )
				t.setDaemon( true );
			if ( t.getPriority() != Thread.NORM_PRIORITY )
				t.setPriority( Thread.NORM_PRIORITY );
			return t;
		}
	}

	/**
	 * Display overlay to show how the display is tiled for rendering.
	 * (For development and debugging purposes.)
	 */
	public DebugTilingOverlay showDebugTileOverlay()
	{
		final DebugTilingOverlay overlay = new DebugTilingOverlay( imageRenderer );
		display.overlays().add( overlay );
		return overlay;
	}
}
