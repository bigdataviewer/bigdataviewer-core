package bdv.cl;

import net.imglib2.realtransform.AffineTransform3D;
import bdv.img.hdf5.Hdf5ImageLoader;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLProgram;

public class RenderSlice
{
	private final Hdf5ImageLoader imgLoader;

	private CLContext cl;

	private CLCommandQueue q;

	public RenderSlice( final Hdf5ImageLoader imgLoader )
	{
		this.imgLoader = imgLoader;

		try
		{
			cl = CLContext.create();

			CLDevice device = null;
			for ( final CLDevice dev : cl.getDevices() )
			{
				if ( "GeForce GT 650M".equals( dev.getName() ) )
				{
					device = dev;
					break;
				}
			}
			q = device.createCommandQueue();

			final CLProgram program = cl.createProgram( this.getClass().getResourceAsStream( "slice.cl" ) ).build();
		}
		catch ( final Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void cleanUp()
	{
		if ( q != null )
			q.release();

		if ( cl != null )
			cl.release();
	}

	public void renderSlice( final AffineTransform3D viewerTransform, final int width, final int height )
	{
		System.out.println( "render slice" );
	}
}
