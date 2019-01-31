/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
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

import static bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection.NONE;
import static bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection.RANGE;
import static bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection.SINGLE;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

/**
 * Optional parameters for {@link TransformedBoxSelectionDialog} and
 * {@link TransformedRealBoxSelectionDialog}.
 *
 * @author Tobias Pietzsch
 */
public class BoxSelectionOptions
{
	public final Values values = new Values();

	/**
	 * Create default {@link BoxSelectionOptions}.
	 *
	 * @return default {@link BoxSelectionOptions}.
	 */
	public static BoxSelectionOptions options()
	{
		return new BoxSelectionOptions();
	}

	public enum TimepointSelection
	{
		NONE,
		SINGLE,
		RANGE
	}

	public BoxSelectionOptions selectSingleTimepoint( final int rangeMin, final int rangeMax )
	{
		values.timepointSelection = SINGLE;
		values.rangeMinTimepoint = rangeMin;
		values.rangeMaxTimepoint = rangeMax;
		return this;
	}

	public BoxSelectionOptions selectSingleTimepoint()
	{
		values.timepointSelection = SINGLE;
		return this;
	}

	public BoxSelectionOptions initialTimepoint( final int t )
	{
		values.initialMinTimepoint = t;
		return this;
	}

	public BoxSelectionOptions initialTimepointRange( final int initialMin, final int initialMax )
	{
		values.initialMinTimepoint = initialMin;
		values.initialMaxTimepoint = initialMax;
		return this;
	}

	public BoxSelectionOptions selectTimepointRange( final int rangeMin, final int rangeMax )
	{
		values.timepointSelection = RANGE;
		values.rangeMinTimepoint = rangeMin;
		values.rangeMaxTimepoint = rangeMax;
		return this;
	}

	public BoxSelectionOptions selectTimepointRange()
	{
		values.timepointSelection = RANGE;
		return this;
	}

	public BoxSelectionOptions title( final String title )
	{
		values.title = title;
		return this;
	}

	/**
	 * Read-only {@link BoxSelectionOptions} values.
	 */
	public static class Values
	{
		private TimepointSelection timepointSelection = NONE;

		private int rangeMinTimepoint = 0;

		private int rangeMaxTimepoint = Integer.MAX_VALUE;

		private int initialMinTimepoint = 0;

		private int initialMaxTimepoint = Integer.MAX_VALUE;

		private String title = "Select Bounding Box";

		private final InputTriggerConfig inputTriggerConfig = null;

		public TimepointSelection getTimepointSelection()
		{
			return timepointSelection;
		}

		public int getRangeMinTimepoint()
		{
			return rangeMinTimepoint;
		}

		public int getRangeMaxTimepoint()
		{
			return rangeMaxTimepoint;
		}

		public int getInitialMinTimepoint()
		{
			return initialMinTimepoint;
		}

		public int getInitialMaxTimepoint()
		{
			return initialMaxTimepoint;
		}

		public String getTitle()
		{
			return title;
		}

		public InputTriggerConfig getInputTriggerConfig()
		{
			return inputTriggerConfig;
		}
	}
}
