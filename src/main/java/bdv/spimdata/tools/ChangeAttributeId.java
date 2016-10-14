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
package bdv.spimdata.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.EntityUtils;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

public class ChangeAttributeId
{
	/**
	 * For the specified {@code spimData} data set, re-assign attribute id's
	 * that are already in use. As a result, attribute ids in {@code spimData}
	 * are updated and the set of used ids {@code idsInUse} is updated.
	 *
	 * @param spimData
	 *            dataset whose attributes to make unique. Will be updated as a
	 *            result of this method.
	 * @param idsInUse
	 *            Maps attribute name to set of ids already used for that
	 *            attribute. Will be updated as a result of this method.
	 * @return a map from attribute name to a map from old id to new id (the
	 *         attributes in {@code spimData} had the old id before calling this
	 *         method, and have the new id after calling this method).
	 */
	public static Map< String, Map< Integer, Integer > > assignNewAttributeIds(
			final AbstractSpimData< ? > spimData,
			final Map< String, Set< Integer > > idsInUse )
	{
		final List< ? extends BasicViewSetup > setups = spimData.getSequenceDescription().getViewSetupsOrdered();

		// attributes occurring in spimData (to make sure every attribute is processed only once)
		final Set< Entity > occurringAttributes = new HashSet<>();

		// maps attribute name to set of attributes of that name that should be changed
		final Map< String, Set< Entity > > attributeNameToChangeSet = new HashMap<>();

		// maps attribute name to map from old attribute id to new attribute id
		final Map< String, Map< Integer, Integer > > attributeNameToIdMap = new HashMap<>();

		for ( final BasicViewSetup setup : setups )
		{
			for ( final Entry< String, Entity > entry : setup.getAttributes().entrySet() )
			{
				final String attributeName = entry.getKey();
				final Entity attribute = entry.getValue();
				if ( occurringAttributes.add( attribute ) )
				{
					Set< Integer > used = idsInUse.get( attributeName );
					if ( used == null )
					{
						used = new HashSet<>();
						idsInUse.put( attributeName, used );
					}

					final int oldId = attribute.getId();
					int newId = oldId;
					while ( used.contains( newId ) )
						++newId;
					if ( newId != oldId )
					{
						Set< Entity > changeSet = attributeNameToChangeSet.get( attributeName );
						if ( changeSet == null )
						{
							changeSet = new HashSet<>();
							attributeNameToChangeSet.put( attributeName, changeSet );
						}

						Map< Integer, Integer > idMap = attributeNameToIdMap.get( attributeName );
						if ( idMap == null )
						{
							idMap = new HashMap<>();
							attributeNameToIdMap.put( attributeName, idMap );
						}

						changeSet.add( attribute );
						idMap.put( oldId, newId );
					}

					used.add( newId );
				}
			}
		}

		for ( final Entry< String, Set< Entity > > entry : attributeNameToChangeSet.entrySet() )
		{
			final String attributeName = entry.getKey();
			final Set< Entity > attributesToChange = entry.getValue();
			final Map< Integer, Integer > oldIdToNewIdMap = attributeNameToIdMap.get( attributeName );
			EntityUtils.changeIds( attributesToChange, oldIdToNewIdMap );
		}

		return attributeNameToIdMap;
	}

	/**
	 * TODO
	 *
	 * @param spimData
	 * @param attributeName
	 * @param oldAttributeId
	 * @param newAttributeId
	 */
	public static void changeAttributeId(
			final AbstractSpimData< ? > spimData,
			final String attributeName,
			final int oldAttributeId,
			final int newAttributeId )
	{
		final List< ? extends BasicViewSetup > setups = spimData.getSequenceDescription().getViewSetupsOrdered();

		final Set< Entity > attributes = new HashSet<>();
		for ( final BasicViewSetup setup : setups )
		{
			final Entity attribute = setup.getAttributes().get( attributeName );
			if ( attribute != null )
				attributes.add( attribute );
		}
		EntityUtils.changeIds( attributes, oldAttributeId, newAttributeId );
	}
}
