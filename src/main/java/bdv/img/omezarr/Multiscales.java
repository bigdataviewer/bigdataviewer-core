package bdv.img.omezarr;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store OME-zarr json metadata.
 *
 * In this class most fields store information in the order it appears in the JSON file.
 * {@code axes} follows the t,ch,z,y,x order; but {@code axisList} has the reversed, java order.
 * {@code coordinateTransformations} follows the JSON order, first element is applicable first.
 *
 * Original code copied from {@code org.embl.mobie.io.ome.zarr.util.OmeZarrMultiscales}
 *
 */
public class Multiscales
{
    // key in json for multiscales
    public static final String MULTI_SCALE_KEY = "multiscales";

    // Serialisation
    private String version;
    private String name;
    private String type;
    private Axis[] axes; // from v0.4+ within JSON
    private Dataset[] datasets;
    private CoordinateTransformations[] coordinateTransformations; // from v0.4+ within JSON

    // Runtime

    // Simply contains the {@code Axes[] axes}
    // but in reversed order to accommodate
    // the Java array ordering of the image data.
    private List< Axis > axisList;
    private int numDimensions;

    public Multiscales() {
    }

    public static class Dataset {
        public String path;
        public CoordinateTransformations[] coordinateTransformations;
    }

    /**
     * Object to represent a coordinateTransformation in the json metadata
     * Elements in {@code scale} and {@code translation} follow the JSON order of axes.
     */
    public static class CoordinateTransformations {
        public String type;
        public double[] scale;
        public double[] translation;
        public String path;
    }

    public static class Axis
    {
        public static final String CHANNEL_TYPE = "channel";
        public static final String TIME_TYPE = "time";
        public static final String SPATIAL_TYPE = "space";

        public static final String X_AXIS_NAME = "x";
        public static final String Y_AXIS_NAME = "y";
        public static final String Z_AXIS_NAME = "z";

        public String name;
        public String type;
        public String unit;
    }

    public void init()
    {
        axisList = new ArrayList<Axis>(axes.length);
        for (int i=axes.length; i-- >0; ) {
            axisList.add(axes[i]);
        }
        numDimensions = axisList.size();
    }

    // TODO Can this be done with a JSONAdapter ?
    public void applyVersionFixes( JsonElement multiscales )
    {
        String version = multiscales.getAsJsonObject().get("version").getAsString();
        if ( version.equals("0.3") ) {
            JsonElement axes = multiscales.getAsJsonObject().get("axes");
            // FIXME
            //   - populate Axes[]
            //   - populate coordinateTransformations[]
            throw new RuntimeException("Parsing version 0.3 not yet implemented.");
        } else if ( version.equals("0.4") ) {
            // This should just work automatically
        } else {
            JsonElement axes = multiscales.getAsJsonObject().get("axes");
            // FIXME
            //   - populate Axes[]
            //   - populate coordinateTransformations[]
            throw new RuntimeException("Parsing version "+ version + " is not yet implemented.");
        }
    }
    /**
     * @return The java index of the channel axis if present. Otherwise -1.
     */
    public int getChannelAxisIndex()
    {
        for ( int d = 0; d < numDimensions; d++ )
            if ( axisList.get( d ).type.equals( Axis.CHANNEL_TYPE ) )
                return d;
        return -1;
    }

    /**
     * @return The java index of the time axis if present. Otherwise -1.
     */
    public int getTimePointAxisIndex()
    {
        for ( int d = 0; d < numDimensions; d++ )
            if ( axisList.get( d ).type.equals( Axis.TIME_TYPE ) )
                return d;
        return -1;
    }

    /**
     * @param axisName The name of the axis as defined in the Axis class.
     * @return The java index of the spatial axis if present. Otherwise -1.
     */
    public int getSpatialAxisIndex( String axisName )
    {
        for ( int d = 0; d < numDimensions; d++ )
            if ( axisList.get( d ).type.equals( Axis.SPATIAL_TYPE )
                 && axisList.get( d ).name.equals( axisName ) )
                return d;
        return -1;
    }

    public List< Axis > getAxes()
    {
        return axisList;
    }

    /**
     * Get the global coordinate transformations of the multiscales section.
     *
     * Note that there are coordinate transformation entries in {@code datasets} that should
     * be applied before these global transformations.
     *
     * @return CoordinateTransformations[]
     */
    public CoordinateTransformations[] getCoordinateTransformations()
    {
        return coordinateTransformations;
    }

    public Dataset[] getDatasets()
    {
        return datasets;
    }

    public int numDimensions()
    {
        return numDimensions;
    }
}
