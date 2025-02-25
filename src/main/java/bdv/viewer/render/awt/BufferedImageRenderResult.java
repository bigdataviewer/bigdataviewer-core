/*-
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
package bdv.viewer.render.awt;

import bdv.util.AWTUtils;
import bdv.viewer.render.RenderResult;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class BufferedImageRenderResult implements RenderResult
{
	private final AffineTransform3D viewerTransform = new AffineTransform3D();

	private int width;
	private int height;

	private int[] data = new int[ 0 ];

	private ARGBScreenImage screenImage;

	private BufferedImage bufferedImage;

	private double scaleFactor;

	@Override
	public void init( final int width, final int height )
	{
		if ( this.width == width && this.height == height )
			return;

		this.width = width;
		this.height = height;

		if ( data.length < width * height )
			data = new int[ width * height ];

		screenImage = new ARGBScreenImage( width, height, data );
		bufferedImage = AWTUtils.getBufferedImage( screenImage );;
	}

	@Override
	public RandomAccessibleInterval< ARGBType > getTargetImage()
	{
		return screenImage;
	}

	public BufferedImage getBufferedImage()
	{
		return bufferedImage;
	}

	@Override
	public AffineTransform3D getViewerTransform()
	{
		return viewerTransform;
	}

	@Override
	public double getScaleFactor()
	{
		return scaleFactor;
	}

	@Override
	public void setScaleFactor( final double scaleFactor )
	{
		this.scaleFactor = scaleFactor;
	}

	@Override
	public void setUpdated()
	{
		// ignored. BufferedImage is always up-to-date
	}

	@Override
	public void patch( final RenderResult patch, final Interval interval, final double ox, final double oy )
	{
		final BufferedImageRenderResult biresult = ( BufferedImageRenderResult ) patch;

		final double s = scaleFactor / patch.getScaleFactor();
		final double tx = ox - interval.min( 0 );
		final double ty = oy - interval.min( 1 );
		final AffineTransform transform = new AffineTransform( s, 0, 0, s, tx, ty );
		final AffineTransformOp op = new AffineTransformOp( transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR );
		op.filter( biresult.getBufferedImage(), subImage( interval ) );
	}

	private BufferedImage subImage( final Interval interval )
	{
		final int x = ( int ) interval.min( 0 );
		final int y = ( int ) interval.min( 1 );
		final int w = ( int ) interval.dimension( 0 );
		final int h = ( int ) interval.dimension( 1 );
		return bufferedImage.getSubimage( x, y, w, h );
	}
}
