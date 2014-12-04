package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * An animator that just executes a constant speed translation of the current
 * viewpoint to a target location, keeping all other view parameters constant.
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com>
 */
public class TranslationAnimator extends AbstractTransformAnimator
{
	private final AffineTransform3D transformStart;

	private final double[] targetTranslation;

	public TranslationAnimator( final AffineTransform3D transformStart, final double[] targetTranslation, final long duration )
	{
		super( duration );
		this.transformStart = transformStart;
		this.targetTranslation = targetTranslation.clone();
	}

	@Override
	protected AffineTransform3D get( final double t )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( transformStart );

		final double sx = transform.get( 0, 3 );
		final double sy = transform.get( 1, 3 );
		final double sz = transform.get( 2, 3 );

		final double tx = targetTranslation[ 0 ];
		final double ty = targetTranslation[ 1 ];
		final double tz = targetTranslation[ 2 ];

		transform.set( sx + t * ( tx - sx ), 0, 3 );
		transform.set( sy + t * ( ty - sy ), 1, 3 );
		transform.set( sz + t * ( tz - sz ), 2, 3 );

		return transform;
	}
}
