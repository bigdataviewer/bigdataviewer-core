package viewer.util;


public abstract class AbstractAnimator
{
	private final long duration;

	private long startTime;

	private boolean started;

	private double complete;

	public AbstractAnimator( final long duration )
	{
		this.duration = duration;
		started = false;
		complete = 0;
	}

	public void setTime( final long time )
	{
		if ( ! started )
		{
			started = true;
			startTime = time;
		}

		complete = ( time - startTime ) / ( double ) duration;
		if ( complete >= 1 )
			complete = 1;
	}

	public boolean isComplete()
	{
		return complete == 1;
	}

	public double ratioComplete()
	{
		return complete;
	}
}
