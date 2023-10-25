package bdv.viewer.location;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;

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

    private final JLabel sourceNameLabel;
    private final JLabel groupNameLabel;
    private final JLabel centerCoordinatesLabel;
    private final JLabel timepointLabel;
    private final JButton editCoordinatesButton;
    private final JLabel mouseCoordinatesLabel;

    private ActionListener editCoordinatesActionListener;

    public SourceInfoToolBar() {
        super("Source Information");
        this.setFloatable(false);

        final JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));
        this.add(flowPanel);

        this.sourceNameLabel = new JLabel();
        this.sourceNameLabel.setToolTipText("Source name");

        this.groupNameLabel = new JLabel();
        this.groupNameLabel.setToolTipText("Group name");

        this.timepointLabel = new JLabel();
        this.timepointLabel.setToolTipText("Timepoint");

        this.centerCoordinatesLabel = new JLabel();
        this.centerCoordinatesLabel.setToolTipText("Center coordinates");

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

        flowPanel.add(this.sourceNameLabel);
        flowPanel.add(this.groupNameLabel);
        flowPanel.add(this.timepointLabel);
        flowPanel.add(this.centerCoordinatesLabel);
        flowPanel.add(this.editCoordinatesButton);
        flowPanel.add(this.mouseCoordinatesLabel);
    }

    public void setSourceNamesAndTimepoint(final String sourceName,
                                           final String groupName,
                                           final String timepointString) {
        if ((sourceName != null) && (! sourceName.isEmpty())) {
            this.sourceNameLabel.setText(sourceName);
            if ((groupName != null) && (! groupName.isEmpty())) {
                this.groupNameLabel.setText("| " + groupName);
                this.groupNameLabel.setVisible(true);
            } else {
                this.groupNameLabel.setVisible(false);
            }
            this.sourceNameLabel.setVisible(true);
        } else {
            this.sourceNameLabel.setVisible(false);
        }
        if ((timepointString != null) && (! timepointString.isEmpty())) {
            this.timepointLabel.setText(timepointString);
        } else {
            this.timepointLabel.setText("");
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
        return String.format(Locale.ROOT, "%s: %1.0f",
                             DimensionCoordinateComponents.getDefaultDimensionName(dimension),
                             value);
    }

}
