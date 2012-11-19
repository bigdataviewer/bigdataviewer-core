package viewer.hdf5;

import static viewer.hdf5.Reorder.reorder;
import ij.ImageJ;

import java.io.File;

import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class LoadCells
{
	public static void main( final String[] args )
	{
		final File hdf5CellsFile = new File( "/home/tobias/Desktop/cells.h5" );
		try
		{
			// open HDF5 output file
			if ( ! hdf5CellsFile.exists() )
				throw new RuntimeException( "cells file not found" );
			final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5CellsFile );
			final HDF5DataSetInformation info = hdf5Reader.getDataSetInformation( "cells" );
			final long[] dimensions = reorder( info.getDimensions() );
			final int[] cellDimensions = reorder( info.tryGetChunkSizes() );

			final int n = dimensions.length;
			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			for ( int d = 0; d < n; ++d ) {
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				borderSize[ d ] = ( int )( dimensions[ d ] - (numCells[ d ] - 1) * cellDimensions[ d ] );
			}

			final Img< FloatType > img = ArrayImgs.floats( dimensions );

			final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( numCells );
			final long[] currentCellMin = new long[ n ];
			final long[] currentCellMax = new long[ n ];
			final int[]  currentCellDim = new int[ n ];
			final long[] currentCellPos = new long[ n ];
			final long[] currentCellMinRM = new long[ n ];
			final int[]  currentCellDimRM = new int[ n ];
			while( i.hasNext() )
			{
				i.fwd();
				i.localize( currentCellPos );
				for ( int d = 0; d < n; ++d )
				{
					currentCellMin[ d ] = currentCellPos[ d ] * cellDimensions[ d ];
					currentCellDim[ d ] = ( currentCellPos[ d ] + 1 == numCells[ d ] ) ? borderSize[ d ] : cellDimensions[ d ];
					currentCellMax[ d ] = currentCellMin[ d ] + currentCellDim[ d ] - 1;
				}
				reorder( currentCellMin, currentCellMinRM );
				reorder( currentCellDim, currentCellDimRM );

				final MDFloatArray array = hdf5Reader.readFloatMDArrayBlockWithOffset( "cells", currentCellDimRM, currentCellMinRM );
				final float[] data = array.getAsFlatArray();
				int j = 0;
				for ( final FloatType t : Views.flatIterable( Views.interval( img, new FinalInterval( currentCellMin, currentCellMax ) ) ) )
					t.set( data[ j++ ] );
			}

			hdf5Reader.close();

			new ImageJ();
			ImageJFunctions.show( img );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
