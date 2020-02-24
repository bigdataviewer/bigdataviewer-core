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

import bdv.viewer.ConverterSetups;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.util.Intervals;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.viewer.ViewerPanel;

/**
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class TransformedRealBoxSelectionDialog extends AbstractTransformedBoxSelectionDialog< TransformedRealBoxSelectionDialog.Result >
{
	private static final long serialVersionUID = 1L;

	protected final TransformedRealBoxModel model;

	protected final TransformedBoxEditor boxEditor;

	protected final JPanel content;

	protected final ViewerPanel viewer;

	protected final RealBoxSelectionPanel boxSelectionPanel;

	protected final TimepointSelectionPanel timepointSelectionPanel;

	protected final BoxSelectionOptions options;

	public TransformedRealBoxSelectionDialog(
			final ViewerPanel viewer,
			final ConverterSetups converterSetups,
			final int setupId,
			final InputTriggerConfig keyConfig,
			final TriggerBehaviourBindings triggerbindings,
			final AffineTransform3D boxTransform,
			final RealInterval initialInterval,
			final RealInterval rangeInterval,
			final BoxSelectionOptions options )
	{
		super( SwingUtilities.getWindowAncestor( viewer ), options.values.getTitle() );
		this.viewer = viewer;
		this.options = options;

		// TODO clip initialInterval if out of range, set to something reasonable if intersection is empty

		/*
		 * Initialize box model with the initial interval and transform.
		 */
		model = new TransformedRealBoxModel(
				initialInterval,
				boxTransform );

		/*
		 * Create box overlay and editor.
		 */
		boxEditor = new TransformedBoxEditor(
				keyConfig,
				viewer,
				converterSetups,
				setupId,
				triggerbindings,
				model );
		boxEditor.setPerspective( 1, 1000 ); // TODO expose, initialize from rangeInterval
		boxEditor.setEditable( true );

		boxSelectionPanel = new RealBoxSelectionPanel( model, rangeInterval );
		timepointSelectionPanel = new TimepointSelectionPanel( viewer, options );

		content = createContent();
		getContentPane().add( content, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				boxEditor.install();
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				boxEditor.uninstall();
			}
		} );
		model.intervalChangedListeners().add( () -> {
			boxSelectionPanel.updateSliders( model.getInterval() );
			viewer.getDisplay().repaint();
		} );
	}

	/**
	 * Adds {@link #boxSelectionPanel} etc to {@link #content}.
	 * Override in subclasses to add more / different stuff.
	 */
	protected JPanel createContent()
	{
		final JPanel content = new JPanel();

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 80 };
		layout.columnWeights = new double[] { 1. };
		content.setLayout( layout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets( 5, 5, 5, 5 );

		final JLabel lblTitle = new JLabel( "Selection:" );
		lblTitle.setFont( content.getFont().deriveFont( Font.BOLD ) );
		content.add( lblTitle, gbc );

		gbc.gridy++;
		final JPanel boundsPanel = new JPanel();
		boundsPanel.setLayout( new BoxLayout( boundsPanel, BoxLayout.PAGE_AXIS ) );
		boundsPanel.add( boxSelectionPanel );
		if ( timepointSelectionPanel.mode != NONE )
			boundsPanel.add( timepointSelectionPanel );
		content.add( boundsPanel, gbc );

		gbc.gridy++;
		gbc.anchor = GridBagConstraints.BASELINE_LEADING;
		gbc.fill = GridBagConstraints.NONE;
		final BoxDisplayModePanel boxModePanel = new BoxDisplayModePanel( boxEditor.boxDisplayMode() );
		content.add( boxModePanel, gbc );

		return content;
	}

	/**
	 * @param status
	 *            {@code true} if ok was clicked, {@code false} if cancelled.
	 */
	@Override
	protected void createResult( final boolean status )
	{
		final int t0, t1;
		switch ( timepointSelectionPanel.mode )
		{
		case NONE:
		default:
			t0 = t1 = 0;
			break;
		case SINGLE:
			t0 = t1 = timepointSelectionPanel.timeValue.getCurrentValue();
			break;
		case RANGE:
			t0 = timepointSelectionPanel.timeRange.getMinBoundedValue().getCurrentValue();
			t1 = timepointSelectionPanel.timeRange.getMaxBoundedValue().getCurrentValue();
			break;
		}
		result = new Result( status, model, t0, t1 );
	}

	public static class Result implements TransformedBox
	{
		private final boolean valid;

		private final RealInterval interval;

		private final int t0;

		private final int t1;

		private final AffineTransform3D transform;

		public Result( final boolean valid, final TransformedBox model, final int t0, final int t1 )
		{
			this.valid = valid;
			this.interval = new FinalRealInterval( model.getInterval() );
			this.t0 = t0;
			this.t1 = t1;
			this.transform = new AffineTransform3D();
			model.getTransform( transform );
		}

		/**
		 * Returns {@code true} iff dialog was closed by clicking "OK".
		 */
		public boolean isValid()
		{
			return valid;
		}

		/**
		 * Returns the selected interval.
		 */
		@Override
		public RealInterval getInterval()
		{
			return interval;
		}

		/**
		 * Stores the box transform into {@code t}.
		 */
		@Override
		public void getTransform( final AffineTransform3D t )
		{
			t.set( transform );
		}

		public int getMinTimepoint()
		{
			return t0;
		}

		public int getMaxTimepoint()
		{
			return t1;
		}

		/**
		 * Returns the transformed box as an imglib2 roi.
		 */
		public RealMaskRealInterval asMask()
		{
			return GeomMasks.closedBox( Intervals.minAsDoubleArray( interval ), Intervals.maxAsDoubleArray( interval ) ).transform( transform.inverse() );
		}
	}
}
