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
import bdv.tools.movie.serilizers.MovieFramesSerializer;
import bdv.util.DelayedPackDialog;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProduceMovieDialog extends DelayedPackDialog {

    private final List<MovieFramePanel> framesPanels;
    private final ViewerPanel viewer;

    private final ProgressWriter progressWriter;
    private final JPanel mainPanel;
    private final JButton removeButton;
    private final MovieSaveDialog saveDialog;
    private final JButton exportJsonButton;
    private final JButton exportPNGsButton;
    private final static int FrameWidth = 920;
    private final static int DEFAULT_SLEEP = 100;
    private final static int DEFAULT_DOWN = 10;
    private PreviewThread previewThread;
    private final JTextField downsampleField;
    private final JTextField sleepField;

    public ProduceMovieDialog(final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter) {
        super(owner, "produce movie", false);
        setLayout(new FlowLayout());
        setSize(new Dimension(FrameWidth, 340));
        JPanel playerPanel = new JPanel();

        downsampleField = new JTextField(String.valueOf(DEFAULT_DOWN));
        sleepField = new JTextField(String.valueOf(DEFAULT_SLEEP));
        playerPanel.add(new JLabel("PREVIEW:  Downsampling:   1/"));
        playerPanel.add(downsampleField);
        playerPanel.add(new JLabel("Sleep:"));
        playerPanel.add(sleepField);
        playerPanel.add(new JLabel("ms    "));

        JButton playButton = new JButton("▶");
        playButton.addActionListener(e -> {
            startPreview();
        });
        playerPanel.add(playButton);

        JButton pauseButton = new JButton("Stop");
        pauseButton.addActionListener(e -> {
            pausePreview();
        });
        playerPanel.add(pauseButton);

//        JButton restartButton = new JButton("Refresh");
//        restartButton.addActionListener(e -> {
//            restartPreview();
//        });
//        playerPanel.add(restartButton);

        playerPanel.setPreferredSize(new Dimension(FrameWidth, 40));

        JPanel makerPanel = new JPanel(new FlowLayout());
        makerPanel.setPreferredSize(new Dimension(FrameWidth, 280));

        this.saveDialog = new MovieSaveDialog(owner, viewer, progressWriter, this);
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

        makerPanel.add(controlPanel);

        this.mainPanel = new JPanel();

        JScrollPane scrollMain = new JScrollPane(mainPanel);
        scrollMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        TitledBorder title = BorderFactory.createTitledBorder("Frames: ");
        scrollMain.setBorder(title);
        scrollMain.setPreferredSize(new Dimension(750, 240));
        makerPanel.add(scrollMain);

        final JPanel exportPanel = new JPanel(new FlowLayout());
        exportPanel.setPreferredSize(new Dimension(100, 200));

        this.exportJsonButton = new JButton("Export Json");
        exportJsonButton.addActionListener(e -> exportJson());
        exportPanel.add(exportJsonButton);

        this.exportPNGsButton = new JButton("Generate PNGs");
        exportPNGsButton.addActionListener(e -> showSavePNGsDialog());
        exportPanel.add(exportPNGsButton);

        JButton importButton = new JButton("Import");
        importButton.addActionListener(e -> importSequence());
        exportPanel.add(importButton);

        makerPanel.add(exportPanel);

        add(playerPanel);
        add(makerPanel);

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
        // setResizable(false);
        validateButtons();
    }

    private void pausePreview() {
        if (previewThread != null)
            previewThread.suspend();
    }

    private void restartPreview() {
        int sleep = Integer.valueOf(sleepField.getText());
        int down = Integer.valueOf(downsampleField.getText());
        int size = framesPanels.size();
        final AffineTransform3D[] transforms = new AffineTransform3D[size];
        final int[] frames = new int[size];
        final int[] accel = new int[size];

        for (int i = 0; i < size; i++) {
            MovieFrame currentFrame = framesPanels.get(i).updateFields().getMovieFrame();
            transforms[i] = currentFrame.getTransform();
            frames[i] = currentFrame.getFrames();
            accel[i] = currentFrame.getAccel();
        }

        previewThread = new PreviewThread(viewer,
                transforms,
                frames,
                accel,
                sleep, down);
    }

    private void startPreview() {
        if (previewThread != null) {
            previewThread.suspend();
        }
        restartPreview();
        previewThread.start();
    }

    //    TODO import
    private void importSequence() {
        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jfc.setDialogTitle("Select a Json");
        jfc.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON File", "json");
        jfc.addChoosableFileFilter(filter);

        int returnValue = jfc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            System.out.println(jfc.getSelectedFile().getPath());
            String path = jfc.getSelectedFile().getPath();
            new Thread(() -> {
                try {
                    removeAllFrames();
                    List<MovieFrame> list = MovieFramesSerializer.getFrom(new File(path));
                    for (MovieFrame frame : list) {
                        AffineTransform3D currentTransform = frame.getTransform().copy();
                        viewer.state().setViewerTransform(currentTransform);
                        Thread.sleep(50);
                        ImagePanel imagePanel = ImagePanel.snapshotOf(viewer);
                        addFrame(frame, imagePanel);
                    }
                    JOptionPane.showMessageDialog(this, "File imported successfully!", "File imported", JOptionPane.INFORMATION_MESSAGE);


                } catch (FileNotFoundException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).run();


        }
    }


}

    private void removeAllFrames() {
        while (!framesPanels.isEmpty())
            removeFrame();
    }

    private void exportJson() {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            List<MovieFrame> list = new ArrayList<>();
            for (MovieFramePanel panels : framesPanels)
                list.add(panels.getMovieFrame());
            MovieFramesSerializer.save(list, new File(path));

            JOptionPane.showMessageDialog(this, "File saved successfully!", "File saved", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showSavePNGsDialog() {
        saveDialog.setVisible(true);
    }

    private void removeFrame() {
        mainPanel.remove(framesPanels.remove(framesPanels.size() - 1));
        validateButtons();
        revalidate();
        repaint();
    }

    private void addFrame(MovieFrame movieFrame, ImagePanel imagePanel) {
        MovieFramePanel movieFramePanel = new MovieFramePanel(movieFrame, imagePanel);
        framesPanels.add(movieFramePanel);
        mainPanel.add(movieFramePanel);
        validateButtons();
        revalidate();
        repaint();
    }

    private void addFrame() {
        AffineTransform3D currentTransform = viewer.state().getViewerTransform();
        MovieFramePanel movieFramePanel = new MovieFramePanel(currentTransform, ImagePanel.snapshotOf(viewer), framesPanels.size());
        framesPanels.add(movieFramePanel);
        mainPanel.add(movieFramePanel);
        validateButtons();
        revalidate();
        repaint();
    }

    public void exportPNGs(int width, int height, File dir) {

        int size = framesPanels.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final AffineTransform3D[] transforms = new AffineTransform3D[size];
                final int[] frames = new int[size];
                final int[] accel = new int[size];

                for (int i = 0; i < size; i++) {
                    MovieFrame currentFrame = framesPanels.get(i).updateFields().getMovieFrame();
                    transforms[i] = currentFrame.getTransform();
                    frames[i] = currentFrame.getFrames();
                    accel[i] = currentFrame.getAccel();
                }

                AffineTransform3D viewerScale = new AffineTransform3D();
                viewerScale.set(
                        1.0, 0, 0, 0,
                        0, 1.0, 0, 0,
                        0, 0, 1.0, 0);

                try {
                    VNCMovie.recordMovie(
                            viewer,
                            width,
                            height,
                            transforms,
                            viewerScale,
                            frames,
                            accel,
                            1,
                            dir.getAbsolutePath());
                    JOptionPane.showMessageDialog(mainPanel, "All Files saved successfully!", "Files saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).run();
    }

    private void validateButtons() {
        if (framesPanels.size() == 2 && !exportPNGsButton.isEnabled()) {
            exportJsonButton.setEnabled(true);
            exportPNGsButton.setEnabled(true);
            exportJsonButton.revalidate();
            exportPNGsButton.revalidate();
        }
        if (framesPanels.size() < 2 && exportPNGsButton.isEnabled()) {
            exportJsonButton.setEnabled(false);
            exportPNGsButton.setEnabled(false);
            exportJsonButton.revalidate();
            exportPNGsButton.revalidate();
        }
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
