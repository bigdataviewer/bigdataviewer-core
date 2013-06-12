package viewer;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.ARGBType;

public interface CellColorInterface 
{
	public Img< ARGBType > createImage( final ImgFactory< ARGBType > factory, final long[] dimensions );
	public int getColorForCell( Cell cell );
}
