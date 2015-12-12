package bdv.util;

import java.io.IOException;

import bdv.behaviour.io.InputTriggerDescriptionsBuilder;
import bdv.behaviour.io.json.JsonConfigIO;
import bdv.viewer.ViewerFrame;

public class DumpInputConfig
{
	public static void writeToJson( final String fileName, final ViewerFrame viewerFrame ) throws IOException
	{
		final InputTriggerDescriptionsBuilder builder = new InputTriggerDescriptionsBuilder();

		builder.addMap( viewerFrame.getKeybindings().getConcatenatedInputMap(), "bdv" );
		builder.addMap( viewerFrame.getTriggerbindings().getConcatenatedInputTriggerMap(), "bdv" );

		new JsonConfigIO().write( builder.getDescriptions(), fileName );
	}
}
