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
package bdv.tools.boundingbox;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Sampler;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * A dummy {@link Source} that represents a {@link TransformedBox}.
 * The transform and interval of the {@code Source} is that of the {@code Box}
 * to make it appear correctly in the BDV overview.
 *
 * @author Tobias Pietzsch
 */
final class TransformedBoxPlaceHolderSource implements Source< Void >
{
	private final UnsignedShortType type = new UnsignedShortType();

	private final String name;

	private final TransformedBox bbSource;

	public TransformedBoxPlaceHolderSource( final String name, final TransformedBox bbSource )
	{
		this.name = name;
		this.bbSource = bbSource;
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
		return true;
	}

	@Override
	public RandomAccessibleInterval< Void > getSource( final int t, final int level )
	{
		return Views.interval( Views.raster( rra ), Intervals.smallestContainingInterval( bbSource.getInterval() ) );
	}

	@Override
	public RealRandomAccessible< Void > getInterpolatedSource( final int t, final int level, final Interpolation method )
	{
		return rra;
	}

	@Override
	public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
	{
		bbSource.getTransform( transform );
	}

	private final RealRandomAccessible< Void > rra = new RealRandomAccessible< Void >()
	{
		@Override
		public int numDimensions()
		{
			return 3;
		}

		@Override
		public RealRandomAccess< Void > realRandomAccess()
		{
			return access;
		}

		@Override
		public RealRandomAccess< Void > realRandomAccess( final RealInterval interval )
		{
			return access;
		}
	};

	private final RealRandomAccess< Void > access = new RealRandomAccess< Void >()
	{
		@Override
		public RealRandomAccess< Void > copyRealRandomAccess()
		{
			return this;
		}

		@Override
		public void localize( final float[] position )
		{}

		@Override
		public void move( final float distance, final int d )
		{}

		@Override
		public void move( final double distance, final int d )
		{}

		@Override
		public void move( final RealLocalizable distance )
		{}

		@Override
		public void move( final float[] distance )
		{}

		@Override
		public void move( final double[] distance )
		{}

		@Override
		public void setPosition( final RealLocalizable position )
		{}

		@Override
		public void setPosition( final float[] position )
		{}

		@Override
		public void setPosition( final double[] position )
		{}

		@Override
		public void setPosition( final float position, final int d )
		{}

		@Override
		public void setPosition( final double position, final int d )
		{}

		@Override
		public void fwd( final int d )
		{}

		@Override
		public void bck( final int d )
		{}

		@Override
		public void move( final int distance, final int d )
		{}

		@Override
		public void move( final long distance, final int d )
		{}

		@Override
		public void move( final Localizable localizable )
		{}

		@Override
		public void move( final int[] distance )
		{}

		@Override
		public void move( final long[] distance )
		{}

		@Override
		public void setPosition( final Localizable localizable )
		{}

		@Override
		public void setPosition( final int[] position )
		{}

		@Override
		public void setPosition( final long[] position )
		{}

		@Override
		public void setPosition( final int position, final int d )
		{}

		@Override
		public void setPosition( final long position, final int d )
		{}

		@Override
		public void localize( final double[] position )
		{}

		@Override
		public float getFloatPosition( final int d )
		{
			return 0;
		}

		@Override
		public double getDoublePosition( final int d )
		{
			return 0;
		}

		@Override
		public int numDimensions()
		{
			return 3;
		}

		@Override
		public Void get()
		{
			return null;
		}

		@Override
		public Sampler< Void > copy()
		{
			return this;
		}
	};
}
