package bdv.viewer.render;

import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;

public class RenderSource< T > extends SourceAndConverter< T > {

	private final Interpolation interpolation;

	public RenderSource(SourceAndConverter<T> soc, Interpolation interpolation) {
		super(soc);
		this.interpolation = interpolation;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	@Override
	public RenderSource<? extends Volatile<T>> asVolatile() {
		SourceAndConverter<? extends Volatile<T>> v = super.asVolatile();
		return v == null ? null : new RenderSource<>(v, interpolation);
	}
}
