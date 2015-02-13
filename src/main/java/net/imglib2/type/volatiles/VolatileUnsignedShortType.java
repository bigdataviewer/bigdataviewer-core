package net.imglib2.type.volatiles;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileShortAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * A {@link Volatile} variant of {@link UnsignedShortType}. It uses an
 * underlying {@link UnsignedShortType} that maps into a
 * {@link VolatileShortAccess}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class VolatileUnsignedShortType extends AbstractVolatileNativeRealType< UnsignedShortType, VolatileUnsignedShortType >
{
	final protected NativeImg< ?, ? extends VolatileShortAccess > img;

	private static class WrappedUnsignedShortType extends UnsignedShortType
	{
		public WrappedUnsignedShortType( final NativeImg<?, ? extends ShortAccess> img )
		{
			super( img );
		}
		
		public WrappedUnsignedShortType( final ShortAccess access )
		{
			super( access );
		}

		public void setAccess( final ShortAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileUnsignedShortType( final NativeImg< ?, ? extends VolatileShortAccess > img )
	{
		super( new WrappedUnsignedShortType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileUnsignedShortType( final VolatileShortAccess access )
	{
		super( new WrappedUnsignedShortType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileUnsignedShortType( final int value )
	{
		this( new VolatileShortArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileUnsignedShortType()
	{
		this( 0 );
	}

	public void set( final int value )
	{
		get().set( value );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileShortAccess a = img.update( c );
		( (WrappedUnsignedShortType) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileUnsignedShortType, ? extends VolatileShortAccess > createSuitableNativeImg( final NativeImgFactory< VolatileUnsignedShortType > storageFactory, final long[] dim )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileUnsignedShortType duplicateTypeOnSameNativeImg()
	{
		return new VolatileUnsignedShortType( img );
	}

	@Override
	public VolatileUnsignedShortType createVariable()
	{
		return new VolatileUnsignedShortType();
	}

	@Override
	public VolatileUnsignedShortType copy()
	{
		final VolatileUnsignedShortType v = createVariable();
		v.set( this );
		return v;
	}
}
