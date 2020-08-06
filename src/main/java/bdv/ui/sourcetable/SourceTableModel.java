/*-
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
package bdv.ui.sourcetable;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.WrappedList;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceToConverterSetupBimap;
import bdv.viewer.ViewerState;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import net.imglib2.type.numeric.ARGBType;

import static gnu.trove.impl.Constants.DEFAULT_CAPACITY;
import static gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR;

/**
 * @author Tobias Pietzsch
 */
public class SourceTableModel extends AbstractTableModel
{
	private final ViewerState state;

	private final SourceToConverterSetupBimap converters;

	private StateModel model;

	public static final int NAME_COLUMN = 0;
	public static final int IS_CURRENT_COLUMN = 1;
	public static final int IS_ACTIVE_COLUMN = 2;
	public static final int COLOR_COLUMN = 3;

	public SourceTableModel( final ViewerState state, final ConverterSetups converterSetups )
	{
		this.state = state;
		this.converters = converterSetups;
		model = new StateModel( state );

		converterSetups.listeners().add( converterSetup ->
		{
			final SourceAndConverter< ? > source = converters.getSource( converterSetup );
			if ( source != null )
			{
				final SourceModel sourceModel = new SourceModel( source, state );
				SwingUtilities.invokeLater( () -> {
					final int row = model.getSources().indexOf( sourceModel );
					if ( row != -1 )
						fireTableRowsUpdated( row, row );
				} );
			}
		} );

		state.changeListeners().add( e ->
		{
			switch ( e )
			{
			case CURRENT_SOURCE_CHANGED:
			case SOURCE_ACTIVITY_CHANGED:
			case NUM_SOURCES_CHANGED:
				final StateModel model = new StateModel( state );
				SwingUtilities.invokeLater( () -> analyzeChanges( model ) );
			}
		} );
	}

	@Override
	public int getRowCount()
	{
		return model.getSources().size();
	}

	@Override
	public int getColumnCount()
	{
		return 4;
	}

	@Override
	public String getColumnName( final int column )
	{
		switch( column )
		{
		case NAME_COLUMN:
			return "name";
		case IS_ACTIVE_COLUMN:
			return "active";
		case IS_CURRENT_COLUMN:
			return "current";
		case COLOR_COLUMN:
			return "color";
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Object getValueAt( final int rowIndex, final int columnIndex )
	{
		final SourceModel source = model.getSources().get( rowIndex );
		switch( columnIndex )
		{
		case NAME_COLUMN:
			return source.getName();
		case IS_ACTIVE_COLUMN:
			return source.isActive();
		case IS_CURRENT_COLUMN:
			return source.isCurrent();
		case COLOR_COLUMN:
			final ConverterSetup c = converters.getConverterSetup( source.getSource() );
			return ( c != null && c.supportsColor() ) ? c.getColor() : null;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Class< ? > getColumnClass( final int columnIndex )
	{
		switch( columnIndex )
		{
		case NAME_COLUMN:
			return String.class;
		case IS_ACTIVE_COLUMN:
		case IS_CURRENT_COLUMN:
			return Boolean.class;
		case COLOR_COLUMN:
			return ARGBType.class;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean isCellEditable( final int rowIndex, final int columnIndex )
	{
		return columnIndex != 0;
	}

	public SourceModel getValueAt( final int rowIndex )
	{
		return model.getSources().get( rowIndex );
	}

	private void analyzeChanges( final StateModel model )
	{
		final StateModel previousModel = this.model;
		this.model = model;

		// -- NUM_SOURCES_CHANGED --

		final List< SourceModel > removedSources = new ArrayList<>();
		for ( SourceModel source : previousModel.getSources() )
			if ( !model.getSources().contains( source ) )
				removedSources.add( source );

		final List< SourceModel > addedSources = new ArrayList<>();
		for ( SourceModel source : model.getSources() )
			if ( !previousModel.getSources().contains( source ) )
				addedSources.add( source );

		// -- CURRENT_SOURCE_CHANGED, SOURCE_ACTIVITY_CHANGED --

		final List< SourceModel > changedSources = new ArrayList<>();
		for ( SourceModel source : model.getSources() )
		{
			final SourceModel previousSource = previousModel.getSources().get( source );
			if ( previousSource != null )
			{
				if ( source.isCurrent() != previousSource.isCurrent() ||
						source.isActive() != previousSource.isActive() )
				{
					changedSources.add( source );
				}
			}
		}

		// -- create corresponding TableModelEvents --

		// sources added or removed
		if ( !addedSources.isEmpty() )
		{
			final int firstRow = model.getSources().indexOf( addedSources.get( 0 ) );
			final int lastRow = model.getSources().indexOf( addedSources.get( addedSources.size() - 1 ) );
			fireTableRowsInserted( firstRow, lastRow );
		}
		else if ( !removedSources.isEmpty() )
		{
			final int firstRow = previousModel.getSources().indexOf( removedSources.get( 0 ) );
			final int lastRow = previousModel.getSources().indexOf( removedSources.get( removedSources.size() - 1 ) );
			fireTableRowsDeleted( firstRow, lastRow );
		}

		// sources that changed currentness or activeness
		if ( !changedSources.isEmpty() )
		{
			final int firstRow = model.getSources().indexOf( changedSources.get( 0 ) );
			final int lastRow = model.getSources().indexOf( changedSources.get( changedSources.size() - 1 ) );
			fireTableRowsUpdated( firstRow, lastRow );
		}
	}

	//
	//  Internal state model
	//

	private static final int NO_ENTRY_VALUE = -1;

	static class StateModel
	{
		private final StateModel.UnmodifiableSources sources;

		public StateModel( final ViewerState state )
		{
			final List< SourceModel > slist = new ArrayList<>();
			final TObjectIntMap< SourceModel > sindices = new TObjectIntHashMap<>( DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, NO_ENTRY_VALUE );
			final List< SourceAndConverter< ? > > ssources = state.getSources();
			for ( int i = 0; i < ssources.size(); ++i )
			{
				final SourceModel sourceModel = new SourceModel( ssources.get( i ), state );
				slist.add( sourceModel );
				sindices.put( sourceModel, i );
			}
			sources = new StateModel.UnmodifiableSources( slist, sindices );
		}

		public StateModel.UnmodifiableSources getSources()
		{
			return sources;
		}

		static class UnmodifiableSources extends WrappedList< SourceModel >
		{
			private final TObjectIntMap< SourceModel > sourceIndices;

			public UnmodifiableSources( final List< SourceModel > sources, final TObjectIntMap< SourceModel > sourceIndices )
			{
				super( Collections.unmodifiableList( sources ) );
				this.sourceIndices = sourceIndices;
			}

			public SourceModel get( SourceModel sourceModel )
			{
				final int index = sourceIndices.get( sourceModel );
				return index == NO_ENTRY_VALUE ? null : get( index );
			}

			@Override
			public boolean contains( final Object o )
			{
				return sourceIndices.containsKey( o );
			}

			@Override
			public boolean containsAll( final Collection< ? > c )
			{
				return sourceIndices.keySet().containsAll( c );
			}

			@Override
			public int indexOf( final Object o )
			{
				return sourceIndices.get( o );
			}

			@Override
			public int lastIndexOf( final Object o )
			{
				return sourceIndices.get( o );
			}
		}
	}

	static class SourceModel
	{
		private final String name;
		private final boolean active;
		private final boolean current;

		private final SourceAndConverter< ? > source;

		public SourceModel( final SourceAndConverter< ? > source, final ViewerState state )
		{
			name = source.getSpimSource().getName();
			active = state.isSourceActive( source );
			current = state.isCurrentSource( source );

			this.source = source;
		}

		public String getName()
		{
			return name;
		}

		public boolean isActive()
		{
			return active;
		}

		public boolean isCurrent()
		{
			return current;
		}

		public SourceAndConverter<?> getSource()
		{
			return source;
		}

		@Override
		public boolean equals( final Object o )
		{
			return ( o instanceof SourceModel ) && source.equals( ( ( SourceModel ) o ).source );
		}

		@Override
		public int hashCode()
		{
			return source.hashCode();
		}
	}
}
