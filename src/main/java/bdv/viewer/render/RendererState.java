package bdv.viewer.render;

import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;

public class RendererState {

	private final AffineTransform3D viewerTransform;

	private final int currentTimepoint;

	private final List< RendererSourceState< ? > > sources;

	public RendererState(AffineTransform3D viewerTransform, int currentTimepoint, List< RendererSourceState<?> > sources) {
		this.viewerTransform = viewerTransform;
		this.currentTimepoint = currentTimepoint;
		this.sources = sources;
	}

	public static RendererState valueOf(ViewerState viewerState) {
		AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerState.getViewerTransform(viewerTransform);
		int currentTimepoint = viewerState.getCurrentTimepoint();
		List<RendererSourceState<?>> sources = new ArrayList<>();
		List<SourceState<?>> allSources = viewerState.getSources();
		for( int i : viewerState.getVisibleSourceIndices())
			sources.add(new RendererSourceState<>(allSources.get(i), viewerState.getInterpolation()));
		return new RendererState(viewerTransform, currentTimepoint, sources);
	}

	public AffineTransform3D getViewerTransform() {
		return viewerTransform;
	}

	public int getCurrentTimepoint() {
		return currentTimepoint;
	}

	public List< RendererSourceState< ? > > getSources() {
		return sources;
	}
}
