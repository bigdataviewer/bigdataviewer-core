package viewer.render.overlay;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import viewer.render.DisplayMode;
import viewer.render.SourceState;
import viewer.render.ViewerState;

/**
 * Render multibox overlay corresponding to a {@link ViewerState} into a
 * {@link Graphics2D}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class MultiBoxOverlayRenderer
{
	/**
	 * Navigation wire-frame cube.
	 */
	final protected MultiBoxOverlay box;

	/**
	 * Screen interval in which to display navigation wire-frame cube.
	 */
	protected Interval boxInterval;

	/**
	 * scaled screenImage interval for {@link #box} rendering
	 */
	protected Interval virtualScreenInterval;

	final protected ArrayList< IntervalAndTransform > boxSources;

	public MultiBoxOverlayRenderer()
	{
		this( 800, 600 );
	}

	public MultiBoxOverlayRenderer( final int screenWidth, final int screenHeight )
	{
		box = new MultiBoxOverlay();
		boxInterval = Intervals.createMinSize( 10, 10, 160, 120 );
		virtualScreenInterval = Intervals.createMinSize( 0, 0, screenWidth, screenHeight );
		boxSources = new ArrayList< IntervalAndTransform >();
	}

	public synchronized void paint( final Graphics2D g )
	{
		box.paint( g, boxSources, virtualScreenInterval, boxInterval );
	}

	// TODO
	public boolean isHighlightInProgress()
	{
		return box.isHighlightInProgress();
	}

	// TODO
	public void highlight( final int sourceIndex )
	{
		box.highlight( sourceIndex );
	}

	/**
	 * Update the screen interval. This is the target 2D interval into which
	 * pixels are rendered. (In the box overlay it is shown as a filled grey
	 * rectangle.)
	 */
	public synchronized void updateVirtualScreenSize( final int screenWidth, final int screenHeight )
	{
		final long oldW = virtualScreenInterval.dimension( 0 );
		final long oldH = virtualScreenInterval.dimension( 1 );
		if ( screenWidth != oldW || screenHeight != oldH )
			virtualScreenInterval = Intervals.createMinSize( 0, 0, screenWidth, screenHeight );
	}

	/**
	 * Update the box interval. This is the screen interval in which to display
	 * navigation wire-frame cube.
	 */
	public synchronized void setBoxInterval( final Interval interval )
	{
		boxInterval = interval;
	}

	/**
	 * Update data to show in the box overlay.
	 */
	public synchronized void setViewerState( final ViewerState viewerState )
	{
		synchronized ( viewerState )
		{
			final List< SourceState< ? > > sources = viewerState.getSources();
			final List< Integer > visible = viewerState.getVisibleSourceIndices();
			final int timepoint = viewerState.getCurrentTimepoint();
			final DisplayMode displayMode = viewerState.getDisplayMode();

			final int numSources = sources.size();
			int numPresentSources = 0;
			for ( final SourceState< ? > source : sources )
				if ( source.getSpimSource().isPresent( timepoint ) )
					numPresentSources++;
			if ( boxSources.size() != numPresentSources )
			{
				while ( boxSources.size() < numPresentSources )
					boxSources.add( new IntervalAndTransform() );
				while ( boxSources.size() > numPresentSources )
					boxSources.remove( boxSources.size() - 1 );
			}

			final AffineTransform3D sourceToViewer = new AffineTransform3D();
			for ( int i = 0, j = 0; i < numSources; ++i )
			{
				final SourceState< ? > source = sources.get( i );
				if ( source.getSpimSource().isPresent( timepoint ) )
				{
					final IntervalAndTransform boxsource = boxSources.get( j++ );
					viewerState.getViewerTransform( sourceToViewer );
					sourceToViewer.concatenate( source.getSpimSource().getSourceTransform( timepoint, 0 ) );
					boxsource.setSourceToViewer( sourceToViewer );
					boxsource.setSourceInterval( source.getSpimSource().getSource( timepoint, 0 ) );
					boxsource.setVisible( visible.contains( i ) );
				}
			}
		}
	}
}
