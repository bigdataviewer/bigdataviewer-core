package bdv.viewer.location;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Panel containing UI components for displaying and editing coordinates for one dimension of a volume.
 *
 * @author Eric Trautman
 */
public class DimensionCoordinatePanel
        extends JPanel {

    public interface ChangeListener {
        void changeLocation(final double toGlobalPosition,
                            final int forDimension);
    }

    private String name;
    private final int dimension;
    private final boolean isReadOnly;
    private final ChangeListener changeListener;
    private final String valueFormat;
    private double position;
    private final JButton readOnlyValue;
    private final JLabel labelForWritableValue;
    private final JTextField valueTextField;

    public DimensionCoordinatePanel(final String name,
                                    final int dimension,
                                    final boolean isReadOnly,
                                    final ChangeListener changeListener) {

        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        this.name = name;
        this.dimension = dimension;
        this.isReadOnly = isReadOnly;
        this.changeListener = changeListener;
        this.valueFormat = "%1.1f"; // TODO: add support for changing this based upon volume size and display scale/size

        this.readOnlyValue = new JButton();
        this.readOnlyValue.setBorderPainted(false);
        this.readOnlyValue.setFocusPainted(false);
        this.readOnlyValue.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        this.readOnlyValue.setOpaque(false);
        this.readOnlyValue.setHorizontalTextPosition(JButton.LEFT);
        this.add(this.readOnlyValue);

        if (isReadOnly) {

            this.readOnlyValue.setEnabled(false);
            this.labelForWritableValue = null;
            this.valueTextField = null;

        } else {

            this.labelForWritableValue = new JLabel(name + ": ");
            this.add(this.labelForWritableValue);
            this.labelForWritableValue.setVisible(false);

            this.valueTextField = new JTextField(10);
            this.add(this.valueTextField);
            this.valueTextField.setVisible(false);

            this.readOnlyValue.addActionListener(e -> {
                this.labelForWritableValue.setVisible(true);
                this.valueTextField.setText(String.format(Locale.ROOT, valueFormat, position));
                this.valueTextField.setEditable(true);
                this.readOnlyValue.setVisible(false);
                this.valueTextField.setVisible(true);
                this.valueTextField.requestFocus();
            });

            this.valueTextField.addActionListener(e -> stopEditing());

            final DimensionCoordinatePanel thisPanel = this;
            this.valueTextField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusLost(java.awt.event.FocusEvent evt) {
                    thisPanel.stopEditing();
                }
            });
        }
    }

    private void stopEditing() {
        if (valueTextField.isEditable()) {
            try {
                final double updatedPosition = Double.parseDouble(valueTextField.getText());
                changeListener.changeLocation(updatedPosition, dimension);
            } catch (NumberFormatException e) {
                // ignore and leave position unchanged
            }
            closeEditor();
        }
    }

    private void closeEditor() {
        this.labelForWritableValue.setVisible(false);
        this.valueTextField.setEditable(false);
        this.valueTextField.setVisible(false);
        this.readOnlyValue.setVisible(true);
    }

    public void setName(final String name) {
        this.name = name;
        this.labelForWritableValue.setText(name + ": ");
    }

    public void setPosition(final double position) {
        this.position = position;
        final String formattedValue = String.format(Locale.ROOT, valueFormat, position);
        if (isReadOnly) {
            this.readOnlyValue.setText(name + ": " + formattedValue);
        } else if (this.readOnlyValue.isVisible()) {
            this.readOnlyValue.setText(name + ": " + formattedValue);
            this.valueTextField.setText(formattedValue);
        }
    }

}
