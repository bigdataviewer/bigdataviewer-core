package bdv.tools.bookmarks;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.InputActionBindings;
import bdv.viewer.ViewerPanel;

public class BookmarkEditor
{
	private boolean active = false;

	private boolean setBookmark = false;

	private final ViewerPanel viewer;

	private final InputActionBindings bindings;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	private final HashMap< String, AffineTransform3D > bookmarks;

	private BookmarkTextOverlayAnimator animator;

	public BookmarkEditor( final ViewerPanel viewer, final InputActionBindings inputActionBindings )
	{
		this.viewer = viewer;
		bindings = inputActionBindings;
		bookmarks = new HashMap< String, AffineTransform3D >();

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
				if ( active && e.getKeyChar() != 'b' && e.getKeyChar() != 'B' )
				{
					final String key = String.valueOf( e.getKeyChar() );
					if ( setBookmark )
					{
						final AffineTransform3D t = new AffineTransform3D();
						viewer.getState().getViewerTransform( t );
						bookmarks.put( key, t );
						animator.fadeOut( "set bookmark: " + key, 500 );
						viewer.requestRepaint();
					}
					else
					{
						final AffineTransform3D t = bookmarks.get( key );
						if ( t != null )
							viewer.setCurrentViewerTransform( t );
						animator.fadeOut( "go to bookmark: " + key, 500 );
					}
					done();
				}
			}
		} );
	}

	public synchronized void abort()
	{
		if ( animator != null )
			animator.clear();
		if ( active )
			done();
	}

	public synchronized void initSetBookmark()
	{
		active = true;
		setBookmark = true;
		bindings.addInputMap( "bookmarks", inputMap, "bdv", "navigation" );
		if ( animator != null )
			animator.clear();
		animator = new BookmarkTextOverlayAnimator( viewer );
		viewer.addOverlayAnimator( animator );
		animator.fadeIn( "set bookmark: ", 100 );
	}

	public synchronized void initGoToBookmark()
	{
		active = true;
		setBookmark = false;
		bindings.addInputMap( "bookmarks", inputMap, "bdv", "navigation" );
		if ( animator != null )
			animator.clear();
		animator = new BookmarkTextOverlayAnimator( viewer );
		viewer.addOverlayAnimator( animator );
		animator.fadeIn( "go to bookmark: ", 100 );
	}

	public synchronized void done()
	{
		active = false;
		bindings.removeInputMap( "bookmarks" );
	}
}
