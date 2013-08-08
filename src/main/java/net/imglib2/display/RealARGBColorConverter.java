package net.imglib2.display;

import net.imglib2.converter.Converter;
import net.imglib2.display.LinearRange;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class RealARGBColorConverter< R extends RealType< ? > > implements LinearRange, Converter< R, ARGBType >
{
	private double min = 0;

	private double max = 1;

	private final ARGBType color = new ARGBType();

	private int A;

	private double scaleR;

	private double scaleG;

	private double scaleB;

	public RealARGBColorConverter()
	{
		update();
	}

	public RealARGBColorConverter( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		update();
	}

	@Override
	public void convert( final R input, final ARGBType output )
	{
		final double v = input.getRealDouble() - min;
		final int r0 = ( int ) ( scaleR * v + 0.5 );
		final int g0 = ( int ) ( scaleG * v + 0.5 );
		final int b0 = ( int ) ( scaleB * v + 0.5 );
		final int r = r0 > 255 ? 255 : r0 < 0 ? 0 : r0;
		final int g = g0 > 255 ? 255 : g0 < 0 ? 0 : g0;
		final int b = b0 > 255 ? 255 : b0 < 0 ? 0 : b0;
		output.set( ARGBType.rgba( r, g, b, A) );
	}

	public ARGBType getColor()
	{
		return color.copy();
	}

	public void setColor( final ARGBType c )
	{
		color.set( c );
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

	private void update()
	{
		final double scale = 1.0 / ( max - min );
		final int value = color.get();
		A = ARGBType.alpha( value );
		scaleR = ARGBType.red( value ) * scale;
		scaleG = ARGBType.green( value ) * scale;
		scaleB = ARGBType.blue( value ) * scale;
	}
}
