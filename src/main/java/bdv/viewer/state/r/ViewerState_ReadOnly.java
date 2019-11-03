package bdv.viewer.state.r;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import net.imglib2.realtransform.AffineTransform3D;

public interface ViewerState_ReadOnly
{
	Interpolation getInterpolation();

	DisplayMode getDisplayMode();

	int getNumTimepoints();

	int getCurrentTimepoint();

	SourceGroups getGroups();

	Sources getSources();

	void getViewerTransform( AffineTransform3D t );

	default AffineTransform3D getViewerTransform()
	{
		final AffineTransform3D t = new AffineTransform3D();
		getViewerTransform( t );
		return t;
	}
}
