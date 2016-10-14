/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.export;

import static bdv.img.hdf5.Util.reorder;
import bdv.img.hdf5.Util;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5IntStorageFeatures;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

class HDF5Access implements IHDF5Access
{
	private final IHDF5Writer hdf5Writer;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedOffset = new long[ 3 ];

	private String datasetPath;

	public HDF5Access( final IHDF5Writer hdf5Writer )
	{
		this.hdf5Writer = hdf5Writer;
	}

	@Override
	public void writeMipmapDescription( final int setupIdPartition, final ExportMipmapInfo mipmapInfo )
	{
		hdf5Writer.writeDoubleMatrix( Util.getResolutionsPath( setupIdPartition ), mipmapInfo.getResolutions() );
		hdf5Writer.writeIntMatrix( Util.getSubdivisionsPath( setupIdPartition ), mipmapInfo.getSubdivisions() );
	}

	@Override
	public void createAndOpenDataset( final String path, final long[] dimensions, final int[] cellDimensions, final HDF5IntStorageFeatures features )
	{
		hdf5Writer.int16().createMDArray( path, reorder( dimensions ), reorder( cellDimensions ), features );
		this.datasetPath = path;
	}

	@Override
	public void writeBlockWithOffset( final short[] data, final long[] blockDimensions, final long[] offset )
	{
		reorder( blockDimensions, reorderedDimensions );
		reorder( offset, reorderedOffset );
		final MDShortArray array = new MDShortArray( data, reorderedDimensions );
		hdf5Writer.int16().writeMDArrayBlockWithOffset( datasetPath, array, reorderedOffset );
	}

	@Override
	public void closeDataset()
	{}

	@Override
	public void close()
	{
		hdf5Writer.close();
	}

	@Override
	public IHDF5Writer getIHDF5Writer()
	{
		return hdf5Writer;
	}
}
