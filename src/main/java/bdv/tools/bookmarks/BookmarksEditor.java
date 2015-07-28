package bdv.tools.bookmarks;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.util.Affine3DHelpers;
import bdv.viewer.InputActionBindings;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;

public class BookmarksEditor
{
	static enum Mode
	{
		INACTIVE,
		SET,
		RECALL_TRANSFORM,
		RECALL_ORIENTATION
	}

	private Mode mode = Mode.INACTIVE;

	private volatile boolean initialKey = false;

	private final ViewerPanel viewer;

	private final InputActionBindings bindings;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	private final Bookmarks bookmarks;

	private BookmarkTextOverlayAnimator animator;

	public BookmarksEditor( final ViewerPanel viewer, final InputActionBindings inputActionBindings, final Bookmarks bookmarks )
	{
		this.viewer = viewer;
		bindings = inputActionBindings;
		this.bookmarks = bookmarks;

		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new AbstractAction( "abort bookmark" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};
		actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort bookmark", abortAction );
		inputMap.put( abortKey, "abort bookmark" );
		bindings.addActionMap( "bookmarks", actionMap );

		viewer.getDisplay().addKeyListener( new KeyAdapter()
		{
			@Override
			public void keyTyped( final KeyEvent e )
			{
				if ( mode != Mode.INACTIVE )
				{
					if ( initialKey )
						initialKey = false;
					else
					{
						final String key = String.valueOf( e.getKeyChar() );
						switch ( mode )
						{
						case SET:
						{
							final AffineTransform3D t = new AffineTransform3D();
							viewer.getState().getViewerTransform( t );
							final double cX = viewer.getDisplay().getWidth() / 2.0;
							final double cY = viewer.getDisplay().getHeight() / 2.0;
							t.set( t.get( 0, 3 ) - cX, 0, 3 );
							t.set( t.get( 1, 3 ) - cY, 1, 3 );
							bookmarks.put( key, t );
							animator.fadeOut( "set bookmark: " + key, 500 );
							viewer.requestRepaint();
						}
							break;
						case RECALL_TRANSFORM:
						{
							final AffineTransform3D t = bookmarks.get( key );
							if ( t != null )
							{
								final AffineTransform3D c = new AffineTransform3D();
								viewer.getState().getViewerTransform( c );
								final double cX = viewer.getDisplay().getWidth() / 2.0;
								final double cY = viewer.getDisplay().getHeight() / 2.0;
								c.set( c.get( 0, 3 ) - cX, 0, 3 );
								c.set( c.get( 1, 3 ) - cY, 1, 3 );
								viewer.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 3000 ) );
							}
							animator.fadeOut( "go to bookmark: " + key, 500 );
						}
							break;
						case RECALL_ORIENTATION:
						{
							final AffineTransform3D t = bookmarks.get( key );
							if ( t != null )
							{
								final AffineTransform3D c = new AffineTransform3D();
								viewer.getState().getViewerTransform( c );
								final Point p = new Point( 2 );
								viewer.getMouseCoordinates( p );
								final double[] qTarget = new double[ 4 ];
								Affine3DHelpers.extractRotation( t, qTarget );
								viewer.setTransformAnimator(
										new RotationAnimator( c, p.getDoublePosition( 0 ), p.getDoublePosition( 1 ), qTarget, 300 ) );
							}
							animator.fadeOut( "go to bookmark orientation: " + key, 500 );
						}
							break;
						default:
							break;
						}
						done();
					}
				}
			}
		} );
	}

	public synchronized void abort()
	{
		if ( animator != null )
			animator.clear();
		done();
	}

	protected synchronized void init( final Mode mode, final String message )
	{
		initialKey = true;
		this.mode = mode;
		bindings.addInputMap( "bookmarks", inputMap, "bdv", "navigation" );
		if ( animator != null )
			animator.clear();
		animator = new BookmarkTextOverlayAnimator( viewer );
		viewer.addOverlayAnimator( animator );
		animator.fadeIn( message, 100 );
	}

	public synchronized void initSetBookmark()
	{
		init( Mode.SET, "set bookmark: " );
	}

	public synchronized void initGoToBookmark()
	{
		init( Mode.RECALL_TRANSFORM, "go to bookmark: " );
	}

	public void initGoToBookmarkRotation()
	{
		init( Mode.RECALL_ORIENTATION, "go to bookmark orientation: " );
	}

	public synchronized void done()
	{
		mode = Mode.INACTIVE;
		initialKey = false;
		bindings.removeInputMap( "bookmarks" );
	}
}
