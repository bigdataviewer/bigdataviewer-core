/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.TypeIdentity;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;


/**
 * The current application for VirtualChannel sources is the following scenario:
 *
 * We have a labeling and want to display it using lookup tables to convert
 * labels to ARGBType. From the same labeling we want to make multiple channels,
 * e.g., selected ROIs, ROIs having property A, property B, etc. These could be
 * added to BDV as individually converted images. So we could use N lookup
 * tables to display N converted images. Instead, it is more efficient to merge
 * the N lookup tables and then display only one converted image. We still want
 * to be able to control display range and color settings for the N
 * "virtual channels" individually. So {@link VirtualChannels} adds N fake
 * sources to the BDV. Each fake source is used to control visibility and
 * settings for one lookup table. Only one of the fake source will then actually
 * render the converted img.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class VirtualChannels
{
	public interface VirtualChannel
	{
		void updateVisibility();

		void updateSetupParameters();
	}

	static List< BdvVirtualChannelSource > show(
			final RandomAccessibleInterval< ARGBType > img,
			final List< ? extends VirtualChannel > virtualChannels,
			final String name,
			final BdvOptions options )
	{
		final Bdv bdv = options.values.addTo();
		final BdvHandle handle = ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		AxisOrder axisOrder = options.values.axisOrder();
		axisOrder = AxisOrder.getAxisOrder( axisOrder, img, handle.is2D() );

		final ArrayList< RandomAccessibleInterval< ARGBType > > stacks = AxisOrder.splitInputStackIntoSourceStacks( img, axisOrder );
		if ( stacks.size() != 1 )
			throw new IllegalArgumentException( "The RandomAccessibleInterval of a VirtualChannelSource must have exactly one channel!" );
		final RandomAccessibleInterval< ARGBType > stack = stacks.get( 0 );

		final List< BdvVirtualChannelSource > bdvSources = new ArrayList<>();

		final int numTimepoints = ( stack.numDimensions() > 3 )	? ( int ) stack.max( 3 ) + 1 : 1;
		final ChannelSourceCoordinator coordinator = new ChannelSourceCoordinator();
		for ( final VirtualChannel vc : virtualChannels )
		{
			final ChannelSource source = ( stack.numDimensions() > 3 )
					? new ChannelSource4D( stack, coordinator, sourceTransform, name )
					: new DefaultChannelSource( stack, coordinator, sourceTransform, name );
			final int setupId = handle.getUnusedSetupId();
			final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, new ARGBType( 0xffffffff ) );
			final SourceAndConverter< ARGBType > soc = source.getSourceAndConverter();
			handle.add( Collections.singletonList( setup ), Collections.singletonList( soc ), numTimepoints );

			final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), soc, setup );
			coordinator.sharedInfos.add( info );
			setup.setupChangeListeners().add( s -> vc.updateSetupParameters() );
			info.visibilityChangeListeners().add( vc::updateVisibility );
			final BdvVirtualChannelSource bdvSource = new BdvVirtualChannelSource( handle, numTimepoints, setup, soc, info, coordinator );
			handle.addBdvSource( bdvSource );
			bdvSources.add( bdvSource );
		}

		return bdvSources;
	}

	static class ChannelSourceCoordinator
	{
		List< PlaceHolderOverlayInfo > sharedInfos = new ArrayList<>();

		boolean shouldBePresent( final ChannelSource source )
		{
			for ( final PlaceHolderOverlayInfo info : sharedInfos )
				if ( info.isVisible() )
					return info.getSource().equals( source.getSourceAndConverter() );
			return false;
		}
	}

	interface ChannelSource
	{
		SourceAndConverter< ARGBType > getSourceAndConverter();
	}

	static class DefaultChannelSource extends RandomAccessibleIntervalSource< ARGBType > implements ChannelSource
	{
		private final ChannelSourceCoordinator coordinator;

		private final SourceAndConverter< ARGBType > soc;

		public DefaultChannelSource(
				final RandomAccessibleInterval< ARGBType > img,
				final ChannelSourceCoordinator coordinator,
				final AffineTransform3D sourceTransform,
				final String name )
		{
			super( img, new ARGBType(), sourceTransform, name );
			this.coordinator = coordinator;
			this.soc = new SourceAndConverter<>( this, new TypeIdentity< >() );
		}

		@Override
		public boolean isPresent( final int t )
		{
			return super.isPresent( t ) && coordinator.shouldBePresent( this );
		}

		@Override
		public SourceAndConverter< ARGBType > getSourceAndConverter()
		{
			return soc;
		}
	}

	static class ChannelSource4D extends RandomAccessibleIntervalSource4D< ARGBType > implements ChannelSource
	{
		private final ChannelSourceCoordinator coordinator;

		private final SourceAndConverter< ARGBType > soc;

		public ChannelSource4D(
				final RandomAccessibleInterval< ARGBType > img,
				final ChannelSourceCoordinator coordinator,
				final AffineTransform3D sourceTransform,
				final String name )
		{
			super( img, new ARGBType(), sourceTransform, name );
			this.coordinator = coordinator;
			this.soc = new SourceAndConverter<>( this, new TypeIdentity< >() );
		}

		@Override
		public boolean isPresent( final int t )
		{
			return super.isPresent( t ) && coordinator.shouldBePresent( this );
		}

		@Override
		public SourceAndConverter< ARGBType > getSourceAndConverter()
		{
			return soc;
		}
	}
}
