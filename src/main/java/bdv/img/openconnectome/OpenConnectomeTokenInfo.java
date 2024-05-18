/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
		final long[][] levelDimensions = new long[ dataset.resolutions.length ][ 3 ];

		final double[][] offsets = getOffsets( mode );

		if ( mode.equals( "neariso" ) )
		{
			for ( int i = 0; i < dataset.resolutions.length; ++i )
				levelDimensions[ i ] = dataset.neariso_imagesize.get( new Integer( dataset.resolutions[ i ] ).toString() ).clone();
		}
		else
		{
			for ( int i = 0; i < dataset.resolutions.length; ++i )
				levelDimensions[ i ] = dataset.imagesize.get( new Integer( dataset.resolutions[ i ] ).toString() ).clone();
		}

		for ( int i = 0; i < dataset.resolutions.length; ++i )
			levelDimensions[ i ][ 2 ] -= offsets[ i ][ 2 ];

		return levelDimensions;
	}

	public int[][] getLevelCellDimensions()
	{
		final int[][] levelCellDimensions = new int[ dataset.cube_dimension.size() ][];

		for ( int i = 0; i < dataset.resolutions.length; ++i )
			levelCellDimensions[ i ] = dataset.cube_dimension.get( new Integer( dataset.resolutions[ i ] ).toString() ).clone();
		return levelCellDimensions;
	}

	public double[][] getLevelScales( final String mode )
	{
		final double[][] levelScales = new double[ dataset.resolutions.length ][];
		final boolean neariso = mode.equals( "neariso" );
		for ( int i = 0; i < dataset.resolutions.length; ++i )
		{
			final long[] voxelres = neariso ? dataset.neariso_voxelres.get( new Integer( dataset.resolutions[ i ] ).toString() ) : dataset.voxelres.get( new Integer( dataset.resolutions[ i ] ).toString() );
			levelScales[ i ] = new double[]{
				voxelres[ 0 ],
				voxelres[ 1 ],
				voxelres[ 2 ] };
		}
		return levelScales;
	}

	public double[][] getOffsets( final String mode )
	{
		final double[][] offsets = new double[ dataset.resolutions.length ][ 3 ];
		if ( mode.equals( "neariso" ) )
			for ( int i = 0; i < dataset.resolutions.length; ++i )
				offsets[ i ] = dataset.neariso_offset.get( new Integer( dataset.resolutions[ i ] ).toString() ).clone();
		else
			for ( int i = 0; i < dataset.resolutions.length; ++i )
				offsets[ i ] = dataset.offset.get( new Integer( dataset.resolutions[ i ] ).toString() ).clone();
		return offsets;
	}

	public AffineTransform3D[] getLevelTransforms( final String mode )
	{

		final AffineTransform3D[] levelTransforms = new AffineTransform3D[ dataset.resolutions.length ];
		final double[][] levelScales = getLevelScales( mode );

		for ( int i = 0; i < dataset.resolutions.length; ++i )
		{
			final double[] levelScale = levelScales[ i ];
			final AffineTransform3D levelTransform = new AffineTransform3D();
			levelTransform.set( levelScale[ 0 ], 0, 0 );
			levelTransform.set( levelScale[ 1 ], 1, 1 );
			levelTransform.set( levelScale[ 2 ], 2, 2 );

			levelTransform.set( -0.5 * ( levelScale[ 0 ] - 1 ), 0, 3 );
			levelTransform.set( -0.5 * ( levelScale[ 1 ] - 1 ), 1, 3 );
			levelTransform.set( 0.5 * ( levelScale[ 1 ] - 1 ), 2, 3 );

			levelTransforms[ i ] = levelTransform;
		}

		return levelTransforms;
	}
}
