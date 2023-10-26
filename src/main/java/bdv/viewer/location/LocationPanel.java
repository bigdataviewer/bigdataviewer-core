package bdv.viewer.location;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import net.imglib2.Interval;

/**
 * Panel containing UI components for displaying and editing location coordinates.
 *
 * @author Eric Trautman
 */
public class LocationPanel
        extends JPanel {

    private final List<DimensionCoordinateComponents> dimensionComponentsList;

    public LocationPanel(final Interval sourceInterval) {

        super(new GridBagLayout());

        this.dimensionComponentsList = new ArrayList<>();
        for (int dimension = 0; dimension < sourceInterval.numDimensions(); dimension++) {
            addDimension(dimension,
                         sourceInterval.min(dimension),
                         sourceInterval.max(dimension));
        }
    }

    /**
     * Set the single listener to be called whenever a dimension value is changed.
     * Typical usage is
     * <pre>
     * e -> {
     *   dcc = (DimensionCoordinateComponents) e.getSource();
     *   viewerPanel.centerViewAt(dcc.getPosition(), dcc.getDimension());
     * }
     * </pre>
     */
    public void setDimensionValueChangeListener(final ChangeListener changeListener) {
        for (final DimensionCoordinateComponents dimensionComponents : dimensionComponentsList) {
            dimensionComponents.setDimensionValueChangeListener(changeListener);
        }
    }

    public void setSourceInterval(final Interval sourceInterval) {

        for (int dimension = 0; dimension < sourceInterval.numDimensions(); dimension++) {
            if (dimension < dimensionComponentsList.size()) {
                dimensionComponentsList.get(dimension).setMinAndMaxPosition(sourceInterval.min(dimension),
                                                                            sourceInterval.max(dimension));
            } else {
                addDimension(dimension,
                             sourceInterval.min(dimension),
                             sourceInterval.max(dimension));
            }
        }

        for (int dimension = dimensionComponentsList.size(); dimension > sourceInterval.numDimensions() ; dimension--) {
            final DimensionCoordinateComponents removedComponents = dimensionComponentsList.remove(dimension);
            this.remove(removedComponents.getValueLabel());
            this.remove(removedComponents.getValueTextField());
            this.remove(removedComponents.getValueSlider());
        }
    }

    public void requestFocusOnFirstComponent() {
        if (! dimensionComponentsList.isEmpty()) {
            this.dimensionComponentsList.get(0).getValueTextField().requestFocus();
        }
    }

    @SuppressWarnings("unused")
    public void setNames(final String[] dimensionNames) {
        for (int i = 0; i < dimensionNames.length && i < this.dimensionComponentsList.size(); i++) {
            this.dimensionComponentsList.get(i).setName(dimensionNames[i]);
        }
    }

    public void setCenterPosition(final double[] centerPosition) {
        for (int i = 0; i < centerPosition.length && i < dimensionComponentsList.size(); i++) {
            dimensionComponentsList.get(i).setPosition(centerPosition[i]);
        }
    }

    private void addDimension(final int dimension,
                              final double minPosition,
                              final double maxPosition) {

        final DimensionCoordinateComponents coordinateComponents =
                new DimensionCoordinateComponents(dimension,
                                                  minPosition,
                                                  maxPosition);

        this.dimensionComponentsList.add(coordinateComponents);

        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = dimension * 2;
        c.weightx = 0.01;
        this.add(coordinateComponents.getValueLabel(), c);

        // text field should grow with card width
        c.gridx = 1;
        c.weightx = 0.99;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(coordinateComponents.getValueTextField(), c);

        c.gridy++;
        c.weightx = 0.99;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(coordinateComponents.getValueSlider(), c);

        if (dimension > 0) {
            // connect dimension components to support multi-value paste
            final DimensionCoordinateComponents prior = dimensionComponentsList.get(dimension - 1);
            prior.setNextDimensionComponents(coordinateComponents);
        }
    }
}
