package bdv.tools.links;

import bdv.viewer.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;

class BdvPropertiesV0
{
	private final AffineTransform3D transform;

	public BdvPropertiesV0()
	{
		transform = new AffineTransform3D();
	}

	public BdvPropertiesV0( AffineTransform3D transform )
	{
		this.transform = transform;
	}

	public AffineTransform3D transform()
	{
		return transform;
	}

	static BdvPropertiesV0 fromViewerState( final ViewerState state )
	{
		return new BdvPropertiesV0( state.getViewerTransform() );
	}
}
