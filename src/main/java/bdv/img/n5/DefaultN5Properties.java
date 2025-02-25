/*-
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
package bdv.img.n5;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;

public class DefaultN5Properties implements N5Properties
{
	@Override
	public String getDatasetPath( final int setupId, final int timepointId, final int level )
	{
		return BdvN5Format.getPathName( setupId, timepointId, level );
	}

	@Override
	public DataType getDataType( final N5Reader n5, final int setupId )
	{
		final String path = BdvN5Format.getPathName( setupId );
		return n5.getAttribute( path, BdvN5Format.DATA_TYPE_KEY, DataType.class );
	}

	@Override
	public double[][] getMipmapResolutions( final N5Reader n5, final int setupId )
	{
		final String path = BdvN5Format.getPathName( setupId );
		return n5.getAttribute( path, BdvN5Format.DOWNSAMPLING_FACTORS_KEY, double[][].class );
	}

	@Override
	public long[] getDimensions( final N5Reader n5, final int setupId, final int timepointId, final int level  )
	{
		final String path = getDatasetPath( setupId, timepointId, level );
		return n5.getDatasetAttributes( path ).getDimensions();
	}
}
