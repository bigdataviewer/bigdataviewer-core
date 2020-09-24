/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.tools.brightness;

import java.util.LinkedHashSet;
import java.util.Set;

import bdv.util.BoundedIntervalDouble;
import bdv.util.BoundedValueDouble;

/**
 * A {@code double} interval. The {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval are stored as
 * {@link BoundedValueDouble}. The min and max can be changed to span any non-empty
 * interval within the range ({@link #getRangeMin()}, {@link #getRangeMax()}).
 * This range can be {@link #setRange(double, double) modified} as well.
 * <p>
 * Some {@link ConverterSetup ConverterSetups} can be
 * {@link #addSetup(ConverterSetup) linked}. They will have their display range
 * set according to {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval.
 * <p>
 * An {@link UpdateListener} (usually a GUI component) can be notified about
 * changes.
 *
 * @author Tobias Pietzsch
 */
public class MinMaxGroup extends BoundedIntervalDouble
{
	private final double fullRangeMin;

	private final double fullRangeMax;

	final Set< ConverterSetup > setups;

	public interface UpdateListener
	{
		void update();
	}

	private UpdateListener updateListener;

	public MinMaxGroup(
			final double fullRangeMin,
			final double fullRangeMax,
			final double rangeMin,
			final double rangeMax,
			final double currentMin,
			final double currentMax,
			final double minIntervalSize )
	{
		super( rangeMin, rangeMax, currentMin, currentMax, minIntervalSize );
		this.fullRangeMin = fullRangeMin;
		this.fullRangeMax = fullRangeMax;
		setups = new LinkedHashSet<>();
		updateListener = null;
	}

	@Override
	protected void updateInterval( final double min, final double max )
	{
		for ( final ConverterSetup setup : setups )
			setup.setDisplayRange( min, max );
	}

	public double getFullRangeMin()
	{
		return fullRangeMin;
	}

	public double getFullRangeMax()
	{
		return fullRangeMax;
	}

	/**
	 * Add a {@link ConverterSetup} which will have its
	 * {@link ConverterSetup#setDisplayRange(double, double) display range} updated to
	 * the interval ({@link #getMinBoundedValue()},
	 * {@link #getMaxBoundedValue()}).
	 *
	 * @param setup
	 */
	public void addSetup( final ConverterSetup setup )
	{
		setups.add( setup );
		setup.setDisplayRange( getMinBoundedValue().getCurrentValue(), getMaxBoundedValue().getCurrentValue() );

		if ( updateListener != null )
			updateListener.update();
	}

	/**
	 * Remove a {@link ConverterSetup} from this group.
	 *
	 * @param setup
	 * @return true, if this group is now empty. false otherwise.
	 */
	public boolean removeSetup( final ConverterSetup setup )
	{
		setups.remove( setup );

		if ( updateListener != null )
			updateListener.update();

		return setups.isEmpty();
	}

	/**
	 * Set an {@link UpdateListener} (usually a GUI component).
	 */
	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}
}
