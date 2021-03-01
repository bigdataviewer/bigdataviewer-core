/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.ui.sourcetable;

import bdv.tools.brightness.ConverterSetup;
import bdv.ui.SourcesTransferable;
import bdv.ui.UIUtils;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceToConverterSetupBimap;
import bdv.viewer.ViewerState;
import com.formdev.flatlaf.ui.FlatUIUtils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import static bdv.ui.sourcetable.SourceTableModel.COLOR_COLUMN;
import static bdv.ui.sourcetable.SourceTableModel.IS_ACTIVE_COLUMN;
import static bdv.ui.sourcetable.SourceTableModel.IS_CURRENT_COLUMN;

/**
 * A {@code JTable} that shows the sources of a {@link ViewerState} and allows
 * to modify activeness, currentness, as well as the color of the corresponding
 * {@link ConverterSetup}.
 *
 * @author Tobias Pietzsch
 */
public class SourceTable extends JTable
{
	private final SourceTableModel model;

	private final ViewerState state;

	private final SourceToConverterSetupBimap converters;

	private Color selectionActiveBackground;
	private Color selectionInactiveBackground;
	private Color selectionActiveForeground;
	private Color selectionInactiveForeground;

	public SourceTable( final ViewerState state, final ConverterSetups converterSetups )
	{
		this( state, converterSetups, new InputTriggerConfig() );
	}

	public SourceTable( final ViewerState state, final ConverterSetups converterSetups, final InputTriggerConfig inputTriggerConfig )
	{
		this.state = state;
		converters = converterSetups;
		model = new SourceTableModel( state, converterSetups );
		setModel( model );
		setTransferHandler( new SourceTableTransferHandler() );

		getColumnModel().getColumn( IS_CURRENT_COLUMN ).setCellRenderer( new RadioButtonRenderer() );
		getColumnModel().getColumn( COLOR_COLUMN ).setCellRenderer( new ColorRenderer() );
		setRowHeight( ( int )Math.round( UIManager.getDefaults().getFont( "Table.font" ).getSize() * 1.5 ) );

		setShowGrid( false );

		getColumnModel().getColumn( IS_CURRENT_COLUMN ).setMinWidth( 20 );
		getColumnModel().getColumn( IS_ACTIVE_COLUMN ).setMinWidth( 20 );
		getColumnModel().getColumn( COLOR_COLUMN ).setMinWidth( 40 );

		this.installActions( inputTriggerConfig );

		this.addFocusListener( new FocusListener()
		{
			@Override
			public void focusGained( final FocusEvent e )
			{
				repaint();
			}

			@Override
			public void focusLost( final FocusEvent e )
			{
				repaint();
			}
		} );
	}

	@Override
	public void updateUI()
	{
		super.updateUI();
		updateColors();
	}

	private void updateColors()
	{
		selectionActiveBackground = UIManager.getColor( "Table.selectionBackground" );
		selectionInactiveBackground = FlatUIUtils.getUIColor(
				"Table.selectionInactiveBackground",
				UIUtils.mix( selectionActiveBackground, UIManager.getColor( "Table.background" ), 0.5)
		);
		selectionActiveForeground = UIManager.getColor( "Table.selectionForeground" );
		selectionInactiveForeground = FlatUIUtils.getUIColor(
				"Table.selectionInactiveForeground",
				UIUtils.mix( selectionActiveForeground, UIManager.getColor( "Table.foreground" ), 0.0 )
		);
	}

	@Override
	protected void paintComponent( final Graphics g )
	{
		final boolean hasFocus = FlatUIUtils.isPermanentFocusOwner( this );
		selectionBackground = hasFocus ? selectionActiveBackground : selectionInactiveBackground;
		selectionForeground = hasFocus ? selectionActiveForeground : selectionInactiveForeground;
		super.paintComponent( g );
	}

	public List< SourceAndConverter< ? > > getSelectedSources()
	{
		final List< SourceAndConverter< ? > > sources = new ArrayList<>();
		for ( final int row : getSelectedRows() )
			sources.add( model.getValueAt( row ).getSource() );
		return sources;
	}

	public List< ConverterSetup > getSelectedConverterSetups()
	{
		return converters.getConverterSetups( getSelectedSources() );
	}

	private void installActions( final InputTriggerConfig inputTriggerConfig )
	{
		final InputActionBindings keybindings = InputActionBindings.installNewBindings( this, JComponent.WHEN_FOCUSED, false );
		final Actions actions = new Actions( inputTriggerConfig, "bdv" );
		actions.install( keybindings, "source table" );
		actions.runnableAction( () -> toggleSelectedActive(), "toggle active", "A" );
		actions.runnableAction( () -> makeSelectedActive( true ), "set active", "not mapped" );
		actions.runnableAction( () -> makeSelectedActive( false ), "set inactive", "not mapped" );
		actions.runnableAction( () -> cycleSelectedCurrent(), "cycle current", "C" );
	}

	private void makeSelectedActive( final boolean active )
	{
		final List< SourceAndConverter< ? > > selectedSources = getSelectedSources();
		state.setSourcesActive( selectedSources, active );
	}

	private void toggleSelectedActive()
	{
		final List< SourceAndConverter< ? > > selectedSources = getSelectedSources();
		if ( !selectedSources.isEmpty() )
			state.setSourcesActive( selectedSources, !state.isSourceActive( selectedSources.get( 0 ) ) );
	}

	private void cycleSelectedCurrent()
	{
		final List< SourceAndConverter< ? > > selectedSources = getSelectedSources();
		if ( !selectedSources.isEmpty() )
		{
			final SourceAndConverter< ? > current = state.getCurrentSource();
			final int i = ( selectedSources.indexOf( current ) + 1 ) % selectedSources.size();
			state.setCurrentSource( selectedSources.get( i ) );
		}
	}

	// -- Process clicks on active and current checkboxes --
	// These clicks are consumed, because they should not cause selection changes, etc, in the table.

	private Point pressedAt;
	private boolean consumeNext = false;
	private long releasedWhen = 0;

	@Override
	protected void processMouseEvent( final MouseEvent e )
	{
		if ( e.getModifiers() == InputEvent.BUTTON1_MASK )
		{
			if ( e.getID() == MouseEvent.MOUSE_PRESSED )
			{
				final Point point = e.getPoint();
				pressedAt = point;
				final int vcol = columnAtPoint( point );
				final int vrow = rowAtPoint( point );
				if ( vcol >= 0 && vrow >= 0 )
				{
					final int mcol = convertColumnIndexToModel( vcol );
					switch ( mcol )
					{
					case IS_ACTIVE_COLUMN:
					case IS_CURRENT_COLUMN:
					case COLOR_COLUMN:
						final int mrow = convertRowIndexToModel( vrow );
						if ( isRowSelected( mrow ) )
						{
							e.consume();
							consumeNext = true;
						}
					}
				}
			}
			else if ( e.getID() == MouseEvent.MOUSE_RELEASED )
			{
				if ( consumeNext )
				{
					releasedWhen = e.getWhen();
					consumeNext = false;
					e.consume();
				}

				if ( pressedAt == null )
					return;

				final Point point = e.getPoint();
				if ( point.distanceSq( pressedAt ) > 2 )
					return;

				final int vcol = columnAtPoint( point );
				final int vrow = rowAtPoint( point );
				if ( vcol >= 0 && vrow >= 0 )
				{
					final int mcol = convertColumnIndexToModel( vcol );
					switch ( mcol )
					{
					case IS_ACTIVE_COLUMN:
					case IS_CURRENT_COLUMN:
					case COLOR_COLUMN:
						final int mrow = convertRowIndexToModel( vrow );
						final SourceAndConverter< ? > source = model.getValueAt( mrow ).getSource();
						if ( mcol == IS_ACTIVE_COLUMN )
						{
							if ( isRowSelected( mrow ) )
								state.setSourcesActive( getSelectedSources(), !state.isSourceActive( source ) );
							else
								state.setSourceActive( source, !state.isSourceActive( source ) );
						}
						else if ( mcol == IS_CURRENT_COLUMN )
						{
							state.setCurrentSource( source );
						}
						else // if ( mcol == COLOR_COLUMN )
						{
							converters.getConverterSetup( source );
							final ConverterSetup c = converters.getConverterSetup( source );
							if ( c != null && c.supportsColor() )
							{
								final Color newColor = JColorChooser.showDialog( null, "Set Source Color", new Color( c.getColor().get() ) );
								if ( newColor != null )
								{
									final ARGBType color = new ARGBType( newColor.getRGB() | 0xff000000 );
									if ( isRowSelected( mrow ) )
									{
										for ( final SourceAndConverter< ? > s : getSelectedSources() )
										{
											final ConverterSetup cs = converters.getConverterSetup( s );
											if ( cs != null && cs.supportsColor() )
												cs.setColor( color );
										}
									}
									else
										c.setColor( color );
								}
							}
						}
					}
				}
			}
			else if ( e.getID() == MouseEvent.MOUSE_CLICKED )
			{
				if ( e.getWhen() == releasedWhen )
					e.consume();
			}
		}
		super.processMouseEvent( e );
	}

	@Override
	protected void processMouseMotionEvent( final MouseEvent e )
	{
		if ( consumeNext && e.getModifiers() == InputEvent.BUTTON1_MASK && e.getID() == MouseEvent.MOUSE_DRAGGED )
			e.consume();
		super.processMouseMotionEvent( e );
	}

	@Override
	public SourceTableModel getModel()
	{
		return model;
	}

	/**
	 * {@code TransferHandler} for transferring sources out of a
	 * {@code SourceTable} via cut/copy/paste and drag and drop.
	 */
	static class SourceTableTransferHandler extends TransferHandler
	{
		@Override
		public int getSourceActions( final JComponent c )
		{
			return TransferHandler.LINK;
		}

		@Override
		protected Transferable createTransferable( final JComponent c )
		{
			if ( ! ( c instanceof JTable ) )
				return null;

			final JTable table = ( JTable ) c;

			if ( ! ( table.getModel() instanceof SourceTableModel ) )
				return null;

			final SourceTableModel model = ( SourceTableModel ) table.getModel();

			final List< SourceAndConverter< ? > > sources = new ArrayList<>();
			for ( final int row : table.getSelectedRows() )
				sources.add( model.getValueAt( row ).getSource() );

			return new SourcesTransferable( sources );
		}

		@Override
		public boolean canImport( final TransferSupport support )
		{
			return false;
		}
	}
}
