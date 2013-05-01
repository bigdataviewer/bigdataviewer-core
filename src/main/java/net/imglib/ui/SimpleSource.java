package net.imglib.ui;

import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.ARGBType;

public interface SimpleSource< T >
{
	/**
	 * Get the image, extended to infinity and interpolated.
	 *
	 * @return the extended and interpolated {@link RandomAccessible image}.
	 */
	public RealRandomAccessible< T > getInterpolatedSource();

	/**
	 * Get the transform from the {@link #getSource(long) source}
	 * into the global coordinate system.
	 *
	 * @return transforms source into the global coordinate system.
	 */
	public AffineTransform2D getSourceTransform();

	/**
	 * Get the {@link Converter} (converts {@link #source} type T to ARGBType
	 * for display).
	 */
	public Converter< T, ARGBType > getConverter();
}
