/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
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
package bdv.ui;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.listeners.Listeners;

/**
 * Dummy Converter and ConverterSetup for testing SourceTable and SourceGroupTree
 *
 * @author Tobias Pietzsch
 */
public class DefaultConverterSetup implements ConverterSetup, Converter< UnsignedShortType, ARGBType >
{
	private final int setupId;

	private final boolean supportsColor;

	private double displayRangeMin;

	private double displayRangeMax;

	private final ARGBType color = new ARGBType();

	private final Listeners.List< SetupChangeListener > listeners = new Listeners.SynchronizedList<>();

	public DefaultConverterSetup( final int setupId, final boolean supportsColor )
	{
		this.setupId = setupId;
		this.supportsColor = supportsColor;
		this.displayRangeMin = 0;
		this.displayRangeMax = 1;
	}

	@Override
	public Listeners< SetupChangeListener > setupChangeListeners()
	{
		return listeners;
	}

	@Override
	public synchronized void setDisplayRange( final double min, final double max )
	{
		if ( displayRangeMin != min || displayRangeMax != max )
		{
			displayRangeMin = min;
			displayRangeMax = max;
			listeners.list.forEach( l -> l.setupParametersChanged( this ) );
		}
	}

	@Override
	public synchronized void setColor( final ARGBType argb )
	{
		if ( supportsColor && color.get() != argb.get() )
		{
			color.set( argb );
			listeners.list.forEach( l -> l.setupParametersChanged( this ) );
		}
	}

	@Override
	public boolean supportsColor()
	{
		return supportsColor;
	}

	@Override
	public int getSetupId()
	{
		return setupId;
	}

	@Override
	public double getDisplayRangeMin()
	{
		return displayRangeMin;
	}

	@Override
	public double getDisplayRangeMax()
	{
		return displayRangeMax;
	}

	@Override
	public ARGBType getColor()
	{
		return color;
	}

	@Override
	public void convert( final UnsignedShortType input, final ARGBType output )
	{
		throw new UnsupportedOperationException();
	}
}
