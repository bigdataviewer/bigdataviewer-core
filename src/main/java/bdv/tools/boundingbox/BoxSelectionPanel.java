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
package bdv.tools.boundingbox;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;

import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedInterval;

/**
 * A {@code JPanel} containing X,Y,Z,... min/max sliders for adjusting a
 * {@code Interval}
 *
 * @author Tobias Pietzsch
 */
public class BoxSelectionPanel extends JPanel
{
	/**
	 * Provides current interval and receives interval changes.
	 */
	public interface Box
	{
		Interval getInterval();

		void setInterval( Interval interval );
	}

	private static final long serialVersionUID = 1L;

	private final BoundedInterval[] ranges;

	private final SliderPanel[] minSliderPanels;

	private final SliderPanel[] maxSliderPanels;

	private final Box selection;

	private int cols;

	private final int n;

	public BoxSelectionPanel( final Box selection, final Interval rangeInterval )
	{
		n = selection.getInterval().numDimensions();
		this.selection = selection;
		ranges = new BoundedInterval[ n ];
		minSliderPanels = new SliderPanel[ n ];
		maxSliderPanels = new SliderPanel[ n ];

		cols = 2;
		for ( int d = 0; d < n; ++d )
		{
			cols = Math.max( cols, Long.toString( rangeInterval.min( d ) ).length() );
			cols = Math.max( cols, Long.toString( rangeInterval.max( d ) ).length() );
		}

		setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
		for ( int d = 0; d < n; ++d )
		{
			final int rangeMin = ( int ) rangeInterval.min( d );
			final int rangeMax = ( int ) rangeInterval.max( d );
			final Interval interval = selection.getInterval();
			final int initialMin = Math.max( ( int ) interval.min( d ), rangeMin );
			final int initialMax = Math.min( ( int ) interval.max( d ), rangeMax );
			final BoundedInterval range = new BoundedInterval( rangeMin, rangeMax, initialMin, initialMax, 0 )
			{
				@Override
				protected void updateInterval( final int min, final int max )
				{
					updateSelection();
				}
			};
			final JPanel sliders = new JPanel();
			sliders.setLayout( new BoxLayout( sliders, BoxLayout.PAGE_AXIS ) );
			final String axis = ( d == 0 ) ? "x" : ( d == 1 ) ? "y" : "z";
			final SliderPanel minPanel = new SliderPanel( axis + " min", range.getMinBoundedValue(), 1 );
			minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			minPanel.setNumColummns( cols );
			sliders.add( minPanel );
			final SliderPanel maxPanel = new SliderPanel( axis + " max", range.getMaxBoundedValue(), 1 );
			maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			maxPanel.setNumColummns( cols );
			sliders.add( maxPanel );
			add( sliders );
			minSliderPanels[ d ] = minPanel;
			maxSliderPanels[ d ] = maxPanel;
			ranges[ d ] = range;
		}
	}

	public void setBoundsInterval( final Interval interval )
	{
		final int oldCols = cols;
		for ( int d = 0; d < n; ++d )
		{
			cols = Math.max( cols, Long.toString( interval.min( d ) ).length() );
			cols = Math.max( cols, Long.toString( interval.max( d ) ).length() );
		}

		for ( int d = 0; d < n; ++d )
		{
			ranges[ d ].setRange( ( int ) interval.min( d ), ( int ) interval.max( d ) );
		}

		if ( oldCols != cols )
		{
			for ( int d = 0; d < n; ++d )
			{
				minSliderPanels[ d ].setNumColummns( cols );
				maxSliderPanels[ d ].setNumColummns( cols );
			}
			invalidate();
		}
	}

	public void updateSelection()
	{
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = ranges[ d ].getMinBoundedValue().getCurrentValue();
			max[ d ] = ranges[ d ].getMaxBoundedValue().getCurrentValue();
		}
		selection.setInterval( new FinalInterval( min, max ) );
	}

	public void updateSliders( final Interval interval )
	{
		if ( interval.numDimensions() != n )
			throw new IllegalArgumentException();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		interval.min( min );
		interval.max( max );
		for ( int d = 0; d < n; ++d )
		{
			ranges[ d ].getMinBoundedValue().setCurrentValue( ( int ) min[ d ] );
			ranges[ d ].getMaxBoundedValue().setCurrentValue( ( int ) max[ d ] );
		}
	}
}
