/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.json.JsonConfigIO;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.viewer.ViewerFrame;

/**
 * @deprecated use {@code bdv.ui.keymap.DumpInputConfig} instead
 */
@Deprecated
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
