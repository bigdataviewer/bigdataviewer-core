package viewer.hdf5;

import static viewer.hdf5.Reorder.reorder;

import java.io.File;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.iterator.LocalizingZeroMinIntervalIterator;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import viewer.SequenceViewsLoader;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

public class CreateCells
{
	final static private String groupFormatString = "t%05d/s%02d/%d";
	final static private String cellsFormatString = "%s/cells";

	public static String getGroupPath( final View view, final int level )
	{
		return String.format( groupFormatString, view.getTimepointIndex(), view.getSetupIndex(), level );
	}

	public static String getCellsPath( final View view, final int level )
	{
		return String.format( cellsFormatString, getGroupPath( view, level ) );
	}

	public static void main( final String[] args )
	{
		final String viewRegistrationsFilename = "/home/tobias/workspace/data/fast fly/111010_weber/e012-reg.xml";
		final File hdf5CellsFile = new File( "/home/tobias/Desktop/e012-cells.h5" );
		try
		{
			final SequenceViewsLoader loader = new SequenceViewsLoader( viewRegistrationsFilename );
			final SequenceDescription seq = loader.getSequenceDescription();
			final int numTimepoints = 100; // seq.numTimepoints();
			final int numSetups = seq.numViewSetups();

			// open HDF5 output file
			if ( hdf5CellsFile.exists() )
				hdf5CellsFile.delete();
			final IHDF5Writer hdf5Writer = HDF5Factory.open( hdf5CellsFile );

			// write image data for all views to the HDF5 file
			final int n = 3;
			final long[] dimensions = new long[ n ];
			for ( int timepoint = 0; timepoint < numTimepoints; ++timepoint )
			{
				System.out.println( String.format( "proccessing timepoint %d / %d", timepoint, numTimepoints ) );
				for ( int setup = 0; setup < numSetups; ++setup )
				{
					final View view = loader.getView( timepoint, setup );
					final ImgPlus< UnsignedShortType > img = seq.imgLoader.getUnsignedShortImage( view );

					for ( int level = 0; level < MipMapDefinition.numLevels; ++level )
					{
						img.dimensions( dimensions );
						final RandomAccessible< UnsignedShortType > source;
						if ( level == 0 )
							source = img;
						else
						{
							final int[] factor = MipMapDefinition.resolutions[ level ];
							for ( int d = 0; d < n; ++d )
								dimensions[ d ] /= factor[ d ];
							final Img< UnsignedShortType > downsampled = ArrayImgs.unsignedShorts( dimensions );
							Downsample.downsample( img, downsampled, factor );
							source = downsampled;
						}

						final int[] cellDimensions = MipMapDefinition.subdivisions[ level ];
						hdf5Writer.createGroup( getGroupPath( view, level ) );
						final String path = getCellsPath( view, level );
						hdf5Writer.createShortMDArray( path, reorder( dimensions ), reorder( cellDimensions ), HDF5IntStorageFeatures.INT_AUTO_SCALING_UNSIGNED );

						final long[] numCells = new long[ n ];
						final int[] borderSize = new int[ n ];
						for ( int d = 0; d < n; ++d )
						{
							numCells[ d ] = ( dimensions[ d ] - 1 ) / cellDimensions[ d ] + 1;
							borderSize[ d ] = ( int ) ( dimensions[ d ] - ( numCells[ d ] - 1 ) * cellDimensions[ d ] );
						}

						final LocalizingZeroMinIntervalIterator i = new LocalizingZeroMinIntervalIterator( numCells );
						final long[] currentCellMin = new long[ n ];
						final long[] currentCellMax = new long[ n ];
						final long[] currentCellDim = new long[ n ];
						final long[] currentCellPos = new long[ n ];
						final long[] currentCellMinRM = new long[ n ];
						final long[] currentCellDimRM = new long[ n ];
						while ( i.hasNext() )
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

							final ArrayImg< UnsignedShortType, ? > cell = ArrayImgs.unsignedShorts( currentCellDim );
							final Cursor< UnsignedShortType > c = Views.flatIterable( Views.interval( source, new FinalInterval( currentCellMin, currentCellMax ) ) ).cursor();
							for ( final UnsignedShortType t : cell )
								t.set( c.next() );

							final MDShortArray array = new MDShortArray( ( ( ShortArray ) cell.update( null ) ).getCurrentStorageArray(), currentCellDimRM );
							hdf5Writer.writeShortMDArrayBlockWithOffset( path, array, currentCellMinRM );
						}
					}
				}
			}
			hdf5Writer.close();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
