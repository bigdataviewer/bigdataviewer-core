package bdv.viewer.render;

import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

public interface RendererSourceState< T > {

	RendererSourceState< ? extends Volatile< T >> asVolatile();

	Source< T > getSpimSource();

	Converter< T, ARGBType> getConverter();
}
