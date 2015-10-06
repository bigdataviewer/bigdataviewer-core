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
package bdv.viewer.state;

import static bdv.viewer.DisplayMode.FUSED;
import static bdv.viewer.DisplayMode.FUSEDGROUP;
import static bdv.viewer.DisplayMode.GROUP;
import static bdv.viewer.DisplayMode.SINGLE;
import static bdv.viewer.Interpolation.NEARESTNEIGHBOR;
import static bdv.viewer.Interpolation.NLINEAR;

import java.util.List;
import java.util.SortedSet;

import mpicbg.spim.data.XmlHelpers;

import org.jdom2.Element;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;


public class XmlIoViewerState
{
	public static final String VIEWERSTATE_TAG = "ViewerState";

	public static final String VIEWERSTATE_SOURCES_TAG = "Sources";
	public static final String VIEWERSTATE_SOURCE_TAG = "Source";
	public static final String VIEWERSTATE_SOURCE_ACTIVE_TAG = "active";

	public static final String VIEWERSTATE_GROUPS_TAG = "SourceGroups";
	public static final String VIEWERSTATE_GROUP_TAG = "SourceGroup";
	public static final String VIEWERSTATE_GROUP_ACTIVE_TAG = "active";
	public static final String VIEWERSTATE_GROUP_NAME_TAG = "name";
	public static final String VIEWERSTATE_GROUP_SOURCEID_TAG = "id";

	public static final String VIEWERSTATE_DISPLAYMODE_TAG = "DisplayMode";
	public static final String VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE = "ss";
	public static final String VIEWERSTATE_DISPLAYMODE_VALUE_GROUP = "sg";
	public static final String VIEWERSTATE_DISPLAYMODE_VALUE_FUSED = "fs";
	public static final String VIEWERSTATE_DISPLAYMODE_VALUE_FUSEDGROUP = "fg";

	public static final String VIEWERSTATE_INTERPOLATION_TAG = "Interpolation";
	public static final String VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR = "nearestneighbor";
	public static final String VIEWERSTATE_INTERPOLATION_VALUE_NLINEAR = "nlinear";

	public static final String VIEWERSTATE_CURRENTSOURCE_TAG = "CurrentSource";
	public static final String VIEWERSTATE_CURRENTGROUP_TAG = "CurrentGroup";
	public static final String VIEWERSTATE_CURRENTTIMEPOINT_TAG = "CurrentTimePoint";

	public String getTagName()
	{
		return VIEWERSTATE_TAG;
	}

	public Element toXml( final ViewerState state )
	{
		final Element elem = new Element( VIEWERSTATE_TAG );
		elem.addContent( sourcesToXml( state.getSources() ) );
		elem.addContent( sourceGroupsToXml( state.getSourceGroups() ) );
		elem.addContent( displayModeToXml( state.getDisplayMode() ) );
		elem.addContent( interpolationModeToXml ( state.getInterpolation() ) );
		elem.addContent( XmlHelpers.intElement( VIEWERSTATE_CURRENTSOURCE_TAG, state.getCurrentSource() ) );
		elem.addContent( XmlHelpers.intElement( VIEWERSTATE_CURRENTGROUP_TAG, state.getCurrentGroup() ) );
		elem.addContent( XmlHelpers.intElement( VIEWERSTATE_CURRENTTIMEPOINT_TAG, state.getCurrentTimepoint() ) );
		return elem;
	}

	/**
	 * @param elem &lt;ViewerState&gt; element.
	 * @param state is restored from the <code>elem</code>.
	 */
	public void restoreFromXml( final Element elem, final ViewerState state )
	{
		restoreSourcesFromXml( elem.getChild( VIEWERSTATE_SOURCES_TAG ), state.getSources() );
		restoreSourceGroupsFromXml( elem.getChild( VIEWERSTATE_GROUPS_TAG ), state.getSourceGroups() );
		state.setDisplayMode( displayModeFromXml( elem.getChild( VIEWERSTATE_DISPLAYMODE_TAG ) ) );
		state.setInterpolation( interpolationModeFromXml( elem.getChild( VIEWERSTATE_INTERPOLATION_TAG ) ) );
		state.setCurrentSource( XmlHelpers.getInt( elem, VIEWERSTATE_CURRENTSOURCE_TAG ) );
		state.setCurrentGroup( XmlHelpers.getInt( elem, VIEWERSTATE_CURRENTGROUP_TAG ) );
		state.setCurrentTimepoint( XmlHelpers.getInt( elem, VIEWERSTATE_CURRENTTIMEPOINT_TAG ) );
	}

	protected Element sourcesToXml( final List< SourceState< ? > > sources )
	{
		final Element elem = new Element( VIEWERSTATE_SOURCES_TAG );
		for ( final SourceState< ? > source : sources )
		{
			final Element sourceElem = new Element( VIEWERSTATE_SOURCE_TAG );
			sourceElem.addContent( XmlHelpers.booleanElement( VIEWERSTATE_SOURCE_ACTIVE_TAG, source.isActive() ) );
			elem.addContent( sourceElem );
		}
		return elem;
	}

	/**
	 * @param elem &lt;Sources&gt; element.
	 * @param sources is restored from the <code>elem</code>.
	 */
	protected void restoreSourcesFromXml( final Element elem, final List< SourceState< ? > > sources )
	{
		final List< Element > sourceElems = elem.getChildren( VIEWERSTATE_SOURCE_TAG );
		if ( sources.size() != sourceElems.size() )
			throw new IllegalArgumentException();
		for ( int i = 0; i < sources.size(); ++i )
			sources.get( i ).setActive( XmlHelpers.getBoolean( sourceElems.get( i ), VIEWERSTATE_SOURCE_ACTIVE_TAG ) );
	}

	public Element sourceGroupsToXml( final List< SourceGroup > groups )
	{
		final Element elem = new Element( VIEWERSTATE_GROUPS_TAG );
		for ( final SourceGroup group : groups )
		{
			final Element groupElem = new Element( VIEWERSTATE_GROUP_TAG );
			groupElem.addContent( XmlHelpers.booleanElement( VIEWERSTATE_GROUP_ACTIVE_TAG, group.isActive() ) );
			groupElem.addContent( XmlHelpers.textElement( VIEWERSTATE_GROUP_NAME_TAG, group.getName() ) );
			for ( final int id : group.getSourceIds() )
				groupElem.addContent( XmlHelpers.intElement( VIEWERSTATE_GROUP_SOURCEID_TAG, id ) );
			elem.addContent( groupElem );
		}
		return elem;
	}

	/**
	 * @param elem &lt;SourceGroups&gt; element.
	 * @param groups is restored from the <code>elem</code>.
	 */
	public void restoreSourceGroupsFromXml( final Element elem, final List< SourceGroup > groups )
	{
		final List< Element > groupElems = elem.getChildren( VIEWERSTATE_GROUP_TAG );
		if ( groups.size() != groupElems.size() )
			throw new IllegalArgumentException();
		for ( int i = 0; i < groups.size(); ++i )
		{
			final SourceGroup group = groups.get( i );
			final Element groupElem = groupElems.get( i );
			group.setActive( XmlHelpers.getBoolean( groupElem, VIEWERSTATE_GROUP_ACTIVE_TAG ) );
			group.setName( groupElem.getChildText( VIEWERSTATE_GROUP_NAME_TAG ) );
			final SortedSet< Integer > ids = group.getSourceIds();
			ids.clear();
			for ( final Element idElem : groupElem.getChildren( VIEWERSTATE_GROUP_SOURCEID_TAG ) )
				ids.add( Integer.parseInt( idElem.getText() ) );
		}
	}

	protected Element displayModeToXml( final DisplayMode mode )
	{
		switch ( mode )
		{
		case GROUP:
			return new Element( VIEWERSTATE_DISPLAYMODE_TAG ).setText( VIEWERSTATE_DISPLAYMODE_VALUE_GROUP );
		case FUSED:
			return new Element( VIEWERSTATE_DISPLAYMODE_TAG ).setText( VIEWERSTATE_DISPLAYMODE_VALUE_FUSED );
		case FUSEDGROUP:
			return new Element( VIEWERSTATE_DISPLAYMODE_TAG ).setText( VIEWERSTATE_DISPLAYMODE_VALUE_FUSEDGROUP );
		case SINGLE:
		default:
			return new Element( VIEWERSTATE_DISPLAYMODE_TAG ).setText( VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE );
		}
	}

	protected DisplayMode displayModeFromXml( final Element elem )
	{
		final String t = elem.getTextTrim();
		if ( VIEWERSTATE_DISPLAYMODE_VALUE_GROUP.equals( t ) )
			return GROUP;
		else if ( VIEWERSTATE_DISPLAYMODE_VALUE_FUSED.equals( t ) )
			return FUSED;
		else if ( VIEWERSTATE_DISPLAYMODE_VALUE_FUSEDGROUP.equals( t ) )
			return FUSEDGROUP;
		else // if ( VIEWERSTATE_DISPLAYMODE_VALUE_SINGLE.equals( t ) )
			return SINGLE;
	}

	protected Element interpolationModeToXml( final Interpolation mode )
	{
		switch ( mode )
		{
		case NLINEAR:
			return new Element( VIEWERSTATE_INTERPOLATION_TAG ).setText( VIEWERSTATE_INTERPOLATION_VALUE_NLINEAR );
		case NEARESTNEIGHBOR:
		default:
			return new Element( VIEWERSTATE_INTERPOLATION_TAG ).setText( VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR );
		}
	}

	protected Interpolation interpolationModeFromXml( final Element elem )
	{
		final String t = elem.getTextTrim();
		if ( VIEWERSTATE_INTERPOLATION_VALUE_NLINEAR.equals( t ) )
			return NLINEAR;
		else // if ( VIEWERSTATE_INTERPOLATION_VALUE_NEARESTNEIGHBOR.equals( t ) )
			return NEARESTNEIGHBOR;
	}
}
