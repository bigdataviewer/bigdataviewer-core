package test;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.export.WriteSequenceToXml;
import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.export.imgloader.ImagePlusImgLoader.MinMaxOption;
import bdv.img.hdf5.Hdf5ImageLoader;

public class ExportTest
{
	public static void main( final String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/flybrain-32bit.tif";
		final ImagePlus imp = new ImagePlus( fn );

		// check the image type
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
		case ImagePlus.GRAY16:
		case ImagePlus.GRAY32:
			break;
		default:
			IJ.showMessage( "Only 8, 16, 32-bit images are supported currently!" );
			return;
		}

		// check the image dimensionality
		if ( imp.getNDimensions() < 3 )
		{
			IJ.showMessage( "Image must be at least 3-dimensional!" );
			return;
		}

		final MinMaxOption minMaxOption = MinMaxOption.SET;
		final double rangeMin = 0;
		final double rangeMax = 65535;

		// create ImgLoader wrapping the image
		final ImgLoader< UnsignedShortType > imgLoader;
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
			imgLoader = ImagePlusImgLoader.createGray8( imp, minMaxOption, rangeMin, rangeMax );
			break;
		case ImagePlus.GRAY16:
			imgLoader = ImagePlusImgLoader.createGray16( imp, minMaxOption, rangeMin, rangeMax );
			break;
		case ImagePlus.GRAY32:
		default:
			imgLoader = ImagePlusImgLoader.createGray32( imp, minMaxOption, rangeMin, rangeMax );
			break;
		}

		final double pw = imp.getCalibration().pixelWidth;
		final double ph = imp.getCalibration().pixelHeight;
		final double pd = imp.getCalibration().pixelDepth;
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();
		final int numTimepoints = imp.getNFrames();
		final int numSetups = imp.getNChannels();

		// create SourceTransform from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );

		// write hdf5
		final File seqFile = new File( "/Users/Pietzsch/Desktop/export.xml" );
		final File hdf5File = new File( "/Users/Pietzsch/Desktop/export.h5" );
		final int[][] resolutions = new int[][] { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		final int[][] subdivisions = new int[][] { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };
		final ProgressWriter progressWriter = new ProgressWriterConsole();
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
			setups.add( new ViewSetup( s, 0, 0, s, w, h, d, pw, ph, pd ) );
		final ArrayList< Integer > timepoints = new ArrayList< Integer >( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( t );
		WriteSequenceToHdf5.writeHdf5File( new SequenceDescription( setups, timepoints, null, imgLoader ), resolutions, subdivisions, hdf5File, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );

		// write xml sequence description
		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, null );
		final SequenceDescription sequenceDescription = new SequenceDescription( setups, timepoints, seqFile.getParentFile(), hdf5Loader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( t, s, sourceTransform ) );
		final ViewRegistrations viewRegistrations = new ViewRegistrations( registrations, 0 );
		try
		{
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, viewRegistrations, seqFile.getAbsolutePath() );
			progressWriter.setProgress( 1.0 );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		progressWriter.out().println( "done" );
	}
}
