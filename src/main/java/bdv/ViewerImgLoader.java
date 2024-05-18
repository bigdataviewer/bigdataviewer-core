/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import bdv.cache.CacheControl;
import bdv.cache.SharedQueue;
import mpicbg.spim.data.generic.sequence.BasicMultiResolutionImgLoader;

public interface ViewerImgLoader extends BasicMultiResolutionImgLoader
{
	@Override
	ViewerSetupImgLoader< ?, ? > getSetupImgLoader( final int setupId );

	CacheControl getCacheControl();

	/**
	 * Set the number of fetcher threads that will be created to asynchronously load
	 * blocks. It is permitted to set {@code n=0}, but that means that the volatile
	 * versions of images may not work correctly.
	 * <p>
	 * This is an optional operation, i.e., some ImgLoader implementations may just
	 * ignore it.
	 * <p>
	 * This method should be called before using the image loader, otherwise it might
	 * not have the desired effect.
	 */
	default void setNumFetcherThreads( int n ) {}

	/**
	 * Use the given {@code SharedQueue} to asynchronously load blocks.
	 * <p>
	 * This is an optional operation, i.e., some ImgLoader implementations may just
	 * ignore it.
	 * <p>
	 * This method should be called before using the image loader, otherwise it might
	 * not have the desired effect.
	 */
	default void setCreatedSharedQueue( SharedQueue createdSharedQueue ) {}
}
