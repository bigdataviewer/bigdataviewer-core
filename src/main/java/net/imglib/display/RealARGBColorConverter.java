package net.imglib.display;

import net.imglib2.converter.Converter;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class RealARGBColorConverter< R extends RealType< ? > > extends AbstractLinearRange implements Converter< R, ARGBType >
{
	protected final ARGBType color = new ARGBType();

	protected int A;

	protected int R;

	protected int G;

	protected int B;

	public RealARGBColorConverter()
	{
		super();
	}

	public RealARGBColorConverter( final double min, final double max )
	{
		super( min, max );
	}

	@Override
	public void convert( final R input, final ARGBType output )
	{
		final double v = ( input.getRealDouble() - min ) / scale;
		final int a = Math.min( 255, roundPositive( Math.max( 0, A ) ) );
		final int r = Math.min( 255, roundPositive( Math.max( 0, R * v ) ) );
		final int g = Math.min( 255, roundPositive( Math.max( 0, G * v ) ) );
		final int b = Math.min( 255, roundPositive( Math.max( 0, B * v ) ) );
		output.set( ARGBType.rgba( r, g, b, a) );
	}

	public ARGBType getColor()
	{
		return color.copy();
	}

	public void setColor( final ARGBType c )
	{
		color.set( c );
		final int value = color.get();
		A = ARGBType.alpha( value );
		R = ARGBType.red( value );
		G = ARGBType.green( value );
		B = ARGBType.blue( value );
	}
}
