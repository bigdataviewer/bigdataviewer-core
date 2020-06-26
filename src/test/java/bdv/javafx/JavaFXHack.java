package bdv.javafx;

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
import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import net.imglib2.ui.overlay.BufferedImageOverlayRenderer;

@SuppressWarnings( "restriction" )
public class JavaFXHack
{

	public static void main( String[] args )
	{
		String fn = "/Users/tinevez/Development/Mastodon/mastodon/samples/datasethdf5.xml";
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
		BdvJfxBehaviours.install( behaviours );

		/*
		 * Create the scene.
		 */

		Group root = new Group();
		Scene scene = new Scene( root );
		scene.addEventFilter( Event.ANY, handler );

		ImageView imageView = new ImageView();
		imageView.setCache( true );
		imageView.setCacheHint( CacheHint.SPEED );
		root.getChildren().add( imageView );

		ViewerPanel viewer = bdv.getViewer();
		BufferedImageOverlayRenderer renderTarget = viewer.getRenderTarget();
		renderTarget.setImageView( imageView );
		return scene;
	}
}
