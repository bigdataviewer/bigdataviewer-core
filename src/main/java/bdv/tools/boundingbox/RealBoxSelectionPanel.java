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
package bdv.tools.boundingbox;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedIntervalDouble;

/**
 * A {@code JPanel} containing X,Y,Z,... min/max sliders for adjusting a
 * {@code RealInterval}
 *
 * @author Tobias Pietzsch
 * @author Christian Tischer
 */
public class RealBoxSelectionPanel extends JPanel
{
	/**
	 * Provides current interval and receives interval changes.
	 */
	public interface RealBox
	{
		RealInterval getInterval();

		void setInterval( RealInterval interval );
	}

	private static final long serialVersionUID = 1L;

	private final BoundedIntervalDouble[] ranges;

	private final SliderPanelDouble[] minSliderPanels;

	private final SliderPanelDouble[] maxSliderPanels;

	private final RealBox selection;

	private int cols;

	private final int n;

	public RealBoxSelectionPanel(
			final RealBox selection,
			final RealInterval rangeInterval )
	{
		n = selection.getInterval().numDimensions();
		this.selection = selection;
		ranges = new BoundedIntervalDouble[ n ];
		minSliderPanels = new SliderPanelDouble[ n ];
		maxSliderPanels = new SliderPanelDouble[ n ];

		cols = 2;
		for ( int d = 0; d < n; ++d )
		{
			cols = Math.max( cols, Double.toString( rangeInterval.realMin( d ) ).length() );
			cols = Math.max( cols, Double.toString( rangeInterval.realMax( d ) ).length() );
		}

		setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
		for ( int d = 0; d < n; ++d )
		{
			final double rangeMin = rangeInterval.realMin( d );
			final double rangeMax = rangeInterval.realMax( d );
			final RealInterval interval = selection.getInterval();
			final double initialMin = Math.max( interval.realMin( d ), rangeMin );
			final double initialMax = Math.min( interval.realMax( d ), rangeMax );
			final BoundedIntervalDouble range = new BoundedIntervalDouble( rangeMin, rangeMax, initialMin, initialMax, 0 )
			{
				@Override
				protected void updateInterval( final double min, final double max )
				{
					updateSelection();
				}
			};
			final JPanel sliders = new JPanel();
			sliders.setLayout( new BoxLayout( sliders, BoxLayout.PAGE_AXIS ) );
			final String axis = ( d == 0 ) ? "x" : ( d == 1 ) ? "y" : "z";
			final SliderPanelDouble minPanel = new SliderPanelDouble( axis + " min", range.getMinBoundedValue(), 1 );
			minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			minPanel.setNumColummns( cols );
			sliders.add( minPanel );
			final SliderPanelDouble maxPanel = new SliderPanelDouble( axis + " max", range.getMaxBoundedValue(), 1 );
			maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
			maxPanel.setNumColummns( cols );
			sliders.add( maxPanel );
			add( sliders );
			minSliderPanels[ d ] = minPanel;
			maxSliderPanels[ d ] = maxPanel;
			ranges[ d ] = range;
		}
	}

	public void setBoundsInterval( final RealInterval interval )
	{
		final int oldCols = cols;
		for ( int d = 0; d < n; ++d )
		{
			cols = Math.max( cols, Double.toString( interval.realMin( d ) ).length() );
			cols = Math.max( cols, Double.toString( interval.realMax( d ) ).length() );
		}

		for ( int d = 0; d < n; ++d )
		{
			ranges[ d ].setRange( ( int ) interval.realMin( d ), ( int ) interval.realMax( d ) );
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
		final double[] min = new double[ n ];
		final double[] max = new double[ n ];
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] = ranges[ d ].getMinBoundedValue().getCurrentValue();
			max[ d ] = ranges[ d ].getMaxBoundedValue().getCurrentValue();
		}
		selection.setInterval( new FinalRealInterval( min, max ) );
	}

	public void updateSliders( final RealInterval interval )
	{
		if ( interval.numDimensions() != n )
			throw new IllegalArgumentException();
		final double[] min = new double[ n ];
		final double[] max = new double[ n ];
		interval.realMin( min );
		interval.realMax( max );
		for ( int d = 0; d < n; ++d )
		{
			ranges[ d ].getMinBoundedValue().setCurrentValue( min[ d ] );
			ranges[ d ].getMaxBoundedValue().setCurrentValue( max[ d ] );
		}
	}
}
