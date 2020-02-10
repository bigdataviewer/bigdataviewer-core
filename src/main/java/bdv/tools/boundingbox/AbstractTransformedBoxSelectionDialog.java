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

import static bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection.RANGE;
import static bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection.SINGLE;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import bdv.tools.boundingbox.BoxSelectionOptions.TimepointSelection;
import bdv.tools.brightness.SliderPanel;
import bdv.util.BoundedInterval;
import bdv.util.BoundedValue;
import bdv.viewer.ViewerPanel;

/**
 * @author Tobias Pietzsch
 */
public abstract class AbstractTransformedBoxSelectionDialog< R > extends JDialog
{
	private static final long serialVersionUID = 1L;

	protected final OkCancelPanel buttons;

	protected R result;

	private final Object monitor = new Object();

	public AbstractTransformedBoxSelectionDialog(
			final Window owner,
			final String title )
	{
		super( owner, title );

		buttons = new OkCancelPanel();
		buttons.onOk( this::ok );
		buttons.onCancel( this::cancel );
		getContentPane().add( buttons, BorderLayout.SOUTH );

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				cancel();
			}
		} );
	}

	private void cancel()
	{
		setVisible( false );
		synchronized ( monitor )
		{
			createResult( false );
			monitor.notifyAll();
		}
	}

	private void ok()
	{
		setVisible( false );
		synchronized ( monitor )
		{
			createResult( true );
			monitor.notifyAll();
		}
	}

	/**
	 * Makes the dialog visible and waits for the user to close the dialog,
	 * click ok, or cancel.
	 *
	 * @return the interval etc selected by the user
	 */
	public R getResult()
	{
		synchronized ( monitor )
		{
			result = null;
			setVisible( true );
			while( true )
			{
				try
				{
					monitor.wait();
					break;
				}
				catch ( final InterruptedException e )
				{
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	/**
	 * @param status
	 *            {@code true} if ok was clicked, {@code false} if cancelled.
	 */
	protected abstract void createResult( boolean status );

	/**
	 * A panel that contains sliders to adjust timepoint, timepoint range, or
	 * nothing.
	 */
	public static class TimepointSelectionPanel extends JPanel
	{
		final BoundedInterval timeRange;

		final BoundedValue timeValue;

		final TimepointSelection mode;

		public TimepointSelectionPanel( final ViewerPanel viewer, final BoxSelectionOptions options )
		{
			setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );

			mode = options.values.getTimepointSelection();
			if ( mode == SINGLE || mode == RANGE )
			{
				final int tmaxViewer = viewer.state().getNumTimepoints() - 1;
				final int tmin = Math.max( 0, Math.min( tmaxViewer, options.values.getRangeMinTimepoint() ) );
				final int tmax = Math.max( tmin, Math.min( tmaxViewer, options.values.getRangeMaxTimepoint() ) );;

				if ( mode == SINGLE )
				{
					final int t0 = Math.max( tmin, Math.min( tmax, options.values.getInitialMinTimepoint() ) );
					timeValue = new BoundedValue( tmin, tmax, t0 );
					timeRange = null;
					final SliderPanel timePanel = new SliderPanel( "t", timeValue, 1 );
					timePanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
					add( timePanel );
				}
				else // tpsel == RANGE
				{
					final int t0 = Math.max( tmin, Math.min( tmax, options.values.getInitialMinTimepoint() ) );
					final int t1 = Math.max( t0, Math.min( tmax, options.values.getInitialMaxTimepoint() ) );
					timeRange = new BoundedInterval( tmin, tmax, t0, t1, 0 );
					timeValue = null;
					final SliderPanel minPanel = new SliderPanel( "t min", timeRange.getMinBoundedValue(), 1 );
					minPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
					add( minPanel );
					final SliderPanel maxPanel = new SliderPanel( "t max", timeRange.getMaxBoundedValue(), 1 );
					maxPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
					add( maxPanel );
				}
			}
			else // tpsel == NONE
			{
				timeValue = null;
				timeRange = null;
			}
		}
	}

	/**
	 * A panel containing OK and Cancel buttons, and callback lists for both.
	 */
	public static class OkCancelPanel extends JPanel
	{
		private final ArrayList< Runnable > runOnOk = new ArrayList<>();

		private final ArrayList< Runnable > runOnCancel = new ArrayList<>();

		public OkCancelPanel()
		{
			final JButton cancelButton = new JButton( "Cancel" );
			cancelButton.addActionListener( e -> runOnCancel.forEach( Runnable::run ) );

			final JButton okButton = new JButton( "OK" );
			okButton.addActionListener( e -> runOnOk.forEach( Runnable::run ) );

			setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );
			add( Box.createHorizontalGlue() );
			add( cancelButton );
			add( okButton );
		}

		public synchronized void onOk( final Runnable runnable )
		{
			runOnOk.add( runnable );
		}

		public synchronized void onCancel( final Runnable runnable )
		{
			runOnCancel.add( runnable );
		}
	}
}
