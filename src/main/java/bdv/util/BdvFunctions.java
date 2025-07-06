/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.cache.CacheControl;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedBoxSelectionDialog;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.VirtualChannels.VirtualChannel;
import bdv.util.volatiles.VolatileView;
import bdv.util.volatiles.VolatileViewData;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;

/**
 * all show methods return a {@link Bdv} which can be used to add more stuff to the same window
 *
 *
 * @author Tobias Pietzsch
 * @author Philipp Hanslovsky
 * @author Igor Pisarev
 * @author Stephan Saalfeld
 */
public class BdvFunctions
{
	public static BdvHandle show()
	{
		return show( Bdv.options() );
	}

	public static BdvHandle show(
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		if ( handle instanceof BdvHandleFrame && ( ( BdvHandleFrame ) handle ).getBigDataViewer() == null )
			handle.createViewer( Collections.emptyList(), Collections.emptyList(), 1 );
		return handle;
	}

	public static < T > BdvStackSource< T > show(
			final RandomAccessibleInterval< T > img,
			final String name )
	{
		return show( img, name, Bdv.options() );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T > BdvStackSource< T > show(
			final RandomAccessibleInterval< T > img,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final AxisOrder axisOrder = AxisOrder.getAxisOrder( options.values.axisOrder(), img, handle.is2D() );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		final T type;
		if ( img instanceof VolatileView )
		{
			final VolatileViewData< ?, ? > viewData = ( ( VolatileView< ?, ? > ) img ).getVolatileViewData();
			type = ( T ) viewData.getVolatileType();
			handle.getCacheControls().addCacheControl( viewData.getCacheControl() );
		}
		else
			type = Util.getTypeFromInterval( img );

		return addRandomAccessibleInterval( handle, ( RandomAccessibleInterval ) img, ( NumericType ) type, name, axisOrder, sourceTransform );
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static < T, R extends RealType< R > > BdvStackSource< T > showMasked(
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< R > mask,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final AxisOrder axisOrder = AxisOrder.getAxisOrder( options.values.axisOrder(), img, handle.is2D() );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		final T type;
		if ( img instanceof VolatileView )
		{
			final VolatileViewData< ?, ? > viewData = ( ( VolatileView< ?, ? > ) img ).getVolatileViewData();
			type = ( T ) viewData.getVolatileType();
			handle.getCacheControls().addCacheControl( viewData.getCacheControl() );
		}
		else
			type = Util.getTypeFromInterval( img );

		return addMaskedRandomAccessibleInterval( handle, ( RandomAccessibleInterval ) img, mask, ( NumericType ) type, name, axisOrder, sourceTransform );
	}

	public static < T extends NumericType< T > > BdvStackSource< T > show(
			final RandomAccessible< T > img,
			final Interval interval,
			final String name )
	{
		return show( img, interval, name, Bdv.options() );
	}

	// TODO version with numTimepoints argument
	@SuppressWarnings( "unchecked" )
	public static < T extends NumericType< T > > BdvStackSource< T > show(
			final RandomAccessible< T > img,
			final Interval interval,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final int numTimepoints = 1;
		final AxisOrder axisOrder = AxisOrder.getAxisOrder( options.values.axisOrder(), img, handle.is2D() );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		final T type;
		if ( img instanceof VolatileView )
		{
			final VolatileViewData< ?, ? > viewData = ( ( VolatileView< ?, ? > ) img ).getVolatileViewData();
			type = ( T ) viewData.getVolatileType();
			handle.getCacheControls().addCacheControl( viewData.getCacheControl() );
		}
		else
			type = Util.getTypeFromInterval( Views.interval( img, interval ) );

		return addRandomAccessible( handle, img, interval, numTimepoints, type, name, axisOrder, sourceTransform );
	}

	public static < T extends Type< T > > BdvStackSource< T > show(
			final RealRandomAccessible< T > img,
			final Interval interval,
			final String name )
	{
		return show( img, interval, name, Bdv.options() );
	}

	public static < T extends Type< T > > BdvStackSource< T > show(
			final RealRandomAccessible< T > img,
			final Interval interval,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final AxisOrder axisOrder = AxisOrder.getAxisOrder( options.values.axisOrder(), img, handle.is2D() );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();
		final T type = img.realRandomAccess().get();
		return addRealRandomAccessible( handle, img, interval, type, name, axisOrder, sourceTransform );
	}

	public static List< BdvVirtualChannelSource > show(
			final RandomAccessibleInterval< ARGBType > img,
			final List< ? extends VirtualChannel > virtualChannels,
			final String name )
	{
		return show( img, virtualChannels, name, Bdv.options() );
	}

	public static List< BdvVirtualChannelSource > show(
			final RandomAccessibleInterval< ARGBType > img,
			final List< ? extends VirtualChannel > virtualChannels,
			final String name,
			final BdvOptions options )
	{
		return VirtualChannels.show( img, virtualChannels, name, options );
	}

	public static < T > BdvStackSource< T > show(
			final Source< T > source )
	{
		return show( source, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final Source< T > source,
			final BdvOptions options )
	{
		return show( source, 1, options );
	}

	public static < T > BdvStackSource< T > show(
			final Source< T > source,
			final int numTimePoints )
	{
		return show( source, numTimePoints, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final Source< T > source,
			final int numTimePoints,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final BdvStackSource< T > stackSource = addSource( handle, ( Source ) source, numTimePoints );
		return stackSource;
	}

	public static < T > BdvStackSource< T > show(
			final SourceAndConverter< T > source )
	{
		return show( source, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final SourceAndConverter< T > source,
			final BdvOptions options )
	{
		return show( source, 1, options );
	}

	public static < T > BdvStackSource< T > show(
			final SourceAndConverter< T > source,
			final int numTimePoints )
	{
		return show( source, numTimePoints, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final SourceAndConverter< T > soc,
			final int numTimepoints,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final T type = soc.getSpimSource().getType();
		final int setupId = handle.getUnusedSetupId();
		final List< ConverterSetup > converterSetups = Collections.singletonList( BigDataViewer.createConverterSetup( soc, setupId ) );
		final List< SourceAndConverter< T > > sources = Collections.singletonList( soc );
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static < T > BdvStackSource< T > show(
			final List< SourceAndConverter< T > > sources,
			final int numTimepoints,
			final BdvOptions options )
	{
		if ( sources.isEmpty() )
			throw new IllegalArgumentException();
		final BdvHandle handle = getHandle( options );
		final T type = sources.get( 0 ).getSpimSource().getType();
		final List< ConverterSetup > converterSetups = new ArrayList<>( sources.size() );
		for ( final SourceAndConverter< T > source : sources )
		{
			final int setupId = handle.getUnusedSetupId();
			ConverterSetup converterSetup = BigDataViewer.createConverterSetup( source, setupId );
			handle.add(  Collections.singletonList( converterSetup ), Collections.singletonList( source ), numTimepoints );
			converterSetups.add( converterSetup );
		}
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static < T > BdvStackSource< T > show( final ChannelSources< T > channels )
	{
		return show( channels, Bdv.options() );
	}

	public static < T > BdvStackSource< T > show(
			final ChannelSources< T > channels,
			final BdvOptions options )
	{
		final List< SourceAndConverter< T > > sources = channels.getSources();
		if ( sources.isEmpty() )
			throw new IllegalArgumentException();
		final int numTimepoints = channels.numTimepoints();

		final BdvHandle handle = getHandle( options );
		final List< ConverterSetup > converterSetups = new ArrayList<>( sources.size() );
		for ( final SourceAndConverter< T > source : sources )
		{
			final int setupId = handle.getUnusedSetupId();
			ConverterSetup converterSetup = BigDataViewer.createConverterSetup( source, setupId );
			handle.add(  Collections.singletonList( converterSetup ), Collections.singletonList( source ), numTimepoints );
			converterSetups.add( converterSetup );
		}
		final T type = channels.getType();
		final BdvStackSource< T > bdvSource = new BdvStackSource( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );

		final CacheControl cacheControl = channels.getCacheControl();
		if ( cacheControl != null )
			handle.getCacheControls().addCacheControl( cacheControl );

		return bdvSource;
	}

	public static List< BdvStackSource< ? > > show(
			final AbstractSpimData< ? > spimData )
	{
		return show( spimData, Bdv.options() );
	}

	public static List< BdvStackSource< ? > > show(
			final AbstractSpimData< ? > spimData,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData );

		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final int numTimepoints = seq.getTimePoints().size();
		final CacheControl cache = ( ( ViewerImgLoader ) seq.getImgLoader() ).getCacheControl();
		handle.getBdvHandle().getCacheControls().addCacheControl( cache );
		if ( cache instanceof VolatileGlobalCellCache )
			( ( VolatileGlobalCellCache ) cache ).clearCache();

		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, new ArrayList<>(), sources );

		final List< BdvStackSource< ? > > bdvSources = new ArrayList<>();
		for ( final SourceAndConverter< ? > source : sources )
			bdvSources.add( addSpimDataSource( handle, source, numTimepoints ) );

		WrapBasicImgLoader.removeWrapperIfPresent( spimData );
		return bdvSources;
	}

	public static BdvPointsSource showPoints(
			final List< ? extends RealLocalizable > points,
			final String name )
	{
		return showPoints( points, name, Bdv.options() );
	}

	public static BdvPointsSource showPoints(
			final List< ? extends RealLocalizable > points,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();

		final int setupId = handle.getUnusedSetupId();
		final ARGBType defaultColor = new ARGBType( 0xff00ff00 );
		final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, defaultColor );
		final PlaceHolderSource source = new PlaceHolderSource( name );
		final SourceAndConverter< Void > soc = new SourceAndConverter<>( source, null );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Collections.singletonList( setup ) );
		final List< SourceAndConverter< Void > > sources = new ArrayList<>( Collections.singletonList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );

		final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), soc, setup );
		final PointsOverlay overlay = new PointsOverlay();
		overlay.setOverlayInfo( info );
		overlay.setPoints( points );
		overlay.setSourceTransform( sourceTransform );
		handle.getViewerPanel().getDisplay().overlays().add( overlay );

		final BdvPointsSource bdvSource = new BdvPointsSource( handle, numTimepoints, setup, soc, info, overlay );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	public static < O extends BdvOverlay > BdvOverlaySource< O > showOverlay(
			final O overlay,
			final String name )
	{
		return showOverlay( overlay, name, Bdv.options() );
	}

	public static < O extends BdvOverlay > BdvOverlaySource< O > showOverlay(
			final O overlay,
			final String name,
			final BdvOptions options )
	{
		final BdvHandle handle = getHandle( options );
		final AffineTransform3D sourceTransform = options.values.getSourceTransform();

		final int setupId = handle.getUnusedSetupId();
		final ARGBType defaultColor = new ARGBType( 0xff00ff00 );
		final PlaceHolderConverterSetup setup = new PlaceHolderConverterSetup( setupId, 0, 255, defaultColor );
		final PlaceHolderSource source = new PlaceHolderSource( name );
		final SourceAndConverter< Void > soc = new SourceAndConverter<>( source, null );

		final List< ConverterSetup > converterSetups = new ArrayList<>( Collections.singletonList( setup ) );
		final List< SourceAndConverter< Void > > sources = new ArrayList<>( Collections.singletonList( soc ) );

		final int numTimepoints = 1;
		handle.add( converterSetups, sources, numTimepoints );

		final PlaceHolderOverlayInfo info = new PlaceHolderOverlayInfo( handle.getViewerPanel(), soc, setup );
		overlay.setOverlayInfo( info );
		overlay.setSourceTransform( sourceTransform );
		handle.getViewerPanel().getDisplay().overlays().add( overlay );

		final BdvOverlaySource< O > bdvSource = new BdvOverlaySource<>( handle, numTimepoints, setup, soc, info, overlay );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	// TODO: move to BdvFunctionUtils

	/**
	 * @deprecated Ok to use for now, but shouldn't be required in the future.
	 */
	public static int getUnusedSetupId( final BigDataViewer bdv )
	{
		return getUnusedSetupId( bdv.getSetupAssignments() );
	}

	// TODO: move to BdvFunctionUtils
	/**
	 * @deprecated Ok to use for now, but shouldn't be required in the future.
	 */
	public static synchronized int getUnusedSetupId( final SetupAssignments setupAssignments )
	{
		return SetupAssignments.getUnusedSetupId( setupAssignments );
	}

	public static TransformedRealBoxSelectionDialog.Result selectRealBox(
			final Bdv bdv,
			final AffineTransform3D boxTransform,
			final RealInterval initialInterval,
			final RealInterval rangeInterval )
	{
		return selectRealBox( bdv, boxTransform, initialInterval, rangeInterval, BoxSelectionOptions.options() );
	}

	public static TransformedRealBoxSelectionDialog.Result selectRealBox(
			final Bdv bdv,
			final AffineTransform3D boxTransform,
			final RealInterval initialInterval,
			final RealInterval rangeInterval,
			final BoxSelectionOptions options )
	{
		final BdvHandle handle = bdv.getBdvHandle();

		InputTriggerConfig keyConfig = options.values.getInputTriggerConfig();
		if ( keyConfig == null )
			keyConfig = new InputTriggerConfig();

		return new TransformedRealBoxSelectionDialog(
				handle.getViewerPanel(),
				handle.getConverterSetups(),
				handle.getUnusedSetupId(),
				keyConfig,
				handle.getTriggerbindings(),
				boxTransform,
				initialInterval,
				rangeInterval,
				options
		).getResult();
	}

	public static TransformedBoxSelectionDialog.Result selectBox(
			final Bdv bdv,
			final AffineTransform3D boxTransform,
			final Interval initialInterval,
			final Interval rangeInterval )
	{
		return selectBox( bdv, boxTransform, initialInterval, rangeInterval, BoxSelectionOptions.options() );
	}

	public static TransformedBoxSelectionDialog.Result selectBox(
			final Bdv bdv,
			final AffineTransform3D boxTransform,
			final Interval initialInterval,
			final Interval rangeInterval,
			final BoxSelectionOptions options )
	{
		final BdvHandle handle = bdv.getBdvHandle();

		InputTriggerConfig keyConfig = options.values.getInputTriggerConfig();
		if ( keyConfig == null )
			keyConfig = new InputTriggerConfig();

		return new TransformedBoxSelectionDialog(
				handle.getViewerPanel(),
				handle.getConverterSetups(),
				handle.getUnusedSetupId(),
				keyConfig,
				handle.getTriggerbindings(),
				boxTransform,
				initialInterval,
				rangeInterval,
				options
		).getResult();
	}

	/**
	 * Get existing {@code BdvHandle} from {@code options} or create a new
	 * {@code BdvHandleFrame}.
	 */
	private static BdvHandle getHandle( final BdvOptions options )
	{
		final Bdv bdv = options.values.addTo();
		return ( bdv == null )
				? new BdvHandleFrame( options )
				: bdv.getBdvHandle();
	}

	/**
	 * Add the given {@link RandomAccessibleInterval} {@code img} to the given
	 * {@link BdvHandle} as a new {@link BdvStackSource}. The {@code img} is
	 * expected to be 2D, 3D, 4D, or 5D with the given {@link AxisOrder}.
	 *
	 * @param handle
	 *            handle to add the {@code img} to.
	 * @param img
	 *            {@link RandomAccessibleInterval} to add.
	 * @param type
	 *            instance of the {@code img} type.
	 * @param name
	 *            name to give to the new source
	 * @param axisOrder
	 *            {@link AxisOrder} of the source, is used to appropriately split {@code img} into channels and timepoints.
	 * @param sourceTransform
	 *            transforms from source coordinates to global coordinates.
	 * @return a new {@link BdvStackSource} handle for the newly added source(s).
	 */
	private static < T extends NumericType< T > > BdvStackSource< T > addRandomAccessibleInterval(
			final BdvHandle handle,
			final RandomAccessibleInterval< T > img,
			final T type,
			final String name,
			final AxisOrder axisOrder,
			final AffineTransform3D sourceTransform )
	{
		final List< ConverterSetup > converterSetups = new ArrayList<>();
		final List< SourceAndConverter< T > > sources = new ArrayList<>();
		final ArrayList< RandomAccessibleInterval< T > > stacks = AxisOrder.splitInputStackIntoSourceStacks( img, axisOrder );
		int numTimepoints = 1;
		for ( final RandomAccessibleInterval< T > stack : stacks )
		{
			final Source< T > s;
			if ( stack.numDimensions() > 3 )
			{
				numTimepoints = ( int ) stack.max( 3 ) + 1;
				s = new RandomAccessibleIntervalSource4D<>( stack, type, sourceTransform, name );
			}
			else
			{
				s = new RandomAccessibleIntervalSource<>( stack, type, sourceTransform, name );
			}
			addSourceToListsGenericType( s, handle.getUnusedSetupId(), converterSetups, sources );
		}
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	/**
	 * Add the given {@link RandomAccessibleInterval} {@code img} to the given
	 * {@link BdvHandle} as a new {@link BdvStackSource}. The {@code img} is
	 * expected to be 2D, 3D, 4D, or 5D with the given {@link AxisOrder}.
	 *
	 * @param handle
	 *            handle to add the {@code img} to.
	 * @param img
	 *            {@link RandomAccessibleInterval} to add.
	 * @param mask
	 *            alpha mask (same size as {@code img}).
	 * @param type
	 *            instance of the {@code img} type.
	 * @param name
	 *            name to give to the new source
	 * @param axisOrder
	 *            {@link AxisOrder} of the source, is used to appropriately split {@code img} into channels and timepoints.
	 * @param sourceTransform
	 *            transforms from source coordinates to global coordinates.
	 * @return a new {@link BdvStackSource} handle for the newly added source(s).
	 */
	private static < T extends NumericType< T >, R extends RealType< R > > BdvStackSource< T > addMaskedRandomAccessibleInterval(
			final BdvHandle handle,
			final RandomAccessibleInterval< T > img,
			final RandomAccessibleInterval< R > mask,
			final T type,
			final String name,
			final AxisOrder axisOrder,
			final AffineTransform3D sourceTransform )
	{
		if ( !Intervals.equals( img, mask ) )
			throw new IllegalArgumentException();

		final List< ConverterSetup > converterSetups = new ArrayList<>();
		final List< SourceAndConverter< T > > sources = new ArrayList<>();
		final ArrayList< RandomAccessibleInterval< T > > stacks = AxisOrder.splitInputStackIntoSourceStacks( img, axisOrder );
		final ArrayList< RandomAccessibleInterval< R > > masks = AxisOrder.splitInputStackIntoSourceStacks( mask, axisOrder );
		int numTimepoints = 1;
		for ( int i = 0; i < stacks.size(); i++ )
		{
			final RandomAccessibleInterval< T > stack = stacks.get( i );
			final RandomAccessibleInterval< R > stackMask = masks.get( i );
			final Source< T > s;
			if ( stack.numDimensions() > 3 )
			{
				numTimepoints = ( int ) stack.max( 3 ) + 1;
				s = new MaskingRandomAccessibleIntervalSource4D<>( stack, stackMask, type, sourceTransform, name );
			}
			else
			{
				s = new MaskingRandomAccessibleIntervalSource<>( stack, stackMask, type, sourceTransform, name );
			}
			addSourceToListsGenericType( s, handle.getUnusedSetupId(), converterSetups, sources );
		}
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	/**
	 * Add the given {@link RandomAccessible} {@code img} to the given
	 * {@link BdvHandle} as a new {@link BdvStackSource}. The {@code img} is
	 * expected to be 2D, 3D, 4D, or 5D with the given {@link AxisOrder}.
	 *
	 * @param handle
	 *            handle to add the {@code img} to.
	 * @param img
	 *            {@link RandomAccessible} to add.
	 * @param interval
	 *            interval of the source (this is only used in the navigation
	 *            box overlay in BDV).
	 * @param numTimepoints
	 *            the number of timepoints of the source.
	 * @param type
	 *            instance of the {@code img} type.
	 * @param name
	 *            name to give to the new source
	 * @param axisOrder
	 *            {@link AxisOrder} of the source, is used to appropriately split {@code img} into channels and timepoints.
	 * @param sourceTransform
	 *            transforms from source coordinates to global coordinates.
	 * @return a new {@link BdvStackSource} handle for the newly added source(s).
	 */
	private static < T extends NumericType< T > > BdvStackSource< T > addRandomAccessible(
			final BdvHandle handle,
			final RandomAccessible< T > img,
			final Interval interval,
			final int numTimepoints,
			final T type,
			final String name,
			final AxisOrder axisOrder,
			final AffineTransform3D sourceTransform )
	{

		final List< ConverterSetup > converterSetups = new ArrayList<>();
		final List< SourceAndConverter< T > > sources = new ArrayList<>();
		final Pair< ArrayList< RandomAccessible< T > >, Interval > stacksAndInterval = AxisOrder.splitInputStackIntoSourceStacks(
				img,
				interval,
				axisOrder );
		final ArrayList< RandomAccessible< T > > stacks = stacksAndInterval.getA();
		final Interval stackInterval = stacksAndInterval.getB();
		for ( final RandomAccessible< T > stack : stacks )
		{
			final Source< T > s;
			if ( stack.numDimensions() > 3 )
				s = new RandomAccessibleSource4D<>( stack, stackInterval, type, sourceTransform, name );
			else
				s = new RandomAccessibleSource<>( stack, stackInterval, type, sourceTransform, name );
			addSourceToListsGenericType( s, handle.getUnusedSetupId(), converterSetups, sources );
		}

		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	/**
	 * Add the given {@link RealRandomAccessible} {@code img} to the given
	 * {@link BdvHandle} as a new {@link BdvStackSource}. The {@code img} is
	 * expected to be 2D or 3D, and the new source will have one timepoint.
	 *
	 * @param handle
	 *            handle to add the {@code img} to.
	 * @param img
	 *            {@link RealRandomAccessible} to add.
	 * @param interval
	 *            interval of the source (this is only used in the navigation
	 *            box overlay in BDV).
	 * @param type
	 *            instance of the {@code img} type.
	 * @param name
	 *            name to give to the new source
	 * @param axisOrder
	 *            {@link AxisOrder} of the source, must be {@link AxisOrder#XY}
	 *            or {@link AxisOrder#XYZ}.
	 * @param sourceTransform
	 *            transforms from source coordinates to global coordinates.
	 * @return a new {@link BdvStackSource} handle for the newly added source.
	 */
	private static < T extends Type< T > > BdvStackSource< T > addRealRandomAccessible(
			final BdvHandle handle,
			RealRandomAccessible< T > img,
			Interval interval,
			final T type,
			final String name,
			final AxisOrder axisOrder,
			final AffineTransform3D sourceTransform )
	{
		/*
		 * If AxisOrder is a 2D variant (has no Z dimension), augment the
		 * sourceStacks by a Z dimension.
		 */
		if ( !axisOrder.hasZ() )
		{
			img = RealViews.addDimension( img );
			interval = new FinalInterval(
					new long[]{ interval.min( 0 ), interval.min( 1 ), 0 },
					new long[]{ interval.max( 0 ), interval.max( 1 ), 0 } );
		}

		final Source< T > s = new RealRandomAccessibleIntervalSource<>( img, interval, type, sourceTransform, name );
		return addSource( handle, s, 1 );
	}

	/**
	 * Add the given {@link Source} to the given {@link BdvHandle} as a new
	 * {@link BdvStackSource}.
	 *
	 * @param handle
	 *            handle to add the {@code source} to.
	 * @param source
	 *            source to add.
	 * @param numTimepoints
	 *            the number of timepoints of the source.
	 * @return a new {@link BdvStackSource} handle for the newly added
	 *         {@code source}.
	 */
	@SuppressWarnings( "rawtypes" )
	private static < T > BdvStackSource< T > addSource(
			final BdvHandle handle,
			final Source< T > source,
			final int numTimepoints )
	{
		final T type = source.getType();
		final List< ConverterSetup > converterSetups = new ArrayList<>();
		final List< SourceAndConverter< T > > sources = new ArrayList<>();
		addSourceToListsGenericType( source, handle.getUnusedSetupId(), converterSetups, sources );
		handle.add( converterSetups, sources, numTimepoints );
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, converterSetups, sources );
		handle.addBdvSource( bdvSource );
		return bdvSource;
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private static < T > void addSourceToListsGenericType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		if ( type instanceof RealType || type instanceof ARGBType || type instanceof VolatileARGBType )
			addSourceToListsNumericType( ( Source ) source, setupId, converterSetups, ( List ) sources );
		else
			throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
	}

	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with an appropriate {@link Converter} to
	 * {@link ARGBType} and into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static < T extends NumericType< T > > void addSourceToListsNumericType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = source.getType();
		final SourceAndConverter< T > soc = BigDataViewer.wrapWithTransformedSource(
				new SourceAndConverter<>( source, BigDataViewer.createConverterToARGB( type ) ) );
		converterSetups.add( BigDataViewer.createConverterSetup( soc, setupId ) );
		sources.add( soc );
	}

	private static < T > BdvStackSource< T > addSpimDataSource(
			final BdvHandle handle,
			final SourceAndConverter< T > source,
			final int numTimepoints )
	{
		final ConverterSetup setup = BigDataViewer.createConverterSetup( source, handle.getUnusedSetupId() );
		final List< ConverterSetup > setups = Collections.singletonList( setup );
		final List< SourceAndConverter< T > > sources = Collections.singletonList( source );
		handle.add( setups, sources, numTimepoints );

		final T type = source.getSpimSource().getType();
		final BdvStackSource< T > bdvSource = new BdvStackSource<>( handle, numTimepoints, type, setups, sources );
		handle.addBdvSource( bdvSource );

		return bdvSource;
	}
}
