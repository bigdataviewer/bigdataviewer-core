/*
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
package bdv.spimdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;

public class MergeTools
{
	/**
	 * Merge multiple non-overlapping {@link SpimDataMinimal} datasets.
	 * <p>
	 * <em>
	 * Merging ImgLoader partitions etc. has to be taken care of separately.
	 * The resulting {@link SpimDataMinimal} has a {@code null} {@link BasicImgLoader}.
	 * </em>
	 *
	 * @param basePath
	 *            the basePath for the created {@link SpimDataMinimal}.
	 * @param spimDatas
	 *            datasets to merge.
	 * @return merged dataset, with {@code null} {@link BasicImgLoader}.
	 */
	public static SpimDataMinimal merge( final File basePath, final SpimDataMinimal... spimDatas )
	{
		return merge( basePath, new ArrayList<>( Arrays.asList( spimDatas ) ) );
	}

	/**
	 * Merge multiple non-overlapping {@link SpimDataMinimal} datasets.
	 * <p>
	 * <em>
	 * Merging ImgLoader partitions etc. has to be taken care of separately.
	 * The resulting {@link SpimDataMinimal} has a {@code null} {@link BasicImgLoader}.
	 * </em>
	 *
	 * @param basePath
	 *            the basePath for the created {@link SpimDataMinimal}.
	 * @param spimDatas
	 *            datasets to merge.
	 * @return merged dataset, with {@code null} {@link BasicImgLoader}.
	 */
	public static SpimDataMinimal merge( final File basePath, final ArrayList< SpimDataMinimal > spimDatas )
	{
		/*
		 * aggregate timepoints.
		 */
		final HashMap< Integer, TimePoint > aggregateTpIds = new HashMap<>();
		for ( final SpimDataMinimal spimData : spimDatas )
		{
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final Map< Integer, TimePoint > tps = seq.getTimePoints().getTimePoints();
			aggregateTpIds.putAll( tps );
		}
		TimePoints aggregateTimepoints = new TimePoints( aggregateTpIds );
		for ( final SpimDataMinimal spimData : spimDatas )
		{
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final Map< Integer, TimePoint > tps = seq.getTimePoints().getTimePoints();
			if ( aggregateTpIds.keySet().equals( tps.keySet() ) )
				aggregateTimepoints = seq.getTimePoints();
		}

		/*
		 * aggregate setups.
		 */
		final HashMap< Integer, BasicViewSetup > aggregateSetups = new HashMap<>();
		for ( final SpimDataMinimal spimData : spimDatas )
		{
			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			final Map< Integer, ? extends BasicViewSetup > setups = seq.getViewSetups();
			aggregateSetups.putAll( setups );
		}

		/*
		 * aggregate registrations and determine missing views.
		 */
		final HashMap< ViewId, ViewRegistration > aggregateRegs = new HashMap<>();
		for ( final SpimDataMinimal spimData : spimDatas )
		{
			for ( final ViewRegistration reg : spimData.getViewRegistrations().getViewRegistrationsOrdered() )
			{
				if ( aggregateRegs.put( reg, reg ) != null )
					throw new IllegalArgumentException(
							"Error: setup " + reg.getViewSetupId() + " timepoint " + reg.getTimePointId() + " present in multiple sequences!" );
			}
		}
		final ArrayList< ViewId > aggregateMissingViewIds = new ArrayList<>();
		for ( final BasicViewSetup setup : aggregateSetups.values() )
		{
			for ( final TimePoint timepoint : aggregateTimepoints.getTimePointsOrdered() )
			{
				final ViewId viewId = new ViewId( timepoint.getId(), setup.getId() );
				if ( !aggregateRegs.containsKey( viewId ) )
					aggregateMissingViewIds.add( viewId );
			}
		}
		final MissingViews aggregateMissingViews = new MissingViews( aggregateMissingViewIds );
		final ViewRegistrations aggregateViewRegistrations = new ViewRegistrations( aggregateRegs.values() );

		/*
		 * aggregate spimData.
		 */
		final BasicImgLoader imgLoader = null;
		final SequenceDescriptionMinimal aggregateSequenceDescription =
				new SequenceDescriptionMinimal( aggregateTimepoints, aggregateSetups, imgLoader, aggregateMissingViews );
		final SpimDataMinimal aggregateSpimData = new SpimDataMinimal( basePath, aggregateSequenceDescription, aggregateViewRegistrations );

		return aggregateSpimData;
	}
}
