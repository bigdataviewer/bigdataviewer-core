/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.viewer;

import java.util.ArrayList;
import java.util.List;

import bdv.tools.brightness.ConverterSetup;

/**
 * Associates {@code ConverterSetup}s to sources ({@code SourceAndConverter}).
 *
 * @author Tobias Pietzsch
 */
public interface SourceToConverterSetupBimap
{
	SourceAndConverter< ? > getSource( ConverterSetup converterSetup );

	ConverterSetup getConverterSetup( SourceAndConverter< ? > source );

	/**
	 * Returns list of all non-{@code null} ConverterSetups associated to
	 * {@code sources}.
	 */
	default List< ConverterSetup > getConverterSetups( final List< ? extends SourceAndConverter< ? > > sources )
	{
		final List< ConverterSetup > converterSetups = new ArrayList<>();
		for ( final SourceAndConverter< ? > source : sources )
		{
			final ConverterSetup converterSetup = getConverterSetup( source );
			if ( converterSetup != null )
				converterSetups.add( converterSetup );
		}
		return converterSetups;
	}
}
