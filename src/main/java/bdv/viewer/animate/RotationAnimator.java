/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv.viewer.animate;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import bdv.util.Affine3DHelpers;

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
	public AffineTransform3D get( final double t )
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
