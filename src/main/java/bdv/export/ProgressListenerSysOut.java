package bdv.export;

public class ProgressListenerSysOut implements ProgressListener
{
	@Override
	public void setProgress( final double completionRatio )
	{
	}

	@Override
	public void println( final String s )
	{
		System.out.println( s );
	}
}
