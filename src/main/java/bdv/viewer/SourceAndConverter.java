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
package bdv.viewer;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;

/**
 * Data source (for one view setup) and a converter to ARGBType.
 */
public class SourceAndConverter< T >
{
	/**
	 * provides image data for all timepoints of one view.
	 */
	protected final Source< T > spimSource;

	/**
	 * converts {@link #spimSource} type T to ARGBType for display
	 */
	protected final Converter< T, ARGBType > converter;

	protected final SourceAndConverter< ? extends Volatile< T > > volatileSourceAndConverter;

	public SourceAndConverter( final Source< T > spimSource, final Converter< T, ARGBType > converter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
		this.volatileSourceAndConverter = null;
	}

	public SourceAndConverter( final Source< T > spimSource, final Converter< T, ARGBType > converter, final SourceAndConverter< ? extends Volatile< T > > volatileSourceAndConverter )
	{
		this.spimSource = spimSource;
		this.converter = converter;
		this.volatileSourceAndConverter = volatileSourceAndConverter;
	}

	/**
	 * copy constructor
	 * @param soc
	 */
	protected SourceAndConverter( final SourceAndConverter< T > soc )
	{
		this.spimSource = soc.spimSource;
		this.converter = soc.converter;
		this.volatileSourceAndConverter = soc.volatileSourceAndConverter;
	}

	/**
	 * Get the {@link Source} (provides image data for all timepoints of one
	 * angle).
	 */
	public Source< T > getSpimSource()
	{
		return spimSource;
	}

	/**
	 * Get the {@link Converter} (converts source type T to ARGBType for
	 * display).
	 */
	public Converter< T, ARGBType > getConverter()
	{
		return converter;
	}

	public SourceAndConverter< ? extends Volatile< T > > asVolatile()
	{
		return volatileSourceAndConverter;
	}
}
