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
package bdv.viewer;

import static bdv.viewer.BlendMode.AVG;
import static bdv.viewer.BlendMode.CUSTOM;
import static bdv.viewer.BlendMode.SUM;
import static bdv.viewer.ViewerStateChange.ACCUMULATE_PROJECTOR_CHANGED;

import org.scijava.listeners.Listeners;

import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.AlphaWeightedAccumulateProjectorARGB;
import net.imglib2.type.numeric.ARGBType;

public class BlendModeSwitcher
{
	private final ViewerState state;

	private AccumulateProjectorFactory< ARGBType > customFactory;

	private BlendMode mode;

	public BlendModeSwitcher( final ViewerState state )
	{
		this.state = state;
		listeners = new Listeners.List<>();

		state.changeListeners().add( c ->
		{
			if ( c == ACCUMULATE_PROJECTOR_CHANGED )
				factoryChanged( state.getAccumulateProjectorFactory() );
		} );
		factoryChanged( state.getAccumulateProjectorFactory() );
	}

	/**
	 * {@code BlendModeChangeListener}s are notified about blend mode changes.
	 */
	public interface BlendModeChangeListener
	{
		void blendModeChanged(BlendMode newMode);
	}

	private final Listeners.List< BlendModeChangeListener > listeners;

	public Listeners< BlendModeChangeListener > changeListeners()
	{
		return listeners;
	}

	public BlendMode getCurrentMode()
	{
		return mode;
	}

	public void switchToNextMode()
	{
		mode = next();
		state.setAccumulateProjectorFactory( factory() );
		listeners.list.forEach( l -> l.blendModeChanged( mode ) );
	}

	private BlendMode next()
	{
		switch ( mode )
		{
		case SUM:
			return AVG;
		case AVG:
			return customFactory != null ? CUSTOM : SUM;
		case CUSTOM:
			return SUM;
		default:
			throw new IllegalArgumentException();
		}
	}

	private AccumulateProjectorFactory< ARGBType > factory()
	{
		switch ( mode )
		{
		case SUM:
			return AccumulateProjectorARGB.factory;
		case AVG:
			return AlphaWeightedAccumulateProjectorARGB.factory;
		case CUSTOM:
			return customFactory;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void factoryChanged( final AccumulateProjectorFactory< ARGBType > factory )
	{
		final BlendMode oldMode = mode;
		mode = BlendMode.of( factory );

		if ( mode == CUSTOM )
			customFactory = factory;

		if ( mode != oldMode )
		{
			listeners.list.forEach( l -> l.blendModeChanged( mode ) );
		}
	}
}
