package bdv.viewer.render;

import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.List;

public class RendererState {

	private final AffineTransform3D viewerTransfrom;

	private final int currentTimepoint;

	private final List< RendererSourceState< ? > > sources;

	public RendererState(AffineTransform3D viewerTransfrom, int currentTimepoint, List< RendererSourceState<?> > sources) {
		this.viewerTransfrom = viewerTransfrom;
		this.currentTimepoint = currentTimepoint;
		this.sources = sources;
	}

	public static RendererState valueOf(ViewerState viewerState) {
		AffineTransform3D viewerTransfrom = new AffineTransform3D();
		viewerState.getViewerTransform(viewerTransfrom);
		int currentTimepoint = viewerState.getCurrentTimepoint();
		List<RendererSourceState<?>> sources = new ArrayList<>();
		List<SourceState<?>> allSources = viewerState.getSources();
		for( int i : viewerState.getVisibleSourceIndices())
			sources.add(new RendererSourceState<>(allSources.get(i), viewerState.getInterpolation(), viewerTransfrom, currentTimepoint));
		return new RendererState(viewerTransfrom, currentTimepoint, sources);
	}

	public AffineTransform3D getViewerTransform() {
		return viewerTransfrom;
	}

	public int getCurrentTimepoint() {
		return currentTimepoint;
	}

	public List< RendererSourceState< ? > > getSources() {
		return sources;
	}
}
