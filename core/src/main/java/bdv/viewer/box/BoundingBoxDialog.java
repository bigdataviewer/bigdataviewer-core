package bdv.viewer.box;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import net.imglib2.Interval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;

// dialog to change bounding box
// while dialog is visible, bounding box is added as a source to the viewer
public class BoundingBoxDialog extends JDialog
{
	private final ViewerPanel viewer;

	private final BoxRealRandomAccessible< UnsignedShortType > boxRealRandomAccessible;

	private final BoxSelectionPanel boxSelectionPanel;

	protected final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	protected final RealARGBColorConverterSetup boxConverterSetup;

	public BoundingBoxDialog( final Frame owner, final ViewerPanel viewer, final SetupAssignments setupAssignments, final int boxSetupId, final Interval initialInterval, final Interval rangeInterval )
	{
		super( owner, "bounding box", false );
		this.viewer = viewer;


		// create a procedural RealRandomAccessible that will render the bounding box
		final UnsignedShortType insideValue = new UnsignedShortType( 1000 ); // inside the box pixel value is 1000
		final UnsignedShortType outsideValue = new UnsignedShortType( 0 ); // outside is 0
		boxRealRandomAccessible = new BoxRealRandomAccessible< UnsignedShortType >( initialInterval, insideValue, outsideValue );

		// create a bdv.viewer.Source providing data from the bbox RealRandomAccessible
		final RealRandomAccessibleSource< UnsignedShortType > boxSource = new RealRandomAccessibleSource< UnsignedShortType >( boxRealRandomAccessible, "selection" )
		{
			@Override
			public Interval getInterval( final int t, final int level )
			{
				return boxRealRandomAccessible.getInterval();
			}
		};

		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final RealARGBColorConverter< UnsignedShortType > converter = new RealARGBColorConverter.Imp1< UnsignedShortType >( 0, 3000 );
		converter.setColor( new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) ) ); // set bounding box color to green

		// create a ConverterSetup (can be used by the brightness dialog to adjust the converter settings)
		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		boxConverterSetup.setViewer( viewer );

		// create a SourceAndConverter (can be added to the viewer for display
		boxSourceAndConverter = new SourceAndConverter< UnsignedShortType >( boxSource, converter );


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


		// button prints the bounding box interval
		final JButton button = new JButton( "ok" );
		button.addActionListener( new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				System.out.println( "bounding box:" + net.imglib2.util.Util.printInterval( boxRealRandomAccessible.getInterval() ) );
			}
		} );


		// when dialog is made visible, add bbox source
		// when dialog is hidden, remove bbox source
		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				viewer.addSource( boxSourceAndConverter );
				setupAssignments.addSetup( boxConverterSetup );
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				viewer.removeSource( boxSourceAndConverter.getSpimSource() );
				setupAssignments.removeSetup( boxConverterSetup );
			}
		} );


		// make ESC key hide dialog
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );


		// layout and show dialog
		final Container content = getContentPane();
		content.add( boxSelectionPanel, BorderLayout.NORTH );
		content.add( button, BorderLayout.SOUTH );
		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}
}
