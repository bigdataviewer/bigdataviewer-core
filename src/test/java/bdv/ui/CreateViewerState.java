package bdv.ui;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Creates dummy sources and converters for testing SourceTable and SourceGroupTree
 *
 * @author Tobias Pietzsch
 */
public class CreateViewerState
{
	static class TestSource implements Source< UnsignedShortType >
	{
		private static final AtomicInteger id = new AtomicInteger();

		private static final Random random = new Random();

		private static String nextName()
		{
			return "source(" + id.getAndIncrement() + ")";
		}

		private String name;

		private final IntPredicate isPresent;

		private final DefaultConverterSetup converterSetup;

		public TestSource()
		{
			this( nextName(), i -> false );
		}

		public TestSource( final String name )
		{
			this( name, i -> false );
		}

		public TestSource( final IntPredicate isPresent )
		{
			this( nextName(), isPresent );
		}

		public TestSource( final String name, final IntPredicate isPresent )
		{
			this.name = name;
			this.isPresent = isPresent;

			final DefaultConverterSetup s = new DefaultConverterSetup( id.get(), random.nextBoolean() );
			final double a = random.nextDouble() * 65535;
			final double b = random.nextDouble() * 65535;
			s.setDisplayRange( Math.min( a, b ), Math.max( a, b ) );
			s.setColor( new ARGBType( random.nextInt() ) );
			converterSetup = s;
		}

		@Override
		public UnsignedShortType getType()
		{
			return new UnsignedShortType();
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public void setName(String name) { this.name = name; }

		@Override
		public VoxelDimensions getVoxelDimensions()
		{
			return null;
		}

		@Override
		public int getNumMipmapLevels()
		{
			return 1;
		}

		@Override
		public boolean isPresent( final int t )
		{
			return isPresent.test( t );
		}

		@Override
		public RandomAccessibleInterval< UnsignedShortType > getSource( final int t, final int level )
		{
			return null;
		}

		@Override
		public RealRandomAccessible< UnsignedShortType > getInterpolatedSource( final int t, final int level, final Interpolation method )
		{
			return null;
		}

		@Override
		public void getSourceTransform( final int t, final int level, final AffineTransform3D transform )
		{
			transform.identity();
		}

		/**
		 * ConverterSetups need to be associated to Sources somehow.
		 */
		public DefaultConverterSetup getConverterSetup()
		{
			return converterSetup;
		}
	}

	public static SourceAndConverter< ? > createSource()
	{
		final TestSource source = new TestSource();
		return new SourceAndConverter<>( source, source.getConverterSetup() );
	}

	public static ConverterSetup getConverterSetup( SourceAndConverter< ? > source )
	{
		if ( source.getSpimSource() instanceof TestSource )
			return ( ( TestSource ) source.getSpimSource() ).getConverterSetup();
		else
			return null;
	};
}
