package bdv.export;

import java.io.PrintStream;

public class SubTaskProgressWriter implements ProgressWriter
{
	protected final ProgressWriter progressWriter;

	protected final double min;

	protected final double scale;

	public SubTaskProgressWriter( final ProgressWriter progressWriter, final double startCompletionRatio, final double endCompletionRatio )
	{
		this.progressWriter = progressWriter;
		this.min = startCompletionRatio;
		this.scale = endCompletionRatio - startCompletionRatio;
	}

	@Override
	public PrintStream out()
	{
		return progressWriter.out();
	}

	@Override
	public PrintStream err()
	{
		return progressWriter.err();
	}

	@Override
	public void setProgress( final double completionRatio )
	{
		progressWriter.setProgress( min + scale * completionRatio );
	}
}
