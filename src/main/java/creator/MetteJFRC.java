package creator;

import java.io.IOException;
import java.util.ArrayList;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.io.ImgIOException;
import net.imglib2.realtransform.AffineTransform3D;
import viewer.hdf5.Partition;
import creator.Scripting.PartitionedSequenceWriter;
import creator.spim.imgloader.StackImageLoader;

public class MetteJFRC
{
	public static void main( final String[] args ) throws ImgIOException, IOException
	{
		final String exportXmlFilename = "/Users/pietzsch/Desktop/mette.xml";
		final int[][] spimresolutions = { { 1, 1, 1 }, { 2, 2, 1 }, { 4, 4, 2 } };
		final int[][] spimsubdivisions = { { 32, 32, 4 }, { 16, 16, 8 }, { 8, 8, 8 } };

		final double zScaling = 6.0;
		final int numTimePoints = 11;
		final int numSetups = 2;
		final String pattern = "/Users/pietzsch/workspace/data/mette-jfrc/Pdu_E1_H2BeGFP-CAAXmCherry-RA.TM%1$04d_timeFused_blending/SPC0_TM%1$04d_CM0_CM1_%2$s.fusedStack.jp2";
		final String[] channelStrings = { "CHN00_CHN01", "CHN02_CHN03" };

		final ArrayList< String > filenames = new ArrayList< String >();
		for ( int tp = 0; tp < numTimePoints; ++tp )
			for ( int s = 0; s < numSetups; ++s )
				filenames.add( String.format( pattern, tp, channelStrings[ s ] ) );
		final StackImageLoader imgLoader = new StackImageLoader( filenames, numSetups, false );

		final ArrayList< ViewSetup > setups = new ArrayList< ViewSetup >();
		for ( int s = 0; s < numSetups; ++s )
			setups.add( new ViewSetup( s, 0, 0, s, 0, 0, 0, 0, 0, 0 ) );

		final ArrayList< Integer > timepoints = new ArrayList< Integer >();
			for ( int tp = 0; tp < numTimePoints; ++tp )
				timepoints.add( tp );

		final SequenceDescription sourceSequence = new SequenceDescription( setups, timepoints, null, imgLoader );

		final AffineTransform3D model = new AffineTransform3D();
		model.set( zScaling, 2, 2 );
		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >();
		for ( int tp = 0; tp < numTimePoints; ++tp )
			for ( int s = 0; s < numSetups; ++s )
				registrations.add( new ViewRegistration( tp, s, model ) );
		final ViewRegistrations sourceRegs = new ViewRegistrations( registrations, 0 );

		final SetupAggregator aggregator = new SetupAggregator();
		for ( final ViewSetup setup : setups )
			aggregator.add( setup, sourceSequence, sourceRegs, spimresolutions, spimsubdivisions );


		// splitting ...
		final int timepointsPerPartition = 0;
		final int setupsPerPartition = 0;
		final ArrayList< Partition > partitions = Scripting.split( aggregator, timepointsPerPartition, setupsPerPartition, exportXmlFilename );

		final PartitionedSequenceWriter writer = new PartitionedSequenceWriter( aggregator, exportXmlFilename, partitions );
		System.out.println( writer.numPartitions() );
		for ( int i = 0; i < writer.numPartitions(); ++i )
			writer.writePartition( i );
		writer.writeXmlAndLinks();
	}
}
