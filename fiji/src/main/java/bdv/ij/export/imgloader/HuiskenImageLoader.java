package bdv.ij.export.imgloader;

import ij.ImagePlus;

import java.io.File;
import java.util.HashMap;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import spimopener.SPIMExperiment;

/**
 * This {@link ImgLoader} implementation uses Benjamin Schmid's
 * <a href="http://fiji.sc/javadoc/spimopener/package-summary.html">spimopener</a>
 * to load images in Jan Husiken's format.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class HuiskenImageLoader implements BasicImgLoader< UnsignedShortType >
{
	private final File expFile;

	private SPIMExperiment exp;

	private boolean hasAlternatingIllumination;

	private final HashMap< Integer, ViewSetup > setups;

	public HuiskenImageLoader( final File file, final HashMap< Integer, ViewSetup > setups )
	{
		this.setups = setups;
		expFile = file;
		exp = null;
	}

	@Override
	public ImgPlus< UnsignedShortType > getImage( final ViewId view )
	{
		ensureExpIsOpen();

		final ViewSetup setup = setups.get( view.getViewSetupId() );
		final int channel = setup.getChannel().getId();
		final int illumination = setup.getIllumination().getId();
		final int angle = setup.getAngle().getId();
		final int timepoint = view.getTimePointId();

		final ImagePlus imp = getImagePlus( view );
		final Img< UnsignedShortType > img = ImageJFunctions.wrapShort( imp );

		final String name = getBasename( timepoint, angle, channel, illumination );

		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		final float zStretching = ( float ) ( exp.pd / exp.pw );
		final double[] calibration = new double[] { 1, 1, zStretching };

		return new ImgPlus< UnsignedShortType >( img, name, axes, calibration );
	}

	@Override
	public UnsignedShortType getImageType()
	{
		return new UnsignedShortType();
	}

	private synchronized void ensureExpIsOpen()
	{
		if ( exp == null )
		{
			exp = new SPIMExperiment( expFile.getAbsolutePath() );
			hasAlternatingIllumination = exp.d < ( exp.planeEnd + 1 - exp.planeStart );
		}
	}

	private ImagePlus getImagePlus( final ViewId view )
	{
		final ViewSetup setup = setups.get( view.getViewSetupId() );
		final int channel = setup.getChannel().getId();
		final int illumination = setup.getIllumination().getId();
		final int angle = setup.getAngle().getId();
		final int timepoint = view.getTimePointId();

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
