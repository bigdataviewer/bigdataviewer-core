package viewer.render;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * A {@link Source} that wraps another {@link Source} and allows to decorate it
 * with an extra {@link AffineTransform3D}.
 * <p>
 * This extra transformation is made to capture manual editing of the actual
 * transform in the SpimViewer.
 * 
 * @author Jean-Yves Tinevez - Sept 2013
 * 
 * @param <T>
 *            the type of the original source.
 */
public class TransformedSource< T > implements Source< T >
{

	private final Source< T > source;

	private final AffineTransform3D transform;

	/**
	 * Instantiates a new {@link TransformedSource} wrapping the specified
	 * source with the identity transform.
	 * 
	 * @param source
	 *            the source to wrap.
	 */
	public TransformedSource( final Source< T > source )
	{
		this.source = source;
		this.transform = new AffineTransform3D();
	}

	/*
	 * EXTRA TRANSFORMATION methods
	 */

	/**
	 * Sets the extra transformation of this instance to have the same values
	 * that of the specified one.
	 * 
	 * @param transform
	 *            the transformation to copy to this instance.
	 */
	public void setTransform( final AffineTransform3D transform )
	{
		this.transform.set( transform.getRowPackedCopy() );
	}

	/**
	 * Copy the values of the transformation wrapped in this instance to the
	 * specified transformation.
	 * 
	 * @param transform
	 *            the transformation to write on.
	 */
	public void getTransform( final AffineTransform3D transform )
	{
		transform.set( this.transform.getRowPackedCopy() );
	}

	/*
	 * SOURCE methods
	 */

	@Override
	public AffineTransform3D getSourceTransform( final int t, final int level )
	{
		final AffineTransform3D composed = transform.copy();
		composed.concatenate( source.getSourceTransform( t, level ) );
		return composed;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return source.isPresent( t );
	}

	@Override
	public RandomAccessibleInterval< T > getSource( final int t, final int level )
	{
		return source.getSource( t, level );
	}

	@Override
	public RealRandomAccessible< T > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return source.getInterpolatedSource( t, level, method );
	}

	@Override
	public T getType()
	{
		return source.getType();
	}

	@Override
	public String getName()
	{
		return source.getName();
	}

	@Override
	public int getNumMipmapLevels()
	{
		return source.getNumMipmapLevels();
	}

}
