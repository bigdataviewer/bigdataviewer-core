/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
