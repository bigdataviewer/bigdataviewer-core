package viewer.gui.transformation;

import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.render.Source;
import viewer.render.VolatileSource;

/**
 * A {@link VolatileSource} that wraps another {@link VolatileSource} and allows
 * to decorate it with an extra {@link AffineTransform3D}.
 * <p>
 * This extra transformation is made to capture manual editing of the actual
 * transform in the SpimViewer.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class TransformedVolatileSource< T, V extends Volatile< T > > extends TransformedSource< V > implements VolatileSource< T, V >
{
	private TransformedSource< T > nonVolatileTransformedSource;

	/**
	 * Instantiates a new {@link TransformedSource} wrapping the specified
	 * source with the identity transform.
	 *
	 * @param source
	 *            the source to wrap.
	 */
	public TransformedVolatileSource( final VolatileSource< T, V > source )
	{
		super( source );
		nonVolatileTransformedSource = new TransformedSource< T >( source.nonVolatile(), incrementalTransform, fixedTransform, sourceTransform, composed );
	}

	@Override
	public Source< T > nonVolatile()
	{
		return nonVolatileTransformedSource;
	}
}
