package bdv.viewer.render;

import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;

public class RendererSourceState< T > extends SourceAndConverter< T > {

	private final Interpolation interpolation;

	public RendererSourceState(SourceAndConverter<T> soc, Interpolation interpolation) {
		super(soc);
		this.interpolation = interpolation;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	@Override
	public RendererSourceState<? extends Volatile<T>> asVolatile() {
		SourceAndConverter<? extends Volatile<T>> v = super.asVolatile();
		return v == null ? null : new RendererSourceState<>(v, interpolation);
	}
}
