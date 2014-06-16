package bdv.util;

import mpicbg.spim.data.SpimDataException;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class PrintSequenceMipmapInfo
{
	public static void printSequenceMipmapInfo( final String xmlFilename ) throws SpimDataException
	{
		System.out.println( "mipmap setup for " + xmlFilename );

		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilename );

		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		if ( seq.getImgLoader() instanceof Hdf5ImageLoader )
		{
			final Hdf5ImageLoader imgLoader = ( Hdf5ImageLoader ) seq.getImgLoader();
			imgLoader.printMipmapInfo();
		}
		else
		{
			System.err.println( "not a hdf5 dataset" );
		}
	}

	public static void main( final String[] args )
	{
		try
		{
			if ( args.length > 0 )
			{
				for ( final String fn : args )
					printSequenceMipmapInfo( fn );
			}
			else
			{
//				printSequenceMipmapInfo( "/Users/pietzsch/Desktop/data/BDV130418A325/BDV130418A325_NoTempReg.xml" );
//				printSequenceMipmapInfo( "/Users/pietzsch/Desktop/data/BDV130418A325-re-tiled-12/rewrite.xml" );
//				printSequenceMipmapInfo( "/Users/pietzsch/Desktop/data/BDV130418A325-re-tiled-16/rewrite.xml" );
//				printSequenceMipmapInfo( "/Users/Pietzsch/Desktop/bdv example/drosophila 2.xml" );
				printSequenceMipmapInfo( "/Users/Pietzsch/Desktop/spimrec2.xml" );
			}
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
	}
}
