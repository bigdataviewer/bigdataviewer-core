/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.type.mask.Masked;

/**
 * A dummy {@link Source} that represents a {@link BdvOverlay}.
 * <p>
 * When a {@code BdvOverlay} is shown (with
 * {@link BdvFunctions#showOverlay(BdvOverlay, String, BdvOptions)}), a dummy
 * {@code Source} and {@code ConverterSetup} are added to the
 * {@code BigDataViewer} such that the visibility, display range, and color for
 * the overlay can be adjusted by the user, like for a normal {@link Source}.
 * <p>
 * {@code PlaceHolderSource} is not {@link PlaceHolderSource#isPresent(int)
 * present} at any time point, so it is never actually visible.
 *
 * @author Tobias Pietzsch
 */
public final class PlaceHolderSource implements Source< Void >
{
	private final String name;

	public PlaceHolderSource( final String name )
	{
		this.name = name;
	}

	@Override
	public Void getType()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public VoxelDimensions getVoxelDimensions()
	{
		return null;
	}

	@Override
	public int getNumMipmapLevels()
	{
		return 1;
	}

	@Override
	public boolean isPresent( final int t )
	{
		return false;
	}

	@Override
	public RandomAccessibleInterval< Void > getSource( final int t, final int level )
	{
		return null;
	}

	@Override
	public RealRandomAccessible< Void > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return null;
	}

	@Override
	public RandomAccessibleInterval< ? extends Masked< Void > > getMaskedSource( final int t, final int level )
	{
		return null;
	}

	@Override
	public RealRandomAccessible< ? extends Masked< Void > > getInterpolatedMaskedSource( final int t, final int level, final Interpolation method )
	{
		return null;
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		transform.identity();
	}
}
