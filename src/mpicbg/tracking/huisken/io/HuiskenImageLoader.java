package mpicbg.tracking.huisken.io;

import ij.ImagePlus;

import java.io.File;

import mpicbg.tracking.data.View;
import mpicbg.tracking.data.ViewSetup;
import mpicbg.tracking.data.io.ImgLoader;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import spimopener.SPIMExperiment;

public class HuiskenImageLoader implements ImgLoader
{
	private File expFile;

	private SPIMExperiment exp;

	private boolean hasAlternatingIllumination;

	public HuiskenImageLoader()
	{
		expFile = null;
		exp = null;
	}

	@Override
	public void init( final Element elem )
	{
		final String path = elem.getElementsByTagName( "path" ).item( 0 ).getTextContent();
		expFile = new File( path );
		exp = null;
	}

	public static HuiskenImageLoader fromXml( final Element elem )
	{
		final HuiskenImageLoader loader = new HuiskenImageLoader();
		loader.init( elem );
		return loader;
	}

	public static Element toXml( final Document doc, final HuiskenImageLoader loader )
	{
		final Element elem = doc.createElement( "ImageLoader" );
		elem.setAttribute( "class", HuiskenImageLoader.class.getCanonicalName() );

		final Element path = doc.createElement( "path" );
		path.appendChild( doc.createTextNode( loader.expFile.getAbsolutePath() ) );
		elem.appendChild( path );

		return elem;
	}


	@Override
	public ImgPlus< UnsignedShortType > getUnsignedShortImage( final View view )
	{
		ensureExpIsOpen();

		final ViewSetup setup = view.getSetup();
		final int channel = setup.getChannel();
		final int illumination = setup.getIllumination();
		final int angle = setup.getAngle();
		final int timepoint = view.getTimepoint();

		final ImagePlus imp = getImagePlus( view );
		final Img< UnsignedShortType > img = ImageJFunctions.wrapShort( imp );

		final String name = getBasename( timepoint, angle, channel, illumination );

		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		final float zStretching = ( float ) ( exp.pd / exp.pw );
		final double[] calibration = new double[] { 1, 1, zStretching };

		return new ImgPlus< UnsignedShortType >( img, name, axes, calibration );
	}

	@Override
	public ImgPlus< FloatType > getImage( final View view )
	{
		ensureExpIsOpen();

		final ViewSetup setup = view.getSetup();
		final int channel = setup.getChannel();
		final int illumination = setup.getIllumination();
		final int angle = setup.getAngle();
		final int timepoint = view.getTimepoint();

		final ImagePlus imp = getImagePlus( view );
		final Img< FloatType > img = ImageJFunctions.convertFloat( imp );
		Normalize.normalize( img, new FloatType( 0 ), new FloatType( 1 ) ); // normalize the image to 0...1

		final String name = getBasename( timepoint, angle, channel, illumination );

		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		final float zStretching = ( float ) ( exp.pd / exp.pw );
		final double[] calibration = new double[] { 1, 1, zStretching };

		return new ImgPlus< FloatType >( img, name, axes, calibration );
	}

	private synchronized void ensureExpIsOpen()
	{
		if ( exp == null )
		{
			exp = new SPIMExperiment( expFile.getAbsolutePath() );
			hasAlternatingIllumination = exp.d < ( exp.planeEnd + 1 - exp.planeStart );
		}
	}

	private ImagePlus getImagePlus( final View view )
	{
		final ViewSetup setup = view.getSetup();
		final int channel = setup.getChannel();
		final int illumination = setup.getIllumination();
		final int angle = setup.getAngle();

		final int timepoint = view.getTimepoint();

		final int s = exp.sampleStart;
		final int r = exp.regionStart;
		final int f = exp.frameStart;
		final int zMin = exp.planeStart;
		final int zMax = exp.planeEnd;
		final int xMin = 0;
		final int xMax = exp.w - 1;
		final int yMin = 0;
		final int yMax = exp.h - 1;

		ImagePlus imp;
		if ( hasAlternatingIllumination )
		{
			final int zStep = 2;
			if ( illumination == 0 )
				imp = exp.openNotProjected( s, timepoint, timepoint, r, angle, channel, zMin, zMax - 1, zStep, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
			else
				imp = exp.openNotProjected( s, timepoint, timepoint, r, angle, channel, zMin + 1, zMax, zStep, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
		}
		else
		{
			imp = exp.openNotProjected( s, timepoint, timepoint, r, angle, channel, zMin, zMax, f, f, yMin, yMax, xMin, xMax, SPIMExperiment.X, SPIMExperiment.Y, SPIMExperiment.Z, false );
		}

		return imp;
	}

	final static private String basenameFormatString = "t%05d-a%03d-c%03d-i%01d";

	private static String getBasename( final int timepoint, final int angle, final int channel, final int illumination )
	{
		return String.format( basenameFormatString, timepoint, angle, channel, illumination );
	}
}
