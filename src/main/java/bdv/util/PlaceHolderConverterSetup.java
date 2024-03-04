/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;

public final class PlaceHolderConverterSetup implements ConverterSetup
{
	private final int setupId;

	private double min;

	private double max;

	private final ARGBType color;

	private final boolean supportsColor;

	private final Listeners.List< SetupChangeListener > listeners;

	public PlaceHolderConverterSetup(
			final int setupId,
			final double min,
			final double max,
			final int rgb )
	{
		this( setupId, min, max, new ARGBType( rgb ) );
	}

	public PlaceHolderConverterSetup(
			final int setupId,
			final double min,
			final double max,
			final ARGBType color )
	{
		this.setupId = setupId;
		this.min = min;
		this.max = max;
		this.color = new ARGBType();
		if ( color != null )
			this.color.set( color );
		this.supportsColor = color != null;
		this.listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public Listeners< SetupChangeListener > setupChangeListeners()
	{
		return listeners;
	}

	@Override
	public int getSetupId()
	{
		return setupId;
	}

	@Override
	public void setDisplayRange( final double min, final double max )
	{
		if ( this.min == min && this.max == max )
			return;

		this.min = min;
		this.max = max;
		listeners.list.forEach( l -> l.setupParametersChanged( this ) );
	}

	@Override
	public void setColor( final ARGBType color )
	{
		setColor( color.get() );
	}

	public void setColor( final int rgb )
	{
		if ( !supportsColor() || color.get() == rgb )
			return;

		color.set( rgb );
		listeners.list.forEach( l -> l.setupParametersChanged( this ) );
	}

	@Override
	public boolean supportsColor()
	{
		return supportsColor;
	}

	@Override
	public double getDisplayRangeMin()
	{
		return min;
	}

	@Override
	public double getDisplayRangeMax()
	{
		return max;
	}

	@Override
	public ARGBType getColor()
	{
		return color;
	}
}
