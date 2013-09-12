package viewer.gui.transformation;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;

public class ManualSourceTransforms
{
	protected final ArrayList< AffineTransform3D > transforms;

	public ManualSourceTransforms( final List< AffineTransform3D > transforms )
	{
		this.transforms = new ArrayList< AffineTransform3D >( transforms );
	}

	public List< AffineTransform3D > getTransforms()
	{
		return transforms;
	}
}
