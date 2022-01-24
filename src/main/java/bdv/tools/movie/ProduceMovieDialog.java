/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.tools.movie;

import bdv.export.ProgressWriter;
import bdv.util.DelayedPackDialog;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ProduceMovieDialog extends DelayedPackDialog {

    private final List<MovieFramePanel> framesPanels;
    private final ViewerPanel viewer;

    private final ProgressWriter progressWriter;
    private final JPanel mainPanel;
    private final JButton removeButton;

    public ProduceMovieDialog(final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter) {
        super(owner, "produce movie", false);
        setLayout(new FlowLayout());
        setSize(new Dimension(820, 240));
        this.viewer = viewer;
        this.progressWriter = progressWriter;
        framesPanels = new ArrayList<>();
        final JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setPreferredSize(new Dimension(50, 200));

        JButton addButton = new JButton("+");
        addButton.setPreferredSize(new Dimension(50, 30));
        addButton.addActionListener(e -> addFrame());
        controlPanel.add(addButton);

        removeButton = new JButton("-");
        removeButton.setPreferredSize(new Dimension(50, 30));
        removeButton.addActionListener(e -> removeFrame());
        controlPanel.add(removeButton);

        add(controlPanel);

        this.mainPanel = new JPanel();
        TitledBorder title = BorderFactory.createTitledBorder("Frames: ");
        mainPanel.setBorder(title);
        JScrollPane scrollMain = new JScrollPane(mainPanel);
        scrollMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        scrollMain.setPreferredSize(new Dimension(650, 200));
//        mainPanel.setPreferredSize(new Dimension(650,200));
        add(scrollMain);

        final JPanel exportPanel = new JPanel(new FlowLayout());
        exportPanel.setPreferredSize(new Dimension(100, 200));

        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(e -> exportVideo());
        exportPanel.add(exportButton);

        add(exportPanel);

        final ActionMap am = getRootPane().getActionMap();
        final InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        final Object hideKey = new Object();
        final Action hideAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setVisible(false);
            }

            private static final long serialVersionUID = 1L;
        };
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), hideKey);
        am.put(hideKey, hideAction);
        setResizable(false);
        validateRemoveButton();
    }

    private void exportVideo() {
        List<AffineTransform3D> transformations = getTransformations();
    }

    private List<AffineTransform3D> getTransformations() {
        List<AffineTransform3D> transformations = new ArrayList<>();
        for (MovieFramePanel panel : framesPanels)
            transformations.add(panel.getTransform());
        return transformations;
    }

    private void removeFrame() {
        mainPanel.remove(framesPanels.remove(framesPanels.size() - 1));
        validateRemoveButton();
        revalidate();
        repaint();
    }

    private void addFrame() {
        AffineTransform3D currentTransform = viewer.state().getViewerTransform();
        MovieFramePanel movieFramePanel = new MovieFramePanel(currentTransform, new ImagePanel(PanelSnapshot.takeSnapShot(viewer)), framesPanels.size());
        framesPanels.add(movieFramePanel);
        mainPanel.add(movieFramePanel);
        validateRemoveButton();
        revalidate();
        repaint();
    }

    private void validateRemoveButton() {
        if (framesPanels.size() > 0) {
            if (!removeButton.isEnabled())
                removeButton.setEnabled(true);
            removeButton.revalidate();
            removeButton.repaint();
        } else if (removeButton.isEnabled()) {
            removeButton.setEnabled(false);
            removeButton.revalidate();
            removeButton.repaint();
        }
    }
}
