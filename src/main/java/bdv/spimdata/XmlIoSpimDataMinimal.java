/*
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
package bdv.spimdata;

import bdv.ViewerImgLoader;
import bdv.cache.SharedQueue;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.XmlIoAbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.XmlIoBasicViewSetups;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoTimePoints;

public class XmlIoSpimDataMinimal extends XmlIoAbstractSpimData< SequenceDescriptionMinimal, SpimDataMinimal >
{
	public XmlIoSpimDataMinimal()
	{
		super( SpimDataMinimal.class,
				new XmlIoAbstractSequenceDescription<>(
						SequenceDescriptionMinimal.class,
						new XmlIoTimePoints(),
						new XmlIoBasicViewSetups<>( BasicViewSetup.class ),
						new XmlIoMissingViews() ),
				new XmlIoViewRegistrations() );
	}

	public SpimDataMinimal load( final String xmlFilename, final int numFetcherThreads ) throws SpimDataException
	{
		final SpimDataMinimal spimData = load( xmlFilename );
		final BasicImgLoader imgLoader = spimData.getSequenceDescription().getImgLoader();
		if ( imgLoader instanceof ViewerImgLoader )
			( ( ViewerImgLoader ) imgLoader ).setNumFetcherThreads( numFetcherThreads );
		return spimData;
	}

	public SpimDataMinimal load( final String xmlFilename, final SharedQueue sharedQueue ) throws SpimDataException
	{
		final SpimDataMinimal spimData = load( xmlFilename );
		final BasicImgLoader imgLoader = spimData.getSequenceDescription().getImgLoader();
		if ( imgLoader instanceof ViewerImgLoader )
			( ( ViewerImgLoader ) imgLoader ).setCreatedSharedQueue( sharedQueue );
		return spimData;
	}
}
