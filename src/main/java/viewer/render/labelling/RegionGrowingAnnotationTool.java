package viewer.render.labelling;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.regiongrowing.RegionGrowingTools.GrowingMode;
import net.imglib2.algorithm.regiongrowing.ThresholdRegionGrowing;
import net.imglib2.labeling.LabelingType;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import viewer.SpimViewer;
import viewer.render.Source;

public class RegionGrowingAnnotationTool<T extends RealType<T>> implements MouseListener, MouseMotionListener {

	private final SpimViewer viewer;
	private Map<long[], Integer> seed;
	private final LabelingSource labelingSource;
	private int label = 0;
	private final long[][] structuringElement;
	private final GrowingMode growingMode;
	private final NativeImgLabeling<Integer, IntType> labeling;
	/**
	 * A {@link RandomAccess} to the source image.
	 */
	private final RandomAccess<T> ra;
	/**
	 * The transform that maps source coordinates to mouse coordinates.
	 */
	private final AffineTransform3D sourceToGlobal;

	public RegionGrowingAnnotationTool(final SpimViewer viewer, final Source<T> source, final LabelingSource labellingSource, final int t) {
		this.viewer = viewer;
		this.labelingSource = labellingSource;
		this.labeling = labellingSource.getLabeling(t);
		this.ra = source.getSource(t, 0).randomAccess();
		this.sourceToGlobal = source.getSourceTransform(t, 0);
		this.structuringElement = AbstractRegionGrowing.get8ConStructuringElement(3);
		this.growingMode = GrowingMode.SEQUENTIAL;

		int freeLabel = 0;
		for (final Integer existingLabel : labeling.getLabels()) {
			if (existingLabel.intValue() > freeLabel) {
				freeLabel = existingLabel.intValue();
			}
		}
		this.label = freeLabel;
	}

	@Override
	public void mouseClicked(final MouseEvent e) {}

	@Override
	public void mouseEntered(final MouseEvent e) {}

	@Override
	public void mouseExited(final MouseEvent e) {}

	@Override
	public void mousePressed(final MouseEvent e) {
		final Point point = getLocationOnCurrentSource();
		final long[] pos = new long[3];
		point.localize(pos);

		if (!labeling.getLabels().isEmpty()) {

			final RandomAccess<LabelingType<Integer>> randomAccess = labeling.randomAccess();
			randomAccess.setPosition(point);

			final List<Integer> existingLabels = randomAccess.get().getLabeling();
			if (!existingLabels.isEmpty()) {

				label = existingLabels.get(0).intValue(); // For future new annotation

				for (final Integer existingLabel : existingLabels) {

					final IterableRegionOfInterest roi = labeling.getIterableRegionOfInterest(existingLabel);
					final IterableInterval<LabelingType<Integer>> overROI = roi.getIterableIntervalOverROI(labeling);
					for (final LabelingType<Integer> labelingType : overROI) {
						final ArrayList<Integer> labels = new ArrayList<Integer>(labelingType.getLabeling());
						labels.clear();
						labelingType.setLabeling(labels);
					}

				}
			} else {

				label++;
			}

		} else {
			label++;
		}

		seed = new HashMap<long[], Integer>(1);
		seed.put(pos, Integer.valueOf(label));
	}

	@Override
	public void mouseReleased(final MouseEvent e) {}

	@Override
	public void mouseDragged(final MouseEvent arg0) {

		/*
		 * Remove old stuff
		 */

		if (labeling.getLabels().contains(Integer.valueOf(label))) {

			final IterableRegionOfInterest iterableRegionOfInterest = labeling.getIterableRegionOfInterest(Integer.valueOf(label));
			for (final LabelingType<Integer> ll : iterableRegionOfInterest.getIterableIntervalOverROI(labeling)) {
				final ArrayList<Integer> labels = new ArrayList<Integer>(ll.getLabeling());
				labels.remove(Integer.valueOf(label));
				ll.setLabeling(labels);
			}

		}

		/*
		 * Make new stuff
		 */

		final Point current = getLocationOnCurrentSource();
		ra.setPosition(current);
		final T threshold = ra.get().copy();

		final ThresholdRegionGrowing<T, Integer> regionGrowing = new ThresholdRegionGrowing<T, Integer>(ra, threshold, seed, growingMode, structuringElement, labeling);

		regionGrowing.checkInput();
		regionGrowing.process();

		labelingSource.updateColorTable();
		viewer.requestRepaint();
	}

	@Override
	public void mouseMoved(final MouseEvent arg0) {}

	private Point getLocationOnCurrentSource() {
		final double[] coordinates = new double[3];
		final RealPoint click = RealPoint.wrap(coordinates);
		viewer.getGlobalMouseCoordinates(click);

		final Point roundedSourcePos = new Point(3);
		sourceToGlobal.applyInverse(new Round<Point>(roundedSourcePos), click);
		return roundedSourcePos;
	}
}
