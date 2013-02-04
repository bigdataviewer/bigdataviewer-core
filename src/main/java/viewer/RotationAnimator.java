package viewer;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

class RotationAnimator
{
	private final AffineTransform3D transformStart;

	private final double[] qAddEnd;

	private final double cX, cY;

	private final long startTime;

	private final long duration;

	private boolean complete;

	public RotationAnimator( final AffineTransform3D transformStart, final double viewerCenterX, final double viewerCenterY, final double[] targetOrientation, final long duration )
	{
		this.transformStart = transformStart;
		cX = viewerCenterX;
		cY = viewerCenterY;

		final double[] qCurrent = new double[4];
		extractRotation( transformStart, qCurrent );

		final double[] qTmp = new double[4];
		qAddEnd = new double[4];
		LinAlgHelpers.quaternionInvert( qCurrent, qTmp );
		LinAlgHelpers.quaternionMultiply( targetOrientation, qTmp, qAddEnd );

		if ( qAddEnd[ 0 ] < 0 )
			for ( int i = 0; i < 4; ++i )
				qAddEnd[ i ] = -qAddEnd[ i ];

		startTime = System.currentTimeMillis();
		this.duration = duration;
		complete = false;
	}

	public boolean isComplete()
	{
		return complete;
	}

	public AffineTransform3D getCurrent()
	{
		double t = ( System.currentTimeMillis() - startTime ) / ( double ) duration;
		if ( t >= 1 )
		{
			complete = true;
			t = 1;
		}
		return get( t );
	}

	public static void extractRotation( final AffineTransform3D transform, final double[] q )
	{
		final double[][] m = new double[ 3 ][ 4 ];
		transform.toMatrix( m );

		// unscale rotation part of matrix
		final double x = m[0][0];
		final double y = m[0][1];
		final double z = m[0][2];
		final double s = 1.0 / Math.sqrt( x * x + y * y + z * z );
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				m[ r ][ c ] *= s;

		LinAlgHelpers.quaternionFromR( m, q );
	}

	public static void extractRotationAnisotropic( final AffineTransform3D transform, final double[] q )
	{
		final double[][] m = new double[ 3 ][ 4 ];
		transform.toMatrix( m );

		// unscale transformed unit axes to get rid of z scaling
		for ( int r = 0; r < 3; ++r )
		{
			double sqSum = 0;
			for ( int c = 0; c < 3; ++c )
				sqSum += m[ c ][ r ]  *m[ c ][ r ];
			final double s = 1.0 / Math.sqrt( sqSum );
			for ( int c = 0; c < 3; ++c )
				m[ c ][ r ] *= s;
		}

		LinAlgHelpers.quaternionFromR( m, q );
	}

	/**
	 * @param t from 0 to 1
	 */
	private AffineTransform3D get( final double t )
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