package bdv.img.remote;

import java.util.ArrayList;

import bdv.img.hdf5.Hdf5ImageLoader;

public class RemoteImageLoaderMetaData
{
	protected final ArrayList< double[][] > perSetupMipmapResolutions;

	protected final ArrayList< int[][] > perSetupSubdivisions;

	protected int numTimepoints;

	protected int numSetups;

	protected int[] maxLevels;

	protected int maxNumLevels;

	/**
	 * An array of long[] arrays with {@link #numTimepoints} *
	 * {@link #numSetups} * {@link #maxNumLevels} entries. Every entry is the
	 * dimensions of one image (identified by flattened index of level, setup,
	 * and timepoint).
	 */
	protected long[][] dimensions;

	/**
	 * An array of Booleans with {@link #numTimepoints} * {@link #numSetups} *
	 * {@link #maxNumLevels} entries. Every entry is the existence of one image
	 * (identified by flattened index of level, setup, and timepoint).
	 */
	protected boolean[] existence;

	public RemoteImageLoaderMetaData()
	{
		perSetupMipmapResolutions = new ArrayList< double[][] >();
		perSetupSubdivisions = new ArrayList< int[][] >();
	}

	public RemoteImageLoaderMetaData( final Hdf5ImageLoader imgLoader, final int numTimepoints, final int numSetups )
	{
		perSetupMipmapResolutions = new ArrayList< double[][] >();
		perSetupSubdivisions = new ArrayList< int[][] >();
		this.numTimepoints = numTimepoints;
		this.numSetups = numSetups;
		maxLevels = new int[ numSetups ];
		maxNumLevels = 0;
		for ( int setup = 0; setup < numSetups; ++setup )
		{
			perSetupMipmapResolutions.add( imgLoader.getMipmapResolutions( setup ) );
			perSetupSubdivisions.add( imgLoader.getSubdivisions( setup ) );
			final int numLevels = imgLoader.numMipmapLevels( setup );
			maxLevels[ setup ] = numLevels - 1;
			if ( numLevels > maxNumLevels )
				maxNumLevels = numLevels;
		}
		final int numImages = numTimepoints * numSetups * maxNumLevels;
		dimensions = new long[ numImages ][];
		existence = new boolean[ numImages ];

		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				for ( int l = 0; l <= maxLevels[ s ]; ++l )
				{
					final int i = l + maxNumLevels * ( s + numSetups * t );
					existence[ i ] = imgLoader.existsImageData( t, s, l );
					dimensions[ i ] = imgLoader.getImageDimension( t, s, l );
				}
	}

	/**
	 * Create an array of int[] with {@link #numTimepoints} * {@link #numSetups}
	 * * {@link #maxNumLevels} entries. Every entry is the dimensions in cells
	 * (instead of pixels) of one image (identified by flattened index of level,
	 * setup, and timepoint).
	 */
	protected int[][] createCellsDimensions()
	{
		final int numImages = numTimepoints * numSetups * maxNumLevels;
		final int[][] cellsDimensions = new int[ numImages ][];
		for ( int t = 0; t < numTimepoints; ++t )
			for ( int s = 0; s < numSetups; ++s )
				for ( int l = 0; l <= maxLevels[ s ]; ++l )
				{
					final int i = l + maxNumLevels * ( s + numSetups * t );
					final long[] imageDimensions = dimensions[ i ];
					final int[] cellSize = perSetupSubdivisions.get( s )[ l ];
					cellsDimensions[ i ] = new int[] {
							( int ) imageDimensions[ 0 ] / cellSize[ 0 ],
							( int ) imageDimensions[ 1 ] / cellSize[ 1 ],
							( int ) imageDimensions[ 2 ] / cellSize[ 2 ] };
				}
		return cellsDimensions;
	}
}
