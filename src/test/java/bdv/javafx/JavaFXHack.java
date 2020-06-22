package bdv.javafx;

import java.io.File;

import javax.swing.JFrame;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
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
		Group root = new Group();
		Scene scene = new Scene( root );
		ImageView imageView = new ImageView();
		root.getChildren().add( imageView );

		ViewerPanel viewer = bdv.getViewer();
		BufferedImageOverlayRenderer renderTarget = viewer.getRenderTarget();
		renderTarget.setImageView( imageView );
		return scene;
	}
}
