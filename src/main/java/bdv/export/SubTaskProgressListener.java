package bdv.export;


public class SubTaskProgressListener implements ProgressListener
{
	protected final ProgressListener progressListener;

	protected final double min;

	protected final double scale;

	public SubTaskProgressListener( final ProgressListener progressListener, final double startCompletionRatio, final double endCompletionRatio )
	{
		this.progressListener = progressListener;
		this.min = startCompletionRatio;
		this.scale = 1.0 / ( endCompletionRatio - startCompletionRatio );
	}

	@Override
	public void setProgress( final double completionRatio )
	{
		progressListener.setProgress( min + scale * completionRatio );
	}

	@Override
	public void println( final String s )
	{
		progressListener.println( s );
	}
}