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
package bdv.img.cache;

import net.imglib2.AbstractLocalizable;
import net.imglib2.Cursor;
import net.imglib2.FlatIterationOrder;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.img.AbstractImg;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.AbstractCells;
import net.imglib2.util.Fraction;
import net.imglib2.util.IntervalIndexer;

public class VolatileImgCells< A extends VolatileAccess > extends AbstractCells< A, VolatileCell< A >, VolatileImgCells< A >.CachedCells >
{
	public static interface CellCache< A extends VolatileAccess >
	{
		/**
		 * Get the cell at a specified index.
		 *
		 * @return cell at index or null if the cell is not in the cache.
		 */
		public VolatileCell< A > get( final long index );

		/**
		 * Load a cell into memory (eventually) and put it into the cache at the
		 * specified index. Depending on the implementation, loading may be
		 * asynchronous, so the {@link VolatileAccess} of the returned cell may
		 * be invalid for a while.
		 *
		 * @param index
		 *            cell is stored at this index in the cache.
		 * @param cellDims
		 *            dimensions of the cell.
		 * @param cellMin
		 *            offset of the cell in image coordinates.
		 * @return cell at index
		 */
		public VolatileCell< A > load( final long index, final int[] cellDims, final long[] cellMin );

		/**
		 * Set {@link CacheHints hints} on how to handle cell requests for this
		 * cache.
		 *
		 * @param cacheHints
		 *            describe handling of cell requests for this cache.
		 */
		public void setCacheHints( CacheHints cacheHints );
	}

	protected final CachedCells cells;

	protected final CellCache< A > cache;

	public VolatileImgCells( final CellCache< A > cache, final Fraction entitiesPerPixel, final long[] dimensions, final int[] cellDimensions )
	{
		super( entitiesPerPixel, dimensions, cellDimensions );
		this.cache = cache;
		cells = new CachedCells( numCells );
	}

	@Override
	protected CachedCells cells()
	{
		return cells;
	}

	public class CachedCells extends AbstractImg< VolatileCell< A > >
	{
		public class CachedCellsRandomAccess extends AbstractLocalizable implements RandomAccess< VolatileCell< A > >
		{
			private long i;

			public CachedCellsRandomAccess()
			{
				super( CachedCells.this.numDimensions() );
			}

			public CachedCellsRandomAccess( final CachedCellsRandomAccess randomAccess )
			{
				super( randomAccess.numDimensions() );

				for ( int d = 0; d < n; ++d )
					position[ d ] = randomAccess.position[ d ];

				i = randomAccess.i;
			}

			@Override
			public VolatileCell< A > get()
			{
				return CachedCells.this.get( i );
			}

			public void set( final VolatileCell< A > t )
			{
				CachedCells.this.set( i, t );
			}

			@Override
			public void fwd( final int d )
			{
				i += step[ d ];
				++position[ d ];
			}

			@Override
			public void bck( final int d )
			{
				i -= step[ d ];
				--position[ d ];
			}

			@Override
			public void move( final int distance, final int d )
			{
				i += step[ d ] * distance;
				position[ d ] += distance;
			}

			@Override
			public void move( final long distance, final int d )
			{
				move( distance, d );
			}

			@Override
			public void move( final Localizable localizable )
			{
				for ( int d = 0; d < n; ++d )
					move( localizable.getLongPosition( d ), d );
			}

			@Override
			public void move( final int[] distance )
			{
				for ( int d = 0; d < n; ++d )
					move( distance[ d ], d );
			}

			@Override
			public void move( final long[] distance )
			{
				for ( int d = 0; d < n; ++d )
					move( distance[ d ], d );
			}

			@Override
			public void setPosition( final Localizable localizable )
			{
				localizable.localize( position );
				i = position[ 0 ];
				for ( int d = 1; d < n; ++d )
					i += position[ d ] * step[ d ];
			}

			@Override
			public void setPosition( final int[] position )
			{
				i = position[ 0 ];
				this.position[ 0 ] = i;
				for ( int d = 1; d < n; ++d )
				{
					final long p = position[ d ];
					i += p * step[ d ];
					this.position[ d ] = p;
				}
			}

			@Override
			public void setPosition( final long[] position )
			{
				i = position[ 0 ];
				this.position[ 0 ] = i;
				for ( int d = 1; d < n; ++d )
				{
					final long p = position[ d ];
					i += p * step[ d ];
					this.position[ d ] = p;
				}
			}

			@Override
			public void setPosition( final int position, final int d )
			{
				i += step[ d ] * ( position - this.position[ d ] );
				this.position[ d ] = position;
			}

			@Override
			public void setPosition( final long position, final int d )
			{
				i += step[ d ] * ( position - this.position[ d ] );
				this.position[ d ] = position;
			}

			@Override
			public CachedCellsRandomAccess copy()
			{
				return new CachedCellsRandomAccess( this );
			}

			@Override
			public CachedCellsRandomAccess copyRandomAccess()
			{
				return copy();
			}
		}

		final protected long[] step;

		protected CachedCells( final long[] dim )
		{
			super( dim );

			step = new long[ n ];
			IntervalIndexer.createAllocationSteps( dimension, step );
		}

		protected VolatileCell< A > get( final long index )
		{
			final VolatileCell< A > cell = cache.get( index );
			if ( cell != null )
				return cell;
			final long[] cellGridPosition = new long[ n ];
			final long[] cellMin = new long[ n ];
			final int[] cellDims  = new int[ n ];
			IntervalIndexer.indexToPosition( index, dimension, cellGridPosition );
			getCellDimensions( cellGridPosition, cellMin, cellDims );
			return cache.load( index, cellDims, cellMin );
		}

		@Override
		public CachedCellsRandomAccess randomAccess()
		{
			return new CachedCellsRandomAccess();
		}

		@Override
		public FlatIterationOrder iterationOrder()
		{
			return new FlatIterationOrder( this );
		}

		@Override
		public CachedCells copy()
		{
			return new CachedCells( dimensions );
		}

		protected void set( final long index, final VolatileCell< A > value )
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		public ImgFactory< VolatileCell< A > > factory()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		public Cursor< VolatileCell< A > > cursor()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}

		@Override
		public Cursor< VolatileCell< A > > localizingCursor()
		{
			throw new UnsupportedOperationException( "Not supported" );
		}
	}
}
