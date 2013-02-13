package viewer.refactor.overlay;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import viewer.refactor.SpimSourceState;
import viewer.refactor.SpimViewerState;

/**
 * Render current source name and current timepoint of a {@link SpimViewerState}
 * into a {@link Graphics2D}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SourceInfoOverlayRenderer
{
	protected String sourceName;

	protected String timepointString;

	public synchronized void paint( final Graphics2D g )
	{
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() / 2, 12 );
		g.drawString( timepointString, ( int ) g.getClipBounds().getWidth() - 170, 12 );
	}

	/**
	 * Update data to show in the overlay.
	 */
	public synchronized void setViewerState( final SpimViewerState state )
	{
		synchronized ( state )
		{
			final List< SpimSourceState< ? > > sources = state.getSources();
			if ( ! sources.isEmpty() )
			{
				final SpimSourceState< ? > source = sources.get( state.getCurrentSource() );
				sourceName = source.getSpimSource().getName();
				timepointString = String.format( "t = %d", state.getCurrentTimepoint() );
			}
		}
	}
}
