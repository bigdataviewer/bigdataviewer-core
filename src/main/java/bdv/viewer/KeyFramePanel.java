package bdv.viewer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmarkChangedListener;
import bdv.tools.bookmarks.bookmark.KeyFrame;

public class KeyFramePanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final DynamicBookmarkChangedListener bookmarkChangedListener = this::repaint;

	private final JSlider timeSlider;

	private final BasicSliderUIAccess timeSliderUI;

	/**
	 * My Context-Menu.
	 */
	private final KeyFramePopupMenu popupMenu = new KeyFramePopupMenu();

	/**
	 * Current dynamic bookmark or null if no bookmark is selected.
	 */
	private DynamicBookmark bookmark = null;

	private KeyFrame currentHoverKeyframe = null;

	/**
	 * KeyFrame-Flag (red-Line) Width.
	 */
	private static final int KF_FLAG_WIDTH = 1;

	private static final int KF_FLAG_WIDTH_HOVER = 8;

	private static final int KF_FLAG_MOUSE_RADIUS = ( KF_FLAG_WIDTH_HOVER / 2 ) + 3;

	private static final Color CL_KF_FLAG_NORMAL = Color.RED;

	private static final Color CL_KF_FLAG_HOVER = Color.BLUE;

	private enum KeyFrameFlagState
	{
		NORMAL, HOVER
	}

	public KeyFramePanel( final JSlider timeSlider )
	{
		this.timeSlider = timeSlider;
		this.timeSliderUI = new BasicSliderUIAccess( timeSlider );

		popupMenu.addPopupMenuListener( new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible( final PopupMenuEvent e )
			{}

			@Override
			public void popupMenuWillBecomeInvisible( final PopupMenuEvent e )
			{
				setCurrentHoverKeyframe( null );
			}

			@Override
			public void popupMenuCanceled( final PopupMenuEvent e )
			{}
		} );

		addMouseListener( new MouseHoverEventAdapter() );
		addMouseMotionListener( new MouseHoverEventAdapter() );
		setFocusable( false );
	}

	/**
	 * Sets current bookmark and updates component (repaint).
	 *
	 * @param b
	 *            bookmark or {@code null} to reset dyn. bookmark.
	 */
	public void setDynamicBookmark( final DynamicBookmark b )
	{
		if ( bookmark != null )
			bookmark.removeDynamicBookmarkChangedListener( bookmarkChangedListener );
		if ( b != null )
			b.addDynamicBookmarkChangedListener( bookmarkChangedListener );
		bookmark = b;
		setCurrentHoverKeyframe( null );
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
		// TODO: should popupMenu really be defined here???
		return popupMenu;
	}

	@Override
	public void paint( final Graphics g )
	{
		super.paint( g );

		if ( bookmark == null )
			return;

		for ( final KeyFrame keyframe : bookmark.getFrameSet() )
		{
			final int posX = timeSliderUI.xPositionForValue( keyframe.getTimepoint() );

			if ( keyframe.equals( currentHoverKeyframe ) )
				paintKeyFrameFlag( g, posX, KeyFrameFlagState.HOVER );
			else
				paintKeyFrameFlag( g, posX, KeyFrameFlagState.NORMAL );
		}
	}

	private void paintKeyFrameFlag( final Graphics g, final int posX, final KeyFrameFlagState flagState )
	{
		if ( flagState == KeyFrameFlagState.NORMAL )
		{
			g.setColor( CL_KF_FLAG_NORMAL );
			g.fillRect( posX, 0, KF_FLAG_WIDTH, getHeight() );
		}
		else
		{
			g.setColor( CL_KF_FLAG_HOVER );
			g.fillRect( posX - ( KF_FLAG_WIDTH_HOVER / 2 ), 0, KF_FLAG_WIDTH_HOVER, getHeight() );
		}
	}

	private KeyFrame determineHoverKeyFrame( final int mouseX )
	{
		if ( bookmark == null )
			return null;

		int minDist = Integer.MAX_VALUE;
		KeyFrame bestKeyframe = null;
		for ( final KeyFrame keyframe : bookmark.getFrameSet() )
		{
			final int keyframeX = timeSliderUI.xPositionForValue( keyframe.getTimepoint() );
			final int dist = Math.abs( keyframeX - mouseX );
			if ( dist < minDist )
			{
				minDist = dist;
				if ( minDist <= KF_FLAG_MOUSE_RADIUS )
					bestKeyframe = keyframe;
			}
		}
		return bestKeyframe;
	}

	private void setCurrentHoverKeyframe( final KeyFrame keyFrame )
	{
		currentHoverKeyframe = keyFrame;

		if ( keyFrame != null )
			setCursor( Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR ) );
		else
			setCursor( Cursor.getDefaultCursor() );

		SwingUtilities.invokeLater( KeyFramePanel.this::repaint );
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
			if ( !isPressing && !popupMenu.isShowing() )
				setCurrentHoverKeyframe( null );
		}

		@Override
		public void mouseDragged( final MouseEvent e )
		{
			if ( currentHoverKeyframe != null )
			{
				final int t = timeSliderUI.valueForXPosition( e.getX() );
				final KeyFrame updatedKeyFrame = bookmark.updateWithoutOverride( currentHoverKeyframe, t );
				if ( updatedKeyFrame != null )
				{
					setCurrentHoverKeyframe( updatedKeyFrame );
				}
				timeSlider.setValue( t );
			}
		}

		@Override
		public void mouseReleased( final MouseEvent e )
		{
			isPressing = false;
			maybeTriggerPopupMenu( e );

			if ( SwingUtilities.isLeftMouseButton( e ) && e.getClickCount() == 1 )
				if ( currentHoverKeyframe != null )
					timeSlider.setValue( currentHoverKeyframe.getTimepoint() );
		}

		@Override
		public void mousePressed( final MouseEvent e )
		{
			isPressing = true;
			maybeTriggerPopupMenu( e );
		}

		private void updateComponent( final MouseEvent e )
		{
			if ( !popupMenu.isShowing() )
				setCurrentHoverKeyframe( determineHoverKeyFrame( e.getX() ) );
		}

		private void maybeTriggerPopupMenu( final MouseEvent e )
		{
			if ( e.isPopupTrigger() )
			{
				popupMenu.setKeyFrameFlagSelected( currentHoverKeyframe );
				popupMenu.show( KeyFramePanel.this, e.getX(), e.getY() );
			}
		}
	}

	/**
	 * Helper class to expose the {@code xPositionForValue(int)} and
	 * {@code valueForXPosition(int)} methods of {@code BasicSliderUI}.
	 */
	static class BasicSliderUIAccess
	{
		private static Method m;

		static
		{
			try
			{
				m = BasicSliderUI.class.getDeclaredMethod( "xPositionForValue", int.class );
				m.setAccessible( true );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}

		private final JSlider slider;

		private final BasicSliderUI basicSliderUI;

		BasicSliderUIAccess( final JSlider slider )
		{
			this.slider = slider;
			final SliderUI sliderUI = slider.getUI();
			if ( m != null && sliderUI instanceof BasicSliderUI )
				basicSliderUI = ( BasicSliderUI ) sliderUI;
			else
				basicSliderUI = null;
		}

		int xPositionForValue( final int value )
		{
			if ( basicSliderUI != null )
			{
				try
				{
					return ( Integer ) m.invoke( basicSliderUI, value );
				}
				catch ( final Exception e )
				{}
			}
			return xPositionForValueFallback( value );
		}

		int valueForXPosition( final int xPos )
		{
			return basicSliderUI != null ? basicSliderUI.valueForXPosition( xPos ) : valueForXPositionFallback( xPos );
		}

		private static int fallbackBorder = 10;

		private int valueForXPositionFallback( final int xPos )
		{
			int value;
			final int minValue = slider.getMinimum();
			final int maxValue = slider.getMaximum();
			final int trackLength = slider.getWidth() - 2 * fallbackBorder;
			final int trackLeft = fallbackBorder;
			final int trackRight = trackLeft + trackLength - 1;
			if ( xPos <= trackLeft )
				value = minValue;
			else if ( xPos >= trackRight )
				value = maxValue;
			else
			{
				final int distanceFromTrackLeft = xPos - trackLeft;
				final double valueRange = ( double ) maxValue - ( double ) minValue;
				final double valuePerPixel = valueRange / trackLength;
				final int valueFromTrackLeft = ( int ) Math.round( distanceFromTrackLeft * valuePerPixel );
				value = minValue + valueFromTrackLeft;
			}
			return value;
		}

		private int xPositionForValueFallback( final int value )
		{
			final int min = slider.getMinimum();
			final int max = slider.getMaximum();
			final int trackLength = slider.getWidth() - 2 * fallbackBorder;
			final int trackLeft = fallbackBorder;
			final int trackRight = trackLeft + trackLength - 1;
			final double valueRange = ( double ) max - ( double ) min;
			final double pixelsPerValue = trackLength / valueRange;
			int xPosition = trackLeft;
			xPosition += Math.round( pixelsPerValue * ( ( double ) value - min ) );
			xPosition = Math.max( trackLeft, xPosition );
			xPosition = Math.min( trackRight, xPosition );
			return xPosition;
		}
	}
}
