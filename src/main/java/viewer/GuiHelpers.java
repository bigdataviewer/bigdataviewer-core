package viewer;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import net.imglib2.display.ARGBScreenImage;

public class GuiHelpers
{
	/**
	 * @param colorModel
	 * @return
	 */
	static final GraphicsConfiguration getSuitableGraphicsConfiguration( final ColorModel colorModel )
	{
		final GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final GraphicsConfiguration defaultGc = device.getDefaultConfiguration();

		final int transparency = colorModel.getTransparency();

		if ( defaultGc.getColorModel( transparency ).equals( colorModel ) )
			return defaultGc;

		for ( final GraphicsConfiguration gc : device.getConfigurations() )
			if ( gc.getColorModel( transparency ).equals( colorModel ) )
				return gc;

		return defaultGc;
	}

	/**
	 * Whether to discard the {@link #screenImage} alpha components when drawing.
	 */
	static final boolean discardAlpha = true;

	static final ColorModel RGB_COLOR_MODEL = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);

	/**
	 * @param screenImage
	 * @return
	 */
	static final BufferedImage getBufferedImage( final ARGBScreenImage screenImage )
	{
		final BufferedImage si = screenImage.image();
		if ( discardAlpha && ( si.getTransparency() != Transparency.OPAQUE ) )
		{
			final SampleModel sampleModel = RGB_COLOR_MODEL.createCompatibleWritableRaster( 1, 1 ).getSampleModel().createCompatibleSampleModel( si.getWidth(), si.getHeight() );
			final DataBuffer dataBuffer = si.getRaster().getDataBuffer();
			final WritableRaster rgbRaster = Raster.createWritableRaster( sampleModel, dataBuffer, null );
			return new BufferedImage( RGB_COLOR_MODEL, rgbRaster, false, null );
		}
		else
			return si;
	}
}
