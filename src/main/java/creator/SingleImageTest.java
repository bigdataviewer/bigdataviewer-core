package creator;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.ArrayList;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.hdf5.Hdf5ImageLoader;
import creator.CreateCells.MipMapDefinition;
import creator.ImagePlusImgLoader.MinMaxOption;
import creator.spim.WriteSequenceToXml;

public class SingleImageTest implements PlugIn
{

	@Override
	public void run( final String arg )
	{
		IJ.log( "Starting SpimViewer import." );

		// get the current image
		final ImagePlus imp = WindowManager.getCurrentImage();

		// make sure there is one
		if ( imp == null )
		{
			IJ.showMessage( "Please open an image first." );
			return;
		}

		// create ImgLoader wrapping the image
		final ImgLoader imgLoader;
		switch( imp.getType() )
		{
			case ImagePlus.GRAY8 :
				imgLoader = ImagePlusImgLoader.createGray8( imp, MinMaxOption.TAKE_FROM_IMAGEPROCESSOR, 0, 0 );
				break;
			case ImagePlus.GRAY16 :
				imgLoader = ImagePlusImgLoader.createGray16( imp, MinMaxOption.TAKE_FROM_IMAGEPROCESSOR, 0, 0 );
				break;
			case ImagePlus.GRAY32 :
				imgLoader = ImagePlusImgLoader.createGray32( imp, MinMaxOption.TAKE_FROM_IMAGEPROCESSOR, 0, 0 );
				break;
			default :
				throw new RuntimeException("Only 8, 16, 32-bit images supported!");
		}

		final double pw = imp.getCalibration().pixelWidth;
		final double ph = imp.getCalibration().pixelHeight;
		final double pd = imp.getCalibration().pixelDepth;
		final int w = imp.getWidth();
		final int h = imp.getHeight();
		final int d = imp.getNSlices();

		// create SourceTransform from the images calibration
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(
				pw, 0, 0, 0,
				0, ph, 0, 0,
				0, 0, pd, 0 );

		// write hdf5
		final File seqFile = new File( "/Users/tobias/Desktop/l1-reconstructed.xml" );
		final File hdf5File = new File( "/Users/tobias/Desktop/l1-reconstructed.h5" );
		final int[][] resolutions = MipMapDefinition.resolutions;
		final int[][] subdivisions = MipMapDefinition.subdivisions;
		CreateCells.createHdf5File( 1, 1, imgLoader, resolutions, subdivisions, hdf5File, null );

		// write xml sequence description
		final ViewSetup[] setups = new ViewSetup[] { new ViewSetup( 0, 0, 0, 0, w, h, d, pw, ph, pd ) };
		final int[] timepoints = new int[] { 0 };
		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File );
		final SequenceDescription sequenceDescription = new SequenceDescription( setups, timepoints, seqFile.getParentFile(), hdf5Loader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		registrations.add( new ViewRegistration( 0, 0, sourceTransform ) );
		final ViewRegistrations viewRegistrations = new ViewRegistrations( registrations, 0 );
		try
		{
			WriteSequenceToXml.writeSequenceToXml( sequenceDescription, viewRegistrations, seqFile.getAbsolutePath() );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		// TODO: progress listener

		// TODO: create dialog to query output paths, resolutions, subdivisions, min-max option, name

		IJ.log( "SpimViewer import done." );
	}

}
