package creator.tiles;

import ij.IJ;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import viewer.hdf5.Hdf5ImageLoader;
import creator.ProgressListener;
import creator.WriteSequenceToHdf5;
import creator.WriteSequenceToXml;

public class CellVoyagerDataExporter
{

	// private static final String SETTINGS_FILENAME = "MeasurementSetting.xml";

	private static final String CHANNELS_ELEMENT = "Channels";

	private Document document;

	private final File measurementSettingFile;

	private final File imageIndexFile;

	/**
	 * Creates a new exporter that will browse the specified measurement folder
	 * to build the data to export.
	 * 
	 * @param measurementSettingFile
	 *            the CellVoyager measurement file to parse. It must be a XML
	 *            file containing the information on the acquisition to export.
	 *            This file is typically named
	 *            <code>MeasurementSetting.xml</code>
	 * @param imageIndexFile
	 *            the CellVoyager image index file. This XML file contains the
	 *            path to and information on each individual image needed to
	 *            rebuild the whole dataset. The file is generally in the same
	 *            folder that og the measurement setting file and named
	 *            <code>ImageIndex.xml</code>.
	 * 
	 */
	public CellVoyagerDataExporter( final File measurementSettingFile, final File imageIndexFile )
	{
		this.measurementSettingFile = measurementSettingFile;
		this.imageIndexFile = imageIndexFile;

		if ( !measurementSettingFile.exists() ) { throw new IllegalArgumentException( "The target file " + measurementSettingFile + " does not exist." ); }
		if ( !measurementSettingFile.isFile() ) { throw new IllegalArgumentException( "The target file " + measurementSettingFile + " is not a file." ); }

		final SAXBuilder builder = new SAXBuilder();
		try
		{
			document = builder.build( measurementSettingFile );
		}
		catch ( final JDOMException e )
		{
			throw new IllegalArgumentException( "The target file " + measurementSettingFile + " is malformed:\n" + e.getMessage() );
		}
		catch ( final IOException e )
		{
			throw new IllegalArgumentException( "Trouble reading " + measurementSettingFile + ":\n" + e.getMessage() );
		}

		if ( !document.getRootElement().getName().equals( "MeasurementSetting" ) ) { throw new IllegalArgumentException( "The target file " + measurementSettingFile + " is not a CellVoyager Measurement Setting file." ); }
	}

	public List< ChannelInfo > readInfo()
	{
		final List< ChannelInfo > channels = new ArrayList< ChannelInfo >();

		final Element root = document.getRootElement();

		/*
		 * Magnification
		 */

		final double objectiveMagnification = Double.parseDouble( root.getChild( "SelectedObjectiveLens" ).getChildText( "Magnification" ) );
		final double zoomLensMagnification = Double.parseDouble( root.getChild( "ZoomLens" ).getChild( "Magnification" ).getChildText( "Value" ) );
		final double magnification = objectiveMagnification * zoomLensMagnification;

		/*
		 * Channels
		 */

		final Element channelsEl = root.getChild( CHANNELS_ELEMENT );
		final List< Element > channelElements = channelsEl.getChildren();

		for ( final Element channelElement : channelElements )
		{
			final boolean isEnabled = Boolean.parseBoolean( channelElement.getChild( "IsEnabled" ).getText() );
			if ( !isEnabled )
			{
				continue;
			}

			final ChannelInfo ci = new ChannelInfo();
			channels.add( ci );

			ci.isEnabled = true;

			ci.channelNumber = Integer.parseInt( channelElement.getChild( "Number" ).getText() );

			final Element acquisitionSettings = channelElement.getChild( "AcquisitionSetting" );

			final Element cameraEl = acquisitionSettings.getChild( "Camera" );
			ci.tileWidth = Integer.parseInt( cameraEl.getChildText( "EffectiveHorizontalPixels_pixel" ) );
			ci.tileHeight = Integer.parseInt( cameraEl.getChildText( "EffectiveVerticalPixels_pixel" ) );

			ci.unmagnifiedPixelWidth = Double.parseDouble( cameraEl.getChildText( "HorizonalCellSize_um" ) );
			ci.unmagnifiedPixelHeight = Double.parseDouble( cameraEl.getChildText( "VerticalCellSize_um" ) );

			final Element colorElement = channelElement.getChild( "ContrastEnhanceParam" ).getChild( "Color" );
			final int r = Integer.parseInt( colorElement.getChildText( "R" ) );
			final int g = Integer.parseInt( colorElement.getChildText( "G" ) );
			final int b = Integer.parseInt( colorElement.getChildText( "B" ) );
			final int a = Integer.parseInt( colorElement.getChildText( "A" ) );
			ci.channelColor = new Color( r, g, b, a );

			ci.bitDepth = channelElement.getChild( "ContrastEnhanceParam" ).getChildText( "BitDepth" );
			ci.pixelWidth = ci.unmagnifiedPixelWidth / magnification;
			ci.pixelHeight = ci.unmagnifiedPixelWidth / magnification;

		}

		/*
		 * Fields, for each channel
		 */

		for ( final ChannelInfo channelInfo : channels )
		{

			final List< Element > fieldElements = root.getChild( "Wells" ).getChild( "Well" ).getChild( "Areas" ).getChild( "Area" ).getChild( "Fields" ).getChildren( "Field" );

			// Read field position in um
			double xmin = Double.POSITIVE_INFINITY;
			double ymin = Double.POSITIVE_INFINITY;
			double xmax = Double.NEGATIVE_INFINITY;
			double ymax = Double.NEGATIVE_INFINITY;
			final ArrayList< double[] > offsetsUm = new ArrayList< double[] >();
			for ( final Element fieldElement : fieldElements )
			{

				final double xum = Double.parseDouble( fieldElement.getChildText( "StageX_um" ) );
				if ( xum < xmin )
				{
					xmin = xum;
				}
				if ( xum > xmax )
				{
					xmax = xum;
				}

				/*
				 * Careful! For the fields to be padded correctly, we need to
				 * invert their Y position, so that it matches the pixel
				 * orientation.
				 */
				final double yum = -Double.parseDouble( fieldElement.getChildText( "StageY_um" ) );
				if ( yum < ymin )
				{
					ymin = yum;
				}
				if ( yum > ymax )
				{
					ymax = yum;
				}

				offsetsUm.add( new double[] { xum, yum } );
			}

			// Convert in pixel position
			final List< long[] > offsets = new ArrayList< long[] >();
			for ( final double[] offsetUm : offsetsUm )
			{
				final long x = ( long ) ( ( offsetUm[ 0 ] - xmin ) / ( channelInfo.unmagnifiedPixelWidth / magnification ) );
				final long y = ( long ) ( ( offsetUm[ 1 ] - ymin ) / ( channelInfo.unmagnifiedPixelHeight / magnification ) );

				offsets.add( new long[] { x, y } );
			}

			channelInfo.offsets = offsets;

			final int width = 1 + ( int ) ( ( xmax - xmin ) / ( channelInfo.unmagnifiedPixelWidth / magnification ) );
			final int height = 1 + ( int ) ( ( ymax - ymin ) / ( channelInfo.unmagnifiedPixelWidth / magnification ) );
			channelInfo.width = width + channelInfo.tileWidth;
			channelInfo.height = height + channelInfo.tileHeight;

		}

		/*
		 * Z range
		 */

		final int nZSlices = Integer.parseInt( root.getChild( "ZRange" ).getChildText( "NumberOfSlices" ) );
		final double zStroke = Double.parseDouble( root.getChild( "ZRange" ).getChildText( "Stroke_um" ) );
		final double pixelDepth = zStroke / ( nZSlices - 1 );

		for ( final ChannelInfo channelInfo : channels )
		{
			channelInfo.nZSlices = nZSlices;
			channelInfo.pixelDepth = pixelDepth;
			channelInfo.spaceUnits = "µm";
		}

		return channels;
	}

	public List< Integer > readTimePoints()
	{
		final Element root = document.getRootElement();
		final int nTimePoints = Integer.parseInt( root.getChild( "TimelapsCondition" ).getChildText( "Iteration" ) );

		final List< Integer > timepoints = new ArrayList< Integer >( nTimePoints );
		for ( int i = 0; i < nTimePoints; i++ )
		{
			timepoints.add( Integer.valueOf( i ) );
		}
		return timepoints;
	}

	public double readFrameInterval()
	{
		final Element root = document.getRootElement();
		final double dt = Double.parseDouble( root.getChild( "TimelapsCondition" ).getChildText( "Interval" ) );
		return dt;
	}

	/**
	 * Export the target dataset to a xml/hd5 file couple.
	 * 
	 * @param seqFile
	 *            the path to the target XML file to write.
	 * @param hdf5File
	 *            the path to the target HDF5 file to write.
	 * @param resolutions
	 *            the resolution definition for each level.
	 * @param chunks
	 *            the chunck size definition for each level.
	 * @param progressListener
	 *            a {@link ProgressListener} that will advance from 0 to 1 while
	 *            this method executes.
	 */
	public void export( final File seqFile, final File hdf5File, final int[][] resolutions, final int[][] chunks, final ProgressListener progressListener )
	{

		progressListener.updateProgress( 0d );

		final List< ChannelInfo > channelInfos = readInfo();
		/*
		 * Create view setups
		 */

		final List< ViewSetup > setups = new ArrayList< ViewSetup >( channelInfos.size() );
		int viewSetupIndex = 0;
		for ( final ChannelInfo channelInfo : channelInfos )
		{
			final ViewSetup viewSetup = new ViewSetup( viewSetupIndex++, 0, 0, channelInfo.channelNumber, channelInfo.width, channelInfo.height, channelInfo.nZSlices, channelInfo.pixelWidth, channelInfo.pixelHeight, channelInfo.pixelDepth );
			setups.add( viewSetup );
		}

		/*
		 * Instantiate the tile loader
		 */

		final TileImgLoader imgLoader = new TileImgLoader( imageIndexFile, channelInfos );

		/*
		 * Time points
		 */

		final List< Integer > timePoints = readTimePoints();

		/*
		 * Sequence description
		 */

		final SequenceDescription sequenceDescriptionHDF5 = new SequenceDescription( setups, timePoints, measurementSettingFile.getParentFile(), imgLoader );

		/*
		 * Write to HDF5
		 */

		WriteSequenceToHdf5.writeHdf5File( sequenceDescriptionHDF5, resolutions, chunks, hdf5File, progressListener );

		/*
		 * write XML sequence description
		 */

		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, null );
		final SequenceDescription sequenceDescriptionXML = new SequenceDescription( setups, timePoints, seqFile.getParentFile(), hdf5Loader );

		/*
		 * Build views
		 */

		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();

		for ( int setupIndex = 0; setupIndex < setups.size(); setupIndex++ )
		{
			final ViewSetup viewSetup = setups.get( setupIndex );

			// A single transform for all the time points of a view
			final double pw = viewSetup.getPixelWidth();
			final double ph = viewSetup.getPixelHeight();
			final double pd = viewSetup.getPixelDepth();
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			sourceTransform.set( pw, 0, 0, 0, 0, ph, 0, 0, 0, 0, pd, 0 );

			for ( int timepointIndex = 0; timepointIndex < timePoints.size(); timepointIndex++ )
			{
				final View view = new View( sequenceDescriptionXML, timepointIndex, setupIndex, sourceTransform );
				registrations.add( view );
			}
		}

		final ViewRegistrations viewRegistrations = new ViewRegistrations( registrations, 0 );
		try
		{
			WriteSequenceToXml.writeSequenceToXml( sequenceDescriptionXML, viewRegistrations, seqFile.getAbsolutePath() );
			IJ.showProgress( 1 );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		progressListener.updateProgress( 1d );

	}

	public static final class ChannelInfo
	{

		public int height;

		public int width;

		public int nZSlices;

		public String spaceUnits;

		public double pixelHeight;

		public double pixelWidth;

		public double pixelDepth;

		public List< long[] > offsets;

		public boolean isEnabled;

		public String bitDepth;

		public Color channelColor;

		public double unmagnifiedPixelHeight;

		public double unmagnifiedPixelWidth;

		public int tileHeight;

		public int tileWidth;

		public int channelNumber;

		@Override
		public String toString()
		{
			final StringBuffer str = new StringBuffer();
			str.append( "Channel " + channelNumber + ": \n" );
			str.append( " - isEnabled: " + isEnabled + "\n" );
			str.append( " - width: " + width + "\n" );
			str.append( " - height: " + height + "\n" );
			str.append( " - tile width: " + tileWidth + "\n" );
			str.append( " - tile height: " + tileHeight + "\n" );
			str.append( " - NZSlices: " + nZSlices + "\n" );
			str.append( " - unmagnifiedPixelWidth: " + unmagnifiedPixelWidth + "\n" );
			str.append( " - unmagnifiedPixelHeight: " + unmagnifiedPixelHeight + "\n" );
			str.append( " - color: " + channelColor + "\n" );
			str.append( " - bitDepth: " + bitDepth + "\n" );
			str.append( " - has " + offsets.size() + " fields:\n" );
			int index = 1;
			for ( final long[] offset : offsets )
			{
				str.append( "    " + index++ + ": x = " + offset[ 0 ] + ", y = " + offset[ 1 ] + "\n" );
			}
			str.append( " - spatial calibration:\n" );
			str.append( "    dx = " + pixelWidth + " " + spaceUnits + "\n" );
			str.append( "    dy = " + pixelHeight + " " + spaceUnits + "\n" );
			str.append( "    dz = " + pixelDepth + " " + spaceUnits + "\n" );
			return str.toString();
		}

	}
}
