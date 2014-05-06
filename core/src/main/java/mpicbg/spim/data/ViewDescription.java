package mpicbg.spim.data;

import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * A view is one stack of of a SPIM sequence. It is identified by a time-point
 * and a view setup (angle, illumination direction, etc).
 *
 * Through the {@link SequenceDescription}, the inherited
 * {@link ViewRegistration#getTimepointIndex() timepoint index} and
 * {@link ViewRegistration#getSetupIndex() view setup index} can be mapped to
 * timepoint id and {@link ViewSetup}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ViewDescription extends ViewRegistration implements Comparable< ViewDescription >
{
	protected final SequenceDescription sequenceDescription;

	public ViewDescription( final SequenceDescription sequenceDescription, final int timepointIndex, final int setupIndex, final AffineTransform3D model )
	{
		super( timepointIndex, setupIndex, model );
		this.sequenceDescription = sequenceDescription;
	}

	/**
	 * Get the {@link ViewSetup} for this view.
	 *
	 * @return the view setup
	 */
	public ViewSetup getSetup()
	{
		return sequenceDescription.getViewSetups().get( setup );
	}

	/**
	 * get the timepoint for this view.
	 *
	 * @return timepoint
	 */
	public TimePoint getTimepoint()
	{
		return sequenceDescription.getTimePoints().getTimePoints().get( timepoint );
	}

	final static protected String basenameFormatString = "t%05d-a%03d-c%03d-i%01d";

	public String getBasename()
	{
		final ViewSetup s = getSetup();
		return String.format( basenameFormatString, getTimepoint(), s.getAngle(), s.getChannel(), s.getIllumination() );
	}

	@Override
	public int compareTo( final ViewDescription o )
	{
		if ( timepoint == o.timepoint )
			return setup - o.setup;
		else
			return timepoint - o.timepoint;
	}
}
