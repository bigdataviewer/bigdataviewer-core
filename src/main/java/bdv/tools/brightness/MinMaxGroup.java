/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import bdv.util.BoundedInterval;
import bdv.util.BoundedValue;

/**
 * An <code>int</code> interval. The {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval are stored as
 * {@link BoundedValue}. The min and max can be changed to span any non-empty
 * interval within the range ({@link #getRangeMin()}, {@link #getRangeMax()}).
 * This range can be {@link #setRange(int, int) modified} as well.
 * <p>
 * Some {@link ConverterSetup ConverterSetups} can be
 * {@link #addSetup(ConverterSetup) linked}. They will have their display range
 * set according to {@link #getMinBoundedValue() min} and
 * {@link #getMaxBoundedValue() max} of the interval.
 * <p>
 * An {@link UpdateListener} (usually a GUI component) can be notified about
 * changes.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class MinMaxGroup extends BoundedInterval
{
	private final int fullRangeMin;

	private final int fullRangeMax;

	final Set< ConverterSetup > setups;

	public interface UpdateListener
	{
		public void update();
	}

	private UpdateListener updateListener;

	public MinMaxGroup( final int fullRangeMin, final int fullRangeMax, final int rangeMin, final int rangeMax, final int currentMin, final int currentMax )
	{
		super( rangeMin, rangeMax, currentMin, currentMax, 2 );
		this.fullRangeMin = fullRangeMin;
		this.fullRangeMax = fullRangeMax;
		setups = new LinkedHashSet< ConverterSetup >();
		updateListener = null;
	}

	@Override
	protected void updateInterval( final int min, final int max )
	{
		for ( final ConverterSetup setup : setups )
			setup.setDisplayRange( min, max );
	}

	public int getFullRangeMin()
	{
		return fullRangeMin;
	}

	public int getFullRangeMax()
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
