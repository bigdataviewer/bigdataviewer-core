package viewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.xml.sax.SAXException;

import viewer.render.SourceAndConverter;

public class ViewRegisteredAngles implements BrightnessDialog.MinMaxListener
{
	final KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_S, 0 );

	final KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );

	final SpimViewer viewer;

	final ArrayList< AbstractLinearRange > displayRanges;

	final BrightnessDialog brightnessDialog;

	public void toggleBrightnessDialog()
	{
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}

	public void showHelp()
	{
		new HelpFrame();
	}

	@Override
	public void setMinMax( final int min, final int max )
	{
		for ( final AbstractLinearRange r : displayRanges )
		{
			r.setMin( min );
			r.setMax( max );
		}
		viewer.requestRepaint();
	}

	private ViewRegisteredAngles( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final int width = 800;
		final int height = 600;

		final SequenceViewsLoader loader = new SequenceViewsLoader( xmlFilename );
		final SequenceDescription seq = loader.getSequenceDescription();

		displayRanges = new ArrayList< AbstractLinearRange >();
		final RealARGBConverter< UnsignedShortType > converter = new RealARGBConverter< UnsignedShortType >( 0, 65535 );
		displayRanges.add( converter );

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		for ( int setup = 0; setup < seq.numViewSetups(); ++setup )
			sources.add( new SourceAndConverter< UnsignedShortType >( new SpimSource( loader, setup, "angle " + seq.setups[ setup ].getAngle() ), converter ) );

		viewer = new SpimViewer( width, height, sources, seq.numTimepoints() );

		viewer.addKeyAction( brightnessKeystroke, new AbstractAction( "brightness settings" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				toggleBrightnessDialog();
			}

			private static final long serialVersionUID = 1L;
		} );

		viewer.addKeyAction( helpKeystroke, new AbstractAction( "help" )
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				showHelp();
			}

			private static final long serialVersionUID = 1L;
		} );

		brightnessDialog = new BrightnessDialog( viewer.frame );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );
	}

	public static void view( final String filename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, SAXException, IOException
	{
		new ViewRegisteredAngles( filename );
	}

	public static void main( final String[] args )
	{
//		final String fn = "/Users/tobias/workspace/data/fast fly/111010_weber/combined.xml";
//		final String fn = "/Users/tobias/Desktop/openspim.xml";
		final String fn = "/Users/tobias/Desktop/e012/test5.xml";
		try
		{
			new ViewRegisteredAngles( fn );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
