package bdv.deform;

import static net.imglib2.algorithm.blocks.dfield.DisplacementFieldTransform.displacementFieldAffine;
import static net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension.border;
import static net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension.zero;

import bdv.ui.UIUtils;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.algorithm.blocks.BlockAlgoUtils;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.dfield.DisplacementField;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;

public class DisplacementFieldTranslation
{
	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		UIUtils.installFlatLafInfos();

		final String path = "/Users/pietzsch/Desktop/data/Janelia/example-displacement-field/boats.tif";
		final ImagePlus imp = IJ.openImage( path );
		final Img< UnsignedByteType > img = ImageJFunctions.wrapByte( imp );

		BdvSource bdv = BdvFunctions.show( img, "original image", Bdv.options().is2D() );

		final double[][] dfieldDatas = {
//				{
//						-.5, -.5,
//						0, 0,
//						0, 0,
//						0, 0,
//				},
//				{
//						0, 0,
//						.5, -.5,
//						0, 0,
//						0, 0,
//				},
//				{
//						0, 0,
//						0, 0,
//						-.5, .5,
//						0, 0,
//				},
//				{
//						0, 0,
//						0, 0,
//						0, 0,
//						.5, .5
//				},
				{
						-.5, -.5,
						.5, -.5,
						-.5, .5,
						.5, .5
				},
		};

		for ( int i = 0; i < dfieldDatas.length; i++ )
		{
			final AffineTransform2D transformFromSource = new AffineTransform2D();
			transformFromSource.scale( 16 );
			transformFromSource.translate( 7.5, 7.5 );

			final Img< DoubleType > dfieldArray = ArrayImgs.doubles( dfieldDatas[ i ], 2, 2, 2 );
			final DisplacementField< DoubleType > dfield = new DisplacementField<>(
					BlockSupplier.of( dfieldArray ),
					new double[] { 16, 16 },
					new double[] { 7.5, 7.5 } );
			final BlockSupplier< UnsignedByteType > blocks = BlockSupplier
					.of( img.view().extend( border() ) )
					.andThen( displacementFieldAffine( transformFromSource, dfield, Transform.Interpolation.NLINEAR ) );
//			final Img< UnsignedByteType > tformedBlocks = blocks.toCellImg( img.dimensionsAsLongArray(), 8, 8 );
			final Img< UnsignedByteType > tformedBlocks = BlockAlgoUtils.arrayImg( blocks, img );
			BdvFunctions.show( tformedBlocks, "transformed (" + i + ")", Bdv.options().addTo( bdv ) );
		}
	}
}
