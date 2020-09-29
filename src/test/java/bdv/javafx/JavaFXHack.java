package bdv.javafx;

import bdv.TransformEventHandler;
import bdv.viewer.render.awt.BufferedImageOverlayRenderer;
import java.io.File;
import java.io.StringReader;
import java.util.List;

import javax.swing.JFrame;

import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.javafx.JfxMouseAndKeyHandler;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.BigDataViewer;
import bdv.TransformState;
import bdv.export.ProgressWriterConsole;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;

@SuppressWarnings( "restriction" )
public class JavaFXHack
{

	public static void main( String[] args )
	{
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
//		String fn = "/Users/tinevez/Development/Mastodon/mastodon/samples/datasethdf5.xml";
		try
		{
			final BigDataViewer bdv = BigDataViewer.open( fn, new File( fn ).getName(), new ProgressWriterConsole(), ViewerOptions.options() );
			System.out.println( bdv );

			// This method is invoked on the EDT thread
			JFrame frame = new JFrame( "JavaFX as secondary display for BDV" );
			final JFXPanel fxPanel = new JFXPanel();
			frame.add( fxPanel );
			frame.setSize( bdv.getViewerFrame().getWidth(), bdv.getViewerFrame().getHeight() );
			frame.setVisible( true );
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

			Platform.runLater( new Runnable()
			{
				@Override
				public void run()
				{
					initFX( fxPanel, bdv );
				}
			} );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	private static void initFX( JFXPanel fxPanel, BigDataViewer bdv )
	{
		// This method is invoked on the JavaFX thread
		Scene scene = createScene( bdv );
		fxPanel.setScene( scene );
	}

	private static Scene createScene( BigDataViewer bdv )
	{
		/*
		 * Prepare behaviour for the JavaFX canvas.
		 */

		/*
		 * Create InputTriggerMap and BehaviourMap. This is analogous to
		 * javax.swing InputMap and ActionMap.
		 */
		final InputTriggerMap inputMap = new InputTriggerMap();
		final BehaviourMap behaviourMap = new BehaviourMap();

		/*
		 * Create a MouseAndKeyHandler that dispatches to registered Behaviours.
		 */
		final JfxMouseAndKeyHandler handler = new JfxMouseAndKeyHandler();
		handler.setInputMap( inputMap );
		handler.setBehaviourMap( behaviourMap );

		/*
		 * Load YAML config "file".
		 */
		final StringReader reader = new StringReader( "---\n" +
				"- !mapping" + "\n" +
				"  action: drag1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [button1, G]" + "\n" +
				"- !mapping" + "\n" +
				"  action: scroll1" + "\n" +
				"  contexts: [all]" + "\n" +
				"  triggers: [scroll]" + "\n" +
				"" );
		final List< InputTriggerDescription > triggers = YamlConfigIO.read( reader );
		final InputTriggerConfig config = new InputTriggerConfig( triggers );

		/*
		 * Create behaviours and input mappings.
		 */
		Behaviours behaviours = new Behaviours( inputMap, behaviourMap, config, "all" );

		/*
		 * Transform Handler
		 */

		SynchronizedViewerState state = bdv.getViewer().state();
		TransformEventHandler transformEventHandler = bdv.getViewer().getOptionValues().getTransformEventHandlerFactory()
				.create( TransformState.from( state::getViewerTransform, state::setViewerTransform ) );
		transformEventHandler.install( behaviours );

		/*
		 * Create the scene.
		 */

		Group root = new Group();
		Scene scene = new Scene( root );
		scene.addEventFilter( Event.ANY, handler );

		ImageView imageView = new ImageView();
		imageView.setCache( true );
		imageView.setCacheHint( CacheHint.SPEED );
		imageView.setSmooth( false );
		imageView.setPreserveRatio( true );

		/*
		 * Resize the display image on the fly to match the window size. Another
		 * better solution would be made within the true GUI.
		 */
		imageView.fitWidthProperty().bind( scene.widthProperty() );
		imageView.fitHeightProperty().bind( scene.heightProperty() );

		/*
		 * Should be true, but conflicts with the other display. If set to true
		 * the panning events cancel each others.
		 */
		boolean updateTransform = false;

		/*
		 * Only used to inform the transform handler that the canvas size
		 * changed. Should impact how the mouse clicks are considered. But now
		 * it does not for some reason. Related to the updateTrasnform flag
		 * above?
		 */
		ChangeListener< Number > canvasSizeListener = ( obs, oldValue,
				newValue ) -> transformEventHandler
						.setCanvasSize(
								( int ) imageView.getFitWidth(),
								( int ) imageView.getFitHeight(),
								updateTransform );
		scene.widthProperty().addListener( canvasSizeListener );
		scene.heightProperty().addListener( canvasSizeListener );

		root.getChildren().add( imageView );

		ViewerPanel viewer = bdv.getViewer();
		BufferedImageOverlayRenderer renderTarget = viewer.getRenderTarget();
		renderTarget.setImageView( imageView );
		return scene;
	}
}
