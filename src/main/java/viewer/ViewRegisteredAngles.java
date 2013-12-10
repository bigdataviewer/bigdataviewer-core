package viewer;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.integer.VolatileUnsignedShortType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import viewer.crop.CropDialog;
import viewer.gui.brightness.BrightnessDialog;
import viewer.gui.brightness.ConverterSetup;
import viewer.gui.brightness.MinMaxGroup;
import viewer.gui.brightness.RealARGBColorConverterSetup;
import viewer.gui.brightness.SetupAssignments;
import viewer.gui.transformation.ManualTransformation;
import viewer.gui.transformation.ManualTransformationEditor;
import viewer.gui.transformation.TransformedVolatileSource;
import viewer.gui.visibility.ActiveSourcesDialog;
import viewer.hdf5.Hdf5ImageLoader;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import viewer.render.SourceState;
import viewer.render.ViewerState;
import viewer.render.VolatileSource;
import viewer.util.Affine3DHelpers;

public class ViewRegisteredAngles
{
	final KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 );
	final KeyStroke activeSourcesKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F6, 0 );
	final KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );
	final KeyStroke helpKeystroke2 = KeyStroke.getKeyStroke( KeyEvent.VK_H, 0 );
	final KeyStroke cropKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 );
	final KeyStroke manualTransformKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_T, 0 );
	final KeyStroke saveKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F11, 0 );
	final KeyStroke loadKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F12, 0 );

	final ViewerFrame viewerFrame;

	final ViewerPanel viewer;

	final SetupAssignments setupAssignments;

	final ManualTransformation manualTransformation;

	final BrightnessDialog brightnessDialog;

	final CropDialog cropDialog;

	final ActiveSourcesDialog activeSourcesDialog;

	final ManualTransformationEditor manualTransformationEditor;

	final JFileChooser fileChooser;

	File proposedSettingsFile;

	public void toggleBrightnessDialog()
	{
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}

	public void toggleActiveSourcesDialog()
	{
		activeSourcesDialog.setVisible( ! activeSourcesDialog.isVisible() );
	}

	public void showHelp()
	{
		new HelpFrame();
	}

	public void crop()
	{
		cropDialog.setVisible( ! cropDialog.isVisible() );
	}

	public void toggleManualTransformation()
	{
		manualTransformationEditor.toggle();
	}

	private ViewRegisteredAngles( final String xmlFilename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException, IOException
	{
		final int width = 800;
		final int height = 600;

		final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
		final SequenceDescription seq = loader.getSequenceDescription();

		fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "xml files";
			}

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
			        final String s = f.getName();
			        final int i = s.lastIndexOf('.');
			        if (i > 0 &&  i < s.length() - 1) {
			            final String ext = s.substring(i+1).toLowerCase();
			            return ext.equals( "xml" );
			        }
				}
				return false;
			}
		} );

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
		{
			final RealARGBColorConverter< VolatileUnsignedShortType > converter = new RealARGBColorConverter< VolatileUnsignedShortType >( 0, 65535 );
			converter.setColor( new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) ) );
			final VolatileSpimSource spimSource = new VolatileSpimSource( loader, setup, "angle " + seq.setups.get( setup ).getAngle() );
			// Decorate each source with an extra transformation, that can be edited manually in this viewer.
			final TransformedVolatileSource< UnsignedShortType, VolatileUnsignedShortType > transformedSource = new TransformedVolatileSource< UnsignedShortType, VolatileUnsignedShortType >( spimSource );
			sources.add( new SourceAndConverter< VolatileUnsignedShortType >( transformedSource, converter ) );
			converterSetups.add( new RealARGBColorConverterSetup< VolatileUnsignedShortType >( setup, converter ) );
		}

		viewerFrame = new ViewerFrame( width, height, sources, seq.numTimepoints(), ( ( Hdf5ImageLoader ) seq.imgLoader ).getCache() );
		viewer = viewerFrame.getViewer();
		manualTransformation = new ManualTransformation( viewer );
		manualTransformationEditor = new ManualTransformationEditor( viewer, viewerFrame.getKeybindings() );

		for ( final ConverterSetup cs : converterSetups )
			if ( RealARGBColorConverterSetup.class.isInstance( cs ) )
				( ( RealARGBColorConverterSetup< ? > ) cs ).setViewer( viewer );

		final ActionMap actionMap = SpimViewerActions.createActionMap( this );
		viewerFrame.getKeybindings().addActionMap( "dialogs", actionMap );

		final InputMap inputMap = new InputMap();
		inputMap.put( brightnessKeystroke, SpimViewerActions.BRIGHTNESS_SETTINGS );
		inputMap.put( activeSourcesKeystroke, SpimViewerActions.VISIBILITY_AND_GROUPING );
		inputMap.put( helpKeystroke, SpimViewerActions.SHOW_HELP );
		inputMap.put( helpKeystroke2, SpimViewerActions.SHOW_HELP );
		inputMap.put( cropKeystroke, SpimViewerActions.CROP );
		inputMap.put( manualTransformKeystroke, SpimViewerActions.MANUAL_TRANSFORM );
		inputMap.put( saveKeystroke, SpimViewerActions.SAVE_SETTINGS );
		inputMap.put( loadKeystroke, SpimViewerActions.LOAD_SETTINGS );
		viewerFrame.getKeybindings().addInputMap( "dialogs", inputMap );

		setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
		final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
		for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
			setupAssignments.moveSetupToGroup( setup, group );
		brightnessDialog = new BrightnessDialog( viewerFrame, setupAssignments );

		cropDialog = new CropDialog( viewerFrame, viewer, seq );

		activeSourcesDialog = new ActiveSourcesDialog( viewerFrame, viewer.getVisibilityAndGrouping() );

		initTransform( width, height );

		final JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu( "File" );
		menubar.add( menu );

		final JMenuItem miLoadSettings = new JMenuItem( actionMap.get( SpimViewerActions.LOAD_SETTINGS ) );
		miLoadSettings.setText( "Load settings" );
		menu.add( miLoadSettings );

		final JMenuItem miSaveSettings = new JMenuItem( actionMap.get( SpimViewerActions.SAVE_SETTINGS ) );
		miSaveSettings.setText( "Save settings" );
		menu.add( miSaveSettings );

		menu = new JMenu( "Settings" );
		menubar.add( menu );

		final JMenuItem miBrightness = new JMenuItem( actionMap.get( SpimViewerActions.BRIGHTNESS_SETTINGS ) );
		miBrightness.setText( "Brightness & Color" );
		menu.add( miBrightness );

		final JMenuItem miVisibility = new JMenuItem( actionMap.get( SpimViewerActions.VISIBILITY_AND_GROUPING ) );
		miVisibility.setText( "Visibility & Grouping" );
		menu.add( miVisibility );

		menu = new JMenu( "Settings" );
		menubar.add( menu );

		final JMenuItem miCrop = new JMenuItem( actionMap.get( SpimViewerActions.CROP ) );
		miCrop.setText( "Crop" );
		menu.add( miCrop );

		final JMenuItem miManualTransform = new JMenuItem( actionMap.get( SpimViewerActions.MANUAL_TRANSFORM ) );
		miManualTransform.setText( "Manual Transform" );
		menu.add( miManualTransform );

		menu = new JMenu( "Help" );
		menubar.add( menu );

		final JMenuItem miHelp = new JMenuItem( actionMap.get( SpimViewerActions.SHOW_HELP ) );
		miHelp.setText( "Show Help" );
		menu.add( miHelp );

		viewerFrame.setJMenuBar( menubar );

		viewerFrame.setVisible( true );

		if( ! tryLoadSettings( xmlFilename ) )
			initBrightness( 0.001, 0.999 );
	}

	boolean tryLoadSettings( final String xmlFilename )
	{
		proposedSettingsFile = null;
		if ( xmlFilename.endsWith( ".xml" ) )
		{
			final String settings = xmlFilename.substring( 0, xmlFilename.length() - ".xml".length() ) + ".settings" + ".xml";
			proposedSettingsFile = new File( settings );
			if ( proposedSettingsFile.isFile() )
			{
				try
				{
					loadSettings( settings );
					return true;
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	void initTransform( final int viewerWidth, final int viewerHeight )
	{
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final ViewerState state = viewer.getState();
		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final int timepoint = state.getCurrentTimepoint();
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform( timepoint, 0 );

		final Interval sourceInterval = source.getSpimSource().getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = ( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[3][4];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );

		viewer.setCurrentViewerTransform( viewerTransform );
	}

	void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		final ViewerState state = viewer.getState();
		Source< ? > source = state.getSources().get( state.getCurrentSource() ).getSpimSource();
		if ( VolatileSource.class.isInstance( source ) )
			source = ( ( VolatileSource< ?, ? > ) source ).nonVolatile();
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval ) source.getSource( state.getCurrentTimepoint(), source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d< UnsignedShortType >( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] {0};
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( 0 );
		minmax.getMinBoundedValue().setCurrentValue( min );
		minmax.getMaxBoundedValue().setCurrentValue( max );
	}

	void saveSettings()
	{
		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				saveSettings( proposedSettingsFile.getCanonicalPath() );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	void saveSettings( final String xmlFilename ) throws IOException
	{
		final Element root = new Element( "Settings" );
		root.addContent( viewer.stateToXml() );
		root.addContent( setupAssignments.toXml() );
		root.addContent( manualTransformation.toXml() );
		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileWriter( xmlFilename ) );
	}

	void loadSettings()
	{
		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				loadSettings( proposedSettingsFile.getCanonicalPath() );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	void loadSettings( final String xmlFilename ) throws IOException, JDOMException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();
		viewer.stateFromXml( root );
		setupAssignments.restoreFromXml( root );
		manualTransformation.restoreFromXml( root );
		activeSourcesDialog.update();
		viewer.requestRepaint();
	}

	public static void view( final String filename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException, IOException
	{
		new ViewRegisteredAngles( filename );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/fast fly/111010_weber/combined.xml";
//		final String fn = "/Users/pietzsch/workspace/data/mette/mette.xml";
//		final String fn = "/Users/tobias/Desktop/openspim.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/fibsem.xml";
//		final String fn = "/Users/pietzsch/Desktop/url-valia.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/Valia/valia.xml";
//		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
//		final String fn = "/Volumes/projects/tomancak_lightsheet/Mette/ZeissZ1SPIM/Maritigrella/021013_McH2BsGFP_CAAX-mCherry/11-use/hdf5/021013_McH2BsGFP_CAAX-mCherry-11-use.xml";
		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			new ViewRegisteredAngles( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
