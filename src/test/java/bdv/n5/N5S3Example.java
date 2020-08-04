package bdv.n5;

import bdv.BigDataViewer;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;

public class N5S3Example
{
	public static void main( String[] args ) throws SpimDataException
	{
		BigDataViewer.open( "https://raw.githubusercontent.com/platybrowser/platybrowser/master/data/1.0.1/images/remote/prospr-6dpf-1-whole-ache.xml", "", null, ViewerOptions.options().numRenderingThreads( 3 ) );
	}
}
