package bdv.viewer.overlay;

import static bdv.viewer.DisplayMode.FUSEDGROUP;
import static bdv.viewer.DisplayMode.GROUP;

import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

import bdv.viewer.DisplayMode;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;

/**
 * Render current source name and current timepoint of a {@link ViewerState}
 * into a {@link Graphics2D}.
 *
 * @author Tobias Pietzsch
 */
public class SourceInfoOverlayRenderer
{
	protected String sourceName;

	protected String groupName;

	protected String timepointString;

	public synchronized void paint( final Graphics2D g )
	{
		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		g.drawString( sourceName, ( int ) g.getClipBounds().getWidth() / 2, 12 );
		g.drawString( groupName, ( int ) g.getClipBounds().getWidth() / 2, 25 );
		g.drawString( timepointString, ( int ) g.getClipBounds().getWidth() - 170, 12 );
	}

	/**
	 * Update data to show in the overlay.
	 */
	public synchronized void setViewerState( final ViewerState state )
	{
		synchronized ( state )
		{
			final List< SourceState< ? > > sources = state.getSources();
			if ( ! sources.isEmpty() )
				sourceName = sources.get( state.getCurrentSource() ).getSpimSource().getName();
			else
				sourceName = "";

			final List< SourceGroup > groups = state.getSourceGroups();
			final DisplayMode mode = state.getDisplayMode();
			if ( ( mode == GROUP || mode == FUSEDGROUP ) && ! groups.isEmpty() )
				groupName = groups.get( state.getCurrentGroup() ).getName();
			else
				groupName = "";

			timepointString = String.format( "t = %d", state.getCurrentTimepoint() );
		}
	}
}
