package bdv.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.json.JsonConfigIO;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.viewer.ViewerFrame;

public class DumpInputConfig
{
	private static List< InputTriggerDescription > buildDescriptions( final ViewerFrame viewerFrame ) throws IOException
	{
		final InputTriggerDescriptionsBuilder builder = new InputTriggerDescriptionsBuilder();

		builder.addMap( viewerFrame.getKeybindings().getConcatenatedInputMap(), "bdv" );
		builder.addMap( viewerFrame.getTriggerbindings().getConcatenatedInputTriggerMap(), "bdv" );

		return builder.getDescriptions();
	}

	public static boolean mkdirs( final String fileName )
	{
		final File dir = new File( fileName ).getParentFile();
		return dir == null ? false : dir.mkdirs();
	}

	public static void writeToJson( final String fileName, final ViewerFrame viewerFrame ) throws IOException
	{
		mkdirs( fileName );
		JsonConfigIO.write( buildDescriptions( viewerFrame ), fileName );
	}

	public static void writeToYaml( final String fileName, final ViewerFrame viewerFrame ) throws IOException
	{
		mkdirs( fileName );
		YamlConfigIO.write(  buildDescriptions( viewerFrame ), fileName );
	}
}
