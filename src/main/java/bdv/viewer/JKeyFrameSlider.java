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
import java.awt.Graphics;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 *
 * @author Riebe, Moritz (moritz.riebe@mz-solutions.de)
 */
public final class JKeyFrameSlider extends JSlider {
    
    /** Current dynamic bookmark or null if no bookmark is selected. */
    private DynamicBookmark bookmark = null;

    public JKeyFrameSlider() {
        this(0, 100, 50);
    }

    public JKeyFrameSlider(int min, int max) {
        this(min, max, min);
    }

    public JKeyFrameSlider(int min, int max, int value) {
        super(min, max, value);
    }
    
    /**
     * Sets current bookmark and updates component (repaint).
     * 
     * @param bookmark     bookmark or {@code null} to reset dyn. bookmark.
     */
    public void setDynamicBookmarks(DynamicBookmark bookmark) {
        this.bookmark = bookmark;
        repaint();
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        if (null == bookmark) {
            return;
        }
        
        final int width = getWidth();
        final int height = getHeight();
        
        final Color clTimepointFlag = Color.RED;
        
        g.setColor(clTimepointFlag);
        
        for (int singleTimepoint : convertKeyFramesAsArray()) {
            final int posX = (int) (width * ((double) singleTimepoint / (double) width));
            
            g.fillRect(posX, 0, 2, height);
        }
        
        ((BasicSliderUI) getUI()).paintThumb(g);
    }
    
    private int[] convertKeyFramesAsArray() {
        return bookmark.getFrameSet().stream().mapToInt(KeyFrame::getTimepoint).toArray();
    }
    
}
