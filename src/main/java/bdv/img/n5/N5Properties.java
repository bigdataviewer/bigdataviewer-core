package bdv.img.n5;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;

/**
 * Provides metadata and resolves dataset path names for (`setupId`,
 * `timePointId` and multiresolution `level`) triples.
 * <p>
 * This allows to adapt {@link N5ImageLoader} subclasses for different storage
 * schemes.
 */
public interface N5Properties
{
	DataType getDataType( N5Reader n5, int setupId );

	double[][] getMipmapResolutions( N5Reader n5, int setupId );

	long[] getDimensions( N5Reader n5, int setupId, int timepointId, int level );

	String getDatasetPath( int setupId, int timepointId, int level );
}
