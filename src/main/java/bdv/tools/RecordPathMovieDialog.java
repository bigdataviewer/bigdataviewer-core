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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.tools.bookmarks.Bookmarks;
import bdv.util.Prefs;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;

public class RecordPathMovieDialog extends JDialog implements OverlayRenderer
{
	private static final String CURRENT = "current";

	private static final long serialVersionUID = 1L;

	private final ViewerPanel viewer;

	private final Bookmarks bookmarks;

	private final int maxTimepoint;

	private final ProgressWriter progressWriter;

	private final JTextField pathTextField;

	private final JSpinner spinnerMaxTimepoint;

//	private final JSpinner spinnerWidth;
//
//	private final JSpinner spinnerHeight;

	private final JComboBox< String > startMark;

	private final JComboBox< String > endMark;

	public RecordPathMovieDialog( final Frame owner, final ViewerPanel viewer, final Bookmarks bookmarks, final ProgressWriter progressWriter )
	{
		super( owner, "record movie", false );
		this.viewer = viewer;
		this.bookmarks = bookmarks;
		maxTimepoint = viewer.getState().getNumTimepoints() - 1;
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

		final JPanel bookmarksPanel = new JPanel();
		bookmarksPanel.setLayout( new GridLayout( 2, 2 ) );
		boxes.add( bookmarksPanel );

		bookmarksPanel.add( new JLabel( "start position (bookmark): " ) );
		startMark = new JComboBox<>();
		startMark.setVisible( true );
		bookmarksPanel.add( startMark );

		bookmarksPanel.add( new JLabel( "end position (bookmark): " ) );
		endMark = new JComboBox<>();
		endMark.setVisible( true );
		bookmarksPanel.add( endMark );

		final JPanel timepointsPanel = new JPanel();
		boxes.add( timepointsPanel );

		timepointsPanel.add( new JLabel( "number of frames: " ) );

		spinnerMaxTimepoint = new JSpinner();
		spinnerMaxTimepoint.setModel( new SpinnerNumberModel( 100, 0, Integer.MAX_VALUE, 1 ) );
		timepointsPanel.add( spinnerMaxTimepoint );

//		final JPanel widthPanel = new JPanel();
//		boxes.add( widthPanel );
//		widthPanel.add( new JLabel( "width" ) );
//		spinnerWidth = new JSpinner();
//		spinnerWidth.setModel( new SpinnerNumberModel( 800, 10, 5000, 1 ) );
//		widthPanel.add( spinnerWidth );
//
//		final JPanel heightPanel = new JPanel();
//		boxes.add( heightPanel );
//		heightPanel.add( new JLabel( "height" ) );
//		spinnerHeight = new JSpinner();
//		spinnerHeight.setModel( new SpinnerNumberModel( 600, 10, 5000, 1 ) );
//		heightPanel.add( spinnerHeight );

		final JPanel buttonsPanel = new JPanel();
		boxes.add( buttonsPanel );
		buttonsPanel.setLayout(new BorderLayout(0, 0));

		final JButton recordButton = new JButton( "Record" );
		buttonsPanel.add( recordButton, BorderLayout.EAST );

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
				final int maxTimepointIndex = ( Integer ) spinnerMaxTimepoint.getValue();
//				final int width = ( Integer ) spinnerWidth.getValue();
//				final int height = ( Integer ) spinnerHeight.getValue();
				new Thread()
				{
					@Override
					public void run()
					{
						try
						{
							recordButton.setEnabled( false );
							recordMovie( maxTimepointIndex,
									getTransform( startMark, viewer ),
									getTransform( endMark, viewer ),
									dir );
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

	private AffineTransform3D getTransform( final JComboBox<String> choice, final ViewerPanel viewer )
	{
		String key = choice.getItemAt( choice.getSelectedIndex());
		if( key.equals( CURRENT ))
		{
			final double cX = viewer.getDisplay().getWidth() / 2.0;
			final double cY = viewer.getDisplay().getHeight() / 2.0;

			AffineTransform3D t = new AffineTransform3D();
			viewer.getState().getViewerTransform( t );
			t.set( t.get( 0, 3 ) - cX, 0, 3 );
			t.set( t.get( 1, 3 ) - cY, 1, 3 );
			return t;
		}
		else
			return bookmarks.get( key );
	}

	@Override
	public void setVisible( boolean visible )
	{
		super.setVisible( visible );

		if( visible )
		{
			startMark.removeAllItems();
			endMark.removeAllItems();

			startMark.addItem( "current" );
			endMark.addItem( "current" );
			for( String mark : bookmarks.keySet() )
			{
				System.out.println( "bookmark: " + mark );
				startMark.addItem( mark );
				endMark.addItem( mark );
			}
		}
	}

	public void recordMovie( final int maxTimepointIndex, final AffineTransform3D start, final AffineTransform3D end, final File dir ) throws IOException
	{
		final ViewerState renderState = viewer.getState();

//		final int canvasW = viewer.getDisplay().getWidth();
//		final int canvasH = viewer.getDisplay().getHeight();

		final int width = viewer.getDisplay().getWidth();
		final int height = viewer.getDisplay().getHeight();

		final double cX = width / 2.0;
		final double cY = height / 2.0;

		renderState.setViewerTransform( start );

		final ScaleBarOverlayRenderer scalebar = Prefs.showScaleBarInMovie() ? new ScaleBarOverlayRenderer() : null;

		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return width;
			}

			@Override
			public int getHeight()
			{
				return height;
			}
		}
		final SimilarityTransformAnimator animator = new SimilarityTransformAnimator( start, end, cX, cY, maxTimepointIndex ); 
		final MyTarget target = new MyTarget();
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
				target, new PainterThread( null ), new double[] { 1 }, 0, false, 1, null, false,
				viewer.getOptionValues().getAccumulateProjectorFactory(), new CacheControl.Dummy() );
		progressWriter.setProgress( 0 );
		for ( int timepoint = 0; timepoint <= maxTimepointIndex; ++timepoint )
		{
			double t = ((double) timepoint ) / maxTimepointIndex;
			renderState.setViewerTransform( animator.get( t ) );

			renderer.requestRepaint();
			renderer.paint( renderState );

			if ( Prefs.showScaleBarInMovie() )
			{
				final Graphics2D g2 = target.bi.createGraphics();
				g2.setClip( 0, 0, width, height );
				scalebar.setViewerState( renderState );
				scalebar.paint( g2 );
			}

			ImageIO.write( target.bi, "png", new File( String.format( "%s/img-%03d.png", dir, timepoint ) ) );
			progressWriter.setProgress( t );
		}
	}

	@Override
	public void drawOverlays( final Graphics g )
	{}

	@Override
	public void setCanvasSize( final int width, final int height )
	{}
}
