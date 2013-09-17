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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.regiongrowing.RegionGrowingTools.GrowingMode;
import net.imglib2.algorithm.regiongrowing.ThresholdRegionGrowing;
import net.imglib2.labeling.LabelingType;
import net.imglib2.ops.operation.randomaccessibleinterval.unary.regiongrowing.AbstractRegionGrowing;
import net.imglib2.position.transform.Round;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.numeric.RealType;
import viewer.SpimViewer;
import viewer.render.Source;

public class RegionGrowingAnnotationTool<T extends RealType<T>> implements MouseListener, MouseMotionListener {

	private final int level;
	private final SpimViewer viewer;
	private Map<long[], Integer> seed;
	private final LabellingSource labelingSource;
	private int label = -1;
	private final Source<T> source;
	private final int t;
	private final long[][] structuringElement;
	private final GrowingMode growingMode;

	public RegionGrowingAnnotationTool(final SpimViewer viewer, final Source<T> source, final LabellingSource labellingSource, final int t, final int level) {
		this.viewer = viewer;
		this.source = source;
		this.labelingSource = labellingSource;
		this.t = t;
		this.level = level;
		this.structuringElement = AbstractRegionGrowing.get8ConStructuringElement(3);
		this.growingMode = GrowingMode.SEQUENTIAL;
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

		if (!labelingSource.getCurrentLabelling().getLabels().isEmpty()) {

			final RandomAccess<LabelingType<Integer>> randomAccess = labelingSource.getCurrentLabelling().randomAccess();
			randomAccess.setPosition(point);

			final List<Integer> existingLabels = randomAccess.get().getLabeling();
			if (!existingLabels.isEmpty()) {

				System.out.println("Found some labels under the seed point: " + existingLabels);// DEBUG

				label = existingLabels.get(0).intValue(); // For future new annotation

				for (final Integer existingLabel : existingLabels) {

					final IterableRegionOfInterest roi = labelingSource.getCurrentLabelling().getIterableRegionOfInterest(existingLabel);
					final IterableInterval<LabelingType<Integer>> overROI = roi.getIterableIntervalOverROI(labelingSource.getCurrentLabelling());
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

		if (labelingSource.getCurrentLabelling().getLabels().contains(Integer.valueOf(label))) {

			final IterableRegionOfInterest iterableRegionOfInterest = labelingSource.getCurrentLabelling().getIterableRegionOfInterest(Integer.valueOf(label));
			for (final LabelingType<Integer> labeling : iterableRegionOfInterest.getIterableIntervalOverROI(labelingSource.getCurrentLabelling())) {
				final ArrayList<Integer> labels = new ArrayList<Integer>(labeling.getLabeling());
				labels.remove(Integer.valueOf(label));
				labeling.setLabeling(labels);
			}

		}

		/*
		 * Make new stuff
		 */

		final RandomAccessibleInterval<T> rai = source.getSource(t, level);
		final RandomAccess<T> ra = rai.randomAccess();

		final Point current = getLocationOnCurrentSource();
		ra.setPosition(current);
		final T threshold = ra.get().copy();

		final ThresholdRegionGrowing<T, Integer> regionGrowing = new ThresholdRegionGrowing<T, Integer>(ra, threshold, seed, growingMode, structuringElement, labelingSource.getCurrentLabelling());

		regionGrowing.checkInput();
		regionGrowing.process();

		labelingSource.updateColorTable();
		viewer.requestRepaint();

		System.out.println(label + ": area = " + regionGrowing.getResult().getArea(Integer.valueOf(label)));// DEBUG
	}

	@Override
	public void mouseMoved(final MouseEvent arg0) {}

	private Point getLocationOnCurrentSource() {

		// Ok, then create this spot, wherever it is.
		final double[] coordinates = new double[3];
		final RealPoint click = RealPoint.wrap(coordinates);
		viewer.getGlobalMouseCoordinates(click);

		final AffineTransform3D sourceToGlobal = source.getSourceTransform(t, level);

		final Point roundedSourcePos = new Point(3);
		sourceToGlobal.applyInverse(new Round<Point>(roundedSourcePos), click);
		return roundedSourcePos;
	}
}
