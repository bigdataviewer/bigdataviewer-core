package creator;

import java.io.File;
import java.util.ArrayList;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;
import mpicbg.spim.data.ViewSetup;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;

import creator.spim.FusionResult;
import creator.spim.SpimRegistrationSequence;

/**
 * Aggregate {@link ViewSetup setups}, i.e., SPIM source angles and fused
 * datasets from multiple {@link SequenceDescription}s. Also keeps for each
 * setup the mipmap resolutions and subdivisions to be created.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SetupAggregator
{
	/**
	 * timepoint id for every timepoint index.
	 */
	final protected ArrayList< Integer > timepoints;

	/**
	 * the id (not index!) of the reference timepoint.
	 */
	protected int referenceTimePoint;

	final protected ArrayList< ViewRegistration > registrations;

	/**
	 * Contains {@link ViewSetupWrapper wrappers} around setups in other sequences.
	 */
	final protected ArrayList< ViewSetupWrapper > setups;

	final protected ArrayList< int[][] > perSetupResolutions;

	final protected ArrayList< int[][] > perSetupSubdivisions;

	/**
	 * An {@link ImgLoader} that forwards to wrapped source sequences.
	 */
	final protected ImgLoader imgLoader;

	/**
	 * Create an empty aggregator.
	 */
	public SetupAggregator()
	{
		timepoints = new ArrayList< Integer >();
		referenceTimePoint = 0;
		registrations = new ArrayList< ViewRegistration >();
		setups = new ArrayList< ViewSetupWrapper >();
		perSetupResolutions = new ArrayList< int[][] >();
		perSetupSubdivisions = new ArrayList< int[][] >();
		imgLoader = new ImgLoader()
		{
			@Override
			public void init( final Element elem, final File basePath )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public Element toXml( final File basePath )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public RandomAccessibleInterval< FloatType > getImage( final View view )
			{
				throw new UnsupportedOperationException( "not implemented" );
			}

			@Override
			public RandomAccessibleInterval< UnsignedShortType > getUnsignedShortImage( final View view )
			{
				final ViewSetupWrapper w = ( ViewSetupWrapper ) view.getSetup();
				return w.getSourceSequence().imgLoader.getUnsignedShortImage( new View( w.getSourceSequence(), view.getTimepointIndex(), w.getSourceSetupIndex(), view.getModel() ) );
			}
		};
	}

	/**
	 * Add a new {@link ViewSetup} to the aggregator.
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
	 * @return the setup index of the new {@link ViewSetup} in the aggregator.
	 */
	public int add( final ViewSetup sourceSetup, final SequenceDescription sourceSequence, final ViewRegistrations sourceRegs, final int[][] resolutions, final int[][] subdivisions )
	{
		final int setupIdx = setups.size();
		setups.add( new ViewSetupWrapper( setupIdx, sourceSequence, sourceSetup ) );
		if ( setupIdx == 0 )
		{
			// if this is the first setup added, initialize the timepoints array and the reference timepoint
			timepoints.addAll( sourceSequence.timepoints );
			referenceTimePoint = sourceRegs.referenceTimePoint;
		}
		final int s = sourceSetup.getId();
		for ( int timepointIdx = 0; timepointIdx < timepoints.size(); ++timepointIdx )
		{
			final int tp = timepoints.get( timepointIdx );
			boolean found = false;
			for( final ViewRegistration r : sourceRegs.registrations )
				if ( s == r.getSetupIndex() && tp == sourceSequence.timepoints.get( r.getTimepointIndex() ) )
				{
					found = true;
					registrations.add( new ViewRegistration( timepointIdx, setupIdx, r.getModel() ) );
					break;
				}
			if ( !found )
				throw new RuntimeException( "could not find ViewRegistration for timepoint " + tp + " in the source sequence." );
		}
		perSetupResolutions.add( resolutions );
		perSetupSubdivisions.add( subdivisions );
		return setupIdx;
	}

	/**
	 * Create a {@link SequenceDescription} for the setups currently aggregated.
	 * This {@link SequenceDescription} can be used to write the sequence (see
	 * {@link WriteSequenceToHdf5} and {@link WriteSequenceToXml}).
	 *
	 * @param basePath
	 * @return a {@link SequenceDescription} with the currently aggregated
	 *         setups.
	 */
	public SequenceDescription createSequenceDescription( final File basePath )
	{
		return new SequenceDescription( setups, timepoints, basePath, imgLoader );
	}

	/**
	 * Create {@link ViewRegistrations} for the setups currently aggregated.
	 * This {@link ViewRegistrations} can be used to write the sequence (see
	 * {@link WriteSequenceToXml}).
	 *
	 * @param basePath
	 * @return {@link ViewRegistrations} for the currently aggregated
	 *         setups.
	 */
	public ViewRegistrations createViewRegistrations()
	{
		return new ViewRegistrations( new ArrayList< ViewRegistration >( registrations ), referenceTimePoint );
	}

	/**
	 * Get the aggregated per-setup mipmap resolutions.
	 */
	public ArrayList< int[][] > getPerSetupResolutions()
	{
		return perSetupResolutions;
	}

	/**
	 * Get the aggregated per-setup mipmap level subdivisions.
	 */
	public ArrayList< int[][] > getPerSetupSubdivisions()
	{
		return perSetupSubdivisions;
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
		final SequenceDescription desc = sequence.getSequenceDescription();
		final ViewRegistrations regs = sequence.getViewRegistrations();
		final int[][] resolutions = PluginHelper.parseResolutionsString( resolutionsString );
		final int[][] subdivisions = PluginHelper.parseResolutionsString( subdivisionsString );
		if ( resolutions.length == 0 )
			throw new RuntimeException( "Cannot parse mipmap resolutions" + resolutionsString );
		if ( subdivisions.length == 0 )
			throw new RuntimeException( "Cannot parse subdivisions " + subdivisionsString );
		else if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		final ViewSetup setup = desc.setups.get( setupIndex );
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
		final SequenceDescription desc = sequence.getSequenceDescription();
		final ViewRegistrations regs = sequence.getViewRegistrations();
		if ( resolutions.length != subdivisions.length )
			throw new RuntimeException( "mipmap resolutions and subdivisions must have the same number of elements" );
		final ViewSetup setup = desc.setups.get( setupIndex );
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
		for ( int s = 0; s < sequence.getSequenceDescription().setups.size(); ++s )
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
		for ( int s = 0; s < sequence.getSequenceDescription().setups.size(); ++s )
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
		for ( final ViewSetup setup : fusionResult.getSequenceDescription().setups )
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
		for ( final ViewSetup setup : fusionResult.getSequenceDescription().setups )
			add( setup, fusionResult.getSequenceDescription(), fusionResult.getViewRegistrations(), resolutions, subdivisions );
	}
}