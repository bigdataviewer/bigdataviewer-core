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
import bdv.tools.bookmarks.bookmark.KeyFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Extends the {@link JSlider}-Component with the ability to flag single {@link KeyFrame}'s.
 * 
 * @author  Riebe, Moritz (moritz.riebe@mz-solutions.de)
 */
public final class JKeyFrameSlider extends JSlider {
    
    /** KeyFrame-Flag (red-Line) Width. */
    private static final int KF_FLAG_WIDTH = 1;
    
    private static final Color CL_KF_FLAG_NORMAL = Color.RED;
    private static final Color CL_KF_FLAG_HOVER = CL_KF_FLAG_NORMAL.darker();
    
    private static enum KeyFrameFlagState {
        NORMAL,
        HOVER
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    /** My Context-Menu. */
    private final KeyFramePopupMenu popupMenu = new KeyFramePopupMenu();
    
    /** Current dynamic bookmark or null if no bookmark is selected. */
    private DynamicBookmark bookmark = null;
    
    private int currentHoverTimepoint = -1;

    public JKeyFrameSlider() {
        this(0, 100, 50);
    }

    public JKeyFrameSlider(int min, int max) {
        this(min, max, min);
    }

    public JKeyFrameSlider(int min, int max, int value) {
        super(min, max, value);
        initComponent();
    }
    
    private void initComponent() {
        addMouseListener(new MouseHoverEventAdapter());
        addMouseMotionListener(new MouseHoverEventAdapter());
        
        setMinimumSize(new Dimension((int) getMinimumSize().getWidth(), 26));
        setPreferredSize(new Dimension((int) getPreferredSize().getWidth(), 26));
        
        setFocusable(false);
    }
    
    /**
     * Sets current bookmark and updates component (repaint).
     * 
     * @param bookmark     bookmark or {@code null} to reset dyn. bookmark.
     */
    public void setDynamicBookmarks(DynamicBookmark bookmark) {
        this.bookmark = bookmark;
        this.currentHoverTimepoint = -1;
        repaint();
    }

    /**
     * Returns the specific {@link KeyFramePopupMenu} of this component.
     * 
     * @return  Returns always the same instance of {@link KeyFramePopupMenu}, never {@code null}.
     */
    public KeyFramePopupMenu getKeyFramePopupMenuPopupMenu() {
        return this.popupMenu;
    }
    
    @Override
    public JPopupMenu getComponentPopupMenu() {
        // Needs to return null! If a popup menu instance is returned, it will be used... without
        // our mouse-events depending on selected key-frames
        return null;
    }

    /**
     * Setting the popup menu is not allowed for this component!.
     * 
     * @param popup     - no -
     */
    @Override
    public void setComponentPopupMenu(JPopupMenu popup) {
        throw new IllegalStateException(JKeyFrameSlider.class.getSimpleName() + " cannot be set");
    }
    
    private boolean isKeyFrameFlagMouseHover() {
        return currentHoverTimepoint != -1;
    }
    
    private KeyFrame getKeyFrameFlagMouseOver() {
        if (isKeyFrameFlagMouseHover() == false) {
            return null;
        }
        
        for (KeyFrame singleFrame : bookmark.getFrameSet()) {
            if (singleFrame.getTimepoint() == this.currentHoverTimepoint) {
                return singleFrame;
            }
        }
        
        return null;
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        if (null == bookmark) {
            return;
        }
        
        for (int singleTimepoint : convertKeyFramesAsArray()) {
            final int posX = determineSliderXPositionOf(singleTimepoint);
            
            if (singleTimepoint == this.currentHoverTimepoint) {
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
            g.fillRect(sliderPositionX - 1, 0, KF_FLAG_WIDTH + 2, getHeight());
        }
    }
    
    private void determineKeyFrameHoverFlag(int inputComponentXCoord) {
        for (int anyTimepoint : convertKeyFramesAsArray()) {
            final int anyValidPosX = determineSliderXPositionOf(anyTimepoint);
            
            final int lowerBound = anyValidPosX;
            final int upperBound = anyValidPosX + KF_FLAG_WIDTH + 1;
            
            final int mouseX = inputComponentXCoord;
            
            if (mouseX >= lowerBound && mouseX <= upperBound) {
                this.currentHoverTimepoint = anyTimepoint;
                return;
            }
        }
        
        this.currentHoverTimepoint = -1;
    }
    
    private int[] convertKeyFramesAsArray() {
        return bookmark.getFrameSet().stream().mapToInt(KeyFrame::getTimepoint).toArray();
    }
    
    private int determineSliderXPositionOf(int timepoint) {
        final double sliderWidth = getWidth();
        return (int) (sliderWidth * ((double) timepoint / sliderWidth));
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
        
        private void updateComponent(MouseEvent event) {
            if (bookmark != null) {
                determineKeyFrameHoverFlag(event.getX());
                SwingUtilities.invokeLater(JKeyFrameSlider.this::repaint);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeTriggerPopupMenu(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeTriggerPopupMenu(e);
        }
        
        private void maybeTriggerPopupMenu(MouseEvent event) {
            if (event.isPopupTrigger() ) {
                if (isKeyFrameFlagMouseHover()) {
                    popupMenu.setKeyFrameFlagSelected(getKeyFrameFlagMouseOver());
                } else {
                    popupMenu.setKeyFrameFlagSelected(null /* no selected frame */);
                }
                
                popupMenu.show(JKeyFrameSlider.this, event.getX(), event.getY());
            }
        }
        
    }
    
}
