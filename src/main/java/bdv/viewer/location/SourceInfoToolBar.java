package bdv.viewer.location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * Toolbar containing source information.
 *
 * @author Eric Trautman
 */
public class SourceInfoToolBar extends JToolBar {

    private final JLabel sourceAndGroupNameLabel;
   private final JLabel centerCoordinatesLabel;
    private final JLabel timepointLabel;
    private final JButton editCoordinatesButton;
    private final JLabel mouseCoordinatesLabel;

    private ActionListener editCoordinatesActionListener;

    public SourceInfoToolBar() {
        super("Source Information");
        this.setFloatable(false);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        this.add(panel);

        this.sourceAndGroupNameLabel = new JLabel();

        this.timepointLabel = new JLabel();
        this.timepointLabel.setToolTipText("Timepoint");

        final Font monospacedFont = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        this.centerCoordinatesLabel = new JLabel();
        this.centerCoordinatesLabel.setToolTipText("Center coordinates");
        this.centerCoordinatesLabel.setHorizontalAlignment(JLabel.LEFT);
        this.centerCoordinatesLabel.setFont(monospacedFont);

        final URL url = this.getClass().getResource("/bdv/ui/location/edit_pencil_20.png");
        if (url == null) {
            this.editCoordinatesButton = new JButton("Edit");
        } else {
            this.editCoordinatesButton = new JButton(new ImageIcon(url));
            this.editCoordinatesButton.setMargin(new Insets(0, 0, 0, 0));
            this.editCoordinatesButton.setContentAreaFilled(false);
        }
        this.editCoordinatesButton.setToolTipText("Edit center coordinates");
        this.editCoordinatesButton.setVisible(false);

        this.mouseCoordinatesLabel = new JLabel();
        this.mouseCoordinatesLabel.setToolTipText("Mouse coordinates");
        this.mouseCoordinatesLabel.setForeground(Color.MAGENTA);
        this.mouseCoordinatesLabel.setVisible(false);
        this.mouseCoordinatesLabel.setHorizontalAlignment(JLabel.LEFT);
        this.mouseCoordinatesLabel.setFont(monospacedFont);

        panel.add(this.sourceAndGroupNameLabel);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(this.centerCoordinatesLabel);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(this.editCoordinatesButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(this.mouseCoordinatesLabel);
        panel.add(Box.createHorizontalGlue());
        panel.add(this.timepointLabel);
    }

    public void updateSource(final String sourceName,
                             final String groupName,
                             final String timepointString) {
        if ((sourceName != null) && (! sourceName.isEmpty())) {
            String sourceAndGroupName = sourceName;
            if ((groupName != null) && (! groupName.isEmpty())) {
                sourceAndGroupName = sourceAndGroupName + " | " + groupName;
            }
            if (sourceAndGroupName.length() > 20) {
                sourceAndGroupNameLabel.setToolTipText(sourceAndGroupName);
                sourceAndGroupName = sourceAndGroupName.substring(0, 17) + "...";
            } else {
                sourceAndGroupNameLabel.setToolTipText("source and group name");
            }
            sourceAndGroupNameLabel.setText(sourceAndGroupName);
            sourceAndGroupNameLabel.setVisible(true);
        } else {
            sourceAndGroupNameLabel.setVisible(false);
        }

        if ((timepointString != null) && (! timepointString.isEmpty())) {
            timepointLabel.setText(timepointString);
            timepointLabel.setVisible(true);
        } else {
            timepointLabel.setVisible(false);
        }
    }

    public void setEditActionListener(final ActionListener editCoordinatesActionListener) {
        if (this.editCoordinatesActionListener != null) {
            this.editCoordinatesButton.removeActionListener(this.editCoordinatesActionListener);
        }
        this.editCoordinatesActionListener = editCoordinatesActionListener;
        if (editCoordinatesActionListener == null) {
            this.editCoordinatesButton.setVisible(false);
        } else {
            this.editCoordinatesButton.addActionListener(editCoordinatesActionListener);
            this.editCoordinatesButton.setVisible(true);
        }
    }

    public void setCenterPosition(final double[] centerPosition) {
        for (int i = 0; i < centerPosition.length; i++) {
            this.centerCoordinatesLabel.setText(formatPosition(centerPosition));
        }
    }

    public void setMousePosition(final double[] mousePosition) {
        for (int i = 0; i < mousePosition.length; i++) {
            this.mouseCoordinatesLabel.setText(formatPosition(mousePosition));
        }
    }

    public void setMouseCoordinatesVisible(final boolean visible) {
        this.mouseCoordinatesLabel.setVisible(visible);
    }

    private String formatPosition(final double[] position) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < position.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(formatCoordinate(i, position[i]));
        }
        return sb.toString();
    }

    private String formatCoordinate(final int dimension,
                                    final double value) {
        return String.format(Locale.ROOT, "%s: % 8.1f",
                             DimensionCoordinateComponents.getDefaultDimensionName(dimension),
                             value);
    }

}
