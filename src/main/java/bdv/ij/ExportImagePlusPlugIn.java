package bdv.ij;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;

import bdv.ij.export.WriteSequenceToHdf5;
import bdv.ij.export.WriteSequenceToXml;
import bdv.ij.export.imgloader.ImagePlusImgLoader;
import bdv.ij.export.imgloader.ImagePlusImgLoader.MinMaxOption;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressListener;
import bdv.img.hdf5.Hdf5ImageLoader;
import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * ImageJ plugin to export the current image to xml/hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ExportImagePlusPlugIn implements PlugIn
{
	@Override
	public void run( final String arg )
	{
		IJ.log( "starting export..." );

		// get the current image
		final ImagePlus imp = WindowManager.getCurrentImage();

		// make sure there is one
		if ( imp == null )
		{
			IJ.showMessage( "Please open an image first." );
			return;
		}

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

		// show dialog to get output paths, resolutions, subdivisions, min-max option
		final Parameters params = getParameters( imp.getDisplayRangeMin(), imp.getDisplayRangeMax() );
		if ( params == null )
			return;

		// create ImgLoader wrapping the image
		final ImgLoader imgLoader;
		switch ( imp.getType() )
		{
		case ImagePlus.GRAY8:
			imgLoader = ImagePlusImgLoader.createGray8( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
			break;
		case ImagePlus.GRAY16:
			imgLoader = ImagePlusImgLoader.createGray16( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
			break;
		case ImagePlus.GRAY32:
		default:
			imgLoader = ImagePlusImgLoader.createGray32( imp, params.minMaxOption, params.rangeMin, params.rangeMax );
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
		final File seqFile = params.seqFile;
		final File hdf5File = params.hdf5File;
		final int[][] resolutions = params.resolutions;
		final int[][] subdivisions = params.subdivisions;
		final ProgressListener progressListener = new PluginHelper.ProgressListenerIJ( 0, 0.95 );
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >( numSetups );
		for ( int s = 0; s < numSetups; ++s )
			setups.add( new ViewSetup( s, 0, 0, s, w, h, d, pw, ph, pd ) );
		final ArrayList< Integer > timepoints = new ArrayList< Integer >( numTimepoints );
		for ( int t = 0; t < numTimepoints; ++t )
			timepoints.add( t );
		WriteSequenceToHdf5.writeHdf5File( new SequenceDescription( setups, timepoints, null, imgLoader ), resolutions, subdivisions, hdf5File, progressListener );

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
			IJ.showProgress( 1 );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		IJ.log( "done" );
	}

	protected static class Parameters
	{
		final int[][] resolutions;

		final int[][] subdivisions;

		final File seqFile;

		final File hdf5File;

		final MinMaxOption minMaxOption;

		final double rangeMin;

		final double rangeMax;

		public Parameters( final int[][] resolutions, final int[][] subdivisions, final File seqFile, final File hdf5File, final MinMaxOption minMaxOption, final double rangeMin, final double rangeMax )
		{
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
			this.minMaxOption = minMaxOption;
			this.rangeMin = rangeMin;
			this.rangeMax = rangeMax;
		}
	}

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

	static String lastChunkSizes = "{32,32,4}, {16,16,8}, {8,8,8}";

	static int lastMinMaxChoice = 2;

	static double lastMin = 0;

	static double lastMax = 65535;

	static String lastExportPath = "./export.xml";

	protected Parameters getParameters( final double impMin, final double impMax )
	{
		if ( lastMinMaxChoice == 0 ) // use ImageJs...
		{
			lastMin = impMin;
			lastMax = impMax;
		}

		while ( true )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "SpimViewer Import" );

			gd.addStringField( "Subsampling factors", lastSubsampling, 25 );
			gd.addStringField( "Hdf5 chunk sizes", lastChunkSizes, 25 );

			gd.addMessage( "" );
			final String[] minMaxChoices = new String[] { "Use ImageJ's current min/max setting", "Compute min/max of the (hyper-)stack", "Use values specified below" };
			gd.addChoice( "Value range", minMaxChoices, minMaxChoices[ lastMinMaxChoice ] );
			final Choice cMinMaxChoices = (Choice) gd.getChoices().lastElement();
			gd.addNumericField( "Min", lastMin, 0 );
			final TextField tfMin = (TextField) gd.getNumericFields().lastElement();
			gd.addNumericField( "Max", lastMax, 0 );
			final TextField tfMax = (TextField) gd.getNumericFields().lastElement();
			gd.addMessage( "" );
			PluginHelper.addSaveAsFileField( gd, "Export path", lastExportPath, 25 );

//			gd.addMessage( "" );
//			gd.addMessage( "This Plugin is developed by Tobias Pietzsch (pietzsch@mpi-cbg.de)\n" );
//			Bead_Registration.addHyperLinkListener( ( MultiLineLabel ) gd.getMessage(), "mailto:pietzsch@mpi-cbg.de" );

			gd.addDialogListener( new DialogListener()
			{
				@Override
				public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
				{
					if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cMinMaxChoices )
					{
						final boolean enable = cMinMaxChoices.getSelectedIndex() == 2;
						tfMin.setEnabled( enable );
						tfMax.setEnabled( enable );
					}
					return true;
				}
			} );

			final boolean enable = lastMinMaxChoice == 2;
			tfMin.setEnabled( enable );
			tfMax.setEnabled( enable );

			gd.showDialog();
			if ( gd.wasCanceled() )
				return null;

			lastSubsampling = gd.getNextString();
			lastChunkSizes = gd.getNextString();
			lastMinMaxChoice = gd.getNextChoiceIndex();
			lastMin = gd.getNextNumber();
			lastMax = gd.getNextNumber();
			lastExportPath = gd.getNextString();

			// parse mipmap resolutions and cell sizes
			final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );
			if ( resolutions.length == 0 )
			{
				IJ.showMessage( "Cannot parse subsampling factors " + lastSubsampling );
				continue;
			}
			if ( subdivisions.length == 0 )
			{
				IJ.showMessage( "Cannot parse hdf5 chunk sizes " + lastChunkSizes );
				continue;
			}
			else if ( resolutions.length != subdivisions.length )
			{
				IJ.showMessage( "subsampling factors and hdf5 chunk sizes must have the same number of elements" );
				continue;
			}

			final MinMaxOption minMaxOption;
			if ( lastMinMaxChoice == 0 )
				minMaxOption = MinMaxOption.TAKE_FROM_IMAGEPROCESSOR;
			else if ( lastMinMaxChoice == 1 )
				minMaxOption = MinMaxOption.COMPUTE;
			else
				minMaxOption = MinMaxOption.SET;

			String seqFilename = lastExportPath;
			if ( !seqFilename.endsWith( ".xml" ) )
				seqFilename += ".xml";
			final File seqFile = new File( seqFilename );
			final File parent = seqFile.getParentFile();
			if ( parent == null || !parent.exists() || !parent.isDirectory() )
			{
				IJ.showMessage( "Invalid export filename " + seqFilename );
				continue;
			}
			final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
			final File hdf5File = new File( hdf5Filename );

			return new Parameters( resolutions, subdivisions, seqFile, hdf5File, minMaxOption, lastMin, lastMax );
		}
	}
}
