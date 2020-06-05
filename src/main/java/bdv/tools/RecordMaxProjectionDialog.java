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
package bdv.tools;

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.util.Prefs;
import bdv.viewer.BasicViewerState;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.RenderResult;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
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
import net.imglib2.Cursor;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.util.LinAlgHelpers;

public class RecordMaxProjectionDialog extends JDialog implements OverlayRenderer
{
	private static final long serialVersionUID = 1L;

	private final ViewerPanel viewer;

	private final int maxTimepoint;

	private final ProgressWriter progressWriter;

	private final JTextField pathTextField;

	private final JSpinner spinnerMinTimepoint;

	private final JSpinner spinnerMaxTimepoint;

	private final JSpinner spinnerWidth;

	private final JSpinner spinnerHeight;

	private final JSpinner spinnerStepSize;

	private final JSpinner spinnerNumSteps;

	public RecordMaxProjectionDialog( final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter )
	{
		super( owner, "record max projection movie", false );
		this.viewer = viewer;
		maxTimepoint = viewer.state().getNumTimepoints() - 1;
		this.progressWriter = progressWriter;

		final JPanel boxes = new JPanel();
		getContentPane().add( boxes, BorderLayout.NORTH );
		boxes.setLayout( new BoxLayout( boxes, BoxLayout.PAGE_AXIS ) );

		final JPanel saveAsPanel = new JPanel();
		saveAsPanel.setLayout( new BorderLayout( 0, 0 ) );
		boxes.add( saveAsPanel );

		saveAsPanel.add( new JLabel( "save to" ), BorderLayout.WEST );

		pathTextField = new JTextField( "./record/" );
		saveAsPanel.add( pathTextField, BorderLayout.CENTER );
		pathTextField.setColumns( 20 );

		final JButton browseButton = new JButton( "Browse" );
		saveAsPanel.add( browseButton, BorderLayout.EAST );

		final JPanel timepointsPanel = new JPanel();
		boxes.add( timepointsPanel );

		timepointsPanel.add( new JLabel( "timepoints from" ) );

		spinnerMinTimepoint = new JSpinner();
		spinnerMinTimepoint.setModel( new SpinnerNumberModel( 0, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMinTimepoint );

		timepointsPanel.add( new JLabel( "to" ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( maxTimepoint, 0, maxTimepoint, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

		final JPanel widthPanel = new JPanel();
		boxes.add( widthPanel );
		widthPanel.add( new JLabel( "width" ) );
		spinnerWidth = new JSpinner();
		spinnerWidth.setModel( new SpinnerNumberModel( 800, 10, 5000, 1 ) );
		widthPanel.add( spinnerWidth );

		final JPanel heightPanel = new JPanel();
		boxes.add( heightPanel );
		heightPanel.add( new JLabel( "height" ) );
		spinnerHeight = new JSpinner();
		spinnerHeight.setModel( new SpinnerNumberModel( 600, 10, 5000, 1 ) );
		heightPanel.add( spinnerHeight );

		final JPanel stepSizePanel = new JPanel();
		boxes.add( stepSizePanel );
		stepSizePanel.add( new JLabel( "slice step size" ) );
		spinnerStepSize = new JSpinner();
		spinnerStepSize.setModel( new SpinnerNumberModel( 1, 0.001, 20, 0.1 ) );
		stepSizePanel.add( spinnerStepSize );

		final JPanel numStepsPanel = new JPanel();
		boxes.add( numStepsPanel );
		numStepsPanel.add( new JLabel( "number of slices" ) );
		spinnerNumSteps = new JSpinner();
		spinnerNumSteps.setModel( new SpinnerNumberModel( 10, 1, 10000, 1 ) );
		numStepsPanel.add( spinnerNumSteps );

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout(new BorderLayout(0, 0));

		final JButton recordButton = new JButton( "Record" );
		buttonsPanel.add( recordButton, BorderLayout.EAST );

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
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

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

		recordButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final String dirname = pathTextField.getText();
				final File dir = new File( dirname );
				if ( !dir.exists() )
					dir.mkdirs();
				if ( !dir.exists() || !dir.isDirectory() )
				{
					System.err.println( "Invalid export directory " + dirname );
					return;
				}
				final int minTimepointIndex = ( Integer ) spinnerMinTimepoint.getValue();
				final int maxTimepointIndex = ( Integer ) spinnerMaxTimepoint.getValue();
				final int width = ( Integer ) spinnerWidth.getValue();
				final int height = ( Integer ) spinnerHeight.getValue();
				final double stepSize = ( Double ) spinnerStepSize.getValue();
				final int numSteps = ( Integer ) spinnerNumSteps.getValue();
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							recordButton.setEnabled( false );
							recordMovie( width, height, minTimepointIndex, maxTimepointIndex, stepSize, numSteps, dir );
							recordButton.setEnabled( true );
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
	 * @param stepSize in multiples of width of a source voxel.
	 */
	public void recordMovie( final int width, final int height, final int minTimepointIndex, final int maxTimepointIndex, final double stepSize, final int numSteps, final File dir ) throws IOException
	{
		final ViewerState renderState = new BasicViewerState( viewer.state().snapshot() );
		final int canvasW = viewer.getDisplay().getWidth();
		final int canvasH = viewer.getDisplay().getHeight();

		final AffineTransform3D tGV = new AffineTransform3D();
		renderState.getViewerTransform( tGV );
		tGV.set( tGV.get( 0, 3 ) - canvasW / 2, 0, 3 );
		tGV.set( tGV.get( 1, 3 ) - canvasH / 2, 1, 3 );
		tGV.scale( ( double ) width / canvasW );
		tGV.set( tGV.get( 0, 3 ) + width / 2, 0, 3 );
		tGV.set( tGV.get( 1, 3 ) + height / 2, 1, 3 );

		final AffineTransform3D affine = new AffineTransform3D();

		// get voxel width transformed to current viewer coordinates
		final AffineTransform3D tSV = new AffineTransform3D();
		renderState.getSources().get( 0 ).getSpimSource().getSourceTransform( 0, 0, tSV );
		tSV.preConcatenate( tGV );
		final double[] sO = new double[] { 0, 0, 0 };
		final double[] sX = new double[] { 1, 0, 0 };
		final double[] vO = new double[ 3 ];
		final double[] vX = new double[ 3 ];
		tSV.apply( sO, vO );
		tSV.apply( sX, vX );
		LinAlgHelpers.subtract( vO, vX, vO );
		final double dd = LinAlgHelpers.length( vO );

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget
		{
			final ARGBScreenImage accumulated = new ARGBScreenImage( width, height );

			final RenderResult renderResult = new RenderResult();

			public void clear()
			{
				for ( final ARGBType acc : accumulated )
					acc.setZero();
			}

			@Override
			public RenderResult getReusableRenderResult()
			{
				return renderResult;
			}

			@Override
			public void setRenderResult( final RenderResult renderResult )
			{
				final BufferedImage bufferedImage = renderResult.getBufferedImage();
				final Img< ARGBType > argbs = ArrayImgs.argbs( ( ( DataBufferInt ) bufferedImage.getData().getDataBuffer() ).getData(), width, height );
				final Cursor< ARGBType > c = argbs.cursor();
				for ( final ARGBType acc : accumulated )
				{
					final int current = acc.get();
					final int in = c.next().get();
					acc.set( ARGBType.rgba(
							Math.max( ARGBType.red( in ), ARGBType.red( current ) ),
							Math.max( ARGBType.green( in ), ARGBType.green( current ) ),
							Math.max( ARGBType.blue( in ), ARGBType.blue( current ) ),
							Math.max( ARGBType.alpha( in ), ARGBType.alpha( current ) )	) );
				}
			}

			@Override
			public final int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );
		progressWriter.setProgress( 0 );
		for ( int timepoint = minTimepointIndex; timepoint <= maxTimepointIndex; ++timepoint )
		{
			target.clear();
			renderState.setCurrentTimepoint( timepoint );

			for ( int step = 0; step < numSteps; ++step )
			{
				affine.set(
						1, 0, 0, 0,
						0, 1, 0, 0,
						0, 0, 1, -dd * stepSize * step );
				affine.concatenate( tGV );
				renderState.setViewerTransform( affine );
				renderer.requestRepaint();
				renderer.paint( renderState );
			}

			final BufferedImage bi = target.accumulated.image();

			if ( Prefs.showScaleBarInMovie() )
			{
				final Graphics2D g2 = bi.createGraphics();
				g2.setClip( 0, 0, width, height );
				scalebar.setViewerState( renderState );
				scalebar.paint( g2 );
			}

			ImageIO.write( bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
			progressWriter.setProgress( ( double ) (timepoint - minTimepointIndex + 1) / (maxTimepointIndex - minTimepointIndex + 1) );
		}
	}

	@Override
	public void drawOverlays( final Graphics g )
	{}

	@Override
	public void setCanvasSize( final int width, final int height )
	{
		spinnerWidth.setValue( width );
		spinnerHeight.setValue( height );
	}
}
