package bdv.export;

import java.io.PrintStream;

public class ProgressWriterConsole implements ProgressWriter
{
	@Override
	public PrintStream out()
	{
		return System.out;
	}

	@Override
	public PrintStream err()
	{
		return System.err;
	}

	@Override
	public void setProgress( final double completionRatio )
	{
		System.out.printf( "progress: %.1f %% complete\n", completionRatio * 100 );
	}
}
