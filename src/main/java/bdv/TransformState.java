package bdv;

import java.util.function.Consumer;
import net.imglib2.realtransform.AffineTransform3D;

public interface TransformState
{
	/**
	 * Get the current transform.
	 *
	 * @param transform
	 *     is set to the current transform
	 */
	void get( AffineTransform3D transform );

	/**
	 * Get the current transform.
	 *
	 * @return a copy of the current transform
	 */
	default AffineTransform3D get()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		get( transform );
		return transform;
	}

	/**
	 * Set the transform.
	 */
	void set( AffineTransform3D transform );

	static TransformState from( Consumer< AffineTransform3D > get, Consumer< AffineTransform3D > set )
	{
		return new TransformState()
		{
			@Override
			public void get( final AffineTransform3D transform )
			{
				get.accept( transform );
			}

			@Override
			public void set( final AffineTransform3D transform )
			{
				set.accept( transform );
			}
		};
	}
}
