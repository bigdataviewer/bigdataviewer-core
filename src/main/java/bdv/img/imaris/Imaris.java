/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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

import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
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

		final HashMap< Integer, TimePoint > timepointMap = new HashMap<>();
		final HashMap< Integer, BasicViewSetup > setupMap = new HashMap<>();

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
		final String unit = access.readImarisAttributeString( path, "Unit", "um" );
		final VoxelDimensions voxelSize = new FinalVoxelDimensions( unit,
				( extMax[ 0 ] - extMin[ 0 ] ) / imageSize[ 0 ],
				( extMax[ 1 ] - extMin[ 1 ] ) / imageSize[ 1 ],
				( extMax[ 2 ] - extMin[ 2 ] ) / imageSize[ 2 ] );

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

								final int channel = Integer.parseInt( channelName.substring( "Channel ".length() ) );
								if ( !setupMap.containsKey( channel ) )
								{
									final String defaultSetupName = "channel " + channel;
									final String name = access.readImarisAttributeString( "DataSetInfo/Channel " + channel, "Name", defaultSetupName );
									final BasicViewSetup setup = new BasicViewSetup( channel, name, new FinalDimensions( imageSize ), voxelSize );
									setupMap.put( channel, setup );
								}
							}
						}
					}
				}
			}
		}

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( new TimePoints( timepointMap ), setupMap, null, null );
		final ImarisImageLoader< ?, ?, ? > imgLoader = new ImarisImageLoader<>( new File( fn ), seq );
		seq.setImgLoader( imgLoader );

		final File basePath = new File( fn ).getParentFile();
		final HashMap< ViewId, ViewRegistration > registrations = new HashMap<>();
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
