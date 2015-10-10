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
package bdv.spimdata.legacy;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.BASEPATH_TAG;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

import bdv.img.catmaid.XmlIoCatmaidImageLoader;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.img.openconnectome.XmlIoOpenConnectomeImageLoader;
import bdv.img.remote.XmlIoRemoteImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;

public class XmlIoSpimDataMinimalLegacy
{
	// load legacy SequenceDescription xml format
	public static SpimDataMinimal fromXml( final Element root, final File xmlFile )
	{
		final File basePath = loadBasePath( root, xmlFile );

		final TimePoints timepoints = createTimepointsFromXml( root );
		final Map< Integer, ? extends BasicViewSetup > setups = createViewSetupsFromXml( root );
		final MissingViews missingViews = null;
		final SequenceDescriptionMinimal sequenceDescription = new SequenceDescriptionMinimal( timepoints, setups, null, missingViews );
		final BasicImgLoader imgLoader = createImgLoaderFromXml( root, basePath, sequenceDescription );
		sequenceDescription.setImgLoader( imgLoader );

		final ViewRegistrations viewRegistrations = createRegistrationsFromXml( root );

		return new SpimDataMinimal( basePath, sequenceDescription, viewRegistrations );
	}

	private static File loadBasePath( final Element root, final File xmlFile )
	{
		File xmlFileParentDirectory = xmlFile.getParentFile();
		if ( xmlFileParentDirectory == null )
			xmlFileParentDirectory = new File( "." );
		return XmlHelpers.loadPath( root, BASEPATH_TAG, ".", xmlFileParentDirectory );
	}

	private static TimePoints createTimepointsFromXml( final Element sequenceDescription )
	{
		final Element timepoints = sequenceDescription.getChild( "Timepoints" );
		final String type = timepoints.getAttributeValue( "type" );
		if ( type.equals( "range" ) )
		{
			final int first = Integer.parseInt( timepoints.getChildText( "first" ) );
			final int last = Integer.parseInt( timepoints.getChildText( "last" ) );
			final ArrayList< TimePoint > tps = new ArrayList< TimePoint >();
			for ( int i = first, t = 0; i <= last; ++i, ++t )
				tps.add( new TimePoint( t ) );
			return new TimePoints( tps );
		}
		else
		{
			throw new RuntimeException( "unknown <Timepoints> type: " + type );
		}
	}

	private static Map< Integer, ? extends BasicViewSetup > createViewSetupsFromXml( final Element sequenceDescription )
	{
		final HashMap< Integer, BasicViewSetup > setups = new HashMap< Integer, BasicViewSetup >();
		final HashMap< Integer, Angle > angles = new HashMap< Integer, Angle >();
		final HashMap< Integer, Channel > channels = new HashMap< Integer, Channel >();
		final HashMap< Integer, Illumination > illuminations = new HashMap< Integer, Illumination >();

		for ( final Element elem : sequenceDescription.getChildren( "ViewSetup" ) )
		{
			final int id = XmlHelpers.getInt( elem, "id" );

			final int angleId = XmlHelpers.getInt( elem, "angle" );
			Angle angle = angles.get( angleId );
			if ( angle == null )
			{
				angle = new Angle( angleId );
				angles.put( angleId, angle );
			}

			final int illuminationId = XmlHelpers.getInt( elem, "illumination" );
			Illumination illumination = illuminations.get( illuminationId );
			if ( illumination == null )
			{
				illumination = new Illumination( illuminationId );
				illuminations.put( illuminationId, illumination );
			}

			final int channelId = XmlHelpers.getInt( elem, "channel" );
			Channel channel = channels.get( channelId );
			if ( channel == null )
			{
				channel = new Channel( channelId );
				channels.put( channelId, channel );
			}

			final long w = XmlHelpers.getInt( elem, "width" );
			final long h = XmlHelpers.getInt( elem, "height" );
			final long d = XmlHelpers.getInt( elem, "depth" );
			final Dimensions size = new FinalDimensions( w, h, d );

			final double pw = XmlHelpers.getDouble( elem, "pixelWidth" );
			final double ph = XmlHelpers.getDouble( elem, "pixelHeight" );
			final double pd = XmlHelpers.getDouble( elem, "pixelDepth" );
			final VoxelDimensions voxelSize = new FinalVoxelDimensions( "px", pw, ph, pd );

			final ViewSetup setup = new ViewSetup( id, null, size, voxelSize, channel, angle, illumination );
			setups.put( id, setup );
		}
		return setups;
	}

	private static BasicImgLoader createImgLoaderFromXml( final Element sequenceDescriptionElem, final File basePath, final SequenceDescriptionMinimal sequenceDescription  )
	{
		final Element elem = sequenceDescriptionElem.getChild( "ImageLoader" );
		final String classn = elem.getAttributeValue( "class" );
		if ( classn.equals( "viewer.hdf5.Hdf5ImageLoader" ) || classn.equals( "bdv.img.hdf5.Hdf5ImageLoader" ) )
		{
			final String path = loadPath( elem, "hdf5", basePath ).toString();
			final ArrayList< Partition > partitions = new ArrayList< Partition >();
			for ( final Element p : elem.getChildren( "partition" ) )
				partitions.add( partitionFromXml( p, basePath ) );
			return new Hdf5ImageLoader( new File( path ), partitions, sequenceDescription );
		}
		else if ( classn.equals( "bdv.img.catmaid.CatmaidImageLoader" ) )
		{
			return new XmlIoCatmaidImageLoader().fromXml( elem, basePath, sequenceDescription );
		}
		else if ( classn.equals( "bdv.img.openconnectome.OpenConnectomeImageLoader" ) )
		{
			return new XmlIoOpenConnectomeImageLoader().fromXml( elem, basePath, sequenceDescription );
		}
		else if ( classn.equals( "bdv.img.remote.RemoteImageLoader" ) )
		{
			return new XmlIoRemoteImageLoader().fromXml( elem, basePath, sequenceDescription );
		}
		else
			throw new RuntimeException( "unknown ImageLoader class" );
	}

	private static Partition partitionFromXml( final Element elem, final File basePath )
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

		/* @param timepointOffset
		 *            The timepoint <em>t</em> in the partition corresponds to
		 *            timepoint <em>t + <code>timepointOffset</code></em> in the
		 *            full sequence.
		 * @param timepointStart
		 *            The first timepoint <em>t</em> contained in this partition
		 *            (relative to the offset, not the full sequence).
		 * @param timepointLength
		 *            How many timepoints are contained in this partition.
		 * @param setupOffset
		 *            The setup <em>s</em> in the partition corresponds to
		 *            setup <em>s + <code>setupOffset</code></em> in the
		 *            full sequence.
		 * @param setupStart
		 *            The first setup <em>s</em> contained in this partition
		 *            (relative to the offset, not the full sequence).
		 * @param setupLength
		 *            How many setups are contained in this partition.
		 */
		final int timepointOffset = Integer.parseInt( elem.getChildText( "timepointOffset" ) );
		final int timepointStart = Integer.parseInt( elem.getChildText( "timepointStart" ) );
		final int timepointLength = Integer.parseInt( elem.getChildText( "timepointLength" ) );
		final int setupOffset = Integer.parseInt( elem.getChildText( "setupOffset" ) );
		final int setupStart = Integer.parseInt( elem.getChildText( "setupStart" ) );
		final int setupLength = Integer.parseInt( elem.getChildText( "setupLength" ) );

		final HashMap< Integer, Integer > timepointIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( int tPartition = timepointStart; tPartition < timepointStart + timepointLength; ++tPartition )
		{
			final int tSequence = tPartition + timepointOffset;
			timepointIdSequenceToPartition.put( tSequence, tPartition );
		}

		final HashMap< Integer, Integer > setupIdSequenceToPartition = new HashMap< Integer, Integer >();
		for ( int sPartition = setupStart; sPartition < setupStart + setupLength; ++sPartition )
		{
			final int sSequence = sPartition + setupOffset;
			setupIdSequenceToPartition.put( sSequence, sPartition );
		}

		return new Partition( path, timepointIdSequenceToPartition, setupIdSequenceToPartition );
	}

	protected static ViewRegistrations createRegistrationsFromXml( final Element sequenceDescriptionElem )
	{
		final Element elem = sequenceDescriptionElem.getChild( "ViewRegistrations" );
		final ArrayList< ViewRegistration > regs = new ArrayList< ViewRegistration >();
		for ( final Element vr : elem.getChildren( "ViewRegistration" ) )
		{
			final int timepointId = XmlHelpers.getInt( vr, "timepoint" );
			final int setupId = XmlHelpers.getInt( vr, "setup" );
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D( vr, "affine" );
			regs.add( new ViewRegistration( timepointId, setupId, transform ) );
		}
		return new ViewRegistrations( regs );
	}

}
