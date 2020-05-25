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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import net.imglib2.Interval;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import bdv.tools.boundingbox.BoxSelectionPanel.Box;
import bdv.tools.brightness.ConverterSetup.SetupChangeListener;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ModifiableInterval;
import bdv.util.RealRandomAccessibleSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;

// dialog to change bounding box
// while dialog is visible, bounding box is added as a source to the viewer

/**
 * @deprecated Use {@link TransformedBoxSelectionDialog} instead.
 */
@Deprecated
public class BoundingBoxDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	protected final ModifiableInterval interval;

	protected final BoxRealRandomAccessible< UnsignedShortType > boxRealRandomAccessible;

	protected final BoxSelectionPanel boxSelectionPanel;

	protected final SourceAndConverter< UnsignedShortType > boxSourceAndConverter;

	protected final RealARGBColorConverterSetup boxConverterSetup;

	protected final TransformedBoxOverlay boxOverlay;

	private boolean contentCreated = false;

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int boxSetupId,
			final Interval initialInterval,
			final Interval rangeInterval )
	{
		this( owner, title, viewer, setupAssignments, boxSetupId, initialInterval, rangeInterval, true, true );
	}

	public BoundingBoxDialog(
			final Frame owner,
			final String title,
			final ViewerPanel viewer,
			final SetupAssignments setupAssignments,
			final int boxSetupId,
			final Interval initialInterval,
			final Interval rangeInterval,
			final boolean showBoxSource,
			final boolean showBoxOverlay )
	{
		super( owner, title, false );

		// create a procedural RealRandomAccessible that will render the bounding box
		final UnsignedShortType insideValue = new UnsignedShortType( 1000 ); // inside the box pixel value is 1000
		final UnsignedShortType outsideValue = new UnsignedShortType( 0 ); // outside is 0
		interval = new ModifiableInterval( initialInterval );
		boxRealRandomAccessible = new BoxRealRandomAccessible<>( interval, insideValue, outsideValue );

		// create a bdv.viewer.Source providing data from the bbox RealRandomAccessible
		final RealRandomAccessibleSource< UnsignedShortType > boxSource = new RealRandomAccessibleSource< UnsignedShortType >( boxRealRandomAccessible, new UnsignedShortType(), "selection" )
		{
			@Override
			public Interval getInterval( final int t, final int level )
			{
				return interval;
			}
		};

		// set up a converter from the source type (UnsignedShortType in this case) to ARGBType
		final RealARGBColorConverter< UnsignedShortType > converter = RealARGBColorConverter.create( new UnsignedShortType(), 0, 3000 );
		converter.setColor( new ARGBType( 0x00994499 ) ); // set bounding box color to magenta

		// create a ConverterSetup (can be used by the brightness dialog to adjust the converter settings)
		boxConverterSetup = new RealARGBColorConverterSetup( boxSetupId, converter );
		final SetupChangeListener requestRepaint = s -> viewer.requestRepaint();
		boxConverterSetup.setupChangeListeners().add( requestRepaint );

		// create a SourceAndConverter (can be added to the viewer for display)
		final TransformedSource< UnsignedShortType > ts = new TransformedSource<>( boxSource );
		boxSourceAndConverter = new SourceAndConverter<>( ts, converter );

		// create an Overlay to show 3D wireframe box
		boxOverlay = new TransformedBoxOverlay( new TransformedBox()
		{
			@Override
			public Interval getInterval()
			{
				return interval;
			}

			@Override
			public void getTransform( final AffineTransform3D transform )
			{
				ts.getSourceTransform( 0, 0, transform );
			}
		} );

		// create a JPanel with sliders to modify the bounding box interval (boxRealRandomAccessible.getInterval())
		boxSelectionPanel = new BoxSelectionPanel(
				new Box()
				{
					@Override
					public void setInterval( final Interval i )
					{
						interval.set( i );
						viewer.requestRepaint();
					}

					@Override
					public Interval getInterval()
					{
						return interval;
					}
				},
				rangeInterval );

		// when dialog is made visible, add bbox source
		// when dialog is hidden, remove bbox source
		addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentShown( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.addSource( boxSourceAndConverter );
					setupAssignments.addSetup( boxConverterSetup );
					boxConverterSetup.setupChangeListeners().add( requestRepaint );

					final int bbSourceIndex = viewer.getState().numSources() - 1;
					final VisibilityAndGrouping vg = viewer.getVisibilityAndGrouping();
					if ( vg.getDisplayMode() != DisplayMode.FUSED )
					{
						for ( int i = 0; i < bbSourceIndex; ++i )
							vg.setSourceActive( i, vg.isSourceVisible( i ) );
						vg.setDisplayMode( DisplayMode.FUSED );
					}
					vg.setSourceActive( bbSourceIndex, true );
					vg.setCurrentSource( bbSourceIndex );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().overlays().add( boxOverlay );
					viewer.addRenderTransformListener( boxOverlay );
				}
			}

			@Override
			public void componentHidden( final ComponentEvent e )
			{
				if ( showBoxSource )
				{
					viewer.removeSource( boxSourceAndConverter.getSpimSource() );
					setupAssignments.removeSetup( boxConverterSetup );
					boxConverterSetup.setupChangeListeners().remove( requestRepaint );
				}
				if ( showBoxOverlay )
				{
					viewer.getDisplay().overlays().remove( boxOverlay );
					viewer.removeTransformListener( boxOverlay );
				}
			}
		} );


		// make ESC key hide dialog
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );

		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
//		createContent();
	}

	@Override
	public void setVisible( final boolean b )
	{
		if ( b && !contentCreated )
		{
			createContent();
			contentCreated = true;
		}
		super.setVisible( b );
	}

	// Override in subclasses
	public void createContent()
	{
		getContentPane().add( boxSelectionPanel, BorderLayout.NORTH );
		pack();
	}
}
