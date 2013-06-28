package creator;

public interface ProgressListener
{
	public void println( String s );

	public void updateProgress( double completionRatio );

	public void updateProgress( int numCompletedTasks, int numTasks );

	public ProgressListener createSubTaskProgressListener( double startCompletionRatio, double endCompletionRatio );

	public ProgressListener createSubTaskProgressListener( int taskToCompleteTasks, int numTasks );
}