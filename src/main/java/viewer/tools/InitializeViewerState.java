package viewer.tools;

import java.awt.Dimension;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import viewer.ViewerPanel;
import viewer.gui.brightness.MinMaxGroup;
import viewer.gui.brightness.SetupAssignments;
import viewer.render.Source;
import viewer.render.SourceState;
import viewer.render.ViewerState;
import viewer.util.Affine3DHelpers;

public class InitializeViewerState
{
	/**
	 * Set a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = dim_z / 2</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by <em>dim_y</em> is visible.
	 * </ul>
	 * This calls {@link #initTransform(int, int, ViewerPanel)}, using the size
	 * of the viewer's display component.
	 *
	 * @param viewer
	 *            the viewer (containing at least one source) to have its
	 *            transform set.
	 */
	public static void initTransform( final ViewerPanel viewer )
	{
		final Dimension dim = viewer.getDisplay().getSize();
		System.out.println();
		System.out.println( dim );
		System.out.println();
		initTransform( dim.width, dim.height, viewer );
	}

	/**
	 * Set a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the first source,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = dim_z / 2</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by <em>dim_y</em> is visible.
	 * </ul>
	 *
	 * @param viewerWidth
	 *            width of the viewer display
	 * @param viewerHeight
	 *            height of the viewer display
	 * @param viewer
	 *            the viewer (containing at least one source) to have its
	 *            transform set.
	 */
	public static void initTransform( final int viewerWidth, final int viewerHeight, final ViewerPanel viewer )
	{
		final int cX = viewerWidth / 2;
		final int cY = viewerHeight / 2;

		final ViewerState state = viewer.getState();
		final SourceState< ? > source = state.getSources().get( state.getCurrentSource() );
		final int timepoint = state.getCurrentTimepoint();
		final AffineTransform3D sourceTransform = source.getSpimSource().getSourceTransform( timepoint, 0 );

		final Interval sourceInterval = source.getSpimSource().getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 + 1 ) / 2;
		final double sY = ( sY0 + sY1 + 1 ) / 2;
		final double sZ = ( sZ0 + sZ1 + 1 ) / 2;

		final double[][] m = new double[3][4];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY, 1, 3 );

		viewer.setCurrentViewerTransform( viewerTransform );
	}

	/**
	 * TODO
	 *
	 * @param cumulativeMinCutoff
	 * @param cumulativeMaxCutoff
	 * @param viewer
	 * @param setupAssignments
	 */
	public static void initBrightness( final double cumulativeMinCutoff, final double cumulativeMaxCutoff, final ViewerPanel viewer, final SetupAssignments setupAssignments )
	{
		final ViewerState state = viewer.getState();
		final Source< ? > source = state.getSources().get( state.getCurrentSource() ).getSpimSource();
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval ) source.getSource( state.getCurrentTimepoint(), source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d< UnsignedShortType >( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] {0};
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( 0 );
		minmax.getMinBoundedValue().setCurrentValue( min );
		minmax.getMaxBoundedValue().setCurrentValue( max );
	}
}
