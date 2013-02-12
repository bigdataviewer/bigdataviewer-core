package viewer.refactor;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import viewer.IntervalAndTransform;
import viewer.MultiBoxOverlay;

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
	public synchronized void setVirtualScreenSize( final int screenWidth, final int screenHeight )
	{
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
	public synchronized void setViewerState( final SpimViewerState viewerState )
	{
		synchronized ( viewerState )
		{
			final List< SpimSourceState< ? > > sources = viewerState.getSources();
			final int timepoint = viewerState.getCurrentTimepoint();
			final boolean singleSourceMode = viewerState.isSingleSourceMode();

			final int numSources = sources.size();
			if ( boxSources.size() != numSources )
			{
				while ( boxSources.size() < numSources )
					boxSources.add( new IntervalAndTransform() );
				while ( boxSources.size() > numSources )
					boxSources.remove( boxSources.size() - 1 );
			}

			final AffineTransform3D sourceToViewer = new AffineTransform3D();
			for ( int i = 0; i < numSources; ++i )
			{
				final SpimSourceState< ? > source = sources.get( i );
				final IntervalAndTransform boxsource = boxSources.get( i );
				viewerState.getViewerTransform( sourceToViewer );
				sourceToViewer.concatenate( source.getSpimSource().getSourceTransform( timepoint, 0 ) );
				boxsource.setSourceToViewer( sourceToViewer );
				boxsource.setSourceInterval( source.getSpimSource().getSource( timepoint, 0 ) );
				boxsource.setVisible( source.isVisible( singleSourceMode ) );
			}
		}
	}
}
