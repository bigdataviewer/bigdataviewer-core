package viewer.render.labelling;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.sparse.NtreeImgFactory;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.labeling.LabelingMapping;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import viewer.display.LabelingTypeARGBConverter;
import viewer.render.Interpolation;
import viewer.render.Source;

public class LabellingSource implements Source<ARGBType> {
	private int currentTimepoint;

	private NativeImgLabeling<Integer, IntType> currentSource;

	private final Source<?> imgSource;

	private final String name;

	final LabelingTypeARGBConverter<Integer> converter;

	final protected static int numInterpolationMethods = 2;

	final protected static int iNearestNeighborMethod = 0;

	final protected static int iNLinearMethod = 1;

	final protected InterpolatorFactory<ARGBType, RandomAccessible<ARGBType>>[] interpolatorFactories;

	final AffineTransform3D sourceTransform = new AffineTransform3D();

	private volatile int oldAlpha = -1;

	private int currentAlpha;

	@SuppressWarnings("unchecked")
	public LabellingSource(final Source<?> imgSource) {
		this.imgSource = imgSource;
		name = imgSource.getName() + " annotations";
		converter = new LabelingTypeARGBConverter<Integer>(new HashMap<List<Integer>, ARGBType>());
		interpolatorFactories = new InterpolatorFactory[numInterpolationMethods];
		interpolatorFactories[iNearestNeighborMethod] = new NearestNeighborInterpolatorFactory<ARGBType>();
		interpolatorFactories[iNLinearMethod] = new NLinearInterpolatorFactory<ARGBType>();
		loadTimepoint(0);
	}

	@Override
	public boolean isPresent(final int t) {
		return imgSource.isPresent(t);
	}

	@Override
	public RandomAccessibleInterval<ARGBType> getSource(final int t, final int level) {
		if (t != currentTimepoint)
			loadTimepoint(t);
		if (currentAlpha != oldAlpha)
			updateColorTable();
		return Converters.convert((RandomAccessibleInterval<LabelingType<Integer>>) currentSource, converter, new ARGBType());
	}

	@Override
	public RealRandomAccessible<ARGBType> getInterpolatedSource(final int t, final int level, final Interpolation method) {
		return Views.interpolate(Views.extendValue(getSource(t, level), new ARGBType()), interpolatorFactories[method == Interpolation.NLINEAR ? iNLinearMethod : iNearestNeighborMethod]);
	}

	@Override
	public int getNumMipmapLevels() {
		return 1;
	}

	void loadTimepoint(final int timepoint) {
		currentTimepoint = timepoint;
		if (isPresent(timepoint)) {
			final Dimensions sourceDimensions = imgSource.getSource(timepoint, 0);

			// TODO: fix this HORRIBLE hack that deals with z-scaling...
			final AffineTransform3D sourceTransform = imgSource.getSourceTransform(timepoint, 0);
			final long[] dim = new long[sourceDimensions.numDimensions()];
			sourceDimensions.dimensions(dim);
			dim[2] *= sourceTransform.get(2, 2);

			final NtreeImgFactory<IntType> factory = new NtreeImgFactory<IntType>();
			final Img<IntType> img = factory.create(dim, new IntType());
			final NativeImgLabeling<Integer, IntType> labeling = new NativeImgLabeling<Integer, IntType>(img);
			currentSource = labeling;
			updateColorTable();
		} else
			currentSource = null;
	}

	@Override
	public AffineTransform3D getSourceTransform(final int t, final int level) {
		return sourceTransform;
	}

	@Override
	public ARGBType getType() {
		return new ARGBType();
	}

	@Override
	public String getName() {
		return name;
	}

	public LabelingTypeARGBConverter<Integer> getConverter() {
		return null;
	}

	private void updateColorTable() {
		final int a = currentAlpha;
		final HashMap<List<Integer>, ARGBType> colorTable = new HashMap<List<Integer>, ARGBType>();
		final LabelingMapping<Integer> mapping = currentSource.getMapping();
		final int numLists = mapping.numLists();
		final Random random = new Random(1);
		for (int i = 0; i < numLists; ++i) {
			final List<Integer> list = mapping.listAtIndex(i);
			final int r = random.nextInt(256);
			final int g = random.nextInt(256);
			final int b = random.nextInt(256);
			colorTable.put(list, new ARGBType(ARGBType.rgba(r, g, b, a)));
		}
		colorTable.put(mapping.emptyList(), new ARGBType(0));
		converter.setColorTable(colorTable);
		oldAlpha = a;
	}
}
