package bdv.viewer.location;

import java.awt.Toolkit;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * UI components for displaying and editing coordinates for one dimension of a volume.
 *
 * @author Eric Trautman
 */
public class DimensionCoordinateComponents {

    public static final String[] DEFAULT_NAMES = { "x", "y", "z", "t" };
    public static final String DEFAULT_VALUE_FORMAT = "%1.0f";

    private final int dimension;
    private double minPosition;
    private double maxPosition;
    private final JLabel valueLabel;
    private final JTextField valueTextField;
    private final JSlider valueSlider;
    private final ChangeListener sliderChangeListener;

    private ChangeListener dimensionValueChangeListener;
    private String valueFormat;
    private double position;
    private DimensionCoordinateComponents nextDimensionComponents;

    public DimensionCoordinateComponents(final int dimension,
                                         final double minPosition,
                                         final double maxPosition) {
        this(getDefaultDimensionName(dimension), dimension, minPosition, maxPosition);
    }

    public DimensionCoordinateComponents(final String name,
                                         final int dimension,
                                         final double minPosition,
                                         final double maxPosition) {
        this(name, dimension, minPosition, maxPosition, DEFAULT_VALUE_FORMAT);
    }

    public DimensionCoordinateComponents(final String name,
                                         final int dimension,
                                         final double minPosition,
                                         final double maxPosition,
                                         final String valueFormat) {

        this.dimension = dimension;
        this.dimensionValueChangeListener = null;
        this.valueFormat = valueFormat;
        this.position = 0.0;

        this.valueLabel = new JLabel(name + ": ");
        this.valueTextField = new JTextField(5);
        this.valueTextField.setHorizontalAlignment(JTextField.RIGHT);

        // parse text and notify listeners when return is hit in text field
        this.valueTextField.addActionListener(e -> stopEditingTextField());
        this.valueTextField.setToolTipText("<html>Enter a value and hit return to change the current position.<br/>" +
                                           "Enter multiple values separated by spaces or commas to change multiple dimensions at once.</html>");

        this.valueSlider = new JSlider();
        this.valueSlider.setMajorTickSpacing(25);
        this.valueSlider.setPaintTicks(true);
        this.valueSlider.setPaintLabels(true);
        this.setMinAndMaxPosition(minPosition, maxPosition);

        this.sliderChangeListener = e -> {
            setPosition(getSliderPosition());
            if (! valueSlider.getValueIsAdjusting()) {
                notifyExternalListener();
            }
        };

        this.valueSlider.addChangeListener(this.sliderChangeListener);

        this.valueSlider.setToolTipText("Drag slider to change the current position.");

        // init valueTextArea to be same as valueSlider
        this.valueTextField.setText(formatValueAsDouble(getSliderPosition()));

        this.nextDimensionComponents = null;
    }

    public int getDimension() {
        return dimension;
    }

    public double getPosition() {
        return position;
    }

    public JLabel getValueLabel() {
        return valueLabel;
    }

    public JTextField getValueTextField() {
        return valueTextField;
    }

    public JSlider getValueSlider() {
        return valueSlider;
    }

    public void setMinAndMaxPosition(final double minPosition,
                                     final double maxPosition) {
        this.minPosition = minPosition;
        this.maxPosition = maxPosition;
        final Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel(formatSliderValue(minPosition)));
        labelTable.put(100, new JLabel(formatSliderValue(maxPosition)));
        this.valueSlider.setLabelTable(labelTable);
    }

    public void setNextDimensionComponents(final DimensionCoordinateComponents nextDimensionComponents) {
        this.nextDimensionComponents = nextDimensionComponents;
    }

    @SuppressWarnings("unused")
    public void setValueFormat(final String valueFormat) {
        if (! valueFormat.equals(this.valueFormat)) {
            this.valueFormat = valueFormat;
            this.valueTextField.setText(formatValueAsDouble(position));
        }
    }

    private void notifyExternalListener() {
        if (dimensionValueChangeListener != null) {
            dimensionValueChangeListener.stateChanged(new ChangeEvent(this));
        }
    }

    private double getSliderPosition() {
        final double sliderPosition = valueSlider.getValue();
        return (sliderPosition / 100.0) * (maxPosition - minPosition) + minPosition;
    }

    private void stopEditingTextField() {
        final List<String> textValues = Arrays.asList(MULTI_VALUE_PATTERN.split(valueTextField.getText()));
        setTextValues(textValues);
    }

    public void setTextValues(final List<String> textValues) {
        final int numberOfValues = textValues.size();
        if (numberOfValues > 0) {
            try {
                final double updatedPosition = Double.parseDouble(textValues.get(0));
                setPosition(updatedPosition);
                notifyExternalListener();
                if ((nextDimensionComponents != null) && numberOfValues > 1) {
                    nextDimensionComponents.setTextValues(textValues.subList(1, numberOfValues));
                }
            } catch (NumberFormatException e) {
                // ignore and leave position unchanged
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public void setDimensionValueChangeListener(final ChangeListener dimensionValueChangeListener) {
        this.dimensionValueChangeListener = dimensionValueChangeListener;
    }

    public void setName(final String name) {
        this.valueLabel.setText(name + ": ");
    }

    public void setPosition(final double position) {
        if (position != this.position) {
            // prevent external position change from triggering change events
            valueSlider.removeChangeListener(sliderChangeListener);

            this.position = position;

            valueTextField.setText(formatValueAsDouble(position));

            final double sliderPosition;
            if (position < minPosition) {
                sliderPosition = 0.0;
            } else if (position > maxPosition) {
                sliderPosition = 100.0;
            } else {
                sliderPosition = ((position - minPosition) / (maxPosition - minPosition)) * 100.0;
            }
            this.valueSlider.setValue((int) Math.round(sliderPosition));

            // restore listener now that we are done
            valueSlider.addChangeListener(sliderChangeListener);
        }
    }

    public String formatValueAsDouble(final double value) {
        return String.format(Locale.ROOT, valueFormat, value);
    }

    public String formatSliderValue(final double value) {
        final String longString = String.valueOf(Double.valueOf(value).longValue());
        return longString.length() < 8 ? longString : String.format(Locale.ROOT, "%1.1e", value);
    }

    private static final Pattern MULTI_VALUE_PATTERN = Pattern.compile("[\\s,]++");

    /**
     * @return default name for the specified dimension.
     */
    public static String getDefaultDimensionName(final int dimension) {
        return (dimension < DEFAULT_NAMES.length) ? DEFAULT_NAMES[dimension] : "d" + dimension;
    }
}
