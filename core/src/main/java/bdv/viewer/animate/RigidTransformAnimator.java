package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bdv.util.Affine3DHelpers;

public class RigidTransformAnimator extends AbstractTransformAnimator
{
	private final double[] qStart;

	private final double[] qDiff;

	private final double[] tStart;

	private final double[] tDiff;

	private final double scaleStart;

	private final double scaleDiff;

	private final double cX;

	private final double cY;

	public RigidTransformAnimator( final AffineTransform3D transformStart, final AffineTransform3D transformEnd, final double cX, final double cY, final long duration )
	{
		super( duration );
		this.cX = cX;
		this.cY = cY;

		qStart = new double[ 4 ];
		Affine3DHelpers.extractRotation( transformStart, qStart );

		qDiff = new double[ 4 ];
		final double[] qEnd = new double[ 4 ];
		final double[] qTmp = new double[ 4 ];
		Affine3DHelpers.extractRotation( transformEnd, qEnd );
		LinAlgHelpers.quaternionInvert( qStart, qTmp );
		LinAlgHelpers.quaternionMultiply( qTmp, qEnd, qDiff );

		scaleStart = Affine3DHelpers.extractScale( transformStart, 0 );
		final double scaleEnd = Affine3DHelpers.extractScale( transformEnd, 0 );
		scaleDiff = scaleEnd - scaleStart;

		tStart = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			tStart[ d ] = transformStart.get( d, 3 ) / scaleStart;

		tDiff = new double[ 3 ];
		for ( int d = 0; d < 3; ++d )
			tDiff[ d ] = transformEnd.get( d, 3 ) / scaleEnd - tStart[ d ];

	}

	@Override
	protected AffineTransform3D get( final double t )
	{
		final double[] qDiffCurrent = new double[ 4 ];
		final double[] qCurrent = new double[ 4 ];
		LinAlgHelpers.quaternionPower( qDiff, t, qDiffCurrent );
		LinAlgHelpers.quaternionMultiply( qStart, qDiffCurrent, qCurrent );

		final double[][] m = new double[ 3 ][ 4 ];
		LinAlgHelpers.quaternionToR( qCurrent, m );

		final double scale = scaleStart + t * scaleDiff;
		for ( int r = 0; r < 3; ++r )
		{
			m[ r ][ 3 ] = tStart[ r ] + t * tDiff[ r ];
			for ( int c = 0; c < 4; ++c )
				m[ r ][ c ] *= scale;
		}
		m[ 0 ][ 3 ] += cX;
		m[ 1 ][ 3 ] += cY;

		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( m );
		return transform;
	}

}
