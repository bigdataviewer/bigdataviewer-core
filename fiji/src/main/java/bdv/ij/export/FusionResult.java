package bdv.ij.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import bdv.ij.export.imgloader.FusionImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;

public class FusionResult
{
	private final SequenceDescriptionMinimal desc;

	private final ViewRegistrations regs;

	public static FusionResult create(
			final SpimRegistrationSequence spimseq,
			final String filepath,
			final String filepattern,
			final int numSlices,
			final double sliceValueMin,
			final double sliceValueMax,
			final Map< Integer, AffineTransform3D > perTimePointFusionTransforms )
	{
		// add one fused ViewSetup per channel in the SpimRegistrationSequence
		final List< Integer > channels = new ArrayList< Integer >();
		for ( final BasicViewSetup setup : spimseq.getSequenceDescription().getViewSetupsOrdered() )
		{
			final int channel = setup.getAttribute( Channel.class ).getId();
			if ( ! channels.contains( channel ) )
				channels.add( channel );
		}
		final TimePoints timepoints = spimseq.getSequenceDescription().getTimePoints();
		return new FusionResult( filepath, filepattern, channels, timepoints, numSlices, sliceValueMin, sliceValueMax, perTimePointFusionTransforms );
	}

	public FusionResult(
			final String filepath,
			final String filepattern,
			final TimePoints timepoints,
			final int numSlices,
			final double sliceValueMin,
			final double sliceValueMax,
			final Map< Integer, AffineTransform3D > perTimePointFusionTransforms )
	{
		final HashMap< Integer, Integer > setupIdToChannelId = new HashMap< Integer, Integer >();
		setupIdToChannelId.put( 0, 0 );
		final ImgLoader< UnsignedShortType > fusionLoader = new FusionImageLoader< FloatType >( filepath +"/" + filepattern, setupIdToChannelId, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
		final int setupId = 0;
		final String name = "fused";
		final ViewId view = new ViewId( timepoints.getTimePointsOrdered().get( 0 ).getId(), setupId );
		final Dimensions size = fusionLoader.getImageSize( view );
		final VoxelDimensions voxelSize = fusionLoader.getVoxelSize( view );
		final BasicViewSetup setup = new BasicViewSetup( setupId, name, size, voxelSize );
		desc = new SequenceDescriptionMinimal( timepoints, Entity.idMap( Arrays.asList( setup ) ), fusionLoader, null );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( final TimePoint timepoint : timepoints.getTimePointsOrdered() )
			registrations.add( new ViewRegistration( timepoint.getId(), 0, perTimePointFusionTransforms.get( timepoint.getId() ) ) );
		regs = new ViewRegistrations( registrations );
	}

	public FusionResult(
			final String filepath,
			final String filepattern,
			final List< Integer > channels,
			final TimePoints timepoints,
			final int numSlices,
			final double sliceValueMin,
			final double sliceValueMax,
			final Map< Integer, AffineTransform3D > perTimePointFusionTransforms )
	{
		final HashMap< Integer, Integer > setupIdToChannelId = new HashMap< Integer, Integer >();
		for ( int setupId = 0; setupId < channels.size(); ++setupId )
		{
			setupIdToChannelId.put( setupId, channels.get( setupId ) );
		}
		final ImgLoader< UnsignedShortType > fusionLoader = new FusionImageLoader< FloatType >( filepath +"/" + filepattern, setupIdToChannelId, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
		final ArrayList< BasicViewSetup > setups = new ArrayList< BasicViewSetup >();
		for ( int setupId = 0; setupId < channels.size(); ++setupId )
		{
			final String name = "fused c " + channels.get( setupId );
			final ViewId view = new ViewId( timepoints.getTimePointsOrdered().get( 0 ).getId(), setupId );
			final Dimensions size = fusionLoader.getImageSize( view );
			final VoxelDimensions voxelSize = fusionLoader.getVoxelSize( view );
			final BasicViewSetup setup = new BasicViewSetup( setupId, name, size, voxelSize );
			setup.setAttribute( new Channel( channels.get( setupId ) ) );
			setups.add( setup );
		}
		desc = new SequenceDescriptionMinimal( timepoints, Entity.idMap( setups ), fusionLoader, null );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( final TimePoint timepoint : timepoints.getTimePointsOrdered() )
			for ( final BasicViewSetup setup : setups )
				registrations.add( new ViewRegistration( timepoint.getId(), setup.getId(), perTimePointFusionTransforms.get( timepoint.getId() ) ) );
		regs = new ViewRegistrations( registrations );
	}

	public SequenceDescriptionMinimal getSequenceDescription()
	{
		return desc;
	}

	public ViewRegistrations getViewRegistrations()
	{
		return regs;
	}
}