package bdv.tools.boundingbox;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import net.imglib2.Interval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;

// dialog to change bounding box
// while dialog is visible, bounding box is added as a source to the viewer
public class BoundingBoxDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	protected final BoxRealRandomAccessible< UnsignedShortType > boxRealRandomAccessible;

	protected final BoxSelectionPanel boxSelectionPanel;

	protected final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	protected final RealARGBColorConverterSetup boxConverterSetup;

	protected final BoundingBoxOverlay boxOverlay;

	private boolean contentCreated = false;

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int boxSetupId,
			final Interval initialInterval,
			final Interval rangeInterval )
	{
		this( owner, title, viewer, setupAssignments, boxSetupId, initialInterval, rangeInterval, true, true );
	}

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int boxSetupId,
			final Interval initialInterval,
			final Interval rangeInterval,
			final boolean showBoxSource,
			final boolean showBoxOverlay )
	{
		super( owner, title, false );

		// create a procedural RealRandomAccessible that will render the bounding box
		final UnsignedShortType insideValue = new UnsignedShortType( 1000 ); // inside the box pixel value is 1000
		final UnsignedShortType outsideValue = new UnsignedShortType( 0 ); // outside is 0
		boxRealRandomAccessible = new BoxRealRandomAccessible< UnsignedShortType >( initialInterval, insideValue, outsideValue );

		// create a bdv.viewer.Source providing data from the bbox RealRandomAccessible
		final RealRandomAccessibleSource< UnsignedShortType > boxSource = new RealRandomAccessibleSource< UnsignedShortType >( boxRealRandomAccessible, new UnsignedShortType(), "selection" )
		{
			@Override
			public Interval getInterval( final int t, final int level )
			{
				return boxRealRandomAccessible.getInterval();
			}
		};

		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1< UnsignedShortType >( 0, 3000 );
		converter.setColor( new ARGBType( 0x00994499 ) ); // set bounding box color to magenta

		// create a ConverterSetup (can be used by the brightness dialog to adjust the converter settings)
		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		boxConverterSetup.setViewer( viewer );

		// create a SourceAndConverter (can be added to the viewer for display)
		boxSourceAndConverter = new SourceAndConverter< UnsignedShortType >( boxSource, converter );

		// create an Overlay to show 3D wireframe box
		boxOverlay = new BoundingBoxOverlay( boxRealRandomAccessible.getInterval() );

		// create a JPanel with sliders to modify the bounding box interval (boxRealRandomAccessible.getInterval())
		boxSelectionPanel = new BoxSelectionPanel( boxRealRandomAccessible.getInterval(), rangeInterval );
		boxSelectionPanel.addSelectionUpdateListener( new BoxSelectionPanel.SelectionUpdateListener() // listen for updates on the bbox to trigger repainting
		{
			@Override
			public void selectionUpdated()
			{
				viewer.requestRepaint();
			}
		} );

		// when dialog is made visible, add bbox source
		// when dialog is hidden, remove bbox source
		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.addSource( boxSourceAndConverter );
					setupAssignments.addSetup( boxConverterSetup );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().addOverlayRenderer( boxOverlay );
					viewer.addRenderTransformListener( boxOverlay );
				}
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.removeSource( boxSourceAndConverter.getSpimSource() );
					setupAssignments.removeSetup( boxConverterSetup );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().removeOverlayRenderer( boxOverlay );
					viewer.removeTransformListener( boxOverlay );
				}
			}
		} );


		// make ESC key hide dialog
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
//		createContent();
	}

	@Override
	public void setVisible( final boolean b )
	{
		if ( b && !contentCreated )
		{
			createContent();
			contentCreated = true;
		}
		super.setVisible( b );
	}

	// Override in subclasses
	public void createContent()
	{
		getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		pack();
	}
}
