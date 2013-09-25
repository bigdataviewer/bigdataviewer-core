package viewer.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.ActionMap;
import javax.swing.InputMap;

public final class InputActionBindings
{
	private final InputMap theInputMap;

	private final ActionMap theActionMap;

	public InputActionBindings()
	{
		theInputMap = new InputMap();
		theActionMap = new ActionMap();
		actions = new ArrayList< Actions >();
		inputs = new ArrayList< Keys >();
	}

	public void addActionMap( final String id, final ActionMap actionMap )
	{
		removeId( actions, id );
		if ( actionMap != null )
			actions.add( new Actions( id, actionMap ) );
		updateTheActionMap();
	}

	public void removeActionMap( final String id )
	{
		if ( removeId( actions, id ) )
			updateTheActionMap();
	}

	public void addInputMap( final String id, final InputMap inputMap, final String... idsToBlock )
	{
		addInputMap( id, inputMap, Arrays.asList( idsToBlock ) );
	}

	public void addInputMap( final String id, final InputMap inputMap, final Collection< String > idsToBlock )
	{
		removeId( inputs, id );
		if ( inputMap != null )
			inputs.add( new Keys( id, inputMap, idsToBlock ) );
		updateTheInputMap();
	}

	public void removeInputMap( final String id )
	{
		if ( removeId( inputs, id ) )
			updateTheInputMap();
	}

	public InputMap getConcatenatedInputMap()
	{
		return theInputMap;
	}

	public ActionMap getConcatenatedActionMap()
	{
		return theActionMap;
	}

	private interface WithId
	{
		public String getId();
	}

	private static class Actions implements WithId
	{
		private final String id;

		private final ActionMap actionMap;

		public Actions( final String id, final ActionMap actionMap )
		{
			this.id = id;
			this.actionMap = actionMap;
		}

		@Override
		public String getId()
		{
			return id;
		}

		public ActionMap getActionMap()
		{
			return actionMap;
		}
	}

	private static class Keys implements WithId
	{
		private final String id;

		private final InputMap inputMap;

		private final HashSet< String > idsToBlock;

		public Keys( final String id, final InputMap inputMap, final Collection< String > idsToBlock )
		{
			this.id = id;
			this.inputMap = inputMap;
			this.idsToBlock = new HashSet< String >( idsToBlock );
		}

		@Override
		public String getId()
		{
			return id;
		}

		public InputMap getInputMap()
		{
			return inputMap;
		}

		public Set< String > getKeysIdsToBlock()
		{
			return idsToBlock;
		}
	}

	private final List< Actions > actions;

	private final List< Keys > inputs;

	private static boolean removeId( final List< ? extends WithId > list, final String id )
	{
		for ( int i = 0; i < list.size(); ++i )
			if ( list.get( i ).getId().equals( id ) )
			{
				list.remove( i );
				return true;
			}
		return false;
	}

	private void updateTheActionMap()
	{
		final ListIterator< Actions > iter = actions.listIterator( actions.size() );
		ActionMap root = theActionMap;
		while ( iter.hasPrevious() )
		{
			final ActionMap map = iter.previous().getActionMap();
			if ( map != null )
			{
				root.setParent( map );
				root = map;
			}
		}
		root.setParent( null );
	}

	private void updateTheInputMap()
	{
		final ListIterator< Keys > iter = inputs.listIterator( inputs.size() );
		InputMap root = theInputMap;
		final HashSet< String > blocked = new HashSet< String >();
		while ( iter.hasPrevious() )
		{
			final Keys tool = iter.previous();

			if ( blocked.contains( tool.getId() ) )
				continue;

			final InputMap map = tool.getInputMap();
			if ( map != null )
			{
				root.setParent( map );
				root = map;
			}

			blocked.addAll( tool.getKeysIdsToBlock() );
			if ( blocked.contains( "all" ) )
				break;
		}
		root.setParent( null );
	}
}
