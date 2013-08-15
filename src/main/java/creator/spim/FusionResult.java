package creator.spim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import creator.spim.imgloader.FusionImageLoader;

public class FusionResult
{
	private final SequenceDescription desc;

	private final ViewRegistrations regs;

	public static FusionResult create(  final SpimRegistrationSequence spimseq, final String filepath, final String filepattern, final int numSlices, final double sliceValueMin, final double sliceValueMax, final List< AffineTransform3D > fusionTransforms )
	{
		final List< Integer > channels = new ArrayList< Integer >();
		for ( final ViewSetup setup : spimseq.getSequenceDescription().setups )
			if ( ! channels.contains( setup.getChannel() ) )
				channels.add( setup.getChannel() );
		final List< Integer > timepoints = spimseq.getSequenceDescription().timepoints;
		final int referenceTimePoint = spimseq.getViewRegistrations().referenceTimePoint;
		return new FusionResult( filepath, filepattern, channels, timepoints, referenceTimePoint, numSlices, sliceValueMin, sliceValueMax, fusionTransforms );
	}

	public FusionResult( final String filepath, final String filepattern, final List< Integer > timepoints, final int referenceTimePoint, final int numSlices, final double sliceValueMin, final double sliceValueMax, final List< AffineTransform3D > fusionTransforms )
	{
		final ImgLoader fusionLoader = new FusionImageLoader< FloatType >( filepath +"/" + filepattern, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
		desc = new SequenceDescription( Arrays.asList( new ViewSetup( 0, 0, 0, 0, 0, 0, numSlices, 1, 1, 1 ) ), timepoints, null, fusionLoader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int timepoint = 0; timepoint < timepoints.size(); ++timepoint )
			registrations.add( new ViewRegistration( timepoint, 0, fusionTransforms.get( timepoint ) ) );
		regs = new ViewRegistrations( registrations, referenceTimePoint );
	}

	public FusionResult( final String filepath, final String filepattern, final List< Integer > channels, final List< Integer > timepoints, final int referenceTimePoint, final int numSlices, final double sliceValueMin, final double sliceValueMax, final List< AffineTransform3D > fusionTransforms )
	{
		final ImgLoader fusionLoader = new FusionImageLoader< FloatType >( filepath +"/" + filepattern, numSlices, new FusionImageLoader.Gray32ImagePlusLoader(), sliceValueMin, sliceValueMax );
		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();
		for ( int id = 0; id < channels.size(); ++id )
			setups.add(  new ViewSetup( id, 0, 0, channels.get( id ), 0, 0, numSlices, 1, 1, 1 ) );
		desc = new SequenceDescription( setups, timepoints, null, fusionLoader );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int timepoint = 0; timepoint < timepoints.size(); ++timepoint )
			for ( int setup = 0; setup < setups.size(); ++setup )
				registrations.add( new ViewRegistration( timepoint, setup, fusionTransforms.get( timepoint ) ) );
		regs = new ViewRegistrations( registrations, referenceTimePoint );
	}

	public SequenceDescription getSequenceDescription()
	{
		return desc;
	}

	public ViewRegistrations getViewRegistrations()
	{
		return regs;
	}
}