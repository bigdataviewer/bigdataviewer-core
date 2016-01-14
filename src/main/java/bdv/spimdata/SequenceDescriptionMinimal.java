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

import java.util.Map;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoints;

public class SequenceDescriptionMinimal extends AbstractSequenceDescription< BasicViewSetup, BasicViewDescription< BasicViewSetup >, BasicImgLoader >
{
	public SequenceDescriptionMinimal( final TimePoints timepoints, final Map< Integer, ? extends BasicViewSetup > setups, final BasicImgLoader imgLoader, final MissingViews missingViews )
	{
		super( timepoints, setups, imgLoader, missingViews );
	}

	@Override
	protected BasicViewDescription< BasicViewSetup > createViewDescription( final int timepointId, final int setupId )
	{
		return new BasicViewDescription< BasicViewSetup >( timepointId, setupId, true, this );
	}

	/**
	 * create copy of a {@link SequenceDescriptionMinimal} with replaced {@link BasicImgLoader}
	 */
	public SequenceDescriptionMinimal( final SequenceDescriptionMinimal other, final BasicImgLoader imgLoader )
	{
		super( other.getTimePoints(), other.getViewSetups(), imgLoader, other.getMissingViews() );
	}

	protected SequenceDescriptionMinimal()
	{}
}
