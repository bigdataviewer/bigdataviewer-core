package bdv.viewer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmarkChangedListener;
import bdv.tools.bookmarks.bookmark.KeyFrame;

public class JKeyFramePanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final DynamicBookmarkChangedListener bookmarkChangedListener = () -> repaint();

	private final JSlider timeSlider;

	private final int numTimepoints; // TODO: shouldn't be final!?

	/**
	 * My Context-Menu.
	 */
	private final KeyFramePopupMenu popupMenu = new KeyFramePopupMenu();

	/**
	 * Current dynamic bookmark or null if no bookmark is selected.
	 */
	private DynamicBookmark bookmark = null;

	/**
	 * KeyFrame-Flag (red-Line) Width.
	 */
	private static final int KF_FLAG_WIDTH = 1;

	private static final int KF_FLAG_WIDTH_HOVER = 8;

	private static final int KF_FLAG_MOUSE_RADIUS = ( KF_FLAG_WIDTH_HOVER / 2 ) + 3;

	private static final Color CL_KF_FLAG_NORMAL = Color.RED;

	private static final Color CL_KF_FLAG_HOVER = Color.BLUE;

	private KeyFrame currentHoverKeyframe = null;

	private static enum KeyFrameFlagState
	{
		NORMAL, HOVER
	}

	public JKeyFramePanel( final JSlider timeSlider )
	{
		this.timeSlider = timeSlider;

		this.numTimepoints = timeSlider.getMaximum(); // TODO: this is not numTimepoints but numTimepoints - 1 !!!

		addMouseListener( new MouseHoverEventAdapter() );
		addMouseMotionListener( new MouseHoverEventAdapter() );

		setMinimumSize( new Dimension( timeSlider.getWidth(), 26 ) );
		setPreferredSize( new Dimension( timeSlider.getWidth(), 26 ) );

		setFocusable( false );
	}

	/**
	 * Sets current bookmark and updates component (repaint).
	 *
	 * @param bookmark
	 * 		bookmark or {@code null} to reset dyn. bookmark.
	 */
	public void setDynamicBookmarks( final DynamicBookmark bookmark )
	{
		this.bookmark = bookmark;
		setCurrentHoverKeyframe( null );

		if ( this.bookmark != null )
		{
			bookmark.removeDynamicBookmarkChangedListener( bookmarkChangedListener );
		}

		this.bookmark = bookmark;

		if ( this.bookmark != null )
		{
			bookmark.addDynamicBookmarkChangedListener( bookmarkChangedListener );
		}

		repaint();
	}

	/**
	 * Returns the specific {@link KeyFramePopupMenu} of this component.
	 *
	 * @return Returns always the same instance of {@link KeyFramePopupMenu},
	 *         never {@code null}.
	 */
	public KeyFramePopupMenu getKeyFramePopupMenuPopupMenu()
	{
		return this.popupMenu;
	}

	@Override
	public void paint( final Graphics g )
	{
		super.paint( g );

		if ( null == bookmark )
		{
			return;
		}

		for ( final KeyFrame keyframe : this.bookmark.getFrameSet() )
		{
			final int posX = determineSliderXPositionOf( keyframe.getTimepoint() );

			if ( keyframe.equals( this.currentHoverKeyframe ) )
			{
				paintKeyFrameFlag( g, posX, KeyFrameFlagState.HOVER );
			}
			else
			{
				paintKeyFrameFlag( g, posX, KeyFrameFlagState.NORMAL );
			}
		}
	}

	private void paintKeyFrameFlag( final Graphics g, final int sliderPositionX, final KeyFrameFlagState flagState )
	{
		if ( flagState == KeyFrameFlagState.NORMAL )
		{
			g.setColor( CL_KF_FLAG_NORMAL );
			g.fillRect( sliderPositionX, 0, KF_FLAG_WIDTH, getHeight() );
		}
		else
		{
			g.setColor( CL_KF_FLAG_HOVER );
			g.fillRect( sliderPositionX - ( KF_FLAG_WIDTH_HOVER / 2 ), 0, KF_FLAG_WIDTH_HOVER, getHeight() );
		}
	}

	private void determineKeyFrameHoverFlag( final int inputComponentXCoord )
	{

		if ( inputComponentXCoord >= 0 && this.bookmark != null )
		{
			for ( final KeyFrame keyframe : this.bookmark.getFrameSet() )
			{
				final int anyValidPosX = determineSliderXPositionOf( keyframe.getTimepoint() );

				final int lowerBound = anyValidPosX - KF_FLAG_MOUSE_RADIUS;
				final int upperBound = anyValidPosX + KF_FLAG_MOUSE_RADIUS;

				final int mouseX = inputComponentXCoord;

				if ( mouseX >= lowerBound && mouseX <= upperBound )
				{
					setCurrentHoverKeyframe( keyframe );
					return;
				}
			}
		}

		setCurrentHoverKeyframe( null );
	}

	private int determineSliderXPositionOf( final int timepoint )
	{
		final Rectangle trackRect = getTimeSliderTrackRect();

		final double trackOffsetX = trackRect.getX();
		final double trackWidth = trackRect.getWidth();

		return ( int ) ( ( ( trackWidth / numTimepoints ) * timepoint ) + trackOffsetX );
	}

	private void setCurrentHoverKeyframe( final KeyFrame hoveredKeyFrame )
	{
		this.currentHoverKeyframe = hoveredKeyFrame;

		if ( hoveredKeyFrame != null )
			setCursor( Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR ) );
		else
			setCursor( Cursor.getDefaultCursor() );
	}

	/**
	 * Returns the {@code trackRect} of {@link BasicSliderUI} to determine the
	 * correct position of the slider thumb.
	 * <p>
	 * If the selected LookAndFeel doesn't inherit from {@link BasicSliderUI}, a
	 * fallback implementation is used instead.
	 * </p>
	 *
	 * @return Rectangle of track part - returns never {@code null}.
	 */
	private Rectangle getTimeSliderTrackRect()
	{
		final SliderUI sliderUI = timeSlider.getUI();

		final boolean fallbackNeeded = ( sliderUI instanceof BasicSliderUI == false );
		if ( fallbackNeeded )
		{
			return timeSlider.getVisibleRect();
		}

		final BasicSliderUI basicSliderUI = ( BasicSliderUI ) sliderUI;
		final Class< ? extends BasicSliderUI > uiClazz = BasicSliderUI.class;

		try
		{
			final Field trackRectField = uiClazz.getDeclaredField( "trackRect" );

			trackRectField.setAccessible( true );

			final Rectangle result = ( Rectangle ) trackRectField.get( basicSliderUI );

			if ( null == result )
			{
				return timeSlider.getVisibleRect();
			}

			return result;

		}
		catch ( final Exception ex )
		{
			return timeSlider.getVisibleRect();
		}
	}

	private class MouseHoverEventAdapter extends MouseAdapter
	{

		private boolean isPressing = false;

		@Override
		public void mouseMoved( final MouseEvent e )
		{
			updateComponent( e );
		}

		@Override
		public void mouseExited( final MouseEvent e )
		{
			if ( !isPressing )
			{
				updateComponent( e );
			}
		}

		@Override
		public void mouseEntered( final MouseEvent e )
		{
			//updateComponent(e);
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{

			if ( currentHoverKeyframe == null )
			{
				// when user drags the thumb, we need to update the hovered keyframe
				// otherwise the thumb will jump back to the currently as hovered marked keyframe
				// though the keyframe is not actually hovered
				//updateComponent(e);
			}
			else
			{
				final Rectangle trackRect = getTimeSliderTrackRect();

				if ( e.getX() < trackRect.x || e.getX() > trackRect.x + trackRect.getWidth() )
					return;

				final int t = ( int ) ( ( e.getX() - trackRect.x ) / trackRect.getWidth() * numTimepoints );
				final KeyFrame updatedKeyFrame = bookmark.updateWithoutOverride( currentHoverKeyframe, t );
				if ( updatedKeyFrame != null )
				{
					setCurrentHoverKeyframe( updatedKeyFrame );
				}
				timeSlider.setValue( t );
			}
		}

		private void updateComponent( final MouseEvent event )
		{
			if ( event.getY() < 0 || event.getY() >= getHeight() )
			{
				// when mouse leaves slider at the top or bottom
				popupMenu.setVisible( false );
				determineKeyFrameHoverFlag( -1 );
			}
			else
			{
				if ( !popupMenu.isShowing() )
				{
					determineKeyFrameHoverFlag( event.getX() );
				}
			}

			SwingUtilities.invokeLater( JKeyFramePanel.this::repaint );
		}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			isPressing = false;
			maybeTriggerPopupMenu( e );

			if ( SwingUtilities.isLeftMouseButton( e ) && e.getClickCount() == 1 )
			{
				if ( currentHoverKeyframe != null )
				{
					timeSlider.setValue( currentHoverKeyframe.getTimepoint() );
				}
			}
		}

		@Override
		public void mousePressed( final MouseEvent e )
		{
			isPressing = true;
			maybeTriggerPopupMenu( e );
		}

		private void maybeTriggerPopupMenu( final MouseEvent event )
		{
			if ( event.isPopupTrigger() )
			{
				popupMenu.setKeyFrameFlagSelected( currentHoverKeyframe );
				popupMenu.show( JKeyFramePanel.this, event.getX(), event.getY() );
			}
		}
	}

}
