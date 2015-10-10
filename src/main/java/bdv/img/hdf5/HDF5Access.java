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
package bdv.img.hdf5;

import static bdv.img.hdf5.Util.reorder;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

class HDF5Access implements IHDF5Access
{
	private final IHDF5Reader hdf5Reader;

	private final int[] reorderedDimensions = new int[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	public HDF5Access( final IHDF5Reader hdf5Reader )
	{
		this.hdf5Reader = hdf5Reader;
	}

	@Override
	public synchronized DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		final String cellsPath = Util.getCellsPath( id );
		HDF5DataSetInformation info = null;
		boolean exists = false;
		try
		{
			info = hdf5Reader.getDataSetInformation( cellsPath );
			exists = true;
		}
		catch ( final Exception e )
		{}
		if ( exists )
			return new DimsAndExistence( reorder( info.getDimensions() ), true );
		else
			return new DimsAndExistence( new long[] { 1, 1, 1 }, false );
	}

	@Override
	public synchronized short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );
		final MDShortArray array = hdf5Reader.int16().readMDArrayBlockWithOffset( Util.getCellsPath( timepoint, setup, level ), reorderedDimensions, reorderedMin );
		return array.getAsFlatArray();
	}

	@Override
	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final short[] dataBlock ) throws InterruptedException
	{
		System.arraycopy( readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min ), 0, dataBlock, 0, dataBlock.length );
		return dataBlock;
	}

	@Override
	public float[] readShortMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );
		final MDFloatArray array = hdf5Reader.float32().readMDArrayBlockWithOffset( Util.getCellsPath( timepoint, setup, level ), reorderedDimensions, reorderedMin );
		final float[] pixels = array.getAsFlatArray();
		unsignedShort( pixels );
		return pixels;
	}

	@Override
	public float[] readShortMDArrayBlockWithOffsetAsFloat( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final float[] dataBlock ) throws InterruptedException
	{
		System.arraycopy( readShortMDArrayBlockWithOffsetAsFloat( timepoint, setup, level, dimensions, min ), 0, dataBlock, 0, dataBlock.length );
		return dataBlock;
	}

	@Override
	public void closeAllDataSets()
	{}

	@Override
	public void close()
	{
		closeAllDataSets();
		hdf5Reader.close();
	}

	protected static final void unsignedShort( final float[] pixels )
	{
		for ( int j = 0; j < pixels.length; ++j )
			pixels[ j ] = ((short)pixels[ j ]) & 0xffff;
	}
}
