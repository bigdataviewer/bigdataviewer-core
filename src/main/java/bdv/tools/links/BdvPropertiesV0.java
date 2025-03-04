package bdv.tools.links;

import java.util.Arrays;

import bdv.viewer.ViewerState;
import net.imglib2.Dimensions;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;

class BdvPropertiesV0
{
	private final AffineTransform3D transform;

	private final int timepoint;

	private final long[] panelsize;

	private final long[] mousepos;

	public BdvPropertiesV0()
	{
		transform = new AffineTransform3D();
		timepoint = 0;
		panelsize = new long[ 2 ];
		mousepos = new long[ 2 ];
	}

	public BdvPropertiesV0( AffineTransform3D transform, final int timepoint, final long[] panelsize, final long[] mousepos )
	{
		this.transform = transform;
		this.timepoint = timepoint;
		this.panelsize = panelsize;
		this.mousepos = mousepos;
	}

	public AffineTransform3D transform()
	{
		return transform;
	}

	public int timepoint()
	{
		return timepoint;
	}

	@Override
	public String toString()
	{
		return "BdvPropertiesV0{" +
				"transform=" + transform +
				", timepoint=" + timepoint +
				", panelsize=" + Arrays.toString( panelsize ) +
				", mousepos=" + Arrays.toString( mousepos ) +
				'}';
	}

	static BdvPropertiesV0 create(
			final ViewerState state,
			final Dimensions panelsize,
			final Point mousepos )
	{
		return new BdvPropertiesV0(
				state.getViewerTransform(),
				state.getCurrentTimepoint(),
				panelsize.dimensionsAsLongArray(),
				mousepos.positionAsLongArray());
	}
}
