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
package bdv.util;

import java.util.Collections;

import net.imglib2.type.numeric.ARGBType;
import bdv.viewer.OverlayRenderer;

import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.SourceAndConverter;

public class BdvOverlaySource< O extends OverlayRenderer > extends BdvSource
{
	protected final PlaceHolderConverterSetup setup;

	private final SourceAndConverter< Void > source;

	private final PlaceHolderOverlayInfo info;

	protected final O overlay;

	public O getOverlay()
	{
		return overlay;
	}

	protected BdvOverlaySource(
			final BdvHandle bdv,
			final int numTimepoints,
			final PlaceHolderConverterSetup setup,
			final SourceAndConverter< Void > source,
			final PlaceHolderOverlayInfo info,
			final O overlay )
	{
		super( bdv, numTimepoints );
		this.setup = setup;
		this.source = source;
		this.info = info;
		this.overlay = overlay;
	}

	@Override
	public void removeFromBdv()
	{
		getBdvHandle().remove(
				Collections.singletonList( setup ),
				Collections.singletonList( source ),
				Collections.singletonList( info ),
				Collections.singletonList( info ),
				Collections.singletonList( info ),
				Collections.singletonList( overlay ) );
		getBdvHandle().removeBdvSource( this );
	}

	@Override
	protected boolean isPlaceHolderSource()
	{
		return true;
	}

	@Override
	public void setDisplayRange( final double min, final double max )
	{
		setup.setDisplayRange( min, max );
	}

	@Override
	public void setDisplayRangeBounds( final double min, final double max )
	{
		getBdvHandle().getConverterSetups().getBounds().setBounds( setup, new Bounds( min, max ) );

		// TODO: REMOVE
		final SetupAssignments sa = getBdvHandle().getSetupAssignments();
		final MinMaxGroup group = sa.getMinMaxGroup( setup );
		group.setRange( min, max );
	}

	@Override
	public void setColor( final ARGBType color )
	{
		setup.setColor( color );
	}

	@Override
	public void setCurrent()
	{
		getBdvHandle().getViewerPanel().state().setCurrentSource( source );
	}

	@Override
	public boolean isCurrent()
	{
		return getBdvHandle().getViewerPanel().state().isCurrentSource( source );
	}

	@Override
	public void setActive( final boolean isActive )
	{
		getBdvHandle().getViewerPanel().state().setSourceActive( source, isActive );
	}
}
