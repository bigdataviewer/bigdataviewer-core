/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;

public class VolatileSpimSource< V extends Volatile< ? > & NumericType< V > >
		extends AbstractSpimSource< V >
		implements MipmapOrdering
{
	protected final ViewerSetupImgLoader< ?, V > imgLoader;

	protected final MipmapOrdering mipmapOrdering;

	@SuppressWarnings( "unchecked" )
	public VolatileSpimSource( final AbstractSpimData< ? > spimData, final int setup, final String name )
	{
		super( spimData, setup, name );
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		imgLoader = ( ViewerSetupImgLoader< ?, V > ) ( ( ViewerImgLoader ) seq.getImgLoader() ).getSetupImgLoader( setup );
		if ( MipmapOrdering.class.isInstance( imgLoader ) )
			mipmapOrdering = ( ( MipmapOrdering ) imgLoader );
		else
			mipmapOrdering = new DefaultMipmapOrdering( this );
		loadTimepoint( 0 );
	}

	@Override
	public V getType()
	{
		return imgLoader.getVolatileImageType();
	}

	@Override
	protected RandomAccessibleInterval< V > getImage( final int timepointId, final int level )
	{
		return imgLoader.getVolatileImage( timepointId, level );
	}

	@Override
	protected AffineTransform3D[] getMipmapTransforms()
	{
		return imgLoader.getMipmapTransforms();
	}

	@Override
	public MipmapHints getMipmapHints( final AffineTransform3D screenTransform, final int timepoint, final int previousTimepoint )
	{
		return mipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
	}
}
