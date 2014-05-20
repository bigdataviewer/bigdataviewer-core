package bdv.ij.export;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewId;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import bdv.export.ExportMipmapInfo;
import bdv.export.WriteSequenceToHdf5;
import bdv.ij.util.PluginHelper;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

/**
 * Aggregate {@link BasicViewSetup setups}, i.e., SPIM source angles and fused
 * datasets from multiple {@link SequenceDescription}s. Also keeps for each
 * setup the mipmap resolutions and subdivisions to be created.
 *
 * Note, that added setups are assigned new, consecutive ids starting from 0.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SetupAggregator
{
	/**
	 * timepoint id for every timepoint index.
	 */
	protected TimePoints timepoints;

	protected final ArrayList< ViewRegistration > registrations;

	/**
	 * Contains {@link ViewSetupWrapper wrappers} around setups in other sequences.
	 */
	protected final ArrayList< ViewSetupWrapper > setups;

	protected final Map< Integer, ExportMipmapInfo > perSetupMipmapInfo;

	/**
	 * An {@link ImgLoader} that forwards to wrapped source sequences.
	 */
	protected final BasicImgLoader< UnsignedShortType > imgLoader;

	/**
	 * Create an empty aggregator.
	 */
	public SetupAggregator()
	{
		timepoints = null;
		registrations = new ArrayList< ViewRegistration >();
		setups = new ArrayList< ViewSetupWrapper >();
		perSetupMipmapInfo = new HashMap< Integer, ExportMipmapInfo >();
		imgLoader = new BasicImgLoader< UnsignedShortType >()
		{
			@Override
			public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
			{
				final ViewSetupWrapper w = setups.get( view.getViewSetupId() );
				@SuppressWarnings( "unchecked" )
				final BasicImgLoader< UnsignedShortType > il = ( BasicImgLoader< UnsignedShortType > ) w.getSourceSequence().getImgLoader();
				return il.getImage( new ViewId( view.getTimePointId(), w.getSourceSetupId() ) );
			}

			@Override
			public UnsignedShortType getImageType()
			{
				return new UnsignedShortType();
			}
		};
	}

	/**
	 * Add a new {@link BasicViewSetup} to the aggregator.
	 *
	 * Adds a setup of the given source {@link SpimRegistrationSequence} to the
	 * aggregator. A reference to the source sequence is kept and the source
	 * {@link ViewRegistrations} are copied. In the viewer format, every image
	 * is stored in multiple resolutions. The resolutions are described as int[]
	 * arrays defining multiple of original pixel size in every dimension. For
	 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
	 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image is
	 * stored as a chunked three-dimensional array (each chunk corresponds to
	 * one cell of a {@link CellImg} when the data is loaded). The chunk sizes
	 * are defined by the subdivisions parameter which is an array of int[], one
	 * per resolution. Each int[] array describes the X,Y,Z chunk size for one
	 * resolution.
	 *
	 * @param sourceSetup
	 *            the setup to add
	 * @param sourceSequence
	 *            the source sequence to which the given setup refers.
	 * @param sourceRegs
	 *            the view registrations associated with the source sequence.
	 *            registrations for the given setup are copied.
	 * @param resolutions
	 *            the set of resolutions to store. each nested int[] array
	 *            defines one resolution.
	 * @param subdivisions
	 *            the set of subdivisions to store. each nested int[] array
	 *            defines one subdivision.
	 * @return the setup id of the new {@link BasicViewSetup} in the aggregator.
	 */
	public int add( final BasicViewSetup sourceSetup, final AbstractSequenceDescription< ?, ?, ? > sourceSequence, final ViewRegistrations sourceRegs, final int[][] resolutions, final int[][] subdivisions )
	{
		final int setupId = setups.size();
		setups.add( new ViewSetupWrapper( setupId, sourceSequence, sourceSetup ) );
		if ( setupId == 0 )
		{
			// if this is the first setup added, initialize the timepoints
			timepoints = sourceSequence.getTimePoints();
		}
		final int sourceSetupId = sourceSetup.getId();
		for ( final TimePoint timepoint : timepoints.getTimePointsOrdered() )
		{
			final int timepointId = timepoint.getId();
			final ViewRegistration r = sourceRegs.getViewRegistrations().get( new ViewId( timepointId, sourceSetupId ) );
			if ( r == null )
				throw new RuntimeException( "could not find ViewRegistration for timepoint " + timepointId + " in the source sequence." );
			registrations.add( new ViewRegistration( timepointId, setupId, r.getModel() ) );
		}
		perSetupMipmapInfo.put( setupId, new ExportMipmapInfo( resolutions, subdivisions ) );
		return setupId;
	}

	/**
	 * Create a {@link SpimDataMinimal} for the setups currently aggregated.
	 * This can be used to write the sequence (see {@link WriteSequenceToHdf5}
	 * and
	 * {@link XmlIoSpimDataMinimal#toXml(bdv.spimdata.SpimDataMinimal, File)}.
	 *
	 * @param basePath
	 * @return a {@link SpimDataMinimal} with the currently aggregated setups.
	 */
	public SpimDataMinimal createSpimData( final File basePath )
	{
		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal( timepoints, Entity.idMap( setups ), imgLoader, null );
		return new SpimDataMinimal( basePath, seq, new ViewRegistrations( registrations ) );
	}

	/**
	 * Get the aggregated per-setup {@link ExportMipmapInfo}s.
	 */
	public Map< Integer, ExportMipmapInfo > getPerSetupMipmapInfo()
	{
		return perSetupMipmapInfo;
	}

	/**
	 * Add the setup (angle) of the given {@link SpimRegistrationSequence}
	 * to this collection. In the viewer format, every image is stored in
	 * multiple resolutions. The resolutions are described as int[] arrays
	 * defining multiple of original pixel size in every dimension. For
	 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
	 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
	 * is stored as a chunked three-dimensional array (each chunk
	 * corresponds to one cell of a {@link CellImg} when the data is
	 * loaded). The chunk sizes are defined by the subdivisions parameter
	 * which is an array of int[], one per resolution. Each int[] array
	 * describes the X,Y,Z chunk size for one resolution.
	 *
	 * @param sequence
	 *            a registered spim sequence. see
	 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
	 * @param setupIndex
	 *            which of the setups of the spim sequence to add.
	 * @param resolutionsString
	 *            the set of resolutions to store, formatted like
	 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
	 *            resolution.
	 * @param subdivisionsString
	 *            the set of subdivisions to store, formatted like
	 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
	 *            defines one subdivision.
	 */
	public void addSetup( final SpimRegistrationSequence sequence, final int setupIndex, final String resolutionsString, final String subdivisionsString )
	{
		final AbstractSequenceDescription< ?, ?, ? > desc = sequence.getSequenceDescription();
		final ViewRegistrations regs = sequence.getViewRegistrations();
		final int[][] resolutions = PluginHelper.parseResolutionsString( resolutionsString );
		final int[][] subdivisions = PluginHelper.parseResolutionsString( subdivisionsString );
		if ( resolutions.length == 0 )
			throw new RuntimeException( "Cannot parse mipmap resolutions" + resolutionsString );
		if ( subdivisions.length == 0 )
			throw new RuntimeException( "Cannot parse subdivisions " + subdivisionsString );
		else if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		final BasicViewSetup setup = desc.getViewSetups().get( setupIndex );
			add( setup, desc, regs, resolutions, subdivisions );
	}

	/**
	 * Add the setup (angle) of the given {@link SpimRegistrationSequence}
	 * to this collection. In the viewer format, every image is stored in
	 * multiple resolutions. The resolutions are described as int[] arrays
	 * defining multiple of original pixel size in every dimension. For
	 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
	 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
	 * is stored as a chunked three-dimensional array (each chunk
	 * corresponds to one cell of a {@link CellImg} when the data is
	 * loaded). The chunk sizes are defined by the subdivisions parameter
	 * which is an array of int[], one per resolution. Each int[] array
	 * describes the X,Y,Z chunk size for one resolution.
	 *
	 * @param sequence
	 *            a registered spim sequence. see
	 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
	 * @param setupIndex
	 *            which of the setups of the spim sequence to add.
	 * @param resolutions
	 *            the set of resolutions to store. each nested int[] array
	 *            defines one resolution.
	 * @param subdivisions
	 *            the set of subdivisions to store. each nested int[] array
	 *            defines one subdivision.
	 */
	public void addSetup( final SpimRegistrationSequence sequence, final int setupIndex, final int[][] resolutions, final int[][] subdivisions )
	{
		final AbstractSequenceDescription< ?, ?, ? > desc = sequence.getSequenceDescription();
		final ViewRegistrations regs = sequence.getViewRegistrations();
		if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		final BasicViewSetup setup = desc.getViewSetups().get( setupIndex );
			add( setup, desc, regs, resolutions, subdivisions );
	}

	/**
	 * Add all setups (angles) of the given {@link SpimRegistrationSequence}
	 * to this collection. In the viewer format, every image is stored in
	 * multiple resolutions. The resolutions are described as int[] arrays
	 * defining multiple of original pixel size in every dimension. For
	 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
	 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
	 * is stored as a chunked three-dimensional array (each chunk
	 * corresponds to one cell of a {@link CellImg} when the data is
	 * loaded). The chunk sizes are defined by the subdivisions parameter
	 * which is an array of int[], one per resolution. Each int[] array
	 * describes the X,Y,Z chunk size for one resolution.
	 *
	 * @param sequence
	 *            a registered spim sequence. see
	 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
	 * @param resolutionsString
	 *            the set of resolutions to store, formatted like
	 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
	 *            resolution.
	 * @param subdivisionsString
	 *            the set of subdivisions to store, formatted like
	 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
	 *            defines one subdivision.
	 */
	public void addSetups( final SpimRegistrationSequence sequence, final String resolutionsString, final String subdivisionsString )
	{
		for ( int s = 0; s < sequence.getSequenceDescription().getViewSetups().size(); ++s )
			addSetup( sequence, s, resolutionsString, subdivisionsString );
	}

	/**
	 * Add all setups (angles) of the given {@link SpimRegistrationSequence}
	 * to this collection. In the viewer format, every image is stored in
	 * multiple resolutions. The resolutions are described as int[] arrays
	 * defining multiple of original pixel size in every dimension. For
	 * example {1,1,1} is the original resolution, {4,4,2} is downsampled by
	 * factor 4 in X and Y and factor 2 in Z. Each resolution of the image
	 * is stored as a chunked three-dimensional array (each chunk
	 * corresponds to one cell of a {@link CellImg} when the data is
	 * loaded). The chunk sizes are defined by the subdivisions parameter
	 * which is an array of int[], one per resolution. Each int[] array
	 * describes the X,Y,Z chunk size for one resolution.
	 *
	 * @param sequence
	 *            a registered spim sequence. see
	 *            {@link Scripting#createSpimRegistrationSequence(String, String, String, String, int, boolean, double)}
	 * @param resolutions
	 *            the set of resolutions to store. each nested int[] array
	 *            defines one resolution.
	 * @param subdivisions
	 *            the set of subdivisions to store. each nested int[] array
	 *            defines one subdivision.
	 */
	public void addSetups( final SpimRegistrationSequence sequence, final int[][] resolutions, final int[][] subdivisions )
	{
		for ( int s = 0; s < sequence.getSequenceDescription().getViewSetups().size(); ++s )
			addSetup( sequence, s, resolutions, subdivisions );
	}

	/**
	 * Add result of SPIM fusion or deconvolution as a setup to this
	 * collection. In the viewer format, every image is stored in multiple
	 * resolutions. The resolutions are described as int[] arrays defining
	 * multiple of original pixel size in every dimension. For example
	 * {1,1,1} is the original resolution, {4,4,2} is downsampled by factor
	 * 4 in X and Y and factor 2 in Z. Each resolution of the image is
	 * stored as a chunked three-dimensional array (each chunk corresponds
	 * to one cell of a {@link CellImg} when the data is loaded). The chunk
	 * sizes are defined by the subdivisions parameter which is an array of
	 * int[], one per resolution. Each int[] array describes the X,Y,Z chunk
	 * size for one resolution.
	 *
	 * @param fusionResult
	 *            a fused spim sequence.
	 *            {@link Scripting#createFusionResult(SpimRegistrationSequence, String, String, int, double, double, AffineTransform3D)}
	 * @param resolutionsString
	 *            the set of resolutions to store, formatted like
	 *            "{1,1,1}, {2,2,1}, {4,4,4}" where each "{...}" defines one
	 *            resolution.
	 * @param subdivisionsString
	 *            the set of subdivisions to store, formatted like
	 *            "{32,32,32}, {16,16,8}, {8,8,8}" where each "{...}"
	 *            defines one subdivision.
	 */
	public void addSetups( final FusionResult fusionResult, final String resolutionsString, final String subdivisionsString )
	{
		final int[][] resolutions = PluginHelper.parseResolutionsString( resolutionsString );
		final int[][] subdivisions = PluginHelper.parseResolutionsString( subdivisionsString );
		if ( resolutions.length == 0 )
			throw new RuntimeException( "Cannot parse mipmap resolutions" + resolutionsString );
		if ( subdivisions.length == 0 )
			throw new RuntimeException( "Cannot parse subdivisions " + subdivisionsString );
		else if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		for( final BasicViewSetup setup : fusionResult.getSequenceDescription().getViewSetupsOrdered() )
			add( setup, fusionResult.getSequenceDescription(), fusionResult.getViewRegistrations(), resolutions, subdivisions );
	}

	/**
	 * Add result of SPIM fusion or deconvolution as a setup to this
	 * collection. In the viewer format, every image is stored in multiple
	 * resolutions. The resolutions are described as int[] arrays defining
	 * multiple of original pixel size in every dimension. For example
	 * {1,1,1} is the original resolution, {4,4,2} is downsampled by factor
	 * 4 in X and Y and factor 2 in Z. Each resolution of the image is
	 * stored as a chunked three-dimensional array (final each chunk
	 * corresponds to one cell of a {@link CellImg} when the data is
	 * loaded). The chunk sizes are defined by the subdivisions parameter
	 * which is an array of int[], one per resolution. Each int[] array
	 * describes the X,Y,Z chunk size for one resolution.
	 *
	 * @param fusionResult
	 *            a fused spim sequence.
	 *            {@link Scripting#createFusionResult(SpimRegistrationSequence, String, String, int, double, double, AffineTransform3D)}
	 * @param resolutions
	 *            the set of resolutions to store. each nested int[] array
	 *            defines one resolution.
	 * @param subdivisions
	 *            the set of subdivisions to store. each nested int[] array
	 *            defines one subdivision.
	 */
	public void addSetups( final FusionResult fusionResult, final int[][] resolutions, final int[][] subdivisions )
	{
		if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		for( final BasicViewSetup setup : fusionResult.getSequenceDescription().getViewSetupsOrdered() )
			add( setup, fusionResult.getSequenceDescription(), fusionResult.getViewRegistrations(), resolutions, subdivisions );
	}
}