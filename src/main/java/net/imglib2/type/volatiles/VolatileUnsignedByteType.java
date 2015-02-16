package net.imglib2.type.volatiles;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileByteAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * A {@link Volatile} variant of {@link UnsignedByteType}. It uses an
 * underlying {@link UnsignedByteType} that maps into a
 * {@link VolatileByteAccess}.
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class VolatileUnsignedByteType extends AbstractVolatileNativeRealType< UnsignedByteType, VolatileUnsignedByteType >
{
	final protected NativeImg< ?, ? extends VolatileByteAccess > img;

	private static class WrappedUnsignedByteType extends UnsignedByteType
	{
		public WrappedUnsignedByteType( final NativeImg<?, ? extends ByteAccess> img )
		{
			super( img );
		}
		
		public WrappedUnsignedByteType( final ByteAccess access )
		{
			super( access );
		}

		public void setAccess( final ByteAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileUnsignedByteType( final NativeImg< ?, ? extends VolatileByteAccess > img )
	{
		super( new WrappedUnsignedByteType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileUnsignedByteType( final VolatileByteAccess access )
	{
		super( new WrappedUnsignedByteType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileUnsignedByteType( final int value )
	{
		this( new VolatileByteArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileUnsignedByteType()
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
		final VolatileByteAccess a = img.update( c );
		( ( WrappedUnsignedByteType )t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileUnsignedByteType, ? extends VolatileByteAccess > createSuitableNativeImg( final NativeImgFactory< VolatileUnsignedByteType > storageFactory, final long[] dim )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileUnsignedByteType duplicateTypeOnSameNativeImg()
	{
		return new VolatileUnsignedByteType( img );
	}

	@Override
	public VolatileUnsignedByteType createVariable()
	{
		return new VolatileUnsignedByteType();
	}

	@Override
	public VolatileUnsignedByteType copy()
	{
		final VolatileUnsignedByteType v = createVariable();
		v.set( this );
		return v;
	}
}
