package bdv.export;

import java.io.OutputStream;
import java.io.PrintStream;

public class ProgressWriterNull implements ProgressWriter
{
	private final PrintStream blackhole;

	public ProgressWriterNull()
	{
		blackhole = new PrintStream( new OutputStream() {
			@Override
			public void write( final int b )
			{}

			@Override
			public void write( final byte[] b )
			{}

			@Override
			public void write( final byte[] b, final int off, final int len )
			{}
		} );
	}

	@Override
	public PrintStream out()
	{
		return blackhole;
	}

	@Override
	public PrintStream err()
	{
		return blackhole;
	}

	@Override
	public void setProgress( final double completionRatio )
	{}
}
