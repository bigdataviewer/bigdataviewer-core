package bdv.deform;

import static net.imglib2.algorithm.blocks.dfield.DisplacementFieldTransform.displacementFieldAffine;
import static net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension.zero;
import static net.imglib2.view.fluent.RandomAccessibleView.Interpolation.nLinear;

import bdv.ui.UIUtils;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.util.BdvStackSource;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.blocks.BlockAlgoUtils;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.dfield.DisplacementField;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Intervals;

public class GenField3D
{
	public static void main( String[] args )
	{
//		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
//		UIUtils.installFlatLafInfos();

		final String path = "/Users/pietzsch/workspace/data/flybrain-8bit.tif";
		final ImagePlus imp = IJ.openImage( path );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		System.out.println( "Intervals.toString(img) = " + Intervals.toString( img ) );

		final int n = 3;
		final int w = 256;
		final int h = 256;
		final int t = 57;
		double[] data = new double[ n * w * h * t ];
		int i = 0;
		for ( int z = 0; z < t; ++z )
		{
			for ( int y = 0; y < h; ++y )
			{
				for ( int x = 0; x < w; ++x )
				{
					double dx = Math.sin( 0.03 * x ) * 15.0;
					double dy = Math.sin( 0.03 * y ) * 15.0;
					double dz = Math.sin( 0.03 * z ) * 15.0;
					data[ i++ ] = dz;
					data[ i++ ] = dx;
					data[ i++ ] = dy;
				}
			}
		}
		final Img< DoubleType > dfieldArray = ArrayImgs.doubles( data, n, w, h, t );


		final AffineTransform3D transformFromSource = new AffineTransform3D();
		final DisplacementField< DoubleType > dfield = new DisplacementField<>(
				BlockSupplier.of( dfieldArray ),
				new double[] { 1, 1, 1 },
				new double[] { 0, 0, 0 } );
		final BlockSupplier< UnsignedByteType > blocks = BlockSupplier
				.of( img.view().extend(zero()) )
				.andThen( displacementFieldAffine( transformFromSource, dfield, Transform.Interpolation.NLINEAR ) )
				.tile( 8 );
		final Img< UnsignedByteType > tformedBlocks = BlockAlgoUtils.arrayImg( blocks, img );


		BdvSource bdv = BdvFunctions.show( img, "original image", Bdv.options() );
//		final BdvStackSource< DoubleType > dx = BdvFunctions.show( dfieldArray.view().slice( 0, 0 ), "dx", Bdv.options().addTo( bdv ) );
//		dx.setDisplayRange( -20, 20 );
//		final BdvStackSource< DoubleType > dy = BdvFunctions.show( dfieldArray.view().slice( 0, 1 ), "dy", Bdv.options().addTo( bdv ) );
//		dy.setDisplayRange( -20, 20 );
//		final BdvStackSource< DoubleType > dz = BdvFunctions.show( dfieldArray.view().slice( 0, 2 ), "dz", Bdv.options().addTo( bdv ) );
//		dz.setDisplayRange( -20, 20 );

		net.imglib2.realtransform.DisplacementFieldTransform tform = new net.imglib2.realtransform.DisplacementFieldTransform( dfieldArray, 1, 1, 1 );
		final RealRandomAccessible< UnsignedByteType > extip = img.view().extend(zero()).interpolate( nLinear() );
		final RandomAccessibleInterval< UnsignedByteType > tformedImg = new RealTransformRandomAccessible<>( extip, tform ).view().interval( img );
		BdvFunctions.show( tformedImg, "transformed image", Bdv.options().addTo( bdv ) );

//		final RandomAccessibleInterval< UnsignedByteType > tformedImgMat = ArrayImgs.unsignedBytes( img.dimensionsAsLongArray() );
//		LoopBuilder.setImages( tformedImg, tformedImgMat ).forEachPixel( ( in, out ) -> out.set( in ) );
//		BdvFunctions.show( tformedImgMat, "transformed image (materialized)", Bdv.options().addTo( bdv ) );

//		final Img< UnsignedByteType > tformedBlocks = blocks.toCellImg( img.dimensionsAsLongArray(), 8, 8, 8 );
		BdvFunctions.show( tformedBlocks, "transformed image (displacementFieldAffine)", Bdv.options().addTo( bdv ) );
	}
}
