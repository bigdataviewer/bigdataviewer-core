package mpicbg.tracking.data;

import mpicbg.tracking.transform.AffineModel3D;

public class View implements Comparable< View >
{
	private final SequenceDescription sequenceDescription;

	/**
	 * The timepoint index.
	 */
	private final int timepoint;

	/**
	 * The setup index (within the timepoint).
	 */
	private final int setup;

	/**
	 * The affine registration model of this view mapping local into world
	 * coordinates.
	 */
	private final AffineModel3D model;

	public View( final SequenceDescription sequenceDescription, final int timepointIndex, final int setupIndex, final AffineModel3D model )
	{
		this.sequenceDescription = sequenceDescription;
		this.timepoint = timepointIndex;
		this.setup = setupIndex;
		this.model = model;
	}

	/**
	 * Get the timepoint index.
	 *
	 * @return timepoint index
	 */
	public int getTimepointIndex()
	{
		return timepoint;
	}

	/**
	 * Get the setup index (within the timepoint).
	 *
	 * @return setup index
	 */
	public int getSetupIndex()
	{
		return setup;
	}

	/**
	 * Get the affine registration model of this view mapping local into world
	 * coordinates.
	 *
	 * @return registration model
	 */
	public AffineModel3D getModel()
	{
		return model;
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


	final static private String basenameFormatString = "t%05d-a%03d-c%03d-i%01d";

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
