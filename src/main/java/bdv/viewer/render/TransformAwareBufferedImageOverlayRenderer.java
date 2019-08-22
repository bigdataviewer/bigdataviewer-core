/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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
package bdv.viewer.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import bdv.util.TripleBuffer;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import net.imglib2.util.Intervals;

public class TransformAwareBufferedImageOverlayRenderer implements OverlayRenderer, RenderTarget {

	private final AffineTransform3D paintedTransform = new AffineTransform3D();

	/**
	 * These listeners will be notified about the transform that is associated
	 * to the currently rendered image. This is intended for example for
	 * {@link OverlayRenderer}s that need to exactly match the transform of
	 * their overlaid content to the transform of the image.
	 */
	private final CopyOnWriteArrayList< TransformListener< AffineTransform3D > > paintedTransformListeners
			= new CopyOnWriteArrayList<>();

	private volatile int width;

	private volatile int height;

	private final TripleBuffer<RenderResult> tripleBuffer = new TripleBuffer<>(() -> null);

	private RenderResult lastCompleteResult = null;

	@Override
	public RandomAccessibleInterval<ARGBType> createOutputImage(int width, int height) {
		int requiredSize = Math.max(width * height, getWidth() * getHeight());
		RenderResult renderResult = tripleBuffer.getWritableBuffer();
		if(renderResult != null) {
			ARGBScreenImage image = (ARGBScreenImage) renderResult.getImage();
			IntArray buffer = image.update(null);
			if (buffer.getArrayLength() == requiredSize)
				return new ARGBScreenImage(width, height, buffer);
		}
		return new ARGBScreenImage(width, height);
	}

	@Override
	public synchronized void setRenderResult(RenderResult result) {
		if ( result.isComplete() ) {
			tripleBuffer.doneWriting(result);
			lastCompleteResult = result;
		} else {
			if( lastCompleteResult == null )
				return;
			ARGBScreenImage source = (ARGBScreenImage) result.getImage();
			ARGBScreenImage target = (ARGBScreenImage) lastCompleteResult.getImage();
			source.forEach(pixel -> pixel.set(pixel.get() | 0xff000000));
			double scale = lastCompleteResult.getScaleFactor() / result.getScaleFactor();
			AffineTransform transform = new AffineTransform();
			transform.scale(scale, scale);
			AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			Interval sourceInterval = result.getPaddedScaledInterval();
			Interval targetInterval = Intervals.largestContainedInterval(scaleInterval(sourceInterval, 1 / result.getScaleFactor()));
			op.filter(getSubimage(source, sourceInterval),
					getSubimage(target, targetInterval));
		}
	}

	private RealInterval scaleInterval(Interval interval, double scale) {
		// TODO: move to intervals class
		// scale the screen repaint request interval into render target coordinates
		int n = interval.numDimensions();
		final double[] min = new double[n];
		final double[] max = new double[n];
		Arrays.setAll(min, d -> interval.min(d) * scale);
		Arrays.setAll(max, d -> interval.max(d) * scale);
		return new FinalRealInterval(min, max);
	}

	private BufferedImage getSubimage(ARGBScreenImage partialImage, Interval interval) {
		interval = Intervals.intersect(interval, partialImage);
		return partialImage.image().getSubimage((int) interval.min(0), (int) interval.min(1), (int) interval.dimension(0), (int) interval.dimension(1));
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		boolean update = tripleBuffer.hasUpdate();
		RenderResult result = tripleBuffer.getReadableBuffer();
		if ( result != null )
		{
			ARGBScreenImage readableBuffer = (ARGBScreenImage) result.getImage();
//			final StopWatch watch = new StopWatch();
//			watch.start();
//			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED );
			( ( Graphics2D ) g ).setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED );
			BufferedImage image = readableBuffer.image();
			int width = Math.max(getWidth(), (int) (image.getWidth() / result.getScaleFactor() + 0.5));
			int height = Math.max(getHeight(), (int) (image.getHeight() / result.getScaleFactor() + 0.5));
			g.drawImage(image, 0, 0, width, height, null );
			if (update)
				notifyTransformListeners(result.getViewerTransform());
//			System.out.println( String.format( "g.drawImage() :%4d ms", watch.nanoTime() / 1000000 ) );
		}
	}

	private String toString(Interval value) {
		return "min:" + Arrays.toString(Intervals.minAsLongArray(value)) +
				",max:" + Arrays.toString(Intervals.maxAsLongArray(value));
	}

	private void notifyTransformListeners(AffineTransform3D viewerTransform) {
		paintedTransform.set(viewerTransform);
		for (final TransformListener<AffineTransform3D> listener : paintedTransformListeners)
			listener.transformChanged(paintedTransform);
	}

	@Override
	public void setCanvasSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 */
	public void addTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		addTransformListener( listener, Integer.MAX_VALUE );
	}

	/**
	 * Add a {@link TransformListener} to notify about viewer transformation
	 * changes. Listeners will be notified when a new image has been rendered
	 * (immediately before that image is displayed) with the viewer transform
	 * used to render that image.
	 *
	 * @param listener
	 *            the transform listener to add.
	 * @param index
	 *            position in the list of listeners at which to insert this one.
	 */
	public void addTransformListener( final TransformListener< AffineTransform3D > listener, final int index )
	{
		synchronized ( paintedTransformListeners )
		{
			final int s = paintedTransformListeners.size();
			paintedTransformListeners.add( index < 0 ? 0 : index > s ? s : index, listener );
			listener.transformChanged( paintedTransform );
		}
	}

	/**
	 * Remove a {@link TransformListener}.
	 *
	 * @param listener
	 *            the transform listener to remove.
	 */
	public void removeTransformListener( final TransformListener< AffineTransform3D > listener )
	{
		synchronized ( paintedTransformListeners )
		{
			paintedTransformListeners.remove( listener );
		}
	}

	/**
	 * DON'T USE THIS.
	 * <p>
	 * This is a work around for JDK bug
	 * https://bugs.openjdk.java.net/browse/JDK-8029147 which leads to
	 * ViewerPanel not being garbage-collected when ViewerFrame is closed. So
	 * instead we need to manually let go of resources...
	 */
	void kill()
	{
		tripleBuffer.clear();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}
}
