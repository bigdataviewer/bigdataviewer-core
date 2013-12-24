package bdv.util;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

/**
 * Helpers to extract parts (rotation, scale, etc) from
 * {@link AffineTransform3D}. Note that most of these helpers assume additional
 * restrictions on the affine transform.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class Affine3DHelpers
{

	/**
	 * Extract the rotation part of a similarity transform stored in a
	 * {@link AffineTransform3D}.
	 *
	 * @param transform
	 *            assumed to be a similarity transform (isotropic scaling +
	 *            rotation + translation).
	 * @param q
	 *            rotation will be stored as a quaternion here.
	 */
	public static void extractRotation( final AffineTransform3D transform, final double[] q )
	{
		final double[][] m = new double[ 3 ][ 4 ];
		transform.toMatrix( m );

		// unscale rotation part of matrix
		final double x = m[ 0 ][ 0 ];
		final double y = m[ 0 ][ 1 ];
		final double z = m[ 0 ][ 2 ];
		final double s = 1.0 / Math.sqrt( x * x + y * y + z * z );
		for ( int r = 0; r < 3; ++r )
			for ( int c = 0; c < 3; ++c )
				m[ r ][ c ] *= s;

		LinAlgHelpers.quaternionFromR( m, q );
	}

	/**
	 * Extract the rotation from an {@link AffineTransform3D} comprising
	 * anisotropic scaling, rotation, and translation.
	 *
	 * @param transform
	 *            assumed to comprise anisotropic scaling, followed by rotation
	 *            and translation.
	 * @param q
	 *            rotation will be stored as a quaternion here.
	 */
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

	/**
	 * Extract a rotation from a general {@link AffineTransform3D}. Because the
	 * affine can comprise shearing which is not purely due to anisotropic
	 * scaling, there is no unique rotation to extract. The rotation is computed
	 * in the following way: The <em>(X, Y, Z)</em> unit axis are transformed
	 * and scaled to unit length. The resulting transformed unit vectors
	 * <em>(X', Y', Z')</em> are not necessarily pairwise orthogonal. We find
	 * unit vectors <em>(X'', Y'', Z'')</em> to form an orthogonal basis as
	 * follows: The axis specified by the <code>coerceAffineDimension</code>
	 * parameter is computed to be orthogonal to the remaining two. Assuming
	 * <code>coerceAffineDimension=2</code>, we would set <em>Z'' = X' x Y'</em>
	 * . The next axis (right handed axis order) after the coerced one is
	 * maintained, in this case <em>X'' = X'</em>. Finally the last axis is made
	 * perpendicular, <em>Y'' = Z'' x X''</em>. The rotation that takes
	 * <em>(X, Y, Z)</em> to <em>(X'', Y'', Z'')</em> is returned. The effect is
	 * that the plane spanned by the two non-coerced axes (if
	 * <code>coerceAffineDimension=2</code>, the XY plane) will be transformed
	 * to the same plane by the computed rotation and the "rotation part" of
	 * <code>transform</code>.
	 *
	 * @param transform
	 *            assumed to comprise anisotropic scaling, followed by rotation
	 *            and translation.
	 * @param q
	 *            rotation will be stored as a quaternion here.
	 * @param coerceAffineDimension
	 *            the plane spanned by the remaining two axes (if
	 *            <code>coerceAffineDimension=2</code>, the XY plane) will be
	 *            transformed to the same plane by the computed rotation and the
	 *            "rotation part" of <code>transform</code>.
	 */
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

	/**
	 * Get the scale factor along the X, Y, or Z axis. For example, a points
	 * A=(x,0,0) and B=(x+1,0,0) on the X axis will be transformed to points
	 * A'=T*A and B'=T*B by the transform T. The distance between A' and B'
	 * gives the X scale factor.
	 *
	 * @param transform
	 *            an affine transform.
	 * @param axis
	 *            index of the axis for which the scale factor should be
	 *            computed.
	 * @return scale factor.
	 */
	public static double extractScale( final AffineTransform3D transform, final int axis )
	{
		double sqSum = 0;
		final int c = axis;
		for ( int r = 0; r < 3; ++r )
		{
			final double x = transform.get( r, c );
			sqSum += x * x;
		}
		return Math.sqrt( sqSum );
	}
}
