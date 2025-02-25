/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.img.hdf5;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * The dimensions of an image and a flag indicating whether that image
 * exists (can be loaded)
 *
 * @author Tobias Pietzsch
 */
public class DimsAndExistence
{
	private final long[] dimensions;

	private final int[] blockSize;

	private final boolean exists;

	public DimsAndExistence( final long[] dimensions, final int[] blockSize, final boolean exists )
	{
		this.dimensions = dimensions;
		this.blockSize = blockSize;
		this.exists = exists;
	}

	public long[] getDimensions()
	{
		return dimensions;
	}

	public int[] getBlockSize()
	{
		return blockSize;
	}

	public boolean exists()
	{
		return exists;
	}

	@Override
	public String toString()
	{
		return new StringJoiner( ", ", DimsAndExistence.class.getSimpleName() + "[", "]" )
				.add( "dimensions=" + Arrays.toString( dimensions ) )
				.add( "blockSize=" + Arrays.toString( blockSize ) )
				.add( "exists=" + exists )
				.toString();
	}
}
