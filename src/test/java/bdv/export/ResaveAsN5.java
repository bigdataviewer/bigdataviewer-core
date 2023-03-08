/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.export;

import bdv.export.n5.WriteSequenceToN5;
import bdv.img.n5.N5ImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;
import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.Lz4Compression;

public class ResaveAsN5
{
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		final String fnInput = "/Users/pietzsch/workspace/Mastodon/mastodon-example-data/tgmm-mini/datasethdf5.xml";
		final String fnOutput = "/Users/pietzsch/workspace/Mastodon/mastodon-example-data/tgmm-mini/datasetn5-gzip.xml";

//		final Compression compression = new Lz4Compression();
		final Compression compression = new GzipCompression( Deflater.BEST_COMPRESSION );

		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
		final SpimDataMinimal spimdata = io.load( fnInput );

		// propose reasonable mipmap settings
		final int maxNumElements = 128 * 64 * 64;
//		final int maxNumElements = 128 * 128 * 64;
		Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo;
		perSetupExportMipmapInfo = new HashMap<>();
		final SequenceDescriptionMinimal seq = spimdata.getSequenceDescription();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final ExportMipmapInfo mipmapInfo = ProposeMipmaps.proposeMipmaps( setup, maxNumElements );
//			final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo(
//					new int[][] { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 1 } },
//					new int[][] { { 128, 128, 64 }, { 128, 128, 64 }, { 128, 128, 64 } } );
			System.out.println( "setup = " + setup );
			System.out.println( "mipmapInfo = " + mipmapInfo );
			perSetupExportMipmapInfo.put( setup.getId(), mipmapInfo );
		}

		// LoopBackHeuristic:
		// - If saving more than 8x on pixel reads use the loopback image over
		//   original image
		final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic = new ExportScalePyramid.LoopbackHeuristic()		{
			@Override
			public boolean decide( final RandomAccessibleInterval< ? > originalImg, final int[] factorsToOriginalImg, final int previousLevel, final int[] factorsToPreviousLevel, final int[] chunkSize )
			{
				if ( previousLevel < 0 )
					return false;
				return Intervals.numElements( factorsToOriginalImg ) / Intervals.numElements( factorsToPreviousLevel ) >= 8;
			}
		};

		final ExportScalePyramid.AfterEachPlane afterEachPlane = null;

		final int numCellCreatorThreads = Runtime.getRuntime().availableProcessors();

		String seqFilename = fnOutput;
		if ( !seqFilename.endsWith( ".xml" ) )
			seqFilename += ".xml";
		final File seqFile = new File( seqFilename );
		final File parent = seqFile.getParentFile();
		if ( parent == null || !parent.exists() || !parent.isDirectory() )
			throw new IllegalArgumentException( "Invalid export filename " + seqFilename );
		final String n5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".n5";
		final File n5File = new File( n5Filename );

		WriteSequenceToN5.writeN5File(
				seq,
				perSetupExportMipmapInfo,
				compression,
				n5File,
				loopbackHeuristic,
				afterEachPlane,
				numCellCreatorThreads,
				new ProgressWriterConsole() );

		seq.setImgLoader( new N5ImageLoader( n5File, seq ) );
		io.save( spimdata, seqFilename );

		System.out.println( "done." );
	}
}
