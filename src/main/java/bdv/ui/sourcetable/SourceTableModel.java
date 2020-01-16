package bdv.ui.sourcetable;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.BasicViewerState;
import bdv.viewer.ConverterSetups;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceToConverterSetupBimap;
import bdv.viewer.ViewerState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import javax.swing.table.AbstractTableModel;
import net.imglib2.type.numeric.ARGBType;

/**
 * @author Tobias Pietzsch
 */
public class SourceTableModel extends AbstractTableModel
{
	private final ViewerState state;

	private final BasicViewerState previousState;

	private final SourceToConverterSetupBimap converters;

	public static final int NAME_COLUMN = 0;
	public static final int IS_ACTIVE_COLUMN = 1;
	public static final int IS_CURRENT_COLUMN = 2;
	public static final int COLOR_COLUMN = 3;

	public SourceTableModel( final ViewerState state, final ConverterSetups converterSetups )
	{
		this.state = state;
		previousState = new BasicViewerState( state );
		this.converters = converterSetups;

		converterSetups.listeners().add( converterSetup ->
		{
			final SourceAndConverter< ? > source = converters.getSource( converterSetup );
			if ( source != null )
			{
				final int row = state.getSources().indexOf( source );
				if ( row != -1 )
					fireTableRowsUpdated( row, row );
			}
		} );

		state.changeListeners().add( e ->
		{
			switch ( e )
			{
			case CURRENT_SOURCE_CHANGED:
			case SOURCE_ACTIVITY_CHANGED:
			case NUM_SOURCES_CHANGED:
				analyzeChanges();
			}
		} );
	}

	private void analyzeChanges()
	{
		// -- NUM_SOURCES_CHANGED --

		final HashSet< SourceAndConverter< ? > > removedSources = new HashSet<>( previousState.getSources() );
		removedSources.removeAll( state.getSources() );

		final HashSet< SourceAndConverter< ? > > addedSources = new HashSet<>( state.getSources() );
		addedSources.removeAll( previousState.getSources() );

		final HashSet< SourceAndConverter< ? > > changedSources = new HashSet<>();

		// -- CURRENT_SOURCE_CHANGED --

		final SourceAndConverter< ? > prevCurrent = previousState.getCurrentSource();
		final SourceAndConverter< ? > curCurrent = state.getCurrentSource();
		if ( !Objects.equals( prevCurrent, curCurrent ) )
		{
			if ( prevCurrent != null )
				changedSources.add( prevCurrent );
			if ( curCurrent != null )
				changedSources.add( curCurrent );
		}

		// -- SOURCE_ACTIVITY_CHANGED --

		for ( SourceAndConverter< ? > source : state.getSources() )
		{
			if ( previousState.getSources().contains( source ) )
			{
				final boolean wasActive = previousState.isSourceActive( source );
				final boolean isActive = state.isSourceActive( source );
				if ( wasActive != isActive )
					changedSources.add( source );
			}
		}

		// -- create corresponding TreeModelEvents --

		// sources added or removed
		if ( !addedSources.isEmpty() )
		{
			final ArrayList< SourceAndConverter< ? > > list = new ArrayList<>( addedSources );
			list.sort( state.sourceOrder() );
			final int firstRow = state.getSources().indexOf( list.get( 0 ) );
			final int lastRow = state.getSources().indexOf( list.get( list.size() - 1 ) );
			fireTableRowsInserted( firstRow, lastRow );
		}
		else if ( !removedSources.isEmpty() )
		{
			final ArrayList< SourceAndConverter< ? > > list = new ArrayList<>( removedSources );
			list.sort( previousState.sourceOrder() );
			final int firstRow = state.getSources().indexOf( list.get( 0 ) );
			final int lastRow = state.getSources().indexOf( list.get( list.size() - 1 ) );
			fireTableRowsDeleted( firstRow, lastRow );
		}

		// sources that changed currentness or activeness
		if ( !changedSources.isEmpty() )
		{
			final ArrayList< SourceAndConverter< ? > > list = new ArrayList<>( changedSources );
			list.sort( state.sourceOrder() );
			final int firstRow = state.getSources().indexOf( list.get( 0 ) );
			final int lastRow = state.getSources().indexOf( list.get( list.size() - 1 ) );
			fireTableRowsUpdated( firstRow, lastRow );
		}

		previousState.set( state );
	}

	@Override
	public int getRowCount()
	{
		return state.getSources().size();
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
		final SourceAndConverter< ? > source = state.getSources().get( rowIndex );
		switch( columnIndex )
		{
		case NAME_COLUMN:
			return source.getSpimSource().getName();
		case IS_ACTIVE_COLUMN:
			return state.isSourceActive( source );
		case IS_CURRENT_COLUMN:
			return state.isCurrentSource( source );
		case COLOR_COLUMN:
			final ConverterSetup c = converters.getConverterSetup( source );
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

	public SourceAndConverter< ? > getValueAt( final int rowIndex )
	{
		return state.getSources().get( rowIndex );
	}
}
