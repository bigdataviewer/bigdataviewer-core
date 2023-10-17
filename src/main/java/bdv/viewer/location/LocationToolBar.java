package bdv.viewer.location;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * Toolbar containing location information along with UI components to change it.
 *
 * @author Eric Trautman
 */
public class LocationToolBar extends JToolBar {

    private final DimensionCoordinatePanel.ChangeListener dimensionChangeListener;
    private final JPanel dimensionPanel;
    private final List<String> dimensionNames;
    private final List<DimensionCoordinatePanel> centerCoordinatePanelList;
    private final List<DimensionCoordinatePanel> mouseCoordinatePanelList;

    public LocationToolBar(final DimensionCoordinatePanel.ChangeListener dimensionChangeListener) {
        super("Location Tools");
        this.setFloatable(false);

        this.dimensionChangeListener = dimensionChangeListener;
        this.dimensionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 1));
        this.add(this.dimensionPanel);

        this.dimensionNames = Arrays.asList("x", "y", "z", "t");
        this.centerCoordinatePanelList = new ArrayList<>();
        this.mouseCoordinatePanelList = new ArrayList<>();
    }

    public void setDimensionNames(final List<String> dimensionNames) {
        this.dimensionNames.clear();
        this.dimensionNames.addAll(dimensionNames);

        for (int i = 0; i < this.centerCoordinatePanelList.size(); i++) {
            this.centerCoordinatePanelList.get(i).setName(getDimensionName(i));
            this.mouseCoordinatePanelList.get(i).setName(getDimensionName(i));
        }
    }

    public void setPositions(final double[] centerPosition,
                             final double[] mousePosition) {
        if (centerPosition.length != this.centerCoordinatePanelList.size()) {
            setNumberOfDimensions(centerPosition.length);
        }
        for (int i = 0; i < centerPosition.length; i++) {
            this.centerCoordinatePanelList.get(i).setPosition(centerPosition[i]);
            this.mouseCoordinatePanelList.get(i).setPosition(mousePosition[i]);
        }
    }

    private synchronized void setNumberOfDimensions(final int numberOfDimensions) {
        if (numberOfDimensions != centerCoordinatePanelList.size()) {
            dimensionPanel.removeAll();
            addDimensions(numberOfDimensions, false, centerCoordinatePanelList);
            addDimensions(numberOfDimensions, true, mouseCoordinatePanelList);
            dimensionPanel.revalidate(); // need this to make panel components visible
        }
    }

    private void addDimensions(final int numberOfDimensions,
                               final boolean isReadOnly,
                               final List<DimensionCoordinatePanel> coordinatePanelList) {
        for (int i = 0; i < numberOfDimensions; i++) {
            final DimensionCoordinatePanel coordinatePanel = new DimensionCoordinatePanel(getDimensionName(i),
                                                                                          i,
                                                                                          isReadOnly,
                                                                                          dimensionChangeListener);
            coordinatePanelList.add(coordinatePanel);
            dimensionPanel.add(coordinatePanel);
        }
    }

    private String getDimensionName(final int dimensionIndex) {
        return dimensionIndex < this.dimensionNames.size() ? this.dimensionNames.get(dimensionIndex) : "d" + dimensionIndex;
    }
}
