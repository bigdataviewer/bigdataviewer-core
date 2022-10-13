package bdv.img.omezarr;


import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.embl.mobie.io.ome.zarr.hackathon.MultiscaleImage;
import org.embl.mobie.io.ome.zarr.hackathon.Multiscales;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class ZarrImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
    private final String zpath;
    private final Map<ViewId, String> zgroups;
    private final AbstractSequenceDescription<?, ?, ?> seq;

    private Map<Integer, SetupImgLoader> setupImgLoaders ;

    public ZarrImageLoader(final String zpath, final Map< ViewId, String > zgroups, final AbstractSequenceDescription<?, ?, ?> sequenceDescription)
    {
        this.zpath = zpath;
        this.zgroups = zgroups;
        this.seq = sequenceDescription;
    }

    synchronized void openZarr()
    {
        if ( setupImgLoaders == null )
        {
            try
            {
                setupImgLoaders = new HashMap<>();
                final List<? extends BasicViewSetup> setups = seq.getViewSetupsOrdered();
                for (final BasicViewSetup setup: setups)
                {
                    final SetupImgLoader newLoader = createSetupImgLoader(setup.getId());
                    setupImgLoaders.put(setup.getId(), newLoader);
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }

        }
    }

    private < T extends NativeType< T > & RealType<T>, V extends Volatile< T > & NativeType< V > & RealType<V>>
    SetupImgLoader< T, V > createSetupImgLoader(final int setupId ) throws IOException {
        final NavigableSet<Integer> tpIds = new TreeSet<>();
        for (final ViewId view : zgroups.keySet()) {
            if (view.getViewSetupId() == setupId) {
                tpIds.add(view.getTimePointId());
            }
        }
        ViewId firstVId = new ViewId(tpIds.pollFirst(),setupId);
        if (firstVId == null)
            return null;

        final MultiscaleImage<T, V> mscImg = new MultiscaleImage<>(Paths.get(zpath,zgroups.get(firstVId)).toString()
                , null);
        return new SetupImgLoader<T, V>(mscImg, firstVId, tpIds);
    }

    @Override
    public CacheControl getCacheControl()
    {
        // FIXME Implement cache control
        return new CacheControl.Dummy();
    }

    class SetupImgLoader< T extends NativeType< T > & RealType<T>, V extends Volatile< T > & NativeType< V > & RealType<V> >
            extends AbstractViewerSetupImgLoader< T, V >
            implements MultiResolutionSetupImgLoader< T >
    {
        private int setupId;

        private NavigableMap<Integer,MultiscaleImage<T,V>> tpMmultiscaleImages = new TreeMap<>();
        private double[][] mipmapresolutions;
        private AffineTransform3D[] mipmaptransforms;

        /**
         * @param firstMscImg First MultiscaleImage instance belonging to this setupId. The only one if 1 timepoint only.
         *                    We use the MultiscaleImage to determine our own type which we need right at the point of construction.
         *
         * @param firstVId First ViewId. Give the setupId for this SetupImgLoader and the timeppointId for the MultiscaleImage.
         * @param tpIdSet A sorted set of remaining timepoints to be added to this loader.
         *                NO check at the moment that these images are compatible in type!
         */
        public SetupImgLoader(final MultiscaleImage<T,V> firstMscImg, final ViewId firstVId, final SortedSet<Integer> tpIdSet)
        {
            super(firstMscImg.getType(), firstMscImg.getVolatileType());
            setupId = firstVId.getViewSetupId();
            /* TODO in zarr nothing guarantees that multiple resolutions of an image are of the same type.
                Must be verified. MultiscaleImage does not support different types of resolutions either.
             */
            tpMmultiscaleImages.put(firstVId.getTimePointId(), firstMscImg);
            for (int tpId: tpIdSet)
            {
                // TODO validate that further timepoints have the same type
                tpMmultiscaleImages.put(tpId,new MultiscaleImage<>(
                        Paths.get(zpath,zgroups.get(new ViewId(tpId,setupId))).toString(),
                        null));
            }

            calculateMipmapTransforms();
        }

        public SetupImgLoader(final T type, final V volatileType, final int setupId )
        {
            super( type, volatileType );
            this.setupId = setupId;
        }

        @Override
        public RandomAccessibleInterval< V > getVolatileImage(final int timepointId, final int level, final ImgLoaderHint... hints )
        {
            final MultiscaleImage<T, V> mscImg = tpMmultiscaleImages.get(timepointId);
            RandomAccessibleInterval<V> volatileImg = mscImg.getVolatileImg(level);
            return Views.hyperSlice(Views.hyperSlice(volatileImg, 4,0),3,0);
        }

        @Override
        public Dimensions getImageSize(final int timepointId, final int level )
        {
            return new FinalDimensions(tpMmultiscaleImages.get(timepointId).getDimensions(level));
        }

        @Override
        public RandomAccessibleInterval< T > getImage( final int timepointId, final int level, final ImgLoaderHint... hints )
        {
            final MultiscaleImage<T, V> mscImg = tpMmultiscaleImages.get(timepointId);
            return mscImg.getImg(level);
        }

        /**
         * Reorder Zarr spatial transformation scaling or translation json vector values into X,Y,Z order.
         *
         * Assume that the input vector is in the OME json order, i.e. usually t, ch, z, y, x while
         * Multiscales.getSpatialAxisIndex already swaps indices to the java order of x, y, z, ch, t.
         *
         * @param mscales Multiscales instance that contains the axes order
         * @param v       Transformation scaling in the order as defined in the OME-Zarr coordinateTransform entry
         * @param c       Default vector value if not all X, Y, Z axes are present. Usually 1 for scaling, 0 for translation vectors.
         * @return double[3]: scale values in X, Y, Z order. If an axis is not defined in zarr metadata c is returned.
         */
        private double[] getXYZjsonVector(final Multiscales mscales, final double[] v, double c)
        {
            final double[] a = {c, c, c};
            final int d = mscales.numDimensions() - 1;
            final int xAxisIndex = mscales.getSpatialAxisIndex(Multiscales.Axis.X_AXIS_NAME);
            if ( xAxisIndex >=0 )
                a[0] = v[d - xAxisIndex];
            final int yAxisIndex = mscales.getSpatialAxisIndex(Multiscales.Axis.Y_AXIS_NAME);
            if ( yAxisIndex >=0 )
                a[1] = v[d - yAxisIndex];
            final int zAxisIndex = mscales.getSpatialAxisIndex(Multiscales.Axis.Z_AXIS_NAME);
            if ( zAxisIndex >=0 )
                a[2] = v[d - zAxisIndex];
            return a;
        }

        /**
         * Convert a coordinateTransformations JSON entry into an AffineTransform3D instance
         *
         * @param mscales Multiscales instance to determine axis indices
         * @param t Transformation as loaded from the OME JSON by GSON
         * @return New AffineTransform3D
         */
        private AffineTransform3D convertOmeTransform(final Multiscales mscales, final Multiscales.CoordinateTransformations t)
        {
            final AffineTransform3D affT = new AffineTransform3D();
            switch (t.type)
            {
                case "scale":
                    final double[] xyzScale = getXYZjsonVector(mscales, t.scale, 1.);
                    affT.scale(xyzScale[0], xyzScale[1], xyzScale[2]);
                    break;
                case "translation":
                    affT.setTranslation(getXYZjsonVector(mscales, t.translation, 0.));
                    break;
                case "identity":
                    break;
                default:
                    throw new RuntimeException("Unknown transformation type");
            }
            return affT;
        }

        /** Concatenates the given OME transformations into one affine transformation.
         *
         * dataset: (t1, t2), multiscales: (t3), then these should be applied in this order.
         *
         * As AffineTransform3D, we need to calculate t3 x t2 x t1
         *
         * @param mscales Multiscales instance to determine axis order.
         * @param arrTransforms Array of arrays of OME coordinateTransformations.
         * @return Concatenated transformations. Return identity if inputs are empty.
         */
        private AffineTransform3D concatenateOMETransforms(final Multiscales mscales,
                                                           final Multiscales.CoordinateTransformations[]... arrTransforms)
        {
            final AffineTransform3D affT = new AffineTransform3D();
            for (int i=arrTransforms.length; i-- >0; ) {
                if (arrTransforms[i] != null)
                    for (int j = arrTransforms[i].length; j-- > 0; )
                        affT.concatenate(convertOmeTransform(mscales, arrTransforms[i][j]));
            }
            return affT;
        }

        /**
         * Create the 3D affine transformations and calculate the resolution factors
         * for the multi resolution display relative to the first resolution.
         *
         * The 0th dataset must be the finest resolution that will have an identity transformation.
         *
         * Convert the OME json transformations into AffineTransform3D objects then normalize them relative
         * to the 0th dataset. (The json transformations convert into physical coordinates.) Handle both scaling and
         * translation. Handle both "multiscale" (global transformations for all datasets)
         * and "dataset" level entries in the OME metadata.
         *
         * Assume that the coordinateTransforms do not correct for the pixel center alignment
         * offsets at the different resolutions levels. Add correction for the pixel center translation here.
         *
         * Assume that dimensions and resolutions are defined exactly the same in case there are multiple timepoints.
         * Definitions are taken from first timepoint.
         *
         * Set "mipmaptransforms" and "mipmapresolutions".
         *
         */
        private void calculateMipmapTransforms()
        {
            // Assume everything is the same in case there are multiple timepoints
            final Multiscales mscale = tpMmultiscaleImages.get(0).getMultiscales();
            final int numResolutions = tpMmultiscaleImages.get(0).numResolutions();
            final int numDimensions = tpMmultiscaleImages.get(0).numDimensions();
            final Multiscales.CoordinateTransformations[] globalTransformations = mscale.getCoordinateTransformations();

            mipmaptransforms = new AffineTransform3D[numResolutions];
            for (int i_res=0; i_res<numResolutions; ++i_res)
            {
                final Multiscales.CoordinateTransformations[] datasetTransformations =
                        mscale.getDatasets()[i_res].coordinateTransformations;
                mipmaptransforms[i_res] = concatenateOMETransforms(mscale, globalTransformations,
                        datasetTransformations);
            }

            // Normalize to the first transformation
            final AffineTransform3D T0inv = mipmaptransforms[0].inverse();
            mipmaptransforms[0] = new AffineTransform3D();  // identity
            for (int j=1; j<numResolutions; ++j)
            {
                final AffineTransform3D affT = T0inv.copy();
                affT.concatenate(mipmaptransforms[j]);
                mipmaptransforms[j] = affT;
            }

            // Copy out the scaling from the transformations to the mipmapresolutions
            mipmapresolutions = new double[numResolutions][];
            for (int j=0; j<numResolutions; ++j)
            {
                mipmapresolutions[j] = new double[] { mipmaptransforms[j].get(0,0),
                        mipmaptransforms[j].get(1,1),
                        mipmaptransforms[j].get(2,2) };
            }

            // Add pixel center correction
            for (int j=1; j<numResolutions; ++j)
            {
                mipmaptransforms[j].translate(
                        0.5*(mipmapresolutions[j][0]-1.),
                        0.5*(mipmapresolutions[j][1]-1.),
                        0.5*(mipmapresolutions[j][2]-1.));
            }

        }

        @Override
        public double[][] getMipmapResolutions()
        {
            return mipmapresolutions;
        }


        @Override
        public AffineTransform3D[] getMipmapTransforms()
        {
            return mipmaptransforms;
        }

        @Override
        public int numMipmapLevels()
        {
            return tpMmultiscaleImages.get(0).numResolutions();
        }

        @Override
        public VoxelDimensions getVoxelSize(final int timepointId )
        {
            return null;
        }
    }

    @Override
    public SetupImgLoader getSetupImgLoader( final int setupId )
    {
        openZarr();
        return setupImgLoaders.get(setupId);
    }

    // TODO ?
//		@Override
//		public void setNumFetcherThreads( final int n ) {}

    // TODO ?
//		@Override
//		public void setCreatedSharedQueue( final SharedQueue createdSharedQueue ) {}
}
