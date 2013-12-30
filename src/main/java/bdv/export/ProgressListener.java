package bdv.export;

public interface ProgressListener
{
	public void println( String s );

	public void setProgress( double completionRatio );
}