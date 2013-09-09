package creator.tiles;

import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class ImportCellVoyagerData
{

	public static void main( final String[] args )
	{

		final File root_folder = new File( "/Users/tinevez/Desktop/1_7_6_1_2/20130703T145244" );

		final int nFields = 6; // Hardcoded
		final int nChannels = 3; // Hardcoded -> MeasurementSetting.xml,
									// IsEnabled
		final int nTimePoints = 1; // Hardcoded, fomr <TimelapsCondition>
		final int nViewSetups = nFields * nChannels;
		final int width = 920; // Hardcoded -> MeasurementSetting.xml
		final int height = 920; // Hardcoded -> MeasurementSetting.xml
		final int depth = 41;// Hardcoded -> MeasurementSetting.xml, ZRange
		final double pixelWidth = 0.1975; // -> to deduce from
											// MeasurementSetting.xml
		final double pixelHeight = 0.1975; // -> to deduce from
											// MeasurementSetting.xml
		final double pixelDepth = 1.0; // -> to deduce from
										// MeasurementSetting.xml <ZRange>;

		final List< ViewSetup > setups = new ArrayList< ViewSetup >();
		int viewSetupIndex = 0;
		for ( int field = 0; field < nFields; field++ )
		{
			for ( int channel = 0; channel < nChannels; channel++ )
			{
				final ViewSetup viewSetup = new ViewSetup( viewSetupIndex++, 0, 0, channel, width, height, depth, pixelWidth, pixelHeight, pixelDepth );
				setups.add( viewSetup );
			}
		}

		final List< Integer > timepoints = new ArrayList< Integer >( nTimePoints );
		for ( int tp = 0; tp < nTimePoints; tp++ )
		{
			timepoints.add( Integer.valueOf( tp ) );
		}

		final TileImgLoader imgLoader = new TileImgLoader( root_folder, nChannels );
		final SequenceDescription sequenceDescription = new SequenceDescription( setups, timepoints, root_folder, imgLoader );

		/*
		 * Build a specific view
		 */

		final AffineTransform3D model = null; // for now
		final int timepointIndex = 0;
		final int setupIndex = 5;
		final View view = new View( sequenceDescription, timepointIndex, setupIndex, model );

		/*
		 * Run
		 */

		final long start = System.currentTimeMillis();

		final RandomAccessibleInterval< UnsignedShortType > img = imgLoader.getUnsignedShortImage( view );

		final long end = System.currentTimeMillis();
		System.out.println( "Processed in " + ( end - start ) + " ms." );

		ImageJ.main( args );
		ImageJFunctions.show( img );

	}
}
