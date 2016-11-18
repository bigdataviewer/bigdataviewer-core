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
package bdv.tools.bookmarks;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.util.Affine3DHelpers;
import bdv.viewer.TimePointListener;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.RotationAnimator;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;

public class BookmarksEditor
{
	static enum Mode
	{
		INACTIVE,
		SET,
		CREATE_DYNAMIC_BOOKMARK,
		RECALL_TRANSFORM,
		RECALL_ORIENTATION,
	}

	private Mode mode = Mode.INACTIVE;

	private volatile boolean initialKey = false;

	private final ArrayList< String  > inputMapsToBlock;

	private final ViewerPanel viewer;

	private final InputActionBindings bindings;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	private BookmarkTextOverlayAnimator animator;

	public BookmarksEditor( final ViewerPanel viewer, final InputActionBindings inputActionBindings, final Bookmarks bookmarks )
	{
		this.viewer = viewer;
		bindings = inputActionBindings;
		inputMapsToBlock = new ArrayList<>( Arrays.asList( "bdv", "navigation" ) );

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
							
							SimpleBookmark bookmark = new SimpleBookmark(key, t);
							bookmarks.put( bookmark );
							
							animator.fadeOut( "set bookmark: " + key, 500 );
							viewer.requestRepaint();
						}
							break;
						case CREATE_DYNAMIC_BOOKMARK:
						{				
							DynamicBookmark bookmark = new DynamicBookmark(key);
							bookmarks.put( bookmark );
							
							animator.fadeOut( "create dynamic bookmark: " + key, 500 );
							viewer.requestRepaint();
						}
							break;
						case RECALL_TRANSFORM:
						{
							final SimpleBookmark simpleBookmark = bookmarks.getSimpleBookmark(key);
							final DynamicBookmark dynamicBookmark = bookmarks.getDynamicBookmark(key);
							if(simpleBookmark != null){
								final AffineTransform3D t = simpleBookmark.getAffineTransform3D();
								if ( t != null )
								{
									final AffineTransform3D c = new AffineTransform3D();
									viewer.getState().getViewerTransform( c );
									final double cX = viewer.getDisplay().getWidth() / 2.0;
									final double cY = viewer.getDisplay().getHeight() / 2.0;
									c.set( c.get( 0, 3 ) - cX, 0, 3 );
									c.set( c.get( 1, 3 ) - cY, 1, 3 );
									viewer.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) );
								}
							} else if(dynamicBookmark != null){
								int currentTimepoint = viewer.getState().getCurrentTimepoint();
								final int previousTimepoint = dynamicBookmark.getPreviousTimepoint(currentTimepoint);
								final int nextTimepoint = dynamicBookmark.getNextTimepoint(currentTimepoint);
								
								AffineTransform3D previousTransform = dynamicBookmark.getTransform(previousTimepoint);
								AffineTransform3D nextTransform = dynamicBookmark.getTransform(nextTimepoint);
								
								if(previousTransform == null){
									previousTransform = new AffineTransform3D();
									viewer.getState().getViewerTransform( previousTransform );
								}
								
								if(nextTransform == null){
									nextTransform = previousTransform;
								}
								
								
								final double centerX = viewer.getDisplay().getWidth() / 2.0;
								final double centerY = viewer.getDisplay().getHeight() / 2.0;
								
								SimilarityTransformAnimator transAnimator = new SimilarityTransformAnimator( previousTransform, nextTransform, centerX, centerY, nextTimepoint - previousTimepoint );
								transAnimator.setTime(0);
								AffineTransform3D targetTransform = transAnimator.getCurrent(currentTimepoint - previousTimepoint);
								
								if ( targetTransform != null )
								{
									final AffineTransform3D viewTransform = new AffineTransform3D();
									viewer.getState().getViewerTransform( viewTransform );
									viewTransform.set( viewTransform.get( 0, 3 ) - centerX, 0, 3 );
									viewTransform.set( viewTransform.get( 1, 3 ) - centerY, 1, 3 );
									targetTransform.set( targetTransform.get( 0, 3 ) - centerX, 0, 3 );
									targetTransform.set( targetTransform.get( 1, 3 ) - centerY, 1, 3 );
									viewer.setTransformAnimator( new SimilarityTransformAnimator( viewTransform, targetTransform, centerX, centerY, 300 ) );
								}
							}
							
							/*
							final AffineTransform3D t = bookmarks.get( key );
							if ( t != null )
							{
								final AffineTransform3D c = new AffineTransform3D();
								viewer.getState().getViewerTransform( c );
								final double cX = viewer.getDisplay().getWidth() / 2.0;
								final double cY = viewer.getDisplay().getHeight() / 2.0;
								c.set( c.get( 0, 3 ) - cX, 0, 3 );
								c.set( c.get( 1, 3 ) - cY, 1, 3 );
								viewer.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) );
							}
							*/
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
	
		viewer.addTimePointListener(new TimePointListener() {
			
			@Override
			public void timePointChanged(int timePointIndex) {
				
			}
		});
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
		bindings.addInputMap( "bookmarks", inputMap, inputMapsToBlock );
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
	
	public synchronized void initCreateDynamicBookmark()
	{
		init( Mode.CREATE_DYNAMIC_BOOKMARK, "create dynamic bookmark: " );
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

	public synchronized void setInputMapsToBlock( final Collection< String > idsToBlock )
	{
		inputMapsToBlock.clear();
		inputMapsToBlock.addAll( idsToBlock );
	}
}
