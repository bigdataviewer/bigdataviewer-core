package bdv.ij;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.TextEvent;
import java.io.File;
import java.io.IOException;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * ImageJ plugin to import a raw image from xml/hdf5.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ImportPlugIn implements PlugIn
{
	public static String xmlFile = "";
	public static int timepoint = 0;
	public static int setup = 0;

	private SequenceDescription openSequence( final String xmlFilename ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException, IOException
	{
		final File f = new File( xmlFilename );
		if ( f.exists() && f.isFile() && f.getName().endsWith( ".xml" ) )
		{
			final SAXBuilder sax = new SAXBuilder();
			final Document doc = sax.build( xmlFilename );
			final Element root = doc.getRootElement();

			final File baseDirectory = new File( xmlFilename ).getParentFile();
			final SequenceDescription seq = new SequenceDescription( root, baseDirectory != null ? baseDirectory : new File( "." ), true );
			return seq;
		}
		else
			return null;
	}

	@Override
	public void run( final String arg0 )
	{
		final GenericDialogPlus gd = new GenericDialogPlus( "Import from BigDataViewer file" );
		gd.addFileField( "xml file", xmlFile );
		final TextField tfXmlFile = (TextField) gd.getStringFields().lastElement();
		gd.addSlider( "timepoint index", 0, 0, timepoint );
		final Scrollbar slTimepoint = (Scrollbar) gd.getSliders().lastElement();
		final TextField tfTimepoint = (TextField) gd.getNumericFields().lastElement();
		gd.addSlider( "setup index", 0, 0, setup );
		final Scrollbar slSetup = (Scrollbar) gd.getSliders().lastElement();
		final TextField tfSetup = (TextField) gd.getNumericFields().lastElement();

		class TryOpen
		{
			void check( final String xmlFilename )
			{
				boolean enable = false;
				try
				{
					final SequenceDescription seq = openSequence( xmlFilename );
					if ( seq != null )
					{
						final int numTimepoints = seq.numTimepoints();
						final int numSetups = seq.numViewSetups();

						slTimepoint.setMaximum( numTimepoints );
						slSetup.setMaximum( numSetups );
						enable = true;
					}
				}
				catch ( final Exception ex )
				{
					IJ.error( ex.getMessage() );
					ex.printStackTrace();
				}
				slTimepoint.setEnabled( enable );
				tfTimepoint.setEnabled( enable );
				slSetup.setEnabled( enable );
				tfSetup.setEnabled( enable );
			}
		}
		final TryOpen tryOpen = new TryOpen();
		tryOpen.check( xmlFile );

		gd.addDialogListener( new DialogListener()
		{
			@Override
			public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
			{
				gd.getNextString();
				gd.getNextNumber();
				gd.getNextNumber();
				if ( e instanceof TextEvent && e.getID() == TextEvent.TEXT_VALUE_CHANGED && e.getSource() == tfXmlFile )
				{
					final TextField tf = ( TextField ) e.getSource();
					final String xmlFilename = tf.getText();
					tryOpen.check( xmlFilename );
				}
				return true;
			}
		} );

		gd.showDialog();
		if ( gd.wasCanceled() )
			return;

		xmlFile = gd.getNextString();
		timepoint = ( int ) gd.getNextNumber();
		setup = ( int ) gd.getNextNumber();

		System.out.println( xmlFile + " " + timepoint + " " + setup );
		try
		{
			final SequenceDescription seq = openSequence( xmlFile );
			if ( seq != null )
			{
				final int numTimepoints = seq.numTimepoints();
				final int numSetups = seq.numViewSetups();
				timepoint = Math.max( Math.min( timepoint, numTimepoints - 1 ), 0 );
				setup = Math.max( Math.min( setup, numSetups - 1 ), 0 );
				@SuppressWarnings( "unchecked" )
				final ImgLoader< UnsignedShortType > il = ( ImgLoader< UnsignedShortType > ) seq.imgLoader;
				final RandomAccessibleInterval< UnsignedShortType > img = il.getImage( new View( seq, timepoint, setup, null ) );
				final ImagePlus imp = net.imglib2.img.display.imagej.ImageJFunctions.wrap( img, "" ).duplicate();
				imp.setTitle( new File( xmlFile ).getName() + " " + timepoint + " " + setup );
				imp.show();
			}
		}
		catch ( final Exception ex )
		{
			IJ.error( ex.getMessage() );
			ex.printStackTrace();
		}
	}

	public static void main( final String[] args )
	{
		new ImageJ();
		new ImportPlugIn().run( null );
	}

}
