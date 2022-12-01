package bdv.tools.movie.panels;

import bdv.export.ProgressWriter;
import bdv.tools.movie.ProduceMovieDialog;
import bdv.util.DelayedPackDialog;
import bdv.viewer.OverlayRenderer;
import bdv.viewer.ViewerPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

public class MovieSaveDialog extends DelayedPackDialog implements OverlayRenderer {
    private static final long serialVersionUID = 1L;

    private final ViewerPanel viewer;

    private final ProgressWriter progressWriter;

    private final JTextField pathTextField;

    private final JSpinner spinnerWidth;

    private final JSpinner spinnerHeight;

    public MovieSaveDialog(final Frame owner, final ViewerPanel viewer, final ProgressWriter progressWriter, ProduceMovieDialog produceMovieDialog) {
        super(owner, "record movie", false);
        this.viewer = viewer;
        this.progressWriter = progressWriter;

        final JPanel boxes = new JPanel();
        getContentPane().add(boxes, BorderLayout.NORTH);
        boxes.setLayout(new BoxLayout(boxes, BoxLayout.PAGE_AXIS));

        final JPanel saveAsPanel = new JPanel();
        saveAsPanel.setLayout(new BorderLayout(0, 0));
        boxes.add(saveAsPanel);

        saveAsPanel.add(new JLabel("save to"), BorderLayout.WEST);

        pathTextField = new JTextField("./record/");
        saveAsPanel.add(pathTextField, BorderLayout.CENTER);
        pathTextField.setColumns(20);

        final JButton browseButton = new JButton("Browse");
        saveAsPanel.add(browseButton, BorderLayout.EAST);

        final JPanel typePanel = new JPanel();
        boxes.add(typePanel);

        typePanel.add(new JLabel("Type:"));

        final JPanel widthPanel = new JPanel();
        boxes.add(widthPanel);
        widthPanel.add(new JLabel("width"));
        spinnerWidth = new JSpinner();
        spinnerWidth.setModel(new SpinnerNumberModel(800, 10, 5000, 1));
        widthPanel.add(spinnerWidth);

        final JPanel heightPanel = new JPanel();
        boxes.add(heightPanel);
        heightPanel.add(new JLabel("height"));
        spinnerHeight = new JSpinner();
        spinnerHeight.setModel(new SpinnerNumberModel(600, 10, 5000, 1));
        heightPanel.add(spinnerHeight);

        final JPanel buttonsPanel = new JPanel();
        boxes.add(buttonsPanel);
        buttonsPanel.setLayout(new BorderLayout(0, 0));

        final JButton recordButton = new JButton("Record");
        buttonsPanel.add(recordButton, BorderLayout.EAST);

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                fileChooser.setSelectedFile(new File(pathTextField.getText()));
                final int returnVal = fileChooser.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    final File file = fileChooser.getSelectedFile();
                    pathTextField.setText(file.getAbsolutePath());
                }
            }
        });

        recordButton.addActionListener(e -> {
            final String dirname = pathTextField.getText();
            final File dir = new File(dirname);
            if (!dir.exists())
                dir.mkdirs();
            if (!dir.exists() || !dir.isDirectory()) {
                System.err.println("Invalid export directory " + dirname);
                return;
            }
            setVisible(false);
            final int width = (Integer) spinnerWidth.getValue();
            final int height = (Integer) spinnerHeight.getValue();
            produceMovieDialog.exportPNGs(dir,width,height,progressWriter);
        });

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

        pack();
    }

    @Override
    public void drawOverlays(final Graphics g) {
    }

    @Override
    public void setCanvasSize(final int width, final int height) {
        spinnerWidth.setValue(width);
        spinnerHeight.setValue(height);
    }
}