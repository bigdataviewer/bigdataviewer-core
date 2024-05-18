/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv;

import bdv.export.ProgressWriterConsole;
import bdv.img.imaris.Imaris;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import java.io.IOException;
import mpicbg.spim.data.SpimDataException;

public class ImarisExample
{
	static void createXmlForIms( String imsFilename, String xmlFilename ) throws IOException, SpimDataException
	{
		final SpimDataMinimal spimData = Imaris.openIms( imsFilename );
		new XmlIoSpimDataMinimal().save( spimData, xmlFilename );
	}

	public static void main( String[] args ) throws IOException, SpimDataException
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		final String imsFilename = "/Users/pietzsch/Desktop/OvernightClaritySmall.ims";
		final String xmlFilename = "/Users/pietzsch/Desktop/OvernightClaritySmall.xml";

//		final String imsFilename = "/Users/pietzsch/Imaris Demo Images/DrosophilaEggChamber_with_objects.ims";
//		final String xmlFilename = "/Users/pietzsch/Imaris Demo Images/DrosophilaEggChamber_with_objects.xml";

		// write xml file for ims
		createXmlForIms( imsFilename, xmlFilename );

		// open xml file in BigDataViewer
		BigDataViewer.open( xmlFilename, "BigDataViewer", new ProgressWriterConsole(), ViewerOptions.options() );
	}
}

