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

/**
 * An animator that just executes a constant speed translation of the current
 * viewpoint to a target location, keeping all other view parameters constant.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt;
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
	public AffineTransform3D get( final double t )
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
