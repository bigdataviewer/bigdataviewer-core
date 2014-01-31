package bdv.export;

import java.io.PrintStream;

public interface ProgressWriter
{
	public PrintStream out();

	public PrintStream err();

	public void setProgress( double completionRatio );
}
