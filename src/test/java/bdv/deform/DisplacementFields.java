package bdv.deform;

import static net.imglib2.view.fluent.RandomAccessibleIntervalView.Extension.zero;
import static net.imglib2.view.fluent.RandomAccessibleView.Interpolation.nLinear;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import bdv.ui.UIUtils;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.blocks.BlockAlgoUtils;
import net.imglib2.algorithm.blocks.BlockSupplier;
import net.imglib2.algorithm.blocks.ClampType;
import net.imglib2.algorithm.blocks.ComputationType;
import net.imglib2.algorithm.blocks.UnaryBlockOperator;
import net.imglib2.algorithm.blocks.dfield.AbstractLookupProcessor;
import net.imglib2.algorithm.blocks.dfield.AbstractDispFieldAffineProcessor;
import net.imglib2.algorithm.blocks.dfield.DispFieldAffine2DProcessor;
import net.imglib2.algorithm.blocks.dfield.DisplacementField;
import net.imglib2.algorithm.blocks.dfield.DisplacementFieldTransform;
import net.imglib2.algorithm.blocks.dfield.DisplacementFieldUnaryBlockOperator;
import net.imglib2.algorithm.blocks.dfield.Lookup2DProcessor;
import net.imglib2.algorithm.blocks.transform.Transform;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.type.PrimitiveType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

public class DisplacementFields
{

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		UIUtils.installFlatLafInfos();


		final String path = "/Users/pietzsch/Desktop/data/Janelia/example-displacement-field/invDisplacements.zarr";

		N5Reader n5 = new N5Factory().openReader( path );
		CachedCellImg< DoubleType, ? > dfieldArray = N5Utils.open( n5, "displacementField" );
		CachedCellImg< UnsignedByteType, ? > img = N5Utils.open( n5, "0" );

		System.out.println( "dfieldArray.getCellGrid() = " + dfieldArray.getCellGrid() );

		net.imglib2.realtransform.DisplacementFieldTransform tform = new net.imglib2.realtransform.DisplacementFieldTransform( dfieldArray, 1, 1 );

		final RealRandomAccessible< UnsignedByteType > extip = img.view().extend(zero()).interpolate( nLinear() );
		final RandomAccessibleInterval< UnsignedByteType > tformedImg = new RealTransformRandomAccessible<>( extip, tform ).view().interval( img );

		BdvSource bdv = BdvFunctions.show( img, "original image", Bdv.options().is2D() );
		BdvFunctions.show( tformedImg, "transformed image", Bdv.options().addTo( bdv ) );

//		BdvSource bdv = BdvFunctions.show( tformedImg, "transformed image", Bdv.options().is2D() );

		System.out.println( "Intervals.toString( img ) = " + Intervals.toString( img ) );
		System.out.println( "Intervals.toString( dfieldArray ) = " + Intervals.toString( dfieldArray ) );
		System.out.println( "Intervals.toString( dfieldArray.view().moveAxis( 0, 2 ) ) = " + Intervals.toString( dfieldArray.view().moveAxis( 0, 2 ) ) );

		{
			final AffineTransform2D transformToSource = new AffineTransform2D();
			final AbstractDispFieldAffineProcessor fieldProcessor = new DispFieldAffine2DProcessor<>(
					transformToSource,
					new double[] { 1, 1 },
					Transform.Interpolation.NLINEAR,
					PrimitiveType.DOUBLE );
			final BlockSupplier< DoubleType > displacementField = BlockSupplier.of( dfieldArray );
//			final AbstractLookupProcessor lookupProcessor = new Lookup2DProcessor(
//					PrimitiveType.DOUBLE,
//					Transform.Interpolation.NEARESTNEIGHBOR,
//					PrimitiveType.BYTE );
//			final UnaryBlockOperator< UnsignedByteType, UnsignedByteType > operator =
//					new DisplacementFieldUnaryBlockOperator<>(
//							new UnsignedByteType(),
//							2,
//							fieldProcessor,
//							displacementField,
//							lookupProcessor );
			final AbstractLookupProcessor lookupProcessor = new Lookup2DProcessor(
					PrimitiveType.DOUBLE,
					Transform.Interpolation.NLINEAR,
					PrimitiveType.FLOAT );
			final UnaryBlockOperator< UnsignedByteType, UnsignedByteType > operator =
					new DisplacementFieldUnaryBlockOperator<>(
							new FloatType(),
							2,
							fieldProcessor,
							displacementField,
							lookupProcessor ).adaptSourceType( new UnsignedByteType(), ClampType.NONE ).adaptTargetType( new UnsignedByteType(), ClampType.CLAMP );;
			final BlockSupplier< UnsignedByteType > blocks = BlockSupplier.of( img.view().extend(zero()) ).andThen( operator );
			final Img< UnsignedByteType > tformedBlocks = BlockAlgoUtils.arrayImg( blocks, img );
			BdvFunctions.show( tformedBlocks, "transformed image (blocks)", Bdv.options().addTo( bdv ) );
		}


		{
			final AffineTransform2D transformToSource = new AffineTransform2D();
			final DisplacementField< DoubleType > dfield = new DisplacementField<>(
					BlockSupplier.of( dfieldArray ),
					new double[] { 1, 1 },
					new double[] { 0, 0 } );

			final UnaryBlockOperator< UnsignedByteType, UnsignedByteType > operator = DisplacementFieldTransform.createDisplacementFieldOperator(
					new UnsignedByteType(),
					transformToSource,
					dfield,
					Transform.Interpolation.NLINEAR,
					ComputationType.AUTO,
					ClampType.CLAMP );

			final BlockSupplier< UnsignedByteType > blocks = BlockSupplier
					.of( img.view().extend(zero()) )
					.andThen( operator );
			final Img< UnsignedByteType > tformedBlocks = BlockAlgoUtils.arrayImg( blocks, img );
			BdvFunctions.show( tformedBlocks, "transformed image (createDisplacementFieldOperator)", Bdv.options().addTo( bdv ) );
		}



//		final BdvStackSource< DoubleType > sources = BdvFunctions.show( dfieldArray.view().moveAxis( 0, 2 ), "displacement field", Bdv.options().is2D().axisOrder( AxisOrder.XYC ) );


//		final ArrayImg< DoubleType, ? > aRender = new ArrayImgFactory<>( new DoubleType() ).create( dfieldArray );
//		LoopBuilder.setImages( dfieldArray, aRender ).forEachPixel( ( i, o ) -> o.set( i ) );
//		final BdvStackSource< DoubleType > source = BdvFunctions.show( aRender.view().moveAxis( 0, 2 ), "displacement field (LoopBuilder)", Bdv.options().is2D().axisOrder( AxisOrder.XYC ) );
//		source.setDisplayRange( 0, 100 );
//		source.setDisplayRangeBounds( 0, 100 );


//		{
//			final Interval interval = Intervals.createMinSize( 0, -2, -2, 2, 722, 578 );
//			final double[] data = new double[ Math.toIntExact( Intervals.numElements( interval ) ) ];
//			final ArrayImg< DoubleType, ? > bRender = ArrayImgs.doubles( data, interval.dimensionsAsLongArray() );
//			final BlockSupplier< DoubleType > blocks = BlockSupplier.of( dfieldArray );
//			blocks.copy( interval, data );
//
//			final BdvStackSource< DoubleType > source2 =
//					BdvFunctions.show( bRender.view().moveAxis( 0, 2 ), "displacement field (block copy)", Bdv.options().is2D().axisOrder( AxisOrder.XYC ).addTo( source ) );
//			source2.setDisplayRange( 0, 100 );
//			source2.setDisplayRangeBounds( 0, 100 );
//		}

/*
		{
			final double[] data = new double[ Math.toIntExact( Intervals.numElements( dfieldArray ) ) ];
			final ArrayImg< DoubleType, ? > bRender = ArrayImgs.doubles( data, dfieldArray.dimensionsAsLongArray() );

			final AffineTransform2D transformFromSource = new AffineTransform2D();
//			final BlockSupplier< DoubleType > blocks = BlockSupplier.of( dfieldArray )
//					.andThen( DisplacementFieldTransform.affine( transformFromSource ) );
//			blocks.copy( new FinalInterval( 720, 576 ), data );

			final UnaryBlockOperator< DoubleType, DoubleType > operator =
					DisplacementFieldTransform.createAffineOperator( new DoubleType(), transformFromSource );
			final BlockSupplier< DoubleType > blocks = BlockSupplier.of( dfieldArray );
			operator.compute( blocks, new FinalInterval( 720, 576 ), data );
			final Affine2DProcessor< ? > processor = ( Affine2DProcessor< ? > ) ( ( DefaultUnaryBlockOperator< DoubleType, DoubleType > ) operator ).blockProcessor();
			System.out.println( "processor.getSourceInterval() = " + Intervals.toString( processor.getSourceInterval() ) );

			final BdvStackSource< DoubleType > source2 =
					BdvFunctions.show( bRender.view().moveAxis( 0, 2 ), "displacement field (BlockSupplier)", Bdv.options().is2D().axisOrder( AxisOrder.XYC ).addTo( source ) );
			source2.setDisplayRange( 0, 100 );
			source2.setDisplayRangeBounds( 0, 100 );
		}
*/
	}
}
