package net.imglib2.display;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileARGBType;

public abstract class ScaledARGBConverter< T > implements ColorConverter, Converter< T, ARGBType >
{
	protected double min = 0;

	protected double max = 1;

	protected double scale;

	private ScaledARGBConverter( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		update();
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMax( final double max )
	{
		this.max = max;
		update();
	}

	@Override
	public void setMin( final double min )
	{
		this.min = min;
		update();
	}

	@Override
	public ARGBType getColor()
	{
		return new ARGBType();
	}

	@Override
	public void setColor( final ARGBType c )
	{}

	@Override
	public boolean supportsColor()
	{
		return false;
	}

	private void update()
	{
		scale = 255.0 / ( max - min );
	}

	int getScaledColor( final int color )
	{
		final int a = ARGBType.alpha( color );
		final int r = Math.min( 255, ( int ) ( scale * Math.max( 0, ARGBType.red( color ) - min ) + 0.5 ) );
		final int g = Math.min( 255, ( int ) ( scale * Math.max( 0, ARGBType.green( color ) - min ) + 0.5 ) );
		final int b = Math.min( 255, ( int ) ( scale * Math.max( 0, ARGBType.blue( color ) - min ) + 0.5 ) );
		return ARGBType.rgba( r, g, b, a );
	}

	public static class ARGB extends ScaledARGBConverter< ARGBType >
	{
		public ARGB( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final ARGBType input, final ARGBType output )
		{
			output.set( getScaledColor( input.get() ) );
		}
	}

	public static class VolatileARGB extends ScaledARGBConverter< VolatileARGBType >
	{
		public VolatileARGB( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final VolatileARGBType input, final ARGBType output )
		{
			output.set( getScaledColor( input.get().get() ) );
		}
	}
}
