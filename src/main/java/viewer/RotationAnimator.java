package viewer;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import viewer.util.AbstractTransformAnimator;

class RotationAnimator extends AbstractTransformAnimator
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
		extractRotation( transformStart, qCurrent );

		final double[] qTmp = new double[4];
		qAddEnd = new double[4];
		LinAlgHelpers.quaternionInvert( qCurrent, qTmp );
		LinAlgHelpers.quaternionMultiply( targetOrientation, qTmp, qAddEnd );

		if ( qAddEnd[ 0 ] < 0 )
			for ( int i = 0; i < 4; ++i )
				qAddEnd[ i ] = -qAddEnd[ i ];
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
		for ( int c = 0; c < 3; ++c )
		{
			double sqSum = 0;
			for ( int r = 0; r < 3; ++r )
				sqSum += m[ r ][ c ] * m[ r ][ c ];
			final double s = 1.0 / Math.sqrt( sqSum );
			for ( int r = 0; r < 3; ++r )
				m[ r ][ c ] *= s;
		}

		LinAlgHelpers.quaternionFromR( m, q );
	}

	public static void extractApproximateRotationAffine( final AffineTransform3D transform, final double[] q, final int coerceAffineDimension )
	{
		final double[][] m = new double[ 3 ][ 4 ];
		transform.toMatrix( m );

		// unscale transformed unit axes to get rid of z scaling
		for ( int c = 0; c < 3; ++c )
		{
			double sqSum = 0;
			for ( int r = 0; r < 3; ++r )
				sqSum += m[ r ][ c ] * m[ r ][ c ];
			final double s = 1.0 / Math.sqrt( sqSum );
			for ( int r = 0; r < 3; ++r )
				m[ r ][ c ] *= s;
		}

		// coerce to rotation matrix
		final double[] x = new double[ 3 ];
		final double[] y = new double[ 3 ];
		final double[] z = new double[ 3 ];
		LinAlgHelpers.getCol( 0, m, x );
		LinAlgHelpers.getCol( 1, m, y );
		LinAlgHelpers.getCol( 2, m, z );
		switch ( coerceAffineDimension )
		{
		case 0:
			LinAlgHelpers.cross( y, z, x );
			LinAlgHelpers.normalize( x );
			LinAlgHelpers.cross( x, y, z );
			break;
		case 1:
			LinAlgHelpers.cross( z, x, y );
			LinAlgHelpers.normalize( y );
			LinAlgHelpers.cross( y, z, x );
			break;
		case 2:
			LinAlgHelpers.cross( x, y, z );
			LinAlgHelpers.normalize( z );
			LinAlgHelpers.cross( z, x, y );
		}
		LinAlgHelpers.setCol( 0, x, m );
		LinAlgHelpers.setCol( 1, y, m );
		LinAlgHelpers.setCol( 2, z, m );

		LinAlgHelpers.quaternionFromR( m, q );
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