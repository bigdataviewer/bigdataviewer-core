package bdv.deform;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import bdv.ui.UIUtils;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvSource;
import bdv.viewer.Interpolation;
import bdv.viewer.MaskUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.realtransform.DisplacementFieldTransform;
import net.imglib2.realtransform.RealTransformRealRandomAccessible;
import net.imglib2.type.mask.Masked;
import net.imglib2.type.mask.MaskedType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Cast;

public class DisplacementFieldPlayground
{

	public static void main( String[] args )
	{
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		System.setProperty( "apple.awt.application.appearance", "system" );
		UIUtils.installFlatLafInfos();


		final String path = "/Users/pietzsch/Desktop/data/Janelia/example-displacement-field/invDisplacements.zarr";

		N5Reader n5 = new N5Factory().openReader( path );
		CachedCellImg< DoubleType, ? > dfieldArray = N5Utils.open( n5, "displacementField" );
		CachedCellImg< UnsignedByteType, ? > img = N5Utils.open( n5, "0" );

		DisplacementFieldTransform tform = new DisplacementFieldTransform( dfieldArray, 1, 1 );

		// TODO: Masked interpolation / extension can not trivially be passed into masked.view().extend(zero())
		//       Is there a way around that? For example, could we extend
		//       RandomAccessibleIntervalView to have something that explicitly
		//       deals with masks?
		// TODO: make MaskedType.withConstant(...) public?
		//       We need MaskedType (something that extends Type) to be able to
		//       interpolate and extend.

		final RandomAccessibleInterval< MaskedType< UnsignedByteType > > masked = Cast.unchecked( Masked.withConstant( img, 1 ) );
		final RealRandomAccessible< MaskedType< UnsignedByteType > > extip;
		extip = MaskUtils.extendAndInterpolateMasked( masked, Interpolation.NLINEAR );
		// If we want to do this explicitly, inline:
		//		final MaskedType< UnsignedByteType > zero = masked.getType().createVariable();
		//		zero.setMask( 0 );
		//		zero.value().setZero();
		//		extip = Views.interpolate(
		//				Views.extendValue( masked, zero ),
		//				new MaskedClampingNLinearInterpolatorFactory<>() );


		final RealRandomAccessible< MaskedType< UnsignedByteType > > tformedImgReal =
				new RealTransformRealRandomAccessible<>( extip, tform );
		final RandomAccessibleInterval< MaskedType< UnsignedByteType > > tformedImg =
				tformedImgReal.realView().raster().interval( img );

		// TODO: consider adding convenience fluent-view like class. Something like
		//       MaskedView.of(img).extendZero().interpolate(nLinear()).invRealTransform().raster().interval()


		BdvSource bdv = BdvFunctions.show( img, "original image", Bdv.options().is2D() );
		BdvFunctions.showMasked( tformedImg, "transformed image", Bdv.options().addTo( bdv ) );
	}
}
