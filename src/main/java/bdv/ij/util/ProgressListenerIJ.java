package bdv.ij.util;

import ij.IJ;
import bdv.export.ProgressListener;

public class ProgressListenerIJ implements ProgressListener
{
	final double min;

	final double scale;

	public ProgressListenerIJ( final double min, final double max )
	{
		this.min = min;
		this.scale = 1.0 / ( max - min );
	}

	@Override
	public void setProgress( final double completionRatio )
	{
		IJ.showProgress( min + scale * completionRatio );
	}

	@Override
	public void println( final String s )
	{
		IJ.log( s );
	}
}
