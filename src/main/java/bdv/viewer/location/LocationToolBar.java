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
 * Toolbar containing location information.
 *
 * @author Eric Trautman
 */
public class LocationToolBar extends JToolBar {

    private final JLabel centerCoordinatesLabel;
    private final JButton editCoordinatesButton;
    private final JLabel mouseCoordinatesLabel;

    private ActionListener editCoordinatesActionListener;

    public LocationToolBar() {
        super("Location Tools");
        this.setFloatable(false);

        final JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));
        this.add(flowPanel);

        this.centerCoordinatesLabel = new JLabel();

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
        this.mouseCoordinatesLabel.setForeground(Color.MAGENTA);

        flowPanel.add(this.centerCoordinatesLabel);
        flowPanel.add(this.editCoordinatesButton);
        flowPanel.add(this.mouseCoordinatesLabel);
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

    public void setPositions(final double[] centerPosition,
                             final double[] mousePosition) {
        for (int i = 0; i < centerPosition.length; i++) {
            this.centerCoordinatesLabel.setText(formatPosition(centerPosition));
            this.mouseCoordinatesLabel.setText(formatPosition(mousePosition));
        }
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
