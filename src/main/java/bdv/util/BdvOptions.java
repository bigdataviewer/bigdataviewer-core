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

import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;
import bdv.TransformEventHandlerFactory;
import bdv.tools.links.DefaultResourceManager;
import bdv.tools.links.ResourceManager;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.links.LinkSettingsManager;
import bdv.viewer.render.AccumulateProjectorARGB;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.MultiResolutionRenderer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

/**
 * Optional parameters for {@link BdvFunctions}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class BdvOptions
{
	public final Values values = new Values();

	/**
	 * Create default {@link BdvOptions}.
	 * @return default {@link BdvOptions}.
	 */
	public static BdvOptions options()
	{
		return new BdvOptions();
	}

	/**
	 * Set preferred size of {@link ViewerPanel} canvas. (This does not include
	 * the time slider).
	 */
	public BdvOptions preferredSize( final int w, final int h )
	{
		values.width = w;
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
	public BdvOptions screenScales( final double[] s )
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
	public BdvOptions targetRenderNanos( final long t )
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
	public BdvOptions numRenderingThreads( final int n )
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
	public BdvOptions numSourceGroups( final int n )
	{
		values.numSourceGroups = n;
		return this;
	}

	public BdvOptions transformEventHandlerFactory( final TransformEventHandlerFactory f )
	{
		values.transformEventHandlerFactory = f;
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
	public BdvOptions accumulateProjectorFactory( final AccumulateProjectorFactory< ARGBType > f )
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
	public BdvOptions inputTriggerConfig( final InputTriggerConfig c )
	{
		values.inputTriggerConfig = c;
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
	public BdvOptions keymapManager( final KeymapManager keymapManager )
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
	public BdvOptions appearanceManager( final AppearanceManager appearanceManager )
	{
		values.appearanceManager = appearanceManager;
		return this;
	}

	/**
	 * Set the {@link LinkSettingsManager}.
	 */
	public BdvOptions linkSettingsManager( final LinkSettingsManager linkSettingsManager )
	{
		values.linkSettingsManager = linkSettingsManager;
		return this;
	}

	/**
	 * Set the {@link ResourceManager}.
	 */
	public BdvOptions resourceManager( final ResourceManager resourceManager )
	{
		if ( resourceManager == null )
			throw new NullPointerException( "resourceManager cannot be null" );
		values.resourceManager = resourceManager;
		return this;
	}

	/**
	 * Set the transform of the {@link BdvSource} to be created.
	 *
	 * @param t
	 *            the source transform.
	 */
	public BdvOptions sourceTransform( final AffineTransform3D t )
	{
		values.sourceTransform.set( t );
		return this;
	}

	/**
	 * Set the title of the BigDataViewer window.
	 *
	 * @param title
	 *            the window title.
	 */
	public BdvOptions frameTitle( final String title )
	{
		values.frameTitle = title;
		return this;
	}

	/**
	 * Set the transform of the {@link BdvSource} to be created to account for
	 * the given calibration (scaling of the source axes).
	 *
	 * @param calibration
	 *            the source calibration (scaling of the source axes).
	 */
	public BdvOptions sourceTransform( final double ... calibration )
	{
		final double sx = calibration.length >= 1 ? calibration[ 0 ] : 1;
		final double sy = calibration.length >= 2 ? calibration[ 1 ] : 1;
		final double sz = calibration.length >= 3 ? calibration[ 2 ] : 1;
		values.sourceTransform.set(
				sx, 0, 0, 0,
				0, sy, 0, 0,
				0, 0, sz, 0 );
		return this;
	}

	/**
	 * Set up the BigDataViewer for 2D navigation.
	 *
	 * TODO: add more detailed explanation
	 *
	 * @return
	 */
	public BdvOptions is2D()
	{
		values.is2D = true;
		transformEventHandlerFactory( TransformEventHandler2D::new );
		return this;
	}

	/**
	 * Specified when adding a stack. Describes how the axes of the stack are
	 * ordered.
	 *
	 * @param axisOrder
	 *            the axis order of a stack to add.
	 */
	public BdvOptions axisOrder( final AxisOrder axisOrder )
	{
		values.axisOrder = axisOrder;
		return this;
	}

	/**
	 * When showing content using one of the {@link BdvFunctions} methods, this
	 * option can be given to specify that the content should be added to an
	 * existing window. (All {@link BdvFunctions} methods return an instance of
	 * {@link Bdv} that can be used that way).
	 *
	 * @param bdv
	 *            to which viewer should the content be added.
	 */
	public BdvOptions addTo( final Bdv bdv )
	{
		values.addTo = bdv;
		return this;
	}

	/**
	 * Read-only {@link BdvOptions} values.
	 */
	public static class Values
	{
		private int width = -1;

		private int height = -1;

		private double[] screenScales = new double[] { 1, 0.75, 0.5, 0.25, 0.125 };

		private long targetRenderNanos = 30 * 1000000l;

		private int numRenderingThreads = 3;

		private int numSourceGroups = 10;

		private TransformEventHandlerFactory transformEventHandlerFactory = TransformEventHandler3D::new;

		private AccumulateProjectorFactory< ARGBType > accumulateProjectorFactory = AccumulateProjectorARGB.factory;

		private InputTriggerConfig inputTriggerConfig = null;

		private KeymapManager keymapManager = null;

		private AppearanceManager appearanceManager = null;

		private LinkSettingsManager linkSettingsManager = null;

		private ResourceManager resourceManager = new DefaultResourceManager();

		private final AffineTransform3D sourceTransform = new AffineTransform3D();

		private String frameTitle = "BigDataViewer";

		private boolean is2D = false;

		private AxisOrder axisOrder = AxisOrder.DEFAULT;

		private Bdv addTo = null;

		Values()
		{
			sourceTransform.identity();
		}

		public BdvOptions optionsFromValues()
		{
			final BdvOptions o = new BdvOptions()
					.preferredSize( width, height )
					.screenScales( screenScales )
					.targetRenderNanos( targetRenderNanos )
					.numRenderingThreads( numRenderingThreads )
					.numSourceGroups( numSourceGroups )
					.transformEventHandlerFactory( transformEventHandlerFactory )
					.accumulateProjectorFactory( accumulateProjectorFactory )
					.inputTriggerConfig( inputTriggerConfig )
					.keymapManager( keymapManager )
					.appearanceManager( appearanceManager )
					.linkSettingsManager( linkSettingsManager )
					.resourceManager( resourceManager )
					.sourceTransform( sourceTransform )
					.frameTitle( frameTitle )
					.axisOrder( axisOrder )
					.addTo( addTo );
			if ( is2D() )
				o.is2D();
			return o;
		}

		public ViewerOptions getViewerOptions()
		{
			final ViewerOptions o = ViewerOptions.options()
					.screenScales( screenScales )
					.targetRenderNanos( targetRenderNanos )
					.numRenderingThreads( numRenderingThreads )
					.numSourceGroups( numSourceGroups )
					.is2D( is2D )
					.transformEventHandlerFactory( transformEventHandlerFactory )
					.accumulateProjectorFactory( accumulateProjectorFactory )
					.inputTriggerConfig( inputTriggerConfig )
					.keymapManager( keymapManager )
					.appearanceManager( appearanceManager )
					.linkSettingsManager( linkSettingsManager )
					.resourceManager( resourceManager );
			if ( hasPreferredSize() )
				o.width( width ).height( height );
			return o;
		}

		public AffineTransform3D getSourceTransform()
		{
			return sourceTransform;
		}

		public String getFrameTitle()
		{
			return frameTitle;
		}

		public boolean is2D()
		{
			return is2D;
		}

		public boolean hasPreferredSize()
		{
			return width > 0 && height > 0;
		}

		public AxisOrder axisOrder()
		{
			return axisOrder;
		}

		public InputTriggerConfig getInputTriggerConfig()
		{
			return inputTriggerConfig;
		}

		public KeymapManager getKeymapManager()
		{
			return keymapManager;
		}

		public AppearanceManager getAppearanceManager()
		{
			return appearanceManager;
		}

		public LinkSettingsManager getLinkSettingsManager()
		{
			return linkSettingsManager;
		}

		public ResourceManager getResourceManager()
		{
			return resourceManager;
		}

		public Bdv addTo()
		{
			return addTo;
		}
	}
}
