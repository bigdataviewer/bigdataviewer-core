package net.imglib2.type.volatiles;

import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileFloatAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileFloatArray;
import net.imglib2.type.numeric.real.FloatType;

public class VolatileFloatType extends AbstractVolatileNativeRealType< FloatType, VolatileFloatType >
{
	final protected NativeImg< ?, ? extends VolatileFloatAccess > img;

	private static class WrappedFloatType extends FloatType
	{
		public WrappedFloatType( final NativeImg<?, ? extends FloatAccess> img )
		{
			super( img );
		}

		public WrappedFloatType( final FloatAccess access )
		{
			super( access );
		}

		public void setAccess( final FloatAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileFloatType( final NativeImg< ?, ? extends VolatileFloatAccess > img )
	{
		super( new WrappedFloatType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileFloatType( final VolatileFloatAccess access )
	{
		super( new WrappedFloatType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileFloatType( final float value )
	{
		this( new VolatileFloatArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileFloatType()
	{
		this( 0 );
	}

	public void set( final float value )
	{
		get().set( value );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileFloatAccess a = img.update( c );
		( ( WrappedFloatType )t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileFloatType, ? extends VolatileFloatAccess > createSuitableNativeImg( final NativeImgFactory< VolatileFloatType > storageFactory, final long[] dim )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileFloatType duplicateTypeOnSameNativeImg()
	{
		return new VolatileFloatType( img );
	}

	@Override
	public VolatileFloatType createVariable()
	{
		return new VolatileFloatType();
	}

	@Override
	public VolatileFloatType copy()
	{
		final VolatileFloatType v = createVariable();
		v.set( this );
		return v;
	}
}
