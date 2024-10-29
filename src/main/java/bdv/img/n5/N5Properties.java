package bdv.img.n5;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;

import bdv.img.cache.VolatileCachedCellImg;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;

public interface N5Properties
{
	// in case of OME-ZARR, it has an underlying 5D container
	public < T extends NativeType<T> > RandomAccessibleInterval<T> extractImg( final VolatileCachedCellImg<T, ?> img, final int setupId, final int timepointId );

	// give the option to pre-fetch the attributes or store them in the XML
	public DatasetAttributes getDatasetAttributes( final N5Reader n5, final String pathName );

	public DataType getDataType( final N5Reader n5, final int setupId );
	public double[][] getMipmapResolutions( final N5Reader n5, final int setupId );
	public String getPath( final int setupId );
	public String getPath( final int setupId, final int timepointId );
	public String getPath( final int setupId, final int timepointId, final int level );
}
