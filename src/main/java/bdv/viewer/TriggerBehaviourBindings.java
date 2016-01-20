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

import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * Maintains lists of {@link BehaviourMap}s and {@link InputTriggerMap}s, which
 * are chained to a {@link #getConcatenatedInputTriggerMap() concatenated
 * InputTriggerMap} and a {@link #getConcatenatedBehaviourMap() concatenated
 * BehaviourMap}. Maps can be added and will be chained in reverse order of
 * addition, that is, the last added map overrides all previous ones. For
 * {@link InputTriggerMap}s it is possible to block maps that were added
 * earlier.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public final class TriggerBehaviourBindings
{
	/**
	 * the root of the {@link InputMap} chain.
	 */
	private final InputTriggerMap theInputTriggerMap;

	/**
	 * the root of the {@link ActionMap} chain.
	 */
	private final BehaviourMap theBehaviourMap;

	/**
	 * Create chained maps just consisting of empty roots.
	 */
	public TriggerBehaviourBindings()
	{
		theInputTriggerMap = new InputTriggerMap();
		theBehaviourMap = new BehaviourMap();
		behaviours = new ArrayList< Behaviours >();
		triggers= new ArrayList< Triggers >();
	}

	/**
	 * Add as {@link BehaviourMap} with the specified id to the end of the list
	 * (overrides maps that were added earlier). If the specified id already
	 * exists in the list, remove the corresponding earlier {@link BehaviourMap}.
	 */
	public void addBehaviourMap( final String id, final BehaviourMap behaviourMap )
	{
		removeId( behaviours, id );
		if ( behaviourMap != null )
			behaviours.add( new Behaviours( id, behaviourMap ) );
		updateTheBehaviourMap();
	}

	/**
	 * Remove the {@link ActionMap} with the given id from the list.
	 */
	public void removeBehaviourMap( final String id )
	{
		if ( removeId( behaviours, id ) )
			updateTheBehaviourMap();
	}

	/**
	 * Add as {@link InputTriggerMap} with the specified id to the end of the
	 * list (overrides maps that were added earlier). If the specified id
	 * already exists in the list, remove the corresponding earlier
	 * {@link InputTriggerMap}.
	 *
	 * @param id
	 * @param inputTriggerMap
	 * @param idsToBlock
	 *            ids of {@link InputTriggerMap}s earlier in the chain that
	 *            should be disabled.
	 */
	public void addInputTriggerMap( final String id, final InputTriggerMap inputTriggerMap, final String... idsToBlock )
	{
		addInputTriggerMap( id, inputTriggerMap, Arrays.asList( idsToBlock ) );
	}

	/**
	 * Add as {@link InputTriggerMap} with the specified id to the end of the
	 * list (overrides maps that were added earlier). If the specified id
	 * already exists in the list, remove the corresponding earlier
	 * {@link InputTriggerMap}.
	 *
	 * @param id
	 * @param inputTriggerMap
	 * @param idsToBlock
	 *            ids of {@link InputTriggerMap}s earlier in the chain that
	 *            should be disabled.
	 */
	public void addInputTriggerMap( final String id, final InputTriggerMap inputTriggerMap, final Collection< String > idsToBlock )
	{
		removeId( triggers, id );
		if ( inputTriggerMap != null )
			triggers.add( new Triggers( id, inputTriggerMap, idsToBlock ) );
		updateTheInputTriggerMap();
	}

	/**
	 * Remove the {@link InputTriggerMap} with the given id from the list.
	 */
	public void removeInputTriggerMap( final String id )
	{
		if ( removeId( triggers, id ) )
			updateTheInputTriggerMap();
	}

	/**
	 * Get the chained {@link InputTriggerMap}. Note, that this will remain the
	 * same instance when maps are added or removed.
	 */
	public InputTriggerMap getConcatenatedInputTriggerMap()
	{
		return theInputTriggerMap;
	}

	/**
	 * Get the chained {@link BehaviourMap}. Note, that this will remain the
	 * same instance when maps are added or removed.
	 */
	public BehaviourMap getConcatenatedBehaviourMap()
	{
		return theBehaviourMap;
	}

	private interface WithId
	{
		public String getId();
	}

	private static class Behaviours implements WithId
	{
		private final String id;

		private final BehaviourMap behaviourMap;

		public Behaviours( final String id, final BehaviourMap behaviourMap )
		{
			this.id = id;
			this.behaviourMap = behaviourMap;
		}

		@Override
		public String getId()
		{
			return id;
		}

		public BehaviourMap getBehaviourMap()
		{
			return behaviourMap;
		}
	}

	private static class Triggers implements WithId
	{
		private final String id;

		private final InputTriggerMap inputTriggerMap;

		private final HashSet< String > idsToBlock;

		public Triggers( final String id, final InputTriggerMap inputTriggerMap, final Collection< String > idsToBlock )
		{
			this.id = id;
			this.inputTriggerMap = inputTriggerMap;
			this.idsToBlock = new HashSet< String >( idsToBlock );
		}

		@Override
		public String getId()
		{
			return id;
		}

		public InputTriggerMap getInputTriggerMap()
		{
			return inputTriggerMap;
		}

		public Set< String > getKeysIdsToBlock()
		{
			return idsToBlock;
		}
	}

	private final List< Behaviours > behaviours;

	private final List< Triggers > triggers;

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

	private void updateTheBehaviourMap()
	{
		final ListIterator< Behaviours > iter = behaviours.listIterator( behaviours.size() );
		BehaviourMap root = theBehaviourMap;
		while ( iter.hasPrevious() )
		{
			final BehaviourMap map = iter.previous().getBehaviourMap();
			if ( map != null )
			{
				root.setParent( map );
				root = map;
			}
		}
		root.setParent( null );
	}

	private void updateTheInputTriggerMap()
	{
		final ListIterator< Triggers > iter = triggers.listIterator( triggers.size() );
		InputTriggerMap root = theInputTriggerMap;
		final HashSet< String > blocked = new HashSet< String >();
		while ( iter.hasPrevious() )
		{
			final Triggers keys = iter.previous();

			if ( blocked.contains( keys.getId() ) )
				continue;

			final InputTriggerMap map = keys.getInputTriggerMap();
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
