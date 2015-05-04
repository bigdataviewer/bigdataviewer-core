package bdv.cl;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLCommandQueue.Mode;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLProgram;

public class CompileTest
{
	public static void main( final String[] args )
	{
		CLContext cl;
		CLCommandQueue queue;
		final BlockTexture blockTexture;
		CLKernel slice;
		try
		{
			cl = CLContext.create();

			CLDevice device = null;
			for ( final CLDevice dev : cl.getDevices() )
			{
				if ( "GeForce GT 650M".equals( dev.getName() ) )
				//				if ( "HD Graphics 4000".equals( dev.getName() ) )
				{
					device = dev;
					System.out.println( "using " + dev.getName() );
					break;
				}
			}
			queue = device.createCommandQueue( Mode.PROFILING_MODE );

			final CLProgram program = cl.createProgram( CompileTest.class.getResourceAsStream( "slice2f4.cl" ) ).build();
			slice = program.createCLKernel( "slice" );

			queue.release();
			cl.release();
		}
		catch ( final Exception e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
