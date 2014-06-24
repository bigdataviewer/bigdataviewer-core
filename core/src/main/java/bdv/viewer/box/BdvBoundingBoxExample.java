package bdv.viewer.box;

import java.util.ArrayList;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import net.imglib2.Interval;
import net.imglib2.util.Intervals;
import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.tools.ToggleDialogAction;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.DisplayMode;
import bdv.viewer.NavigationActions;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;


public class BdvBoundingBoxExample
{
	protected BdvBoundingBoxExample( final String xmlFilename ) throws SpimDataException
	{
		// =============== Load spimdata and create sources for display ==================

		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilename );
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigDataViewer.initSetups( spimData, converterSetups, sources );

		final int width = 800;
		final int height = 600;
		final int numTimepoints = seq.getTimePoints().getTimePointsOrdered().size();
		final ViewerFrame viewerFrame = new ViewerFrame( width, height, sources, numTimepoints,
				( ( ViewerImgLoader< ?, ? > ) seq.getImgLoader() ).getCache() );
		final ViewerPanel viewer = viewerFrame.getViewerPanel();


		// =============== Create SetupAssignments, which encapsulate source color and brightness settings ==================

		for ( final ConverterSetup cs : converterSetups )
			if ( RealARGBColorConverterSetup.class.isInstance( cs ) )
				( ( RealARGBColorConverterSetup ) cs ).setViewer( viewer );

		final SetupAssignments setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}


		// =============== Dialogs for brightness and visibility&grouping ==================

		final BrightnessDialog brightnessDialog = new BrightnessDialog( viewerFrame, setupAssignments );
		final VisibilityAndGroupingDialog activeSourcesDialog = new VisibilityAndGroupingDialog( viewerFrame, viewer.getVisibilityAndGrouping() );


		// =============== the bounding box dialog ==================

		final int boxSetupId = 9999; // some non-existing setup id
		final Interval initialInterval = Intervals.createMinMax( 500, 100, -100, 1500, 800, 300 ); // the initially selected bounding box
		final Interval rangeInterval = Intervals.createMinMax( -500, -500, -500, 3000, 3000, 1000 ); // the range (bounding box of possible bounding boxes)
		final BoundingBoxDialog boundingBoxDialog = new BoundingBoxDialog( viewerFrame, viewer, setupAssignments, boxSetupId, initialInterval, rangeInterval );


		// =============== install standard navigation shortcuts and S/F6/B for brightness/visibility&grouping/boundingbox dialogs ==================

		final KeyProperties keyProperties = KeyProperties.readPropertyFile();
		NavigationActions.installActionBindings( viewerFrame.getKeybindings(), viewer, keyProperties );
		final String BRIGHTNESS_SETTINGS = "brightness settings";
		final String VISIBILITY_AND_GROUPING = "visibility and grouping";
		final String BOUNDING_BOX_DIALOG = "bounding box";
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder amap = new NamedActionAdder( actionMap );
		amap.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS, brightnessDialog ) );
		amap.put( new ToggleDialogAction( VISIBILITY_AND_GROUPING, activeSourcesDialog ) );
		amap.put( new ToggleDialogAction( BOUNDING_BOX_DIALOG, boundingBoxDialog ) );
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder imap = new KeyProperties( null ).adder( inputMap );
		imap.put( BRIGHTNESS_SETTINGS, "S" );
		imap.put( VISIBILITY_AND_GROUPING, "F6" );
		imap.put( BOUNDING_BOX_DIALOG, "B" );
		viewerFrame.getKeybindings().addActionMap( "bdvbox", actionMap );
		viewerFrame.getKeybindings().addInputMap( "bdvbox", inputMap );

		// =============== go to fused mode (overlay all sources) such that bounding box will be visible ==================
		viewer.setDisplayMode( DisplayMode.FUSED );

		// show viewer window
		viewerFrame.setVisible( true );

		// initialize transform and brightness
		InitializeViewerState.initTransform( viewer );
		InitializeViewerState.initBrightness( 0.001, 0.999, viewer, setupAssignments );

//		( ( Hdf5ImageLoader ) seq.imgLoader ).initCachedDimensionsFromHdf5( false );
	}

	public static void main( final String[] args )
	{
		final String fn = "/Users/Pietzsch/Desktop/bdv example/drosophila 2.xml";
		try
		{
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			new BdvBoundingBoxExample( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

}
