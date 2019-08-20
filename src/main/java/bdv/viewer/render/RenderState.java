package bdv.viewer.render;

import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;

public class RenderState {

	private final AffineTransform3D viewerTransform;

	private final int currentTimepoint;

	private final List<RenderSource< ? >> sources;

	public RenderState(AffineTransform3D viewerTransform, int currentTimepoint, List<RenderSource<?>> sources) {
		this.viewerTransform = viewerTransform;
		this.currentTimepoint = currentTimepoint;
		this.sources = sources;
	}

	public static RenderState valueOf(ViewerState viewerState) {
		AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerState.getViewerTransform(viewerTransform);
		int currentTimepoint = viewerState.getCurrentTimepoint();
		List<RenderSource<?>> sources = new ArrayList<>();
		List<SourceState<?>> allSources = viewerState.getSources();
		for( int i : viewerState.getVisibleSourceIndices())
			sources.add(new RenderSource<>(allSources.get(i), viewerState.getInterpolation()));
		return new RenderState(viewerTransform, currentTimepoint, sources);
	}

	public AffineTransform3D getViewerTransform() {
		return viewerTransform;
	}

	public int getCurrentTimepoint() {
		return currentTimepoint;
	}

	public List<RenderSource< ? >> getSources() {
		return sources;
	}
}
