package viewer;

import java.util.Random;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.ARGBType;

public class RandomCellColor implements CellColorInterface
{
	final Random rnd = new Random();
	
	@Override
	public int getColorForCell( final Cell cell ) 
	{
		final int r = rnd.nextInt( 127 ) + 128;
		final int g = rnd.nextInt( 127 ) + 128;
		final int b = rnd.nextInt( 127 ) + 128;
		return ARGBType.rgba( r, g, b, 0 );
	}

	@Override
	public Img<ARGBType> createImage( final ImgFactory<ARGBType> factory, final long[] dimensions )
	{
		return factory.create( dimensions, new ARGBType() );
	}
}
