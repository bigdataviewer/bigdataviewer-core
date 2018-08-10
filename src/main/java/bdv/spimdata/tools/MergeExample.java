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
package bdv.spimdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;

/**
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class MergeExample
{
	/**
	 * Merge multiple HDF5 datasets, where each dataset contains the same
	 * timepoints but different views.
	 *
	 * @param inputFilenames
	 * 	xml file names for input datasets
	 * @param transforms
	 *  transforms to apply to each input dataset
	 * @param outputXmlFilename
	 * 	xml filename into which to store the merged dataset. An HDF5 link master file with the same basename and extension ".h5" will be created that links into the source hdf5s.
	 * @throws SpimDataException
	 */
	public static void mergeHdf5ViewsSetups(
			final List< String > inputFilenames,
			final List< ViewTransform > transforms,
			final String outputXmlFilename )
					throws SpimDataException
	{
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();

		final HashMap< String, Set< Integer > > attributeIdsInUse = new HashMap<>();
		final HashSet< Integer > setupIdsInUse = new HashSet<>();
		final ArrayList< Partition > newPartitions = new ArrayList<>();
		final Map< Integer, ExportMipmapInfo > newMipmapInfos = new HashMap<>();
		final ArrayList< SpimDataMinimal > spimDatas = new ArrayList<>();
		for ( int i = 0; i < inputFilenames.size(); ++i )
		{
			final String fn = inputFilenames.get( i );
			final ViewTransform transform = transforms.get( i );

			final SpimDataMinimal spimData = io.load( fn );
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final ArrayList< Partition > partitions = MergePartitionList.getPartitions( seq );
			final Map< Integer, ExportMipmapInfo > mipmapInfos = MergePartitionList.getHdf5PerSetupExportMipmapInfos( seq );

			final Map< String, Map< Integer, Integer > > attributesReassigned = ChangeAttributeId.assignNewAttributeIds( spimData, attributeIdsInUse );
			final Map< Integer, Integer > setupsReassigned = ChangeViewSetupId.assignNewViewSetupIds( spimData, setupIdsInUse );

			for ( final Partition partition : partitions )
			{
				final Map< Integer, Integer > seqToPart = partition.getSetupIdSequenceToPartition();
				final Map< Integer, Integer > newSeqToPart = new HashMap<>();
				for ( final Entry< Integer, Integer > entry : seqToPart.entrySet() )
				{
					final int oldSeq = entry.getKey();
					final int newSeq = setupsReassigned.containsKey( oldSeq ) ? setupsReassigned.get( oldSeq ) : oldSeq;
					newSeqToPart.put( newSeq, entry.getValue() );
				}
				newPartitions.add( new Partition( partition.getPath(), partition.getTimepointIdSequenceToPartition(), newSeqToPart ) );
			}

			for ( final Entry< Integer, ExportMipmapInfo > entry : mipmapInfos.entrySet() )
			{
				final int oldSeq = entry.getKey();
				final int newSeq = setupsReassigned.containsKey( oldSeq ) ? setupsReassigned.get( oldSeq ) : oldSeq;
				newMipmapInfos.put( newSeq, entry.getValue() );
			}

			final ViewRegistrations regs = spimData.getViewRegistrations();
			if ( transform != null )
				for ( final ViewRegistration reg : regs.getViewRegistrationsOrdered() )
					reg.concatenateTransform( transform );

			spimDatas.add( spimData );
		}

		final File xmlFile = new File( outputXmlFilename );
		final File path = xmlFile.getParentFile();
		final String xmlFilename = xmlFile.getAbsolutePath();
		final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
		final File h5File = new File( basename + ".h5" );
		if ( h5File.exists() )
			h5File.delete();

		final SpimDataMinimal spimData = MergeTools.merge( path, spimDatas );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader imgLoader = new Hdf5ImageLoader( h5File, newPartitions, seq, false );
		seq.setImgLoader( imgLoader );

		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, newMipmapInfos );
		io.save( spimData, xmlFilename );
	}


	/**
	 * Append multiple HDF5 datasets, where each dataset contains the same
	 * setups but different timepoints.
	 *
	 * @param inputFilenames
	 * 	xml file names for input datasets
	 * @param outputXmlFilename
	 * 	xml filename into which to store the merged dataset. An HDF5 link master file with the same basename and extension ".h5" will be created that links into the source hdf5s.
	 * @throws SpimDataException
	 */
	public static void mergeHdf5ViewsTimepoints(
			final List< String > inputFilenames,
			final String outputXmlFilename )
					throws SpimDataException
	{
		final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();

		final ArrayList< Partition > newPartitions = new ArrayList<>();
		final Map< Integer, ExportMipmapInfo > newMipmapInfos = new HashMap<>();
		final ArrayList< SpimDataMinimal > spimDatas = new ArrayList<>();

		int maxUsedTimepointId = -1;

		for ( int i = 0; i < inputFilenames.size(); ++i )
		{
			final String fn = inputFilenames.get( i );
			final SpimDataMinimal spimData = io.load( fn );

			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final ArrayList< Partition > partitions = MergePartitionList.getPartitions( seq );
			final Map< Integer, ExportMipmapInfo > mipmapInfos = MergePartitionList.getHdf5PerSetupExportMipmapInfos( seq );
			newMipmapInfos.putAll( mipmapInfos );

			final List< TimePoint > tpsOrdered = seq.getTimePoints().getTimePointsOrdered();
			final int oldFirst = tpsOrdered.get( 0 ).getId();
			final int newFirst = Math.max( maxUsedTimepointId + 1, oldFirst );
			final int offset = newFirst - oldFirst;
			maxUsedTimepointId = tpsOrdered.get( tpsOrdered.size() - 1 ).getId() + offset;
			if ( offset == 0 )
			{
				newPartitions.addAll( partitions );
				spimDatas.add( spimData );
			}
			else
			{
				final Map< Integer, TimePoint > tps = new HashMap<>();
				for ( final TimePoint tp : tpsOrdered )
				{
					final TimePoint tpReassigned = new TimePoint( tp.getId() + offset );
					tps.put( tpReassigned.getId(), tpReassigned );
				}
				final TimePoints newTps = new TimePoints( tps );

				for ( final Partition partition : partitions )
				{
					final Map< Integer, Integer > seqToPart = partition.getTimepointIdSequenceToPartition();
					final Map< Integer, Integer > newSeqToPart = new HashMap<>();
					for ( final Entry< Integer, Integer > entry : seqToPart.entrySet() )
					{
						final int oldSeq = entry.getKey();
						final int newSeq = oldSeq + offset;
						newSeqToPart.put( newSeq, entry.getValue() );
					}
					newPartitions.add( new Partition( partition.getPath(), newSeqToPart, partition.getSetupIdSequenceToPartition() ) );
				}

				final ViewRegistrations regs = spimData.getViewRegistrations();
				final Map< ViewId, ViewRegistration > newRegsMap = new HashMap<>();
				for ( final ViewRegistration reg : regs.getViewRegistrationsOrdered() )
				{
					final ViewRegistration newReg = new ViewRegistration(
							reg.getTimePointId() + offset,
							reg.getViewSetupId(),
							new ArrayList<>( reg.getTransformList() ) );
					newRegsMap.put( newReg, newReg );
				}
				final ViewRegistrations newRegs = new ViewRegistrations( newRegsMap );

				final Set< ViewId > mv = seq.getMissingViews().getMissingViews();
				final Set< ViewId > newMv = new HashSet<>();
				for ( final ViewId view : mv )
					newMv.add( new ViewId( view.getTimePointId() + offset, view.getViewSetupId() ) );
				final MissingViews newMissingViews = new MissingViews( newMv );

				final SpimDataMinimal newSpimData = new SpimDataMinimal(
						spimData.getBasePath(),
						new SequenceDescriptionMinimal( newTps, seq.getViewSetups(), seq.getImgLoader(), newMissingViews ),
						newRegs );

				spimDatas.add( newSpimData );
			}
		}

		final File xmlFile = new File( outputXmlFilename );
		final File path = xmlFile.getParentFile();
		final String xmlFilename = xmlFile.getAbsolutePath();
		final String basename = xmlFilename.endsWith( ".xml" ) ? xmlFilename.substring( 0, xmlFilename.length() - 4 ) : xmlFilename;
		final File h5File = new File( basename + ".h5" );
		if ( h5File.exists() )
			h5File.delete();

		final SpimDataMinimal spimData = MergeTools.merge( path, spimDatas );
		final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader imgLoader = new Hdf5ImageLoader( h5File, newPartitions, seq, false );
		seq.setImgLoader( imgLoader );

		WriteSequenceToHdf5.writeHdf5PartitionLinkFile( seq, newMipmapInfos );
		io.save( spimData, xmlFilename );
	}
}
