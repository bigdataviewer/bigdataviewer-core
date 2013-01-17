package creator;

import ij.ImageJ;

import java.io.File;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import net.imglib2.algorithm.stats.Max;
import net.imglib2.algorithm.stats.Min;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TestLoader
{
	public static void main( final String[] args )
	{
		final File seqFile = new File( "/Users/tobias/Desktop/celegans/celegans-desc.xml" );
		try
		{
			final SequenceDescription seq = SequenceDescription.load( seqFile.getAbsolutePath(), true );
			final int numTimepoints = seq.numTimepoints();
			final int numSetups = seq.numViewSetups();

			final int timepoint = 0;
			final int setup = 0;
			final View view = new View( seq, timepoint, setup, null );
			final ImgPlus< UnsignedShortType > img = seq.imgLoader.getUnsignedShortImage( view );
			System.out.println( "min = " + Min.findMin( img ).get().get() );
			System.out.println( "max = " + Max.findMax( img ).get().get() );
			System.out.println( -Short.MIN_VALUE + Short.MAX_VALUE );
			new ImageJ();
			ImageJFunctions.show( img );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}
