/*-
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
package bdv.img.n5;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Reader;

public class BdvN5Format implements N5Properties
{
	public static final String DOWNSAMPLING_FACTORS_KEY = "downsamplingFactors";
	public static final String DATA_TYPE_KEY = "dataType";

	@Override
	public String getPath( final int setupId )
	{
		return getPathName( setupId );
	}

	@Override
	public String getPath( final int setupId, final int timepointId)
	{
		return getPathName( setupId, timepointId );
	}

	@Override
	public String getPath( final int setupId, final int timepointId, final int level)
	{
		return getPathName( setupId, timepointId, level );
	}

	@Override
	public DataType getDataType( final N5Reader n5, final int setupId )
	{
		return n5.getAttribute( getPath( setupId ), DATA_TYPE_KEY, DataType.class );
	}

	@Override
	public double[][] getMipmapResolutions( final N5Reader n5, final int setupId )
	{
		return n5.getAttribute( getPath( setupId ), DOWNSAMPLING_FACTORS_KEY, double[][].class );
	}

	// left the old code for compatibility 
	public static String getPathName( final int setupId )
	{
		return String.format( "setup%d", setupId );
	}

	public static String getPathName( final int setupId, final int timepointId )
	{
		return String.format( "setup%d/timepoint%d", setupId, timepointId );
	}

	public static String getPathName( final int setupId, final int timepointId, final int level )
	{
		return String.format( "setup%d/timepoint%d/s%d", setupId, timepointId, level );
	}
}
