package viewer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib.ui.OverlayRenderer;
import net.imglib.ui.PainterThread;
import net.imglib.ui.component.InteractiveDisplay3DCanvas;
import net.imglib2.Pair;
import net.imglib2.Positionable;
import net.imglib2.RealPoint;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.TransformListener3D;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.util.ValuePair;
import viewer.TextOverlayAnimator.TextPosition;
import viewer.refactor.Interpolation;
import viewer.refactor.MultiResolutionRenderer;
import viewer.refactor.SpimSourceState;
import viewer.refactor.SpimViewerState;
import viewer.refactor.overlay.MultiBoxOverlayRenderer;
import viewer.refactor.overlay.SourceInfoOverlayRenderer;

public class SpimViewer implements OverlayRenderer, TransformListener3D, PainterThread.Paintable
{
	protected SpimViewerState state;

	protected MultiResolutionRenderer imageRenderer;

	protected MultiBoxOverlayRenderer multiBoxOverlayRenderer;

	protected SourceInfoOverlayRenderer sourceInfoOverlayRenderer;

	/**
	 * Transformation set by the interactive viewer.
	 */
	final protected AffineTransform3D viewerTransform;

	/**
	 * Canvas used for displaying the rendered {@link #screenImages screen image}.
	 */
	final protected InteractiveDisplay3DCanvas display;

	/**
	 * Thread that triggers repainting of the display.
	 */
	final protected PainterThread painterThread;

	final protected JFrame frame;

	final protected JSlider sliderTime;

	final protected MouseCoordinateListener mouseCoordinates;

	final protected ArrayList< Pair< KeyStroke, Action > > keysActions;

	/**
	 *
	 * @param width
	 *            width of the display window.
	 * @param height
	 *            height of the display window.
	 * @param sources
	 *            the {@link SpimSourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param numMipmapLevels
	 *            number of available mipmap levels.
	 */
	public SpimViewer( final int width, final int height, final Collection< SpimSourceAndConverter< ? > > sources, final int numTimePoints, final int numMipmapLevels )
	{
		state = new SpimViewerState( sources, numTimePoints, numMipmapLevels );
		if ( ! sources.isEmpty() )
			state.setCurrentSource( 0 );
		multiBoxOverlayRenderer = new MultiBoxOverlayRenderer( width, height );
		sourceInfoOverlayRenderer = new SourceInfoOverlayRenderer();

		painterThread = new PainterThread( this );
		viewerTransform = new AffineTransform3D();
		display = new InteractiveDisplay3DCanvas( width, height, this, this );

		final double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };
		final long targetRenderNanos = 30 * 1000000;
		final long targetIoNanos = 10 * 1000000;
		final int badIoFrameBlockFrames = 5;
		imageRenderer = new MultiResolutionRenderer( display, painterThread, screenScales, numMipmapLevels, targetRenderNanos, targetIoNanos, badIoFrameBlockFrames );

		mouseCoordinates = new MouseCoordinateListener() ;
		display.addHandler( mouseCoordinates );

//		final SourceSwitcher sourceSwitcher = new SourceSwitcher();
//		display.addKeyListener( sourceSwitcher );

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, numTimePoints - 1, 0 );
//		sliderTime.addKeyListener( display.getTransformEventHandler() );
//		sliderTime.addKeyListener( sourceSwitcher );
		sliderTime.addChangeListener( new ChangeListener() {
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( e.getSource().equals( sliderTime ) )
					updateTimepoint( sliderTime.getValue() );
			}
		} );

//		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( ARGBScreenImage.ARGB_COLOR_MODEL );
		final GraphicsConfiguration gc = GuiHelpers.getSuitableGraphicsConfiguration( GuiHelpers.RGB_COLOR_MODEL );
		frame = new JFrame( "multi-angle viewer", gc );
		frame.getRootPane().setDoubleBuffered( true );
		final Container content = frame.getContentPane();
		content.add( display, BorderLayout.CENTER );
		content.add( sliderTime, BorderLayout.SOUTH );
		frame.pack();
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				painterThread.interrupt();
			}
		} );
//		frame.addKeyListener( display.getTransformEventHandler() );
//		frame.addKeyListener( sourceSwitcher );
		frame.setVisible( true );

		keysActions = new ArrayList< Pair< KeyStroke, Action > >();
		createKeyActions();
		installKeyActions( frame );

		painterThread.start();

		animatedOverlay = new TextOverlayAnimator( "Press <F1> for help.", 3000, TextPosition.CENTER );
	}

	public void addHandler( final Object handler )
	{
		display.addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
			frame.addKeyListener( ( KeyListener ) handler );
	}

	public void getGlobalMouseCoordinates( final RealPositionable gPos )
	{
		assert gPos.numDimensions() == 3;
		final RealPoint lPos = new RealPoint( 3 );
		mouseCoordinates.getMouseCoordinates( lPos );
		viewerTransform.applyInverse( gPos, lPos );
	}

	@Override
	public void paint()
	{
		imageRenderer.paint( state );

		synchronized( this )
		{
			if ( currentAnimator != null )
			{
				final TransformEventHandler3D handler = display.getTransformEventHandler();
				final AffineTransform3D transform = currentAnimator.getCurrent();
				handler.setTransform( transform );
				transformChanged( transform );
				if ( currentAnimator.isComplete() )
					currentAnimator = null;
			}
		}

		display.repaint();
	}

	// TODO remove?
	public void requestRepaint()
	{
		imageRenderer.requestRepaint();
	}

	TextOverlayAnimator animatedOverlay = null;

	@Override
	public void drawOverlays( final Graphics g )
	{
		multiBoxOverlayRenderer.setViewerState( state );
		multiBoxOverlayRenderer.updateVirtualScreenSize( display.getWidth(), display.getHeight() );
		multiBoxOverlayRenderer.paint( ( Graphics2D ) g );

		sourceInfoOverlayRenderer.setViewerState( state );
		sourceInfoOverlayRenderer.paint( ( Graphics2D ) g );

		final RealPoint gPos = new RealPoint( 3 );
		getGlobalMouseCoordinates( gPos );
		final String mousePosGlobalString = String.format( "(%6.1f,%6.1f,%6.1f)", gPos.getDoublePosition( 0 ), gPos.getDoublePosition( 1 ), gPos.getDoublePosition( 2 ) );

		g.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
		g.drawString( mousePosGlobalString, ( int ) g.getClipBounds().getWidth() - 170, 25 );

		if ( animatedOverlay != null )
		{
			animatedOverlay.paint( ( Graphics2D ) g );
			if ( animatedOverlay.isComplete() )
				animatedOverlay = null;
			else
				display.repaint();
		}
		if ( multiBoxOverlayRenderer.isHighlightInProgress() )
			display.repaint();
	}

	@Override
	public synchronized void transformChanged( final AffineTransform3D transform )
	{
		viewerTransform.set( transform );
		state.setViewerTransform( transform );
		requestRepaint();
	}

	RotationAnimator currentAnimator = null;

	static enum AlignPlane
	{
		XY,
		ZY,
		XZ
	}

	private final static double c = Math.cos( Math.PI / 4 );
	private final static double[] qAlignXY = new double[] { 1,  0,  0, 0 };
	private final static double[] qAlignZY = new double[] { c,  0, -c, 0 };
	private final static double[] qAlignXZ = new double[] { c,  c,  0, 0 };

	protected synchronized void align( final AlignPlane plane )
	{
		final SpimSourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform( state.getCurrentTimepoint(), 0 );

		final double[] qSource = new double[ 4 ];
		RotationAnimator.extractRotationAnisotropic( sourceTransform, qSource );

		final double[] qTmpSource;
		if ( plane == AlignPlane.XY )
			qTmpSource = qSource;
		else
		{
			qTmpSource = new double[4];
			if ( plane == AlignPlane.ZY )
				LinAlgHelpers.quaternionMultiply( qSource, qAlignZY, qTmpSource );
			else // if ( plane == AlignPlane.XZ )
				LinAlgHelpers.quaternionMultiply( qSource, qAlignXZ, qTmpSource );
		}

		final double[] qTarget = new double[ 4 ];
		LinAlgHelpers.quaternionInvert( qTmpSource, qTarget );

		final AffineTransform3D transform = display.getTransformEventHandler().getTransform();
		currentAnimator = new RotationAnimator( transform, mouseCoordinates.getX(), mouseCoordinates.getY(), qTarget, 300 );
		transformChanged( transform );
	}

	protected synchronized void updateTimepoint( final int timepoint )
	{
		if ( state.getCurrentTimepoint() != timepoint )
		{
			state.setCurrentTimepoint( timepoint );
			requestRepaint();
		}
	}

	final int indicatorTime = 800;

	protected synchronized void toggleInterpolation()
	{
		final Interpolation interpolation = state.getInterpolation();
		if ( interpolation == Interpolation.NEARESTNEIGHBOR )
		{
			state.setInterpolation( Interpolation.NLINEAR );
			animatedOverlay = new TextOverlayAnimator( "tri-linear interpolation", indicatorTime );
		}
		else
		{
			state.setInterpolation( Interpolation.NEARESTNEIGHBOR );
			animatedOverlay = new TextOverlayAnimator( "nearest-neighbor interpolation", indicatorTime );
		}
		requestRepaint();
	}

	public synchronized void toggleSingleSourceMode()
	{
		final boolean singleSourceMode = ! state.isSingleSourceMode();
		state.setSingleSourceMode( singleSourceMode );
		animatedOverlay = new TextOverlayAnimator( singleSourceMode ? "single-source mode" : "fused mode", indicatorTime );
		requestRepaint();
	}

	public synchronized void toggleVisibility( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < state.numSources() )
		{
			final SpimSourceState< ? > source = state.getSources().get( sourceIndex );
			source.setActive( !source.isActive() );
			multiBoxOverlayRenderer.highlight( sourceIndex );
			if ( ! state.isSingleSourceMode() )
				requestRepaint();
			else
				display.repaint();
		}
	}

	/**
	 * Set the index of the source to display.
	 */
	public synchronized void setCurrentSource( final int sourceIndex )
	{
		if ( sourceIndex >= 0 && sourceIndex < state.numSources() )
		{
			state.setCurrentSource( sourceIndex );
			multiBoxOverlayRenderer.highlight( sourceIndex );
			if ( state.isSingleSourceMode() )
				requestRepaint();
			else
				display.repaint();
		}
	}

	/**
	 * Create Keystrokes and corresponding Actions.
	 *
	 * @return list of KeyStroke-Action-pairs.
	 */
	protected void createKeyActions()
	{
		KeyStroke key = KeyStroke.getKeyStroke( KeyEvent.VK_I, 0 );
		Action action = new AbstractAction( "toogle interpolation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				toggleInterpolation();
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		key = KeyStroke.getKeyStroke( KeyEvent.VK_F, 0 );
		action = new AbstractAction( "toogle display mode" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				toggleSingleSourceMode();
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		final int[] numkeys = new int[] { KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_0 };

		for ( int i = 0; i < numkeys.length; ++i )
		{
			final int index = i;

			key = KeyStroke.getKeyStroke( numkeys[ i ], 0 );
			action = new AbstractAction( "set current source " + i )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					setCurrentSource( index );
				}

				private static final long serialVersionUID = 1L;
			};
			keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

			key = KeyStroke.getKeyStroke( numkeys[ i ], KeyEvent.SHIFT_DOWN_MASK );
			action = new AbstractAction( "toggle source visibility " + i )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					toggleVisibility( index );
				}

				private static final long serialVersionUID = 1L;
			};
			keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );
		}

		key = KeyStroke.getKeyStroke( KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK );
		action = new AbstractAction( "align XY plane" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				align( AlignPlane.XY );
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		key = KeyStroke.getKeyStroke( KeyEvent.VK_X, KeyEvent.SHIFT_DOWN_MASK );
		action = new AbstractAction( "align ZY plane" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				align( AlignPlane.ZY );
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		key = KeyStroke.getKeyStroke( KeyEvent.VK_Y, KeyEvent.SHIFT_DOWN_MASK );
		action = new AbstractAction( "align XZ plane" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				align( AlignPlane.XZ );
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );
		key = KeyStroke.getKeyStroke( KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK );
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		key = KeyStroke.getKeyStroke( KeyEvent.VK_CLOSE_BRACKET, 0, false );
		action = new AbstractAction( "next timepoint" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				sliderTime.setValue( sliderTime.getValue() + 1 );
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );
		key = KeyStroke.getKeyStroke( KeyEvent.VK_M, 0 );
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );

		key = KeyStroke.getKeyStroke( KeyEvent.VK_OPEN_BRACKET, 0, false );
		action = new AbstractAction( "previous timepoint" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				sliderTime.setValue( sliderTime.getValue() - 1 );
			}

			private static final long serialVersionUID = 1L;
		};
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );
		key = KeyStroke.getKeyStroke( KeyEvent.VK_N, 0 );
		keysActions.add( new ValuePair< KeyStroke, Action >( key, action ) );
	}

	public void addKeyAction( final KeyStroke keystroke, final Action action )
	{
		keysActions.add( new ValuePair< KeyStroke, Action >( keystroke, action ) );
		installKeyActions( frame );
	}

	/**
	 * Add Keystrokes and corresponding Actions from {@link #keysActions} to a container.
	 */
	public void installKeyActions( final RootPaneContainer container )
	{
		final JRootPane rootpane = container.getRootPane();
		final ActionMap am = rootpane.getActionMap();
		final InputMap im = rootpane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		for ( final Pair< KeyStroke, Action > keyAction : keysActions )
		{
			final KeyStroke key = keyAction.getA();
			final Action action = keyAction.getB();
			im.put( key, action.getValue( Action.NAME ) );
			am.put( action.getValue( Action.NAME ), action );
		}
	}

	protected class MouseCoordinateListener implements MouseMotionListener
	{
		private int x;

		private int y;

		public synchronized void getMouseCoordinates( final Positionable p )
		{
			p.setPosition( x, 0 );
			p.setPosition( y, 1 );
		}

		@Override
		public synchronized void mouseDragged( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
		}

		@Override
		public synchronized void mouseMoved( final MouseEvent e )
		{
			x = e.getX();
			y = e.getY();
			display.repaint(); // TODO: only when overlays are visible
		}

		public synchronized int getX()
		{
			return x;
		}

		public synchronized int getY()
		{
			return y;
		}
	}
}
