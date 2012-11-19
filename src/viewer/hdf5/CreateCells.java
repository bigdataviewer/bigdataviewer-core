package viewer.hdf5;

import static viewer.hdf5.Reorder.reorder;

import java.io.File;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import viewer.SequenceViewsLoader;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5FloatStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

public class CreateCells
{
	public static void main( final String[] args )
	{
		final String viewRegistrationsFilename = "/home/tobias/workspace/data/fast fly/111010_weber/e012-reg.xml";
		final File hdf5CellsFile = new File( "/home/tobias/Desktop/cells.h5" );
		try
		{
			final SequenceViewsLoader loader = new SequenceViewsLoader( viewRegistrationsFilename );
			final SequenceDescription seq = loader.getSequenceDescription();

			final int timepoint = 0;
			final int setup = 0;
			final View view = loader.getView( timepoint, setup );
			final ImgPlus< FloatType > img = seq.imgLoader.getImage( view );

			// open HDF5 output file
			if ( hdf5CellsFile.exists() )
				hdf5CellsFile.delete();
			final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5CellsFile );

			// write img data to the hdf5 file
			final int[] cellDimensions =  new int[] { 50, 50, 5 };

			final int n = img.numDimensions();
			final long[] dimensions = new long[ n ];
			img.dimensions( dimensions );

			final long[] numCells = new long[ n ];
			final int[] borderSize = new int[ n ];
			for ( int d = 0; d < n; ++d ) {
				numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
				borderSize[ d ] = ( int )( dimensions[ d ] - (numCells[ d ] - 1) * cellDimensions[ d ] );
			}

			hdf5Writer.createFloatMDArray( "cells", reorder( dimensions ), reorder( cellDimensions ), HDF5FloatStorageFeatures.FLOAT_DEFLATE );

			final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( numCells );
			final long[] currentCellMin = new long[ n ];
			final long[] currentCellMax = new long[ n ];
			final long[] currentCellDim = new long[ n ];
			final long[] currentCellPos = new long[ n ];
			final long[] currentCellMinRM = new long[ n ];
			final long[] currentCellDimRM = new long[ n ];
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

				final ArrayImg< FloatType, ? > cell = ArrayImgs.floats( currentCellDim );
				final Cursor< FloatType > c = Views.flatIterable( Views.interval( img, new FinalInterval( currentCellMin, currentCellMax ) ) ).cursor();
				for ( final FloatType t : cell )
					t.set( c.next() );

				final MDFloatArray array = new MDFloatArray( ( ( FloatArray ) cell.update( null ) ).getCurrentStorageArray(), currentCellDimRM );
				hdf5Writer.writeFloatMDArrayBlockWithOffset( "cells", array, currentCellMinRM );
			}

			hdf5Writer.close();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
