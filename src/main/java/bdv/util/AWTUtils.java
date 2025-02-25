/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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
package bdv.util;

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
import net.imglib2.display.screenimage.awt.ARGBScreenImage;

/**
 * Static helper methods for setting up {@link GraphicsConfiguration} and
 * {@link BufferedImage BufferedImages}.
 *
 * @author Tobias Pietzsch
 */
public class AWTUtils
{
	/**
	 * Get a {@link GraphicsConfiguration} from the default screen
	 * {@link GraphicsDevice} that matches the
	 * {@link ColorModel#getTransparency() transparency} of the given
	 * <code>colorModel</code>. If no matching configuration is found, the
	 * default configuration of the {@link GraphicsDevice} is returned.
	 */
	public static GraphicsConfiguration getSuitableGraphicsConfiguration( final ColorModel colorModel )
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

	public static final ColorModel ARGB_COLOR_MODEL = new DirectColorModel( 32, 0xff0000, 0xff00, 0xff, 0xff000000 );

	public static final ColorModel RGB_COLOR_MODEL = new DirectColorModel( 24, 0xff0000, 0xff00, 0xff );

	/**
	 * Get a {@link BufferedImage} for the given {@link ARGBScreenImage}.
	 *
	 * @param screenImage
	 * 		the image.
	 * @param discardAlpha
	 * 		Whether to discard the <code>screenImage</code> alpha
	 * 		components when drawing.
	 */
	public static BufferedImage getBufferedImage( final ARGBScreenImage screenImage, final boolean discardAlpha )
	{
		final BufferedImage si = screenImage.image();
		if ( discardAlpha && ( si.getTransparency() != Transparency.OPAQUE ) )
		{
			final SampleModel sampleModel = RGB_COLOR_MODEL.createCompatibleWritableRaster( 1, 1 ).getSampleModel().createCompatibleSampleModel( si.getWidth(), si.getHeight() );
			final DataBuffer dataBuffer = si.getRaster().getDataBuffer();
			final WritableRaster rgbRaster = Raster.createWritableRaster( sampleModel, dataBuffer, null );
			return new BufferedImage( RGB_COLOR_MODEL, rgbRaster, false, null );
		}
		return si;
	}

	/**
	 * Get a {@link BufferedImage} for the given {@link ARGBScreenImage}.
	 * Discard the <code>screenImage</code> alpha components when drawing.
	 *
	 * @param screenImage
	 * 		the image.
	 */
	public static BufferedImage getBufferedImage( final ARGBScreenImage screenImage )
	{
		return getBufferedImage( screenImage, true );
	}
}
