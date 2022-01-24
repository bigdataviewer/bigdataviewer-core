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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ProduceMovieDialog extends DelayedPackDialog
{

    private final List<MovieFramePanel> framesPanels;
    private final ViewerPanel viewer;

    private final ProgressWriter progressWriter;
    private final JPanel mainPanel;

    public ProduceMovieDialog( final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter )
    {
        super( owner, "produce movie", false );
        setSize(new Dimension(800,200));
        this.viewer = viewer;
        this.progressWriter = progressWriter;
        framesPanels = new ArrayList<>();
        final JPanel controlPanel = new JPanel();
        controlPanel.setSize(50,200);
        controlPanel.setBackground(Color.BLACK);
        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> addFrame());
        controlPanel.add(addButton);
        getContentPane().add(controlPanel);

        this.mainPanel = new JPanel();
        mainPanel.setSize(new Dimension(700,200));
        mainPanel.setBackground(Color.BLUE);
        getContentPane().add( mainPanel );

        final JPanel exportPanel = new JPanel();
        exportPanel.setSize(new Dimension(50,200));
        exportPanel.setBackground(Color.green);
        getContentPane().add( exportPanel );

        final ActionMap am = getRootPane().getActionMap();
        final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
        final Object hideKey = new Object();
        final Action hideAction = new AbstractAction()
        {
            @Override
            public void actionPerformed( final ActionEvent e )
            {
                setVisible( false );
            }

            private static final long serialVersionUID = 1L;
        };
        im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
        am.put( hideKey, hideAction );

    }

    private void addFrame() {
        AffineTransform3D currentTransform = viewer.state().getViewerTransform();
        MovieFramePanel movieFramePanel = new MovieFramePanel(currentTransform, new ImagePanel(PanelSnapshot.takeSnapShot(viewer)),framesPanels.size());
        framesPanels.add(movieFramePanel);
        mainPanel.add(movieFramePanel);
        revalidate();
        repaint();
    }
}
