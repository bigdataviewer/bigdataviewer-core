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
package bdv.viewer.render;

import bdv.viewer.SourceAndConverter;

/**
 * A {@code SourceAndConverter} and its 2D bounding box.
 */
class SourceBounds
{
	private final SourceAndConverter< ? > source;

	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;

	public SourceBounds( final SourceAndConverter< ? > source, final int minX, final int minY, final int maxX, final int maxY )
	{
		this.source = source;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public SourceAndConverter< ? > source()
	{
		return source;
	}

	public int minX()
	{
		return minX;
	}

	public int minY()
	{
		return minY;
	}

	public int maxX()
	{
		return maxX;
	}

	public int maxY()
	{
		return maxY;
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder( "SourceBounds{" );
		sb.append( "\"" ).append( source.getSpimSource().getName() ).append( "\" " );
		sb.append( "(" ).append( minX );
		sb.append( ", " ).append( minY );
		sb.append( ") -- (" ).append( maxX );
		sb.append( ", " ).append( maxY );
		sb.append( ")}" );
		return sb.toString();
	}
}
