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
package bdv.spimdata;

import static mpicbg.spim.data.XmlKeys.SPIMDATA_TAG;

import java.io.File;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.XmlIoAbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.XmlIoBasicViewSetups;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoTimePoints;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import bdv.spimdata.legacy.XmlIoSpimDataMinimalLegacy;

public class XmlIoSpimDataMinimal extends XmlIoAbstractSpimData< SequenceDescriptionMinimal, SpimDataMinimal >
{
	public XmlIoSpimDataMinimal()
	{
		super( SpimDataMinimal.class,
				new XmlIoAbstractSequenceDescription< BasicViewSetup, SequenceDescriptionMinimal >(
						SequenceDescriptionMinimal.class,
						new XmlIoTimePoints(),
						new XmlIoBasicViewSetups< BasicViewSetup >( BasicViewSetup.class ),
						new XmlIoMissingViews() ),
				new XmlIoViewRegistrations() );
	}

	@Override
	public SpimDataMinimal load( final String xmlFilename ) throws SpimDataException
		{
			final SAXBuilder sax = new SAXBuilder();
			Document doc;
			try
			{
				doc = sax.build( xmlFilename );
			}
			catch ( final Exception e )
			{
				throw new SpimDataIOException( e );
			}
			final Element root = doc.getRootElement();

			if ( root.getName().equals( "SequenceDescription" ) )
				return XmlIoSpimDataMinimalLegacy.fromXml( root, new File( xmlFilename ) );

			if ( root.getName() != SPIMDATA_TAG )
				throw new RuntimeException( "expected <" + SPIMDATA_TAG + "> root element. wrong file?" );

			return fromXml( root, new File( xmlFilename ) );
		}
}
