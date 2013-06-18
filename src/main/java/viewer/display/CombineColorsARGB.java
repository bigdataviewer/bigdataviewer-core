package viewer.display;

import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.ARGBType;

public class CombineColorsARGB extends Accumulate< ARGBType >
{
	final int[] r, g, b;
	
	public CombineColorsARGB( final List< RandomAccessible< ARGBType > > sources, final List< ARGBType > colors )
	{
		super( sources, new ARGBType() );
		
		this.r = new int[ colors.size() ];
		this.g = new int[ colors.size() ];
		this.b = new int[ colors.size() ];
		
		for ( int s = 0; s < colors.size(); ++s )
		{
			final ARGBType color = colors.get( s );
			final int value = color.get();
			
			this.r[ s ] =  ARGBType.red( value );
			this.g[ s ] =  ARGBType.green( value );
			this.b[ s ] =  ARGBType.blue( value );
		}
	}

	@Override
	protected void accumulate( final RandomAccess< ARGBType >[] accesses, final ARGBType target )
	{
		int rSum = 0, gSum = 0, bSum = 0;
		for ( int i = 0; i < accesses.length; ++i )
		{
			final int value = accesses[ i ].get().get();
			final float tmp = ( (float)ARGBType.red( value ) + (float)ARGBType.green( value ) + (float)ARGBType.blue( value ) ) / ( 3.0f * 255.0f );
			
			rSum += this.r[ i ] * tmp;
			gSum += this.g[ i ] * tmp;
			bSum += this.b[ i ] * tmp;
		}
		
		if ( rSum > 255 )
			rSum = 255;
		if ( gSum > 255 )
			gSum = 255;
		if ( bSum > 255 )
			bSum = 255;
		
		target.set( ARGBType.rgba( rSum, gSum, bSum, 255 ) );
	}
}
