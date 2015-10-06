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
package bdv.img.openconnectome;

import java.io.Serializable;

import net.imglib2.realtransform.AffineTransform3D;

public class OpenConnectomeTokenInfo implements Serializable
{
	private static final long serialVersionUID = -560051267067033900L;

	public OpenConnectomeDataset dataset;
	public OpenConnectomeProject project;
	
	public long[][] getLevelDimensions( final String mode )
	{
		final long[][] levelDimensions = new long[ dataset.imagesize.size() ][ 3 ];

		if ( mode.equals( "neariso" ) )
		{
			final double zScale0 = dataset.voxelres.get( "0" )[ 2 ];
			for ( int i = 0; i < dataset.imagesize.size(); ++i )
			{
				final long[] xyz = dataset.imagesize.get( new Integer( i ).toString() );
				final double[] voxelres = dataset.voxelres.get( new Integer( i ).toString() );
				levelDimensions[ i ][ 0 ] = xyz[ 0 ];
				levelDimensions[ i ][ 1 ] = xyz[ 1 ];
//				levelDimensions[ i ][ 2 ] = ( long )( xyz[ 2 ] * zScale0 / voxelres[ 2 ] );
				levelDimensions[ i ][ 2 ] = ( long ) ( xyz[ 2 ] * zScale0 / dataset.neariso_scaledown.get( new Integer( i ).toString() ) / voxelres[ 2 ] );
			}
		}
		else
		{
			for ( int i = 0; i < dataset.imagesize.size(); ++i )
			{
				final long[] xyz = dataset.imagesize.get( new Integer( i ).toString() );
				levelDimensions[ i ] = xyz.clone();
			}
		}

		return levelDimensions;
	}
	
	public int[][] getLevelCellDimensions()
	{
		final int[][] levelCellDimensions = new int[ dataset.cube_dimension.size() ][];
		
		for ( int i = 0; i < dataset.cube_dimension.size(); ++i )
			levelCellDimensions[ i ] = dataset.cube_dimension.get( new Integer( i ).toString() ).clone();
		return levelCellDimensions;
	}
	
	public double[][] getLevelScales( final String mode )
	{
		final double[][] levelScales = new double[ dataset.resolutions.length ][ 3 ];
		long s = 1;
		final double zScale0 = dataset.voxelres.get( "0" )[ 2 ];
		if ( mode.equals( "neariso" ) )
		{
			for ( int i = 0; i < dataset.neariso_scaledown.size(); ++i, s <<= 1 )
			{
				levelScales[ i ][ 0 ] = s;
				levelScales[ i ][ 1 ] = s;
				levelScales[ i ][ 2 ] = zScale0 * dataset.neariso_scaledown.get( new Integer( i ).toString() );
			}
		}
		else
		{
			for ( int i = 0; i < dataset.resolutions.length; ++i, s <<= 1 )
			{
				levelScales[ i ][ 0 ] = s;
				levelScales[ i ][ 1 ] = s;
				levelScales[ i ][ 2 ] = zScale0 * s;
			}
		}

		return levelScales;
	}
	
	public long getMinZ()
	{
		return dataset.offset.get( "0" )[ 2 ];
	}
	
	public AffineTransform3D[] getLevelTransforms( final String mode )
	{
		final int n = dataset.cube_dimension.size();
		final AffineTransform3D[] levelTransforms = new AffineTransform3D[ n ];
		final boolean neariso = mode.equals( "neariso" );
		final double zScale0 = dataset.voxelres.get( "0" )[ 2 ];

		long s = 1;
		for ( int i = 0; i < n; ++i, s <<= 1 )
		{
			final AffineTransform3D levelTransform = new AffineTransform3D();
			levelTransform.set( s, 0, 0 );
			levelTransform.set( s, 1, 1 );

			levelTransform.set( -0.5 * ( s - 1 ), 0, 3 );
			levelTransform.set( -0.5 * ( s - 1 ), 1, 3 );

			if ( neariso )
			{
				final double zScale = zScale0 * dataset.neariso_scaledown.get( new Integer( i ).toString() );
				levelTransform.set( zScale, 2, 2 );
				levelTransform.set( 0.5 * ( zScale - 1 ), 2, 3 );
			}
			levelTransforms[ i ] = levelTransform;
		}

		return levelTransforms;
	}
}
