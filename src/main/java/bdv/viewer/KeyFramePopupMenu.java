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

import bdv.tools.bookmarks.bookmark.KeyFrame;
import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Context-Menu for the {@link JKeyFrameSlider}-Component.
 * 
 * @author Riebe, Moritz (moritz.riebe@mz-solutions.de)
 */
public final class KeyFramePopupMenu extends JPopupMenu {
    
	private static final long serialVersionUID = 1L;

	
	private final JMenuItem itemCopyKF = new JMenuItem();
	private final JMenuItem itemSetTransformKF = new JMenuItem();
    private final JMenuItem itemRemoveKF = new JMenuItem();
    
    private final JMenuItem itemOpenBookmarkManager = new JMenuItem();
    private final JSeparator sepContextFromNonContext = new Separator();
    
    /** User-selected {@link KeyFrame} or {@code null} if no frame is selected. */
    private volatile KeyFrame selectedKeyFrame = null;
    
    KeyFramePopupMenu() {
        initComponents();
    }
    
    private void initComponents() {
        add(itemCopyKF);
        add(itemSetTransformKF);
        add(itemRemoveKF);
        add(sepContextFromNonContext);
        add(itemOpenBookmarkManager);
        
        itemCopyKF.setText("Copy");
        itemCopyKF.addActionListener(this::onActionPerformedEventItemSelectKF);
        
        itemSetTransformKF.setText("Set transformation");
        itemSetTransformKF.addActionListener(this::onActionPerformedEventItemRemoveFK);
        
        itemRemoveKF.setText("Remove");
        itemRemoveKF.addActionListener(this::onActionPerformedEventItemRemoveFK);
        
        itemOpenBookmarkManager.setText("Open Bookmark Management...");
        itemOpenBookmarkManager.addActionListener(this::onActionPerformedEventOpenBookmarkManager);
        
        setKeyFrameFlagSelected(null);
    }
    
    protected void setKeyFrameFlagSelected(KeyFrame selectedKeyFrameOrNull) {
        final boolean visibleKeyFrameActions = (null != selectedKeyFrameOrNull);
        
        this.itemCopyKF.setVisible(visibleKeyFrameActions);
        this.itemSetTransformKF.setVisible(visibleKeyFrameActions);
        this.itemRemoveKF.setVisible(visibleKeyFrameActions);
        this.sepContextFromNonContext.setVisible(visibleKeyFrameActions);
        
        this.selectedKeyFrame = selectedKeyFrameOrNull;
    }
    
    /**
     * Returns current user-selected {@link KeyFrame} or nothing if no frame is selected.
     * @return  {@code Optional.empty()} if no frame is selected or {@code Optional.of(..)}.
     */
    protected Optional<KeyFrame> getSelectedKeyFrame() {
        return Optional.ofNullable(this.selectedKeyFrame);
    }
    
    private void testAction() {
        JOptionPane.showMessageDialog(getParent(),
                "Selected KeyFrame Timepoint: " + getSelectedKeyFrame().get().getTimepoint(),
                "KeyFrame",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void onActionPerformedEventItemSelectKF(ActionEvent event) {
        testAction();
    }
    
    private void onActionPerformedEventItemRemoveFK(ActionEvent event) {
        testAction();
    }
    
    private void onActionPerformedEventOpenBookmarkManager(ActionEvent event) {
        // no testAction() -> event may be called when no frame is selected
    }
    
}
