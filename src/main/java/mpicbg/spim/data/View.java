package mpicbg.spim.data;

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
public class View extends ViewRegistration implements Comparable< View >
{
	protected final SequenceDescription sequenceDescription;

	public View( final SequenceDescription sequenceDescription, final int timepointIndex, final int setupIndex, final AffineTransform3D model )
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
		return sequenceDescription.setups[ setup ];
	}

	/**
	 * get the timepoint id for this view.
	 *
	 * @return timepoint
	 */
	public int getTimepoint()
	{
		return sequenceDescription.timepoints[ timepoint ];
	}

	final static protected String basenameFormatString = "t%05d-a%03d-c%03d-i%01d";

	public String getBasename()
	{
		final ViewSetup s = getSetup();
		return String.format( basenameFormatString, getTimepoint(), s.getAngle(), s.getChannel(), s.getIllumination() );
	}

	@Override
	public int compareTo( final View o )
	{
		if ( timepoint == o.timepoint )
			return setup - o.setup;
		else
			return timepoint - o.timepoint;
	}
}
