package net.imglib2.display;

import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util;

public abstract class ARGBARGBColorConverter<R> implements ColorConverter, Converter< R, ARGBType >
{
	protected double min = 0;

	protected double max = 1;

	protected final ARGBType color = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );

	protected int A;

	protected double scaleR;

	protected double scaleG;

	protected double scaleB;
	
	protected int black = 0;
	
	public ARGBARGBColorConverter( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		update();
	}

	@Override
	public ARGBType getColor()
	{
		return color.copy();
	}

	@Override
	public void setColor( final ARGBType c )
	{
		color.set( c );
		update();
	}

	@Override
	public boolean supportsColor()
	{
		return true;
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

	private void update()
	{
		final double scale = 1.0 / ( max - min );
		final int value = color.get();
		A = ARGBType.alpha( value );
		scaleR = ARGBType.red( value ) * scale;
		scaleG = ARGBType.green( value ) * scale;
		scaleB = ARGBType.blue( value ) * scale;
		black = 0;
	}
	
	int convertColor( final int color )
	{
		final int a = ARGBType.alpha( color );
		int r = ARGBType.red( color );
		int g = ARGBType.green( color );
		int b = ARGBType.blue( color );
		
		final int v = Math.min( 255, Math.max( 0, ( r + g + b ) / 3 ) );
		
		final int newR = (int)Math.min( 255, Util.round( scaleR * v ));
		final int newG = (int)Math.min( 255, Util.round( scaleG * v ));
		final int newB = (int)Math.min( 255, Util.round( scaleB * v ));
		
		return ARGBType.rgba( newR, newG, newB, a );
	}

	public static class ToGray extends ARGBARGBColorConverter<ARGBType>
	{
		public ToGray( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final ARGBType input, final ARGBType output )
		{
			output.set( convertColor( input.get() ));
		}
	}
	
	public static class VolatileToGray extends ARGBARGBColorConverter<VolatileARGBType>
	{
		public VolatileToGray( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final VolatileARGBType input, final ARGBType output )
		{
			output.set( convertColor( input.get().get() ));
		}
	}
}
