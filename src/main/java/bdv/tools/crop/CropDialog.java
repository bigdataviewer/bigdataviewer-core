package bdv.tools.crop;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.Beans;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import bdv.ij.export.WriteSequenceToHdf5;
import bdv.ij.export.WriteSequenceToXml;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Util;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceState;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;

public class CropDialog extends JDialog
{
	private final ViewerPanel viewer;

	private final SequenceDescription sequenceDescription;

	private final JTextField pathTextField;

	private final JSpinner spinnerMinTimepoint;

	private final JSpinner spinnerMaxTimepoint;

	@Override
	public void setVisible( final boolean b )
	{
		if ( b )
		{
			final int tp = viewer.getState().getCurrentTimepoint();
			spinnerMinTimepoint.setValue( tp );
			spinnerMaxTimepoint.setValue( tp );
		}
		super.setVisible( b );
	}

	public CropDialog( final Frame owner, final ViewerPanel viewer, final SequenceDescription sequenceDescription )
	{
		super( owner, "crop and save", false );
		this.viewer = viewer;
		this.sequenceDescription = sequenceDescription;

		final JPanel boxes = new JPanel();
		getContentPane().add( boxes, BorderLayout.NORTH );
		boxes.setLayout( new BoxLayout( boxes, BoxLayout.PAGE_AXIS ) );

		final JPanel saveAsPanel = new JPanel();
		saveAsPanel.setLayout( new BorderLayout( 0, 0 ) );
		boxes.add( saveAsPanel );

		saveAsPanel.add( new JLabel( "save as" ), BorderLayout.WEST );

		pathTextField = new JTextField( "./crop.xml" );
		saveAsPanel.add( pathTextField, BorderLayout.CENTER );
		pathTextField.setColumns( 20 );

		final JButton browseButton = new JButton( "Browse" );
		saveAsPanel.add( browseButton, BorderLayout.EAST );

		final JPanel timepointsPanel = new JPanel();
		boxes.add( timepointsPanel );

		timepointsPanel.add( new JLabel( "timepoints from" ) );

		spinnerMinTimepoint = new JSpinner();
		spinnerMinTimepoint.setModel( new SpinnerNumberModel( 0, 0, sequenceDescription.numTimepoints() - 1, 1 ) );
		timepointsPanel.add( spinnerMinTimepoint );

		timepointsPanel.add( new JLabel( "to" ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( 0, 0, sequenceDescription.numTimepoints() - 1, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout(new BorderLayout(0, 0));

		final JButton cropButton = new JButton( "Crop and Save" );
		buttonsPanel.add( cropButton, BorderLayout.EAST );

		if ( ! Beans.isDesignTime() )
		{
			spinnerMinTimepoint.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int min = ( Integer ) spinnerMinTimepoint.getValue();
					final int max = ( Integer ) spinnerMaxTimepoint.getValue();
					if ( max < min )
						spinnerMaxTimepoint.setValue( min );
				}
			} );

			spinnerMaxTimepoint.addChangeListener( new ChangeListener()
			{
				@Override
				public void stateChanged( final ChangeEvent e )
				{
					final int min = ( Integer ) spinnerMinTimepoint.getValue();
					final int max = ( Integer ) spinnerMaxTimepoint.getValue();
					if (min > max)
						spinnerMinTimepoint.setValue( max );
				}
			} );

			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled( false );
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

			browseButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					fileChooser.setSelectedFile( new File( pathTextField.getText() ) );
					final int returnVal = fileChooser.showSaveDialog( null );
					if ( returnVal == JFileChooser.APPROVE_OPTION )
					{
						final File file = fileChooser.getSelectedFile();
						pathTextField.setText( file.getAbsolutePath() );
					}
				}
			} );

			cropButton.addActionListener( new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					String seqFilename = pathTextField.getText();
					if ( !seqFilename.endsWith( ".xml" ) )
						seqFilename += ".xml";
					final File seqFile = new File( seqFilename );
					final File parent = seqFile.getParentFile();
					if ( parent == null || !parent.exists() || !parent.isDirectory() )
					{
						System.err.println( "Invalid export filename " + seqFilename );
						return;
					}
					final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
					final File hdf5File = new File( hdf5Filename );

					final int minTimepointIndex = ( Integer ) spinnerMinTimepoint.getValue();
					final int maxTimepointIndex = ( Integer ) spinnerMaxTimepoint.getValue();

					new Thread()
					{
						@Override
						public void run()
						{
							try
							{
								cropGlobal( minTimepointIndex, maxTimepointIndex, hdf5File, seqFile );
							}
							catch ( final Exception ex )
							{
								ex.printStackTrace();
							}
						}
					}.start();
				}
			} );
		}

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

			private static final long serialVersionUID = 1L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		pack();
		setDefaultCloseOperation( JDialog.HIDE_ON_CLOSE );
	}

	/**
	 * @param minTimepointIndex
	 * @param maxTimepointIndex
	 * @param hdf5File
	 * @param xmlFile
	 */
	public void cropGlobal( final int minTimepointIndex, final int maxTimepointIndex, final File hdf5File, final File xmlFile ) throws IOException
	{
		final AffineTransform3D globalToCropTransform = new AffineTransform3D();
		viewer.getState().getViewerTransform( globalToCropTransform );

		final int w = viewer.getDisplay().getWidth();
		final int h = viewer.getDisplay().getHeight();
		final int d = Math.min( w, h );
		final int x = 0;
		final int y = 0;
		final int z = - d / 2;

		final RealInterval cropInterval = Intervals.createMinSizeReal( x, y, z, w, h, d );
		final ArrayList< Source< UnsignedShortType > > sources = new ArrayList< Source< UnsignedShortType > >();
		for( final SourceState< ? > s : viewer.getState().getSources() )
			sources.add( ( Source ) s.getSpimSource() );
		final ArrayList< Integer > timepoints = new ArrayList< Integer >();
		final ArrayList< Integer > cropperTimepointMap = new ArrayList< Integer >();
		for ( int tp = minTimepointIndex; tp <= maxTimepointIndex; ++tp )
		{
			timepoints.add( tp - minTimepointIndex );
			cropperTimepointMap.add( tp );
		}
		final CropImgLoader cropper = new CropImgLoader( sources, globalToCropTransform, cropInterval, cropperTimepointMap );

		final ArrayList< ViewSetup > setups = sequenceDescription.setups;
		final File basePath = xmlFile.getParentFile();
		final SequenceDescription seq = new SequenceDescription( setups, timepoints, basePath, cropper );


		final Hdf5ImageLoader loader = ( Hdf5ImageLoader ) sequenceDescription.imgLoader;
		final ArrayList< int[][] > perSetupResolutions = new ArrayList< int[][] >();
		final ArrayList< int[][] > perSetupSubdivisions = new ArrayList< int[][] >();
		for ( int setup = 0; setup < sequenceDescription.numViewSetups(); ++setup )
		{
			perSetupResolutions.add( Util.castToInts( loader.getMipmapResolutions( setup ) ) );
			perSetupSubdivisions.add( loader.getSubdivisions( setup ) );
		}

		WriteSequenceToHdf5.writeHdf5File( seq, perSetupResolutions, perSetupSubdivisions, hdf5File, null );

		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		for ( int tp = minTimepointIndex; tp <= maxTimepointIndex; ++tp )
		{
			final int timepoint = tp - minTimepointIndex;
			for ( int setup = 0; setup < sequenceDescription.numViewSetups(); ++setup )
			{
				final AffineTransform3D model = cropper.getCroppedTransform( new View( seq, timepoint, setup, null ) );
				regs.add( new ViewRegistration( timepoint, setup, model ) );
			}
		}
		final ViewRegistrations registrations = new ViewRegistrations( regs, 0 );
		WriteSequenceToXml.writeSequenceToXml( new SequenceDescription( setups, timepoints, basePath, new Hdf5ImageLoader( hdf5File, null, false ) ), registrations, xmlFile.getAbsolutePath() );
	}

	private static final long serialVersionUID = 924395364255873920L;
}
