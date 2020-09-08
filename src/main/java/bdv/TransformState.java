/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
