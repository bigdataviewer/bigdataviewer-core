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
package bdv.img.hdf5;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_TAG;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.jdom2.Element;

@ImgLoaderIo( format = "bdv.hdf5", type = Hdf5ImageLoader.class )
public class XmlIoHdf5ImageLoader implements XmlIoBasicImgLoader< Hdf5ImageLoader >
{
	@Override
	public Element toXml( final Hdf5ImageLoader imgLoader, final File basePath )
	{
		final Element elem = new Element( IMGLOADER_TAG );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.hdf5" );
		elem.setAttribute( "pixelType", imgLoader.hdf5PixelType);
		elem.addContent( XmlHelpers.pathElement( "hdf5", imgLoader.getHdf5File(), basePath ) );
		for ( final Partition partition : imgLoader.getPartitions() )
			elem.addContent( partitionToXml( partition, basePath ) );
		return elem;
	}

	@Override
	public Hdf5ImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		//check that this XML file was created by us
		if (! elem.getAttributeValue(IMGLOADER_FORMAT_ATTRIBUTE_NAME).startsWith("bdv.hdf5") )
			throw new RuntimeException("The XML file was not created for BDV.");

		//read out the pixel type used with this file
		final String pxType = elem.getAttributeValue("pixelType");

		final String path = loadPath( elem, "hdf5", basePath ).toString();
		final ArrayList< Partition > partitions = new ArrayList<>();
		for ( final Element p : elem.getChildren( "partition" ) )
			partitions.add( partitionFromXml( p, basePath ) );

		//create the loader the usual way, nothing has changed here
		final Hdf5ImageLoader HDF5loader = new Hdf5ImageLoader( new File( path ), partitions, sequenceDescription );

		//if we manage to read specific voxel type info, inject it;
		//otherwise we don't change it, which leaves the HDF5loader
		//with its default value (which is backward compatible UnsignedShortType)
		if (pxType != null) HDF5loader.hdf5PixelType = pxType;

		return HDF5loader;
	}

	private Element partitionToXml( final Partition partition, final File basePath )
	{
		final Element elem = new Element( "partition" );
		elem.addContent( XmlHelpers.pathElement( "path", new File( partition.getPath() ), basePath ) );

		final Pair< int[], int[] > timepointIdVecs = idMapToVectors( partition.getTimepointIdSequenceToPartition() );
		final int[] timepointIds = timepointIdVecs.getA();
		final int[] timepointIdsPartition = timepointIdVecs.getB();
		elem.addContent( XmlHelpers.intArrayElement( "timepoints", timepointIds ) );
		if ( ! Arrays.equals( timepointIds, timepointIdsPartition ) )
			elem.addContent( XmlHelpers.intArrayElement( "timepointsMapped", timepointIdsPartition ) );

		final Pair< int[], int[] > setupIdVecs = idMapToVectors( partition.getSetupIdSequenceToPartition() );
		final int[] setupIds = setupIdVecs.getA();
		final int[] setupIdsPartition = setupIdVecs.getB();
		elem.addContent( XmlHelpers.intArrayElement( "setups", setupIds ) );
		if ( ! Arrays.equals( setupIds, setupIdsPartition ) )
			elem.addContent( XmlHelpers.intArrayElement( "setupsMapped", setupIdsPartition ) );

		return elem;
	}

	private static Pair< int[], int[] > idMapToVectors( final Map< Integer, Integer > setupIdMap )
	{
		final ArrayList< Integer > seqIdList = new ArrayList<>( setupIdMap.keySet() );
		Collections.sort( seqIdList );
		final int[] seqIds = new int[ seqIdList.size() ];
		final int[] parIds = new int[ seqIdList.size() ];
		int i = 0;
		for ( final Integer seqId : seqIdList )
		{
			seqIds[ i ] = seqId;
			parIds[ i ] = setupIdMap.get( seqId );
			++i;
		}
		return new ValuePair<>( seqIds, parIds );
	}

	private Partition partitionFromXml( final Element elem, final File basePath )
	{
		String path;
		try
		{
			path = XmlHelpers.loadPath( elem, "path", basePath ).toString();
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		final int[] timepointIds = XmlHelpers.getIntArray( elem, "timepoints" );
		final int[] timepointIdsPartition = XmlHelpers.getIntArray( elem, "timepointsMapped", timepointIds );
		final int[] setupIds = XmlHelpers.getIntArray( elem, "setups" );
		final int[] setupIdsPartition = XmlHelpers.getIntArray( elem, "setupsMapped", setupIds );

		return new Partition( path, timepointIds, setupIds, timepointIdsPartition, setupIdsPartition );
	}
}
