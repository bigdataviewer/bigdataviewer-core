/*
 * Copyright (c) 2017, Fiji
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package bdv.viewer;

import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmarkChangedListener;
import bdv.tools.bookmarks.bookmark.KeyFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Extends the {@link JSlider}-Component with the ability to flag single
 * {@link KeyFrame}'s.
 * 
 * @author Riebe, Moritz (moritz.riebe@mz-solutions.de)
 */
public final class JKeyFrameSlider extends JSlider {

	private class ChangeListener implements DynamicBookmarkChangedListener {

		@Override
		public void changed() {
			repaint();
		}

	}

	private final int numTimepoints;

	/** KeyFrame-Flag (red-Line) Width. */
	private static final int KF_FLAG_WIDTH = 1;
	private static final int KF_FLAG_WIDTH_HOVER = 8;
	private static final int KF_FLAG_MOUSE_RADIUS = (KF_FLAG_WIDTH_HOVER / 2) + 3;

	private static final Color CL_KF_FLAG_NORMAL = Color.RED;
	private static final Color CL_KF_FLAG_HOVER = Color.BLUE;

	private static enum KeyFrameFlagState {
		NORMAL, HOVER
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/** My Context-Menu. */
	private final KeyFramePopupMenu popupMenu = new KeyFramePopupMenu();

	private final DynamicBookmarkChangedListener bookmarkChangedListener = new ChangeListener();

	/** Current dynamic bookmark or null if no bookmark is selected. */
	private DynamicBookmark bookmark = null;

	private KeyFrame currentHoverKeyframe = null;

	public JKeyFrameSlider() {
		this(0, 100, 50);
	}

	public JKeyFrameSlider(int min, int max) {
		this(min, max, min);
	}

	public JKeyFrameSlider(int min, int max, int value) {
		super(min, max, value);
		this.numTimepoints = max;

		initComponent();
	}

	private void initComponent() {
		addMouseListener(new MouseHoverEventAdapter());
		addMouseMotionListener(new MouseHoverEventAdapter());

		setMinimumSize(new Dimension((int) getMinimumSize().getWidth(), 26));
		setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), 26));

		// setFocusable(false);
	}

	/**
	 * Sets current bookmark and updates component (repaint).
	 * 
	 * @param bookmark
	 *            bookmark or {@code null} to reset dyn. bookmark.
	 */
	public void setDynamicBookmarks(DynamicBookmark bookmark) {
		this.bookmark = bookmark;
		this.currentHoverKeyframe = null;

		if (this.bookmark != null) {
			bookmark.removeDynamicBookmarkChangedListener(bookmarkChangedListener);
		}

		this.bookmark = bookmark;

		if (this.bookmark != null) {
			bookmark.addDynamicBookmarkChangedListener(bookmarkChangedListener);
		}

		repaint();
	}

	/**
	 * Returns the specific {@link KeyFramePopupMenu} of this component.
	 * 
	 * @return Returns always the same instance of {@link KeyFramePopupMenu},
	 *         never {@code null}.
	 */
	public KeyFramePopupMenu getKeyFramePopupMenuPopupMenu() {
		return this.popupMenu;
	}

	@Override
	public JPopupMenu getComponentPopupMenu() {
		// Needs to return null! If a popup menu instance is returned, it will
		// be used... without
		// our mouse-events depending on selected key-frames
		return null;
	}

	/**
	 * Setting the popup menu is not allowed for this component!.
	 * 
	 * @param popup
	 *            - no -
	 */
	@Override
	public void setComponentPopupMenu(JPopupMenu popup) {
		throw new IllegalStateException(JKeyFrameSlider.class.getSimpleName() + " cannot be set");
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (null == bookmark) {
			return;
		}

		for (KeyFrame keyframe : this.bookmark.getFrameSet()) {
			final int posX = determineSliderXPositionOf(keyframe.getTimepoint());

			if (keyframe.equals(this.currentHoverKeyframe)) {
				paintKeyFrameFlag(g, posX, KeyFrameFlagState.HOVER);
			} else {
				paintKeyFrameFlag(g, posX, KeyFrameFlagState.NORMAL);
			}
		}

		((BasicSliderUI) getUI()).paintThumb(g);
	}

	private void paintKeyFrameFlag(Graphics g, int sliderPositionX, KeyFrameFlagState flagState) {
		if (flagState == KeyFrameFlagState.NORMAL) {
			g.setColor(CL_KF_FLAG_NORMAL);
			g.fillRect(sliderPositionX, 0, KF_FLAG_WIDTH, getHeight());
		} else {
			g.setColor(CL_KF_FLAG_HOVER);
			g.fillRect(sliderPositionX - (KF_FLAG_WIDTH_HOVER / 2), 0, KF_FLAG_WIDTH_HOVER, getHeight());
		}
	}

	private void determineKeyFrameHoverFlag(int inputComponentXCoord) {

		if (inputComponentXCoord >= 0 && this.bookmark != null) {
			for (KeyFrame keyframe : this.bookmark.getFrameSet()) {
				final int anyValidPosX = determineSliderXPositionOf(keyframe.getTimepoint());

				final int lowerBound = anyValidPosX - KF_FLAG_MOUSE_RADIUS;
				final int upperBound = anyValidPosX + KF_FLAG_MOUSE_RADIUS;

				final int mouseX = inputComponentXCoord;

				if (mouseX >= lowerBound && mouseX <= upperBound) {
					this.currentHoverKeyframe = keyframe;
					return;
				}
			}
		}

		this.currentHoverKeyframe = null;
	}

	/**
	 * Returns the {@code trackRect} of {@link BasicSliderUI} to determine the
	 * correct position of the slider thumb.
	 * 
	 * <p>
	 * If the selected LookAndFeel doesn't inherit from {@link BasicSliderUI}, a
	 * fallback implementation is used instead.
	 * </p>
	 * 
	 * @return Rectangle of track part - returns never {@code null}.
	 */
	private Rectangle getTrackRect() {
		final SliderUI sliderUI = getUI();

		final boolean fallbackNeeded = (sliderUI instanceof BasicSliderUI == false);
		if (fallbackNeeded) {
			return getVisibleRect();
		}

		final BasicSliderUI basicSliderUI = (BasicSliderUI) sliderUI;
		final Class<? extends BasicSliderUI> uiClazz = BasicSliderUI.class;

		try {
			final Field trackRectField = uiClazz.getDeclaredField("trackRect");

			trackRectField.setAccessible(true);

			final Rectangle result = (Rectangle) trackRectField.get(basicSliderUI);

			if (null == result) {
				return getVisibleRect();
			}

			return result;

		} catch (Exception ex) {
			return getVisibleRect();
		}
	}

	private int determineSliderXPositionOf(int timepoint) {
		final Rectangle trackRect = getTrackRect();

		final double trackOffsetX = trackRect.getX();
		final double trackWidth = trackRect.getWidth();

		return (int) (((trackWidth / numTimepoints) * timepoint) + trackOffsetX);
	}

	private class MouseHoverEventAdapter extends MouseAdapter {

		@Override
		public void mouseMoved(MouseEvent e) {
			updateComponent(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateComponent(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			updateComponent(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			// when user drags the thumb, we need to update the hovered keyframe
			// otherwise the thumb will jump back to the currently as hovered marked keyframe
			// though the keyframe is not actually hovered
			updateComponent(e);
		}

		private void updateComponent(MouseEvent event) {
			if (event.getY() < 0 || event.getY() >= getHeight()) {
				// when mouse leaves slider at the top or bottom
				popupMenu.setVisible(false);
				determineKeyFrameHoverFlag(-1);
			} else {
				if (!popupMenu.isShowing()) {
					determineKeyFrameHoverFlag(event.getX());
				}
			}

			SwingUtilities.invokeLater(JKeyFrameSlider.this::repaint);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeTriggerPopupMenu(e);

			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
				// when left mouse button is released the thumb will jump
				// to the currently hovered keyframe (snapping)
				// to prevent this behavior the user can press the control key
				if (currentHoverKeyframe != null) {
					setValue(currentHoverKeyframe.getTimepoint());
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeTriggerPopupMenu(e);
		}

		private void maybeTriggerPopupMenu(MouseEvent event) {
			if (event.isPopupTrigger()) {
				popupMenu.setKeyFrameFlagSelected(currentHoverKeyframe);
				popupMenu.show(JKeyFrameSlider.this, event.getX(), event.getY());
			}
		}
	}
}
