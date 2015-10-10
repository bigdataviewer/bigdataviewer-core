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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.hdf5.MipmapInfo;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.util.MipmapTransforms;
import ch.systemsx.cisd.hdf5.HDF5DataClass;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class Imaris
{
	public static SpimDataMinimal openIms( final String fn ) throws IOException
	{
		final IHDF5Reader reader = HDF5Factory.openForReading( fn );
		final IHDF5Access access;
		try
		{
			access = new HDF5AccessHack( reader );
		}
		catch ( final Exception e )
		{
			throw new RuntimeException( e );
		}

		final HashMap< Integer, double[] > levelToResolution = new HashMap< Integer, double[] >();
		final HashMap< Integer, int[] > levelToSubdivision = new HashMap< Integer, int[] >();
		final HashMap< Integer, long[] > levelToDimensions = new HashMap< Integer, long[] >();
		final HashMap< Integer, TimePoint > timepointMap = new HashMap< Integer, TimePoint >();
		final HashMap< Integer, BasicViewSetup > setupMap = new HashMap< Integer, BasicViewSetup >();

		String path = "DataSetInfo/Image";
		final double[] extMax = new double[] {
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMax0" ) ),
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMax1" ) ),
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMax2" ) ),
		};
		final double[] extMin = new double[] {
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMin0" ) ),
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMin1" ) ),
				Double.parseDouble( access.readImarisAttributeString( path, "ExtMin2" ) ),
		};
		final int[] imageSize = new int[] {
				Integer.parseInt( access.readImarisAttributeString( path, "X" ) ),
				Integer.parseInt( access.readImarisAttributeString( path, "Y" ) ),
				Integer.parseInt( access.readImarisAttributeString( path, "Z" ) ),
		};
		final String unit = access.readImarisAttributeString( path, "Unit" );
		final VoxelDimensions voxelSize = new FinalVoxelDimensions( unit, new double[] {
				( extMax[ 0 ] - extMin[ 0 ] ) / imageSize[ 0 ],
				( extMax[ 1 ] - extMin[ 1 ] ) / imageSize[ 1 ],
				( extMax[ 2 ] - extMin[ 2 ] ) / imageSize[ 2 ]
		} );

		DataTypes.DataType< ?, ?, ? > dataType = null;
		final List< String > resolutionNames = reader.getGroupMembers( "DataSet" );
		for ( final String resolutionName : resolutionNames )
		{
			if ( !resolutionName.startsWith( "ResolutionLevel " ) )
			{
				throw new IOException( "unexpected content '" + resolutionName + "' while reading " + fn );
			}
			else
			{
				final int level = Integer.parseInt( resolutionName.substring( "ResolutionLevel ".length() ) );
				final List< String > timepointNames = reader.getGroupMembers( "DataSet/" + resolutionName );
				for ( final String timepointName : timepointNames )
				{
					if ( !timepointName.startsWith( "TimePoint " ) )
					{
						throw new IOException( "unexpected content '" + timepointName + "' while reading " + fn );
					}
					else
					{
						final int timepoint = Integer.parseInt( timepointName.substring( "TimePoint ".length() ) );
						if ( !timepointMap.containsKey( timepoint ) )
							timepointMap.put( timepoint, new TimePoint( timepoint ) );

						final List< String > channelNames = reader.getGroupMembers( "DataSet/" + resolutionName + "/" + timepointName );
						for ( final String channelName : channelNames )
						{
							if ( !channelName.startsWith( "Channel " ) )
							{
								throw new IOException( "unexpected content '" + channelName + "' while reading " + fn );
							}
							else
							{
								final HDF5DataSetInformation info = reader.getDataSetInformation( "DataSet/" + resolutionName + "/" + timepointName + "/" + channelName + "/Data" );
								if (  dataType == null )
								{
									final HDF5DataTypeInformation ti = info.getTypeInformation();
									if ( ti.getDataClass().equals( HDF5DataClass.INTEGER ) )
									{
										switch ( ti.getElementSize() )
										{
										case 1:
											dataType = DataTypes.UnsignedByte;
											break;
										case 2:
											dataType = DataTypes.UnsignedShort;
											break;
										default:
											throw new IOException( "expected datatype" + ti );
										}
									}
									else if ( ti.getDataClass().equals( HDF5DataClass.FLOAT ) )
									{
										switch ( ti.getElementSize() )
										{
										case 4:
											dataType = DataTypes.Float;
											break;
										default:
											throw new IOException( "expected datatype" + ti );
										}
									}
								}

								final int channel = Integer.parseInt( channelName.substring( "Channel ".length() ) );
								if ( !setupMap.containsKey( channel ) )
								{
									final String defaultSetupName = "channel " + channel;
									final String name = access.readImarisAttributeString( "DataSetInfo/Channel " + channel, "Description", defaultSetupName );
									final BasicViewSetup setup = new BasicViewSetup( channel, name, new FinalDimensions( imageSize ), voxelSize );
									setupMap.put( channel, setup );
								}

								double[] resolution = levelToResolution.get( level );
								if ( resolution == null ) {
									path = "DataSet/" + resolutionName + "/" + timepointName + "/" + channelName;

									final long[] dims = new long[] {
											Integer.parseInt( access.readImarisAttributeString( path, "ImageSizeX" ) ),
											Integer.parseInt( access.readImarisAttributeString( path, "ImageSizeY" ) ),
											Integer.parseInt( access.readImarisAttributeString( path, "ImageSizeZ" ) ),
									};

									final int[] blockDims = new int[] { 16, 16, 16 };
									try
									{
										blockDims[ 0 ] = Integer.parseInt( access.readImarisAttributeString( path, "ImageBlockSizeX" ) );
										blockDims[ 1 ] = Integer.parseInt( access.readImarisAttributeString( path, "ImageBlockSizeY" ) );
										blockDims[ 2 ] = Integer.parseInt( access.readImarisAttributeString( path, "ImageBlockSizeZ" ) );
									} catch( final NumberFormatException e )
									{
										final int[] chunkSizes = info.tryGetChunkSizes();
										if ( chunkSizes != null )
											for ( int d = 0; d < 3; ++d )
												blockDims[ d ] = chunkSizes[ d ];
									}

									resolution = new double[] {
											imageSize[ 0 ] / dims[ 0 ],
											imageSize[ 1 ] / dims[ 1 ],
											imageSize[ 2 ] / dims[ 2 ],
									};

									levelToDimensions.put( level, dims );
									levelToResolution.put( level, resolution );
									levelToSubdivision.put( level, blockDims );
								}
							}
						}
					}
				}
			}
		}

		final int numLevels = levelToResolution.size();
		final long[][] dimensions = new long[ numLevels ][];
		final double[][] resolutions = new double[ numLevels ][];
		final int[][] subdivisions = new int[ numLevels ][];
		final AffineTransform3D[] transforms = new AffineTransform3D[ numLevels ];
		for ( int level = 0; level < numLevels; ++level )
		{
			dimensions[ level ] = levelToDimensions.get( level );
			resolutions[ level ] = levelToResolution.get( level );
			subdivisions[ level ] = levelToSubdivision.get( level );
			transforms[ level ] = MipmapTransforms.getMipmapTransformDefault( resolutions[ level ] );
		}
		final MipmapInfo mipmapInfo = new MipmapInfo( resolutions, transforms, subdivisions );

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepointMap ), setupMap, null, null );
		@SuppressWarnings( { "rawtypes", "unchecked" } ) // TODO: when ij2 supports java 7, replace SuppressWarnings by new ImarisImageLoader<>(...)
		final ImarisImageLoader<?,?,?> imgLoader = new ImarisImageLoader( dataType, new File( fn ), mipmapInfo, dimensions, seq );
		seq.setImgLoader( imgLoader );

		final File basePath = new File( fn ).getParentFile();
		final HashMap< ViewId, ViewRegistration > registrations = new HashMap< ViewId, ViewRegistration >();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
		{
			final int setupId = setup.getId();
			final AffineTransform3D calib = new AffineTransform3D();
			final double sx = voxelSize.dimension( 0 );
			final double sy = voxelSize.dimension( 1 );
			final double sz = voxelSize.dimension( 2 );
			calib.set(
					sx,  0,  0, 0,
					 0, sy,  0, 0,
					 0,  0, sz, 0 );
			for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() )
			{
				final int timepointId = timepoint.getId();
				registrations.put( new ViewId( timepointId, setupId ), new ViewRegistration( timepointId, setupId, calib ) );
			}
		}
		final SpimDataMinimal spimData = new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );

		reader.close();

		return spimData;
	}
}
