/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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
package bdv.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.ActionMap;
import javax.swing.InputMap;

/**
 * Maintains lists of {@link ActionMap}s and {@link InputMap}s, which are
 * chained to a {@link #getConcatenatedInputMap() concatenated InputMap} and a
 * {@link #getConcatenatedActionMap() concatenated ActionMap}.
 * Maps can be added and will be chained in reverse order of addition, that is, the last added map overrides all previous ones.
 * For {@link InputMap}s it is possible to block maps that were added earlier.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public final class InputActionBindings
{
	/**
	 * the root of the {@link InputMap} chain.
	 */
	private final InputMap theInputMap;

	/**
	 * the root of the {@link ActionMap} chain.
	 */
	private final ActionMap theActionMap;

	/**
	 * Create chained maps just consisting of empty roots.
	 */
	public InputActionBindings()
	{
		theInputMap = new InputMap();
		theActionMap = new ActionMap();
		actions = new ArrayList< Actions >();
		inputs = new ArrayList< Keys >();
	}

	/**
	 * Add as {@link ActionMap} with the specified id to the end of the list (overrides maps that were added earlier).
	 * If the specified id already exists in the list, remove the corresponding earlier {@link ActionMap}.
	 */
	public void addActionMap( final String id, final ActionMap actionMap )
	{
		removeId( actions, id );
		if ( actionMap != null )
			actions.add( new Actions( id, actionMap ) );
		updateTheActionMap();
	}

	/**
	 * Remove the {@link ActionMap} with the given id from the list.
	 */
	public void removeActionMap( final String id )
	{
		if ( removeId( actions, id ) )
			updateTheActionMap();
	}

	/**
	 * Add as {@link InputMap} with the specified id to the end of the list
	 * (overrides maps that were added earlier).
	 * If the specified id already exists in the list, remove the corresponding
	 * earlier {@link InputMap}.
	 *
	 * @param id
	 * @param inputMap
	 * @param idsToBlock
	 *            ids of {@link InputMap}s earlier in the chain that should be
	 *            disabled.
	 */
	public void addInputMap( final String id, final InputMap inputMap, final String... idsToBlock )
	{
		addInputMap( id, inputMap, Arrays.asList( idsToBlock ) );
	}

	/**
	 * Add as {@link InputMap} with the specified id to the end of the list
	 * (overrides maps that were added earlier).
	 * If the specified id already exists in the list, remove the corresponding
	 * earlier {@link InputMap}.
	 *
	 * @param id
	 * @param inputMap
	 * @param idsToBlock
	 *            ids of {@link InputMap}s earlier in the chain that should be
	 *            disabled.
	 */
	public void addInputMap( final String id, final InputMap inputMap, final Collection< String > idsToBlock )
	{
		removeId( inputs, id );
		if ( inputMap != null )
			inputs.add( new Keys( id, inputMap, idsToBlock ) );
		updateTheInputMap();
	}

	/**
	 * Remove the {@link InputMap} with the given id from the list.
	 */
	public void removeInputMap( final String id )
	{
		if ( removeId( inputs, id ) )
			updateTheInputMap();
	}

	/**
	 * Get the chained {@link InputMap}. Note, that this will remain the same
	 * instance when maps are added or removed.
	 */
	public InputMap getConcatenatedInputMap()
	{
		return theInputMap;
	}

	/**
	 * Get the chained {@link ActionMap}. Note, that this will remain the same
	 * instance when maps are added or removed.
	 */
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
			final Keys keys = iter.previous();

			if ( blocked.contains( keys.getId() ) )
				continue;

			final InputMap map = keys.getInputMap();
			if ( map != null )
			{
				root.setParent( map );
				root = map;
			}

			blocked.addAll( keys.getKeysIdsToBlock() );
			if ( blocked.contains( "all" ) )
				break;
		}
		root.setParent( null );
	}
}
