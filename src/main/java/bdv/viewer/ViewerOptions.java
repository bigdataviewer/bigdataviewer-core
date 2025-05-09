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
package bdv.viewer;

import java.awt.event.KeyListener;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;
import bdv.TransformEventHandlerFactory;
import bdv.ui.UIUtils;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import bdv.viewer.animate.MessageOverlayAnimator;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.MultiResolutionRenderer;
import net.imglib2.type.numeric.ARGBType;

/**
 * Optional parameters for {@link ViewerPanel}.
 *
 * @author Tobias Pietzsch
 */
public class ViewerOptions
{
	public final Values values = new Values();

	/**
	 * Create default {@link ViewerOptions}.
	 * @return default {@link ViewerOptions}.
	 */
	public static ViewerOptions options()
	{
		return new ViewerOptions();
	}

	/**
	 * Set width of {@link ViewerPanel} canvas.
	 */
	public ViewerOptions width( final int w )
	{
		values.width = w;
		return this;
	}

	/**
	 * Set height of {@link ViewerPanel} canvas.
	 */
	public ViewerOptions height( final int h )
	{
		values.height = h;
		return this;
	}

	/**
	 * Set the number and scale factors for scaled screen images.
	 *
	 * @param s
	 *            Scale factors from the viewer canvas to screen images of
	 *            different resolutions. A scale factor of 1 means 1 pixel in
	 *            the screen image is displayed as 1 pixel on the canvas, a
	 *            scale factor of 0.5 means 1 pixel in the screen image is
	 *            displayed as 2 pixel on the canvas, etc.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions screenScales( final double[] s )
	{
		values.screenScales = s;
		return this;
	}

	/**
	 * Set target rendering time in nanoseconds.
	 *
	 * @param t
	 *            Target rendering time in nanoseconds. The rendering time for
	 *            the coarsest rendered scale should be below this threshold.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions targetRenderNanos( final long t )
	{
		values.targetRenderNanos = t;
		return this;
	}

	/**
	 * Set how many threads to use for rendering.
	 *
	 * @param n
	 *            How many threads to use for rendering.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions numRenderingThreads( final int n )
	{
		values.numRenderingThreads = n;
		return this;
	}

	/**
	 * Set how many source groups there are initially.
	 *
	 * @param n
	 *            How many source groups to create initially.
	 */
	public ViewerOptions numSourceGroups( final int n )
	{
		values.numSourceGroups = n;
		return this;
	}

	/**
	 * Set whether volatile versions of sources should be used if available.
	 *
	 * @param v
	 *            whether volatile versions of sources should be used if
	 *            available.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions useVolatileIfAvailable( final boolean v )
	{
		values.useVolatileIfAvailable = v;
		return this;
	}

	public ViewerOptions msgOverlay( final MessageOverlayAnimator o )
	{
		values.msgOverlay = o;
		return this;
	}

	public ViewerOptions transformEventHandlerFactory( final TransformEventHandlerFactory f )
	{
		values.transformEventHandlerFactory = f;
		return this;
	}

	/**
	 * Set up the BigDataViewer for 2D navigation.
	 * Note, that Sources still need to provide 3D stacks, etc.
	 * This option only prevents out-of-plane movement.
	 *
	 * @param is2D whether to restrict navigation to 2D (default is {@code false}).
	 */
	public ViewerOptions is2D( final boolean is2D )
	{
		values.is2D = is2D;
		transformEventHandlerFactory( is2D ? TransformEventHandler2D::new : TransformEventHandler3D::new );
		return this;
	}

	/**
	 * Set the factory for creating {@link AccumulateProjector}. This can be
	 * used to customize how sources are combined.
	 *
	 * @param f
	 *            factory for creating {@link AccumulateProjector}.
	 * @see MultiResolutionRenderer
	 */
	public ViewerOptions accumulateProjectorFactory( final AccumulateProjectorFactory< ARGBType > f )
	{
		values.accumulateProjectorFactory = f;
		return this;
	}

	/**
	 * Set the {@link InputTriggerConfig} from which keyboard and mouse action mapping is loaded.
	 * <p>
	 * Note that this will override the managed {@code InputTriggerConfig}, that is,
	 * modifying the keymap through the preferences dialog will have no effect.
	 *
	 * @param c the {@link InputTriggerConfig} from which keyboard and mouse action mapping is loaded
	 */
	public ViewerOptions inputTriggerConfig( final InputTriggerConfig c )
	{
		values.inputTriggerConfig = c;
		return this;
	}

	/**
	 * Set the {@link KeyPressedManager} to share
	 * {@link KeyListener#keyPressed(java.awt.event.KeyEvent)} events with other
	 * ui-behaviour windows.
	 * <p>
	 * The goal is to make keyboard click/drag behaviours work like mouse
	 * click/drag: When a behaviour is initiated with a key press, the window
	 * under the mouse receives focus and the behaviour is handled there.
	 * </p>
	 *
	 * @param manager
	 * @return
	 */
	public ViewerOptions shareKeyPressedEvents( final KeyPressedManager manager )
	{
		values.keyPressedManager = manager;
		return this;
	}

	/**
	 * Set the {@link KeymapManager} to share keymap settings with other
	 * BigDataViewer windows.
	 * <p>
	 * This can be used to link multiple BigDataViewer windows such that they
	 * use (and modify) the same {@code Keymap}, shortcuts and mouse gestures.
	 * </p>
	 */
	public ViewerOptions keymapManager( final KeymapManager keymapManager )
	{
		values.keymapManager = keymapManager;
		return this;
	}

	/**
	 * Set the {@link AppearanceManager} to share appearance settings with other
	 * BigDataViewer windows.
	 * <p>
	 * This can be used to link multiple BigDataViewer windows such that they
	 * use (and modify) the same {@code Appearance} settings, e.g., LookAndFeel,
	 * scalebar overlay settings, etc.
	 * </p>
	 */
	public ViewerOptions appearanceManager( final AppearanceManager appearanceManager )
	{
		values.appearanceManager = appearanceManager;
		return this;
	}

	/**
	 * Read-only {@link ViewerOptions} values.
	 */
	public static class Values
	{
		private int width = ( int )Math.round( 800 * UIUtils.getUIScaleFactor( this ) );

		private int height = ( int )Math.round( 600 * UIUtils.getUIScaleFactor( this ) );

		private double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

		private long targetRenderNanos = 30 * 1000000l;

		private int numRenderingThreads = Runtime.getRuntime().availableProcessors();

		private int numSourceGroups = 10;

		private boolean useVolatileIfAvailable = true;

		private MessageOverlayAnimator msgOverlay = new MessageOverlayAnimator( 800 );

		private TransformEventHandlerFactory transformEventHandlerFactory = TransformEventHandler3D::new;

		private boolean is2D = false;

		private AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory = AccumulateProjectorARGB.factory;

		private InputTriggerConfig inputTriggerConfig = null;

		private KeyPressedManager keyPressedManager = null;

		private KeymapManager keymapManager = null;

		private AppearanceManager appearanceManager = null;

		public ViewerOptions optionsFromValues()
		{
			return new ViewerOptions().
				width( width ).
				height( height ).
				screenScales( screenScales ).
				targetRenderNanos( targetRenderNanos ).
				numRenderingThreads( numRenderingThreads ).
				numSourceGroups( numSourceGroups ).
				useVolatileIfAvailable( useVolatileIfAvailable ).
				msgOverlay( msgOverlay ).
				is2D( is2D ).
				transformEventHandlerFactory( transformEventHandlerFactory ).
				accumulateProjectorFactory( accumulateProjectorFactory ).
				inputTriggerConfig( inputTriggerConfig ).
				shareKeyPressedEvents( keyPressedManager ).
				keymapManager( keymapManager ).
				appearanceManager( appearanceManager );
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}

		public double[] getScreenScales()
		{
			return screenScales;
		}

		public long getTargetRenderNanos()
		{
			return targetRenderNanos;
		}

		public int getNumRenderingThreads()
		{
			return numRenderingThreads;
		}

		public int getNumSourceGroups()
		{
			return numSourceGroups;
		}

		public boolean isUseVolatileIfAvailable()
		{
			return useVolatileIfAvailable;
		}

		public MessageOverlayAnimator getMsgOverlay()
		{
			return msgOverlay;
		}

		public TransformEventHandlerFactory getTransformEventHandlerFactory()
		{
			return transformEventHandlerFactory;
		}

		public boolean is2D()
		{
			return is2D;
		}

		public AccumulateProjectorFactory< ARGBType > getAccumulateProjectorFactory()
		{
			return accumulateProjectorFactory;
		}

		public InputTriggerConfig getInputTriggerConfig()
		{
			return inputTriggerConfig;
		}

		public KeyPressedManager getKeyPressedManager()
		{
			return keyPressedManager;
		}

		public KeymapManager getKeymapManager()
		{
			return keymapManager;
		}

		public AppearanceManager getAppearanceManager()
		{
			return appearanceManager;
		}
	}
}
