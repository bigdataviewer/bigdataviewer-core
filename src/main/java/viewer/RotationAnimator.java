package viewer;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import viewer.util.AbstractTransformAnimator;
import viewer.util.Affine3DHelpers;

public class RotationAnimator extends AbstractTransformAnimator
{
	private final AffineTransform3D transformStart;

	private final double[] qAddEnd;

	private final double cX, cY;

	public RotationAnimator( final AffineTransform3D transformStart, final double viewerCenterX, final double viewerCenterY, final double[] targetOrientation, final long duration )
	{
		super( duration );

		this.transformStart = transformStart;
		cX = viewerCenterX;
		cY = viewerCenterY;

		final double[] qCurrent = new double[4];
		Affine3DHelpers.extractRotation( transformStart, qCurrent );

		final double[] qTmp = new double[4];
		qAddEnd = new double[4];
		LinAlgHelpers.quaternionInvert( qCurrent, qTmp );
		LinAlgHelpers.quaternionMultiply( targetOrientation, qTmp, qAddEnd );

		if ( qAddEnd[ 0 ] < 0 )
			for ( int i = 0; i < 4; ++i )
				qAddEnd[ i ] = -qAddEnd[ i ];
	}

	@Override
	protected AffineTransform3D get( final double t )
	{
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( transformStart );

		// center shift
		transform.set( transform.get( 0, 3 ) - cX, 0, 3 );
		transform.set( transform.get( 1, 3 ) - cY, 1, 3 );

		// rotate
		final double[] qAddCurrent = new double[4];
		final AffineTransform3D tAddCurrent = new AffineTransform3D();
		LinAlgHelpers.quaternionPower( qAddEnd, t, qAddCurrent );
		final double[][] m = new double[3][4];
		LinAlgHelpers.quaternionToR( qAddCurrent, m );
		tAddCurrent.set( m );
		transform.preConcatenate( tAddCurrent );

		// center un-shift
		transform.set( transform.get( 0, 3 ) + cX, 0, 3 );
		transform.set( transform.get( 1, 3 ) + cY, 1, 3 );

		return transform;
	}
}