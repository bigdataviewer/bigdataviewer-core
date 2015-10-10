/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.util;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.export.ProposeMipmaps;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.MipmapInfo;
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

			final int timepointId = seq.getTimePoints().getTimePointsOrdered().get( 0 ).getId();
			for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
			{
				final int setupId = setup.getId();
				System.out.println( "setup " + setupId );
				final int numLevels = imgLoader.getSetupImgLoader( setupId ).numMipmapLevels();
				final ViewRegistration reg = spimData.getViewRegistrations().getViewRegistration( timepointId, setupId );
				final AffineTransform3D model = reg.getModel();
				final double[] scale = new double[ 3 ];
				for ( int d = 0; d < 3; ++d )
				{
					scale[ d ] = Affine3DHelpers.extractScale( model, d );
				}
				System.out.println( "    normalized voxel scale:" );
				for ( int level = 0; level < numLevels; ++level )
				{
					final MipmapInfo mipmapInfo = imgLoader.getSetupImgLoader( setupId ).getMipmapInfo();
					final double[] res = mipmapInfo.getResolutions()[ level ];
					final double[] voxelScale = new double[ 3 ];
					for ( int d = 0; d < 3; ++d )
						voxelScale[ d ] = scale[ d ] * res[ d ];
					ProposeMipmaps.normalizeVoxelSize( voxelScale );
					System.out.println( "    " + level + ": " + net.imglib2.util.Util.printCoordinates( voxelScale ) );
				}
			}
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
				printSequenceMipmapInfo( "/Users/pietzsch/Desktop/data/BDV130418A325-re-tiled-16/rewrite.xml" );
//				printSequenceMipmapInfo( "/Users/Pietzsch/Desktop/bdv example/drosophila 2.xml" );
//				printSequenceMipmapInfo( "/Users/Pietzsch/Desktop/spimrec2.xml" );
//				printSequenceMipmapInfo( "/Users/pietzsch/workspace/data/111010_weber_full.xml" );

			}
		}
		catch ( final SpimDataException e )
		{
			e.printStackTrace();
		}
	}
}
