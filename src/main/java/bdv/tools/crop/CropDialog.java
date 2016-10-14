/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.tools.crop;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import bdv.AbstractSpimSource;
import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.Util;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceState;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class CropDialog extends JDialog
{
	private final ViewerPanel viewer;

	private final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;

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

	public CropDialog( final Frame owner, final ViewerPanel viewer, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
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
		final int maxTimePointIndex = sequenceDescription.getTimePoints().size() - 1;
		spinnerMinTimepoint.setModel( new SpinnerNumberModel( 0, 0, maxTimePointIndex, 1 ) );
		timepointsPanel.add( spinnerMinTimepoint );

		timepointsPanel.add( new JLabel( "to" ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( 0, 0, maxTimePointIndex, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout(new BorderLayout(0, 0));

		final JButton cropButton = new JButton( "Crop and Save" );
		buttonsPanel.add( cropButton, BorderLayout.EAST );

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
				if ( min > max )
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
					final int i = s.lastIndexOf( '.' );
					if ( i > 0 && i < s.length() - 1 )
					{
						final String ext = s.substring( i + 1 ).toLowerCase();
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
							cropButton.setEnabled( false );
							cropGlobal( minTimepointIndex, maxTimepointIndex, hdf5File, seqFile );
							cropButton.setEnabled( true );
						}
						catch ( final Exception ex )
						{
							ex.printStackTrace();
						}
					}
				}.start();
			}
		} );

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
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}

	/**
	 * @param minTimepointIndex
	 * @param maxTimepointIndex
	 * @param hdf5File
	 * @param xmlFile
	 */
	public void cropGlobal( final int minTimepointIndex, final int maxTimepointIndex, final File hdf5File, final File xmlFile ) throws SpimDataException
	{
		final AffineTransform3D globalToCropTransform = new AffineTransform3D();
		viewer.getState().getViewerTransform( globalToCropTransform );

		final int w = viewer.getDisplay().getWidth();
		final int h = viewer.getDisplay().getHeight();
		final int d = Math.min( w, h );
		final int x = 0;
		final int y = 0;
		final int z = - d / 2;
		final RealInterval cropInterval = Intervals.createMinMaxReal( x, y, z, x + w, y + h, z + d );

		// list of timepoints of the original sequence
		final List< TimePoint > sequenceTimepointsOrdered = sequenceDescription.getTimePoints().getTimePointsOrdered();

		// list of setups of the original sequence
		final List< ? extends BasicViewSetup > sequenceSetupsOrdered = sequenceDescription.getViewSetupsOrdered();
		// next unused setup id, in case we need to create new BasicViewSetup for sources that are not AbstractSpimSources
		int nextSetupIndex = sequenceSetupsOrdered.get( sequenceSetupsOrdered.size() - 1 ).getId() + 1;

		// TODO: fix comment
		// List of all sources. if they are not of UnsignedShortType, cropping
		// will not work...
		final ArrayList< Source< ? > > sources = new ArrayList<>();
		// Map from setup id to BasicViewSetup. These are setups from the
		// original sequence if available, or newly created ones otherwise.
		// This contains all BasicViewSetups for the new cropped sequence.
		final HashMap< Integer, BasicViewSetup > cropSetups = new HashMap<>();
		// Map from setup id to index of corresponding source in sources list.
		// This is needed because the CropImgLoader is asked for (timepointId,
		// setupId) pair and needs to retrieve from corresponding source.
		final HashMap< Integer, Integer > setupIdToSourceIndex = new HashMap<>();
		for( final SourceState< ? > s : viewer.getState().getSources() )
		{
			Source< ? > source = s.getSpimSource();
			sources.add( source );

			// try to find the BasicViewSetup for the source
			final BasicViewSetup setup;

			// strip TransformedSource wrapper
			while ( source instanceof TransformedSource )
				source = ( ( TransformedSource< ? > ) source ).getWrappedSource();

			if ( source instanceof AbstractSpimSource )
			{
				 final int setupId = ( ( AbstractSpimSource< ? > ) source ).getSetupId();
				 setup = sequenceDescription.getViewSetups().get( setupId );
			}
			else
			{
				final int setupId = nextSetupIndex++;
				setup = new BasicViewSetup( setupId, Integer.toString( setupId ), null, null );
			}
			cropSetups.put( setup.getId(), setup );
			setupIdToSourceIndex.put( setup.getId(), sources.size() - 1 );
		}

		// Map from timepoint id to timepoint index (in the list of timepoints
		// of the original sequence). This is needed because the CropImgLoader
		// is asked for (timepointId, setupId) pair and needs to retrieve by
		// timepoint index from its sources.
		final HashMap< Integer, Integer > timepointIdToTimepointIndex = new HashMap<>();
		// This contains all TimePoints for the new cropped sequence.
		final ArrayList< TimePoint > timepointsToCrop = new ArrayList<>();
		for ( int tp = minTimepointIndex; tp <= maxTimepointIndex; ++tp )
		{
			final TimePoint timepoint = sequenceTimepointsOrdered.get( tp );
			timepointIdToTimepointIndex.put( timepoint.getId(), tp );
			timepointsToCrop.add( timepoint );
		}

		final CropImgLoader cropper = new CropImgLoader( sources, globalToCropTransform, cropInterval, timepointIdToTimepointIndex, setupIdToSourceIndex );
		// the new cropped sequence to be written to hdf5
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepointsToCrop ), cropSetups, cropper, null );

		// Gather ExportMipmapInfo for all setups of the cropped sequence.
		// Re-use mipmapInfos for setups of the original sequence. Use default
		// for newly created setups.
		final HashMap< Integer, ExportMipmapInfo > perSetupMipmapInfo = new HashMap<>();
		final Hdf5ImageLoader loader = ( Hdf5ImageLoader ) sequenceDescription.getImgLoader();
		for ( final int setupId : cropSetups.keySet() )
		{
			final MipmapInfo info = loader.getSetupImgLoader( setupId ).getMipmapInfo();;
			if ( info == null )
				perSetupMipmapInfo.put( setupId, new ExportMipmapInfo(
						new int[][] { { 1, 1, 1 } },
						new int[][] { { 64, 64, 64 } } ) );
			else
				perSetupMipmapInfo.put( setupId, new ExportMipmapInfo(
						Util.castToInts( info.getResolutions() ),
						info.getSubdivisions() ) );
		}

		final int numThreads = Math.max( 1, Runtime.getRuntime().availableProcessors() - 2 );
		WriteSequenceToHdf5.writeHdf5File( seq, perSetupMipmapInfo, true, hdf5File, null, null, numThreads, null );

		// Build ViewRegistrations with adjusted transforms.
		final ArrayList< ViewRegistration > registrations = new ArrayList<>();
		for ( final TimePoint timepoint : timepointsToCrop )
		{
			final int timepointId = timepoint.getId();
			for ( final BasicViewSetup setup : cropSetups.values() )
			{
				final int setupId = setup.getId();
				final AffineTransform3D model = cropper.getCroppedTransform( new ViewId( timepointId, setupId ) );
				registrations.add( new ViewRegistration( timepointId, setupId, model ) );
			}
		}

		// create SpimDataMinimal for cropped sequence, now with a Hdf5ImageLoader for the hdf5File that we just wrote.
		seq.setImgLoader( new Hdf5ImageLoader( hdf5File, null, seq, false ) );
		final File basePath = xmlFile.getParentFile();
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );

		new XmlIoSpimDataMinimal().save( spimData, xmlFile.getAbsolutePath() );
	}

	private static final long serialVersionUID = 924395364255873920L;
}
