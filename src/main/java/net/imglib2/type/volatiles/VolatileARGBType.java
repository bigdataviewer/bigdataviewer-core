package net.imglib2.type.volatiles;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileIntAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileShortAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * A {@link Volatile} variant of {@link UnsignedShortType}. It uses an
 * underlying {@link UnsignedShortType} that maps into a
 * {@link VolatileShortAccess}.
 *
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class VolatileARGBType extends AbstractVolatileNativeNumericType< ARGBType, VolatileARGBType >
{
	final protected NativeImg< ?, ? extends VolatileIntAccess > img;

	private static class WrappedARGBType extends ARGBType
	{
		public WrappedARGBType( final NativeImg< ?, ? extends IntAccess > img )
		{
			super( img );
		}
		
		public WrappedARGBType( final IntAccess access )
		{
			super( access );
		}

		public void setAccess( final IntAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileARGBType( final NativeImg< ?, ? extends VolatileIntAccess > img )
	{
		super( new WrappedARGBType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileARGBType( final VolatileIntAccess access )
	{
		super( new WrappedARGBType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileARGBType( final int value )
	{
		this( new VolatileIntArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileARGBType()
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
		final VolatileIntAccess a = img.update( c );
		( ( WrappedARGBType ) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileARGBType, ? extends VolatileIntAccess > createSuitableNativeImg( final NativeImgFactory< VolatileARGBType > storageFactory, final long[] dim )
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public VolatileARGBType duplicateTypeOnSameNativeImg()
	{
		return new VolatileARGBType( img );
	}

	@Override
	public VolatileARGBType createVariable()
	{
		return new VolatileARGBType();
	}

	@Override
	public VolatileARGBType copy()
	{
		final VolatileARGBType v = createVariable();
		v.set( this );
		return v;
	}
}
