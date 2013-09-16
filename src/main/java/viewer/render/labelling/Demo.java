package viewer.render.labelling;

import ij.ImageJ;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.region.localneighborhood.HyperSphereShape;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.labeling.LabelingType;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import viewer.SequenceViewsLoader;
import viewer.SpimSource;
import viewer.SpimViewer;
import viewer.gui.brightness.BrightnessDialog;
import viewer.gui.brightness.ConverterSetup;
import viewer.gui.brightness.MinMaxGroup;
import viewer.gui.brightness.SetupAssignments;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import viewer.render.SourceState;
import viewer.render.ViewerState;
import viewer.util.Affine3DHelpers;

public class Demo {

	private ArrayList<AbstractLinearRange> displayRanges;
	private LabellingSource overlay;
	private final SpimViewer viewer;
	private int nTimepoints;
	private ArrayList<SourceAndConverter<?>> sources;
	private SetupAssignments setupAssignments;
	private final BrightnessDialog brightnessDialog;
	private int nextCellId = 0;
	private final int defaultRadius = 5;
	private boolean editMode = false;
	private TransformEventHandler<AffineTransform3D> transformEventHandler;
	private final RegionGrowingAnnotationTool regionGrowingAnnotationTool;

	public Demo(final File file) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException {

		/*
		 * Load image source
		 */

		prepareSources(file);

		viewer = newViewer();
		initTransform(viewer, 800, 600);
		initBrightness(viewer, 0, 1);

		/*
		 * Region annotation listener
		 */

		regionGrowingAnnotationTool = new RegionGrowingAnnotationTool(viewer, sources.get(0).getSpimSource(), overlay, 0, 0);

		/*
		 * Brightness
		 */

		brightnessDialog = new BrightnessDialog(null, setupAssignments);

	}

	private void prepareSources(final File dataFile) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException {
		final SequenceViewsLoader loader = new SequenceViewsLoader(dataFile.getAbsolutePath());
		final SequenceDescription seq = loader.getSequenceDescription();
		nTimepoints = seq.numTimepoints();
		sources = new ArrayList<SourceAndConverter<?>>();
		final ArrayList<ConverterSetup> converterSetups = new ArrayList<ConverterSetup>();
		for (int setup = 0; setup < seq.numViewSetups(); ++setup) {
			final RealARGBColorConverter<UnsignedShortType> converter = new RealARGBColorConverter<UnsignedShortType>(0, 65535);
			converter.setColor(new ARGBType(ARGBType.rgba(255, 255, 255, 255)));
			sources.add(new SourceAndConverter<UnsignedShortType>(new SpimSource(loader, setup, "angle " + seq.setups.get(setup).getAngle()), converter));
			final int id = setup;
			converterSetups.add(new ConverterSetup() {
				@Override
				public void setDisplayRange(final int min, final int max) {
					converter.setMin(min);
					converter.setMax(max);
					requestRepaintAllViewers();
				}

				@Override
				public void setColor(final ARGBType color) {
					converter.setColor(color);
					requestRepaintAllViewers();
				}

				@Override
				public int getSetupId() {
					return id;
				}

				@Override
				public int getDisplayRangeMin() {
					return (int) converter.getMin();
				}

				@Override
				public int getDisplayRangeMax() {
					return (int) converter.getMax();
				}

				@Override
				public ARGBType getColor() {
					return converter.getColor();
				}
			});
		}

		/*
		 * Create setup assignments (for managing brightness and color).
		 */

		setupAssignments = new SetupAssignments(converterSetups, 0, 65535);
		final MinMaxGroup group = setupAssignments.getMinMaxGroups().get(0);
		for (final ConverterSetup setup : setupAssignments.getConverterSetups()) {
			setupAssignments.moveSetupToGroup(setup, group);
		}

		overlay = new LabellingSource(sources.get(0).getSpimSource());
		sources.add(new SourceAndConverter<ARGBType>(overlay, new TypeIdentity<ARGBType>()));
		overlay.getSource(0, 0);
	}

	private SpimViewer newViewer() {
		final SpimViewer viewer = new SpimViewer(800, 600, sources, nTimepoints);

		viewer.addKeyAction(KeyStroke.getKeyStroke("A"), new AbstractAction("add cell") {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				final RealPoint p = new RealPoint(3);
				viewer.getGlobalMouseCoordinates(p);
				addCellAt(p);
			}

			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(KeyStroke.getKeyStroke("ESCAPE"), new AbstractAction("toggle mode") {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				toggleMode();
			}

			private static final long serialVersionUID = 1L;
		});

		viewer.addKeyAction(KeyStroke.getKeyStroke("S"), new AbstractAction("brightness settings") {
			@Override
			public void actionPerformed(final ActionEvent arg0) {
				toggleBrightnessDialog();
			}

			private static final long serialVersionUID = 1L;
		});

		return viewer;

	}

	public void toggleBrightnessDialog() {
		brightnessDialog.setVisible(!brightnessDialog.isVisible());
	}

	private void requestRepaintAllViewers() {
		if (null != viewer) {
			viewer.requestRepaint();
		}
	}

	private void initBrightness(final SpimViewer viewer, final double cumulativeMinCutoff, final double cumulativeMaxCutoff) {
		final ViewerState state = viewer.getState();
		final Source<?> source = state.getSources().get(state.getCurrentSource()).getSpimSource();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final RandomAccessibleInterval<UnsignedShortType> img = (RandomAccessibleInterval) source.getSource(state.getCurrentTimepoint(), source.getNumMipmapLevels() - 1);
		final long z = (img.min(2) + img.max(2) + 1) / 2;

		final int numBins = 6535;
		final Histogram1d<UnsignedShortType> histogram = new Histogram1d<UnsignedShortType>(Views.iterable(Views.hyperSlice(img, 2, z)), new Real1dBinMapper<UnsignedShortType>(0, 65535, numBins, false));
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] { 0 };
		double cumulative = 0;
		int i = 0;
		for (; i < numBins && cumulative < cumulativeMinCutoff; ++i) {
			bin[0] = i;
			cumulative += dfd.relativeFrequency(bin);
		}
		final int min = i * 65535 / numBins;
		for (; i < numBins && cumulative < cumulativeMaxCutoff; ++i) {
			bin[0] = i;
			cumulative += dfd.relativeFrequency(bin);
		}
		final int max = i * 65535 / numBins;
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get(0);
		minmax.getMinBoundedValue().setCurrentValue(min);
		minmax.getMaxBoundedValue().setCurrentValue(max);
	}

	private void initTransform(final SpimViewer viewer, final int viewerWidth, final int viewerHeight) {
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final ViewerState state = viewer.getState();
		final SourceState<?> source = state.getSources().get(state.getCurrentSource());
		final int timepoint = state.getCurrentTimepoint();
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform(timepoint, 0);

		final Interval sourceInterval = source.getSpimSource().getSource(timepoint, 0);
		final double sX0 = sourceInterval.min(0);
		final double sX1 = sourceInterval.max(0);
		final double sY0 = sourceInterval.min(1);
		final double sY1 = sourceInterval.max(1);
		final double sZ0 = sourceInterval.min(2);
		final double sZ1 = sourceInterval.max(2);
		final double sX = (sX0 + sX1 + 1) / 2;
		final double sY = (sY0 + sY1 + 1) / 2;
		final double sZ = (sZ0 + sZ1 + 1) / 2;

		final double[][] m = new double[3][4];

		// rotation
		final double[] qSource = new double[4];
		final double[] qViewer = new double[4];
		Affine3DHelpers.extractApproximateRotationAffine(sourceTransform, qSource, 2);
		LinAlgHelpers.quaternionInvert(qSource, qViewer);
		LinAlgHelpers.quaternionToR(qViewer, m);

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[3];
		final double[] translation = new double[3];
		sourceTransform.apply(centerSource, centerGlobal);
		LinAlgHelpers.quaternionApply(qViewer, centerGlobal, translation);
		LinAlgHelpers.scale(translation, -1, translation);
		LinAlgHelpers.setCol(3, translation, m);

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set(m);

		// scale
		final double[] pSource = new double[] { sX1, sY1, sZ };
		final double[] pGlobal = new double[3];
		final double[] pScreen = new double[3];
		sourceTransform.apply(pSource, pGlobal);
		viewerTransform.apply(pGlobal, pScreen);
		final double scaleX = cX / pScreen[0];
		final double scaleY = cY / pScreen[1];
		final double scale = Math.min(scaleX, scaleY);
		viewerTransform.scale(scale);

		// window center offset
		viewerTransform.set(viewerTransform.get(0, 3) + cX, 0, 3);
		viewerTransform.set(viewerTransform.get(1, 3) + cY, 1, 3);

		viewer.setCurrentViewerTransform(viewerTransform);
	}

	synchronized void addCellAt(final RealLocalizable p) {
		final int cellId = nextCellId;
		++nextCellId;
		final int radius = defaultRadius;
		addLabelHyperSphere(p, radius, cellId);
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	synchronized void removeCellsAt(final RealLocalizable p) {
		final RandomAccess<LabelingType<Integer>> a = overlay.getCurrentLabelling().randomAccess();
		new Round<RandomAccess<LabelingType<Integer>>>(a).setPosition(p);
		// TODO
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	synchronized void modifiyCellRadiusAt(final RealPoint p, final int diff) {
		final RandomAccess<LabelingType<Integer>> a = overlay.getCurrentLabelling().randomAccess();
		new Round<RandomAccess<LabelingType<Integer>>>(a).setPosition(p);
		for (final Integer label : a.get().getLabeling()) {
			// TODO
		}
		overlay.updateColorTable();
		viewer.requestRepaint();
	}

	private void addLabelHyperSphere(final RealLocalizable centerGlobal, final int radius, final Integer label) {
		final AffineTransform3D globalToSource = overlay.getSourceTransform(viewer.getState().getCurrentTimepoint(), 0).inverse();
		final RealPoint centerLocal = new RealPoint(centerGlobal.numDimensions());
		globalToSource.apply(centerGlobal, centerLocal);
		final HyperSphereShape sphere = new HyperSphereShape(radius);
		final IntervalView<LabelingType<Integer>> ext = Views.interval(Views.extendValue(overlay.getCurrentLabelling(), new LabelingType<Integer>()), Intervals.expand(overlay.getCurrentLabelling(), radius));
		final RandomAccess<Neighborhood<LabelingType<Integer>>> na = sphere.neighborhoodsRandomAccessible(ext).randomAccess();
		new Round<RandomAccess<Neighborhood<LabelingType<Integer>>>>(na).setPosition(centerLocal);
		for (final LabelingType<Integer> t : na.get()) {
			final List<Integer> l = t.getLabeling();
			if (!l.contains(label)) {
				final ArrayList<Integer> labels = new ArrayList<Integer>(t.getLabeling());
				labels.add(label);
				t.setLabeling(labels);
			}
		}
	}

	private void removeLabelHyperSphere(final RealLocalizable center, final int radius, final Integer label) {
		final HyperSphereShape sphere = new HyperSphereShape(radius);
		final IntervalView<LabelingType<Integer>> ext = Views.interval(Views.extendValue(overlay.getCurrentLabelling(), new LabelingType<Integer>()), Intervals.expand(overlay.getCurrentLabelling(), radius));
		final RandomAccess<Neighborhood<LabelingType<Integer>>> na = sphere.neighborhoodsRandomAccessible(ext).randomAccess();
		new Round<RandomAccess<Neighborhood<LabelingType<Integer>>>>(na).setPosition(center);
		for (final LabelingType<Integer> t : na.get()) {
			final List<Integer> l = t.getLabeling();
			if (l.contains(label)) {
				final ArrayList<Integer> labels = new ArrayList<Integer>(t.getLabeling());
				labels.remove(label);
				t.setLabeling(labels);
			}
		}
	}

	private void toggleMode() {
		this.editMode = !editMode;
		System.out.println(editMode);// DEBUG

		if (editMode) {
			transformEventHandler = viewer.getDisplay().getTransformEventHandler();
			viewer.getDisplay().removeHandler(transformEventHandler);
			viewer.getDisplay().addHandler(regionGrowingAnnotationTool);
		} else {
			viewer.getDisplay().removeHandler(regionGrowingAnnotationTool);
			viewer.getDisplay().addHandler(transformEventHandler);
		}
	}

	public static void main(final String[] args) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, JDOMException {
		ImageJ.main(args);

		final File file = new File("/Users/tinevez/Desktop/Data/Mamut/parhyale-crop/parhyale-crop-2.xml");
		final Demo plugin = new Demo(file);
	}

}
