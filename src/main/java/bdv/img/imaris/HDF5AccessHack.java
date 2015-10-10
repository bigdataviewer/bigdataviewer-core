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
package bdv.img.imaris;

import static bdv.img.hdf5.Util.reorder;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aexists;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aget_type;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aopen_name;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5A.H5Aread;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dget_space;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dopen;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5D.H5Dread;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5GLO.H5Oclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5GLO.H5Oopen;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Screate_simple;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sget_simple_extent_dims;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sget_simple_extent_ndims;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5S.H5Sselect_hyperslab;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5T.H5Tclose;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5T.H5Tget_class;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5T.H5Tget_cset;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5T.H5Tget_size;
import static ch.systemsx.cisd.hdf5.hdf5lib.H5T.H5Tis_variable_str;
import static ch.systemsx.cisd.hdf5.hdf5lib.HDF5Constants.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import bdv.img.hdf5.DimsAndExistence;
import bdv.img.hdf5.Util;
import bdv.img.hdf5.ViewLevelId;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class HDF5AccessHack implements IHDF5Access
{
	private final IHDF5Reader hdf5Reader;

	private final int fileId;

	private final int numericConversionXferPropertyListID;

	private final long[] reorderedDimensions = new long[ 3 ];

	private final long[] reorderedMin = new long[ 3 ];

	private static final int MAX_OPEN_DATASETS = 48;

	private class OpenDataSet
	{
		final int dataSetId;

		final int fileSpaceId;

		public OpenDataSet( final String cellsPath )
		{
			dataSetId = H5Dopen( fileId, cellsPath, H5P_DEFAULT );
			fileSpaceId = H5Dget_space( dataSetId );
		}

		public void close()
		{
			H5Sclose( fileSpaceId );
			H5Dclose( dataSetId );
		}
	}

	private class OpenDataSetCache extends LinkedHashMap< ViewLevelId, OpenDataSet >
	{
		private static final long serialVersionUID = 1L;

		public OpenDataSetCache()
		{
			super( MAX_OPEN_DATASETS, 0.75f, true );
		}

		@Override
		protected boolean removeEldestEntry( final Entry< ViewLevelId, OpenDataSet > eldest )
		{
			if ( size() > MAX_OPEN_DATASETS )
			{
				eldest.getValue().close();
				return true;
			}
			else
				return false;
		}

		public OpenDataSet getDataSet( final ViewLevelId id )
		{
			OpenDataSet openDataSet = super.get( id );
			if ( openDataSet == null )
			{
				openDataSet = new OpenDataSet( getCellsPath( id ) );
				put( id, openDataSet );
			}
			return openDataSet;
		}
	}

	private final OpenDataSetCache openDataSetCache;

	public HDF5AccessHack( final IHDF5Reader hdf5Reader ) throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		this.hdf5Reader = hdf5Reader;

		final Class< ? > k = Class.forName( "ch.systemsx.cisd.hdf5.HDF5Reader" );
		final Field f = k.getDeclaredField( "baseReader" );
		f.setAccessible( true );
		final Object baseReader = f.get( hdf5Reader );

		final Class< ? > k2 = Class.forName( "ch.systemsx.cisd.hdf5.HDF5BaseReader" );
		final Field f2 = k2.getDeclaredField( "fileId" );
		f2.setAccessible( true );
		fileId = ( ( Integer ) f2.get( baseReader ) ).intValue();

		final Field f3 = k2.getDeclaredField( "h5" );
		f3.setAccessible( true );
		final Object h5 = f3.get( baseReader );

		final Class< ? > k4 = Class.forName( "ch.systemsx.cisd.hdf5.HDF5" );
		final Field f4 = k4.getDeclaredField( "numericConversionXferPropertyListID" );
		f4.setAccessible( true );
		numericConversionXferPropertyListID = ( ( Integer ) f4.get( h5 ) ).intValue();

		openDataSetCache = new OpenDataSetCache();
	}

	@Override
	public synchronized DimsAndExistence getDimsAndExistence( final ViewLevelId id )
	{
		final long[] realDimensions = new long[ 3 ];
		boolean exists = false;
		try
		{
			final OpenDataSet dataset = openDataSetCache.getDataSet( id );
			final long[] dimensions = new long[ H5S_MAX_RANK ];
			final long[] maxDimensions = new long[ H5S_MAX_RANK ];
			final int rank = H5Sget_simple_extent_dims( dataset.fileSpaceId, dimensions, maxDimensions );
			System.arraycopy( dimensions, 0, realDimensions, 0, rank );
			exists = true;
		}
		catch ( final Exception e )
		{}
		if ( exists )
			return new DimsAndExistence( reorder( realDimensions ), true );
		else
			return new DimsAndExistence( new long[] { 1, 1, 1 }, false );
	}

	@Override
	public synchronized byte[] readByteMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final byte[] dataBlock = new byte[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		readByteMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min, dataBlock );
		return dataBlock;
	}

	@Override
	public synchronized byte[] readByteMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final byte[] dataBlock ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final OpenDataSet dataset = openDataSetCache.getDataSet( new ViewLevelId( timepoint, setup, level ) );
		final int memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, H5T_NATIVE_UCHAR, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );

		return dataBlock;
	}

	@Override
	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final short[] dataBlock = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		readShortMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min, dataBlock );
		return dataBlock;
	}

	// TODO: this could be unified with readByteMDArrayBlockWithOffset? (final Object dataBlock, int H5T_NATIVE_type)?
	@Override
	public short[] readShortMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final short[] dataBlock ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final OpenDataSet dataset = openDataSetCache.getDataSet( new ViewLevelId( timepoint, setup, level ) );
		final int memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, H5T_NATIVE_USHORT, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );

		return dataBlock;
	}

	@Override
	public float[] readFloatMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min ) throws InterruptedException
	{
		final float[] dataBlock = new float[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
		readFloatMDArrayBlockWithOffset( timepoint, setup, level, dimensions, min, dataBlock );
		return dataBlock;
	}

	// TODO: this could be unified with readByteMDArrayBlockWithOffset? (final Object dataBlock, int H5T_NATIVE_type)?
	@Override
	public float[] readFloatMDArrayBlockWithOffset( final int timepoint, final int setup, final int level, final int[] dimensions, final long[] min, final float[] dataBlock ) throws InterruptedException
	{
		if ( Thread.interrupted() )
			throw new InterruptedException();
		Util.reorder( dimensions, reorderedDimensions );
		Util.reorder( min, reorderedMin );

		final OpenDataSet dataset = openDataSetCache.getDataSet( new ViewLevelId( timepoint, setup, level ) );
		final int memorySpaceId = H5Screate_simple( reorderedDimensions.length, reorderedDimensions, null );
		H5Sselect_hyperslab( dataset.fileSpaceId, H5S_SELECT_SET, reorderedMin, null, reorderedDimensions, null );
		H5Dread( dataset.dataSetId, H5T_NATIVE_FLOAT, memorySpaceId, dataset.fileSpaceId, numericConversionXferPropertyListID, dataBlock );
		H5Sclose( memorySpaceId );

		return dataBlock;
	}

	@Override
	public synchronized String readImarisAttributeString( final String objectPath, final String attributeName )
	{
		return readImarisAttributeString( objectPath, attributeName, null );
	}

	@Override
	public synchronized String readImarisAttributeString( final String objectPath, final String attributeName, final String defaultValue )
	{
		String attrString = defaultValue;
		final int objectId = H5Oopen( fileId, objectPath, H5P_DEFAULT );
		if ( H5Aexists( objectId, attributeName ) )
		{
			final int attributeId = H5Aopen_name( objectId, attributeName );
			final int dataTypeId = H5Aget_type( attributeId );
			final int classType = H5Tget_class( dataTypeId );
			if ( classType != H5T_STRING )
				throw new IllegalArgumentException( "Attribute " + attributeName + " of object " + objectPath + " needs to be a String." );

			final int dataTypeSize = H5Tget_size( dataTypeId );
			if ( dataTypeSize != 1 )
				throw new IllegalArgumentException( "Attribute " + attributeName + " of object " + objectPath + ": unexpected data type size." );
			final boolean isVariableStr = H5Tis_variable_str( dataTypeId );
			if ( isVariableStr )
				throw new IllegalArgumentException( "Attribute " + attributeName + " of object " + objectPath + ": expected fixed length string." );
			final int cValue = H5Tget_cset( dataTypeId );
			final String cset;
			if ( cValue == H5T_CSET_ASCII )
				cset = "US-ASCII";
			else if ( cValue == H5T_CSET_UTF8 )
				cset = "UTF-8";
			else
				throw new IllegalArgumentException( "Attribute " + attributeName + " of object " + objectPath + ": unexpected character set." );

			final int attrSpaceId = H5Aget_space( attributeId );
			final int ndims = H5Sget_simple_extent_ndims( attrSpaceId );
			if ( ndims != 1 )
				throw new IllegalArgumentException( "Attribute " + attributeName + " of object " + objectPath + ": unexpected number of dimensions." );

			final long[] dims = new long[ ndims ];
			final long[] maxdims = new long[ ndims ];
			H5Sget_simple_extent_dims( attrSpaceId, dims, maxdims );

			final byte[] data = new byte[ ( int ) dims[ 0 ] ];
			H5Aread( attributeId, dataTypeId, data );

			try
			{
				attrString = new String( data, cset );
			}
			catch ( final UnsupportedEncodingException e )
			{}

			H5Sclose( attrSpaceId );
			H5Tclose( dataTypeId );
			H5Aclose( attributeId );
		}
		H5Oclose( objectId );
		return attrString;
	}

	@Override
	protected void finalize() throws Throwable
	{
		try
		{
			for ( final OpenDataSet dataset : openDataSetCache.values() )
				dataset.close();
			hdf5Reader.close();
		}
		finally
		{
			super.finalize();
		}
	}

	final static private String cellsFormatString = "DataSet/ResolutionLevel %d/TimePoint %d/Channel %d/Data";

	static String getCellsPath( final int timepoint, final int setup, final int level )
	{
		return String.format( cellsFormatString, level, timepoint, setup );
	}

	static String getCellsPath( final ViewLevelId viewLevelId )
	{
		return getCellsPath( viewLevelId.getTimePointId(), viewLevelId.getViewSetupId(), viewLevelId.getLevel() );
	}
}
