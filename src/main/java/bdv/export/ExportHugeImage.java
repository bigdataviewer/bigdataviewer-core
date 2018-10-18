package bdv.export;

import bdv.export.WriteSequenceToHdf5.AfterEachPlane;
import bdv.export.WriteSequenceToHdf5.LoopbackHeuristic;
import bdv.spimdata.SequenceDescriptionMinimal;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import mpicbg.spim.data.generic.sequence.BasicSetupImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.TypedBasicImgLoader;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ExportHugeImage
{
	private final File hdf5File;

	private final RandomAccessibleInterval< UnsignedShortType > input;

	public ExportHugeImage( final File hdf5File )
	{
		input = Views.interval(
				Views.extendZero( ArrayImgs.unsignedShorts( 1, 1, 1 ) ),
				Intervals.createMinSize( 0, 0, 0, 10000, 10000, 10000 ) );
		this.hdf5File = hdf5File;
	}

	public static class RAIImgLoader< T > implements TypedBasicImgLoader< T >
	{
		private final BasicSetupImgLoader< T > setupImgLoader;

		public RAIImgLoader( final RandomAccessibleInterval< T > rai )
		{
			this( rai, Util.getTypeFromInterval( rai ) );
		}

		public RAIImgLoader( final RandomAccessibleInterval< T > rai, final T type )
		{
			setupImgLoader = new BasicSetupImgLoader< T >()
			{
				@Override
				public RandomAccessibleInterval< T > getImage( final int timepointId, final ImgLoaderHint... hints )
				{
					return rai;
				}

				@Override
				public T getImageType()
				{
					return type;
				}
			};
		}

		@Override
		public BasicSetupImgLoader< T > getSetupImgLoader( final int setupId )
		{
			return setupImgLoader;
		}
	}

	private static VoxelDimensions defaultVoxelDimensions( int numDimensions )
	{
		double[] dimensions = new double[ numDimensions ];
		Arrays.fill( dimensions, 1.0 );
		return new FinalVoxelDimensions( "px", dimensions );
	}

	public void run()
	{
		final String name = "img";
		final RandomAccessibleInterval< UnsignedShortType > rai = input;

		final VoxelDimensions voxelSize = defaultVoxelDimensions( rai.numDimensions() );
		final BasicViewSetup setup = new BasicViewSetup( 0, name, rai, voxelSize );
		final Map< Integer, BasicViewSetup > setups = Collections.singletonMap( setup.getId(), setup );
		final TimePoints timepoints = new TimePoints( Collections.singletonList( new TimePoint( 0 ) ) );
		final RAIImgLoader< UnsignedShortType > imgLoader = new RAIImgLoader<>( rai );
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( timepoints, setups, imgLoader, null );

		final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = Collections.singletonMap( setup.getId(), ProposeMipmaps.proposeMipmaps( setup ) );
		final boolean deflate = false;
		final int numCellCreatorThreads = 10;
		final ProgressWriterConsole progressWriter = new ProgressWriterConsole();
		final LoopbackHeuristic loopbackHeuristic = null;
		final AfterEachPlane afterEachPlane = null;

		WriteSequenceToHdf5.writeHdf5File( seq, perSetupExportMipmapInfo, deflate, hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, progressWriter );
	}

	public static void main( String[] args )
	{
		File file = null;
		if ( args.length > 0 )
			file = new File( args[ 0 ] );
		else
			file = new File( "/Users/pietzsch/Desktop/huge.h5" );
		new ExportHugeImage( file ).run();
	}
}
