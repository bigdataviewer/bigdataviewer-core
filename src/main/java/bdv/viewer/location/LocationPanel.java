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

            final DimensionCoordinateComponents coordinateComponents =
                    new DimensionCoordinateComponents(dimension,
                                                      sourceInterval.min(dimension),
                                                      sourceInterval.max(dimension));

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
        }

        // connect dimension components to support multi-value paste
        for (int d = 1; d < dimensionComponentsList.size(); d++) {
            final DimensionCoordinateComponents prior = dimensionComponentsList.get(d - 1);
            prior.setNextDimensionComponents(dimensionComponentsList.get(d));
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
            this.dimensionComponentsList.get(dimension).setMinAndMaxPosition(sourceInterval.min(dimension),
                                                                             sourceInterval.max(dimension));
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
        for (int i = 0; i < centerPosition.length; i++) {
            this.dimensionComponentsList.get(i).setPosition(centerPosition[i]);
        }
    }

}
