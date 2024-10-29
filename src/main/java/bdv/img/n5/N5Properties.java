package bdv.img.n5;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;

public interface N5Properties
{
	public DataType getDataType( final N5Reader n5, final int setupId );
	public double[][] getMipmapResolutions( final N5Reader n5, final int setupId );
	public String getPath( final int setupId );
	public String getPath( final int setupId, final int timepointId );
	public String getPath( final int setupId, final int timepointId, final int level );
}
