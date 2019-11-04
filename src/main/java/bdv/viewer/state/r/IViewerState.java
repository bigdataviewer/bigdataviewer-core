package bdv.viewer.state.r;

import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

public interface IViewerState
{
	Interpolation getInterpolation();

	DisplayMode getDisplayMode();

	int getNumTimepoints();

	int getCurrentTimepoint();

	void getViewerTransform( AffineTransform3D transform );

	default AffineTransform3D getViewerTransform()
	{
		final AffineTransform3D t = new AffineTransform3D();
		getViewerTransform( t );
		return t;
	}

	SourceGroups getGroups();

	Sources getSources();

	// -- modification --

	/**
	 * TODO (optional operation).
 	 */
	void setInterpolation( Interpolation interpolation );

	/**
	 * TODO (optional operation).
	 */
	void setDisplayMode( DisplayMode mode );

	/**
	 * TODO (optional operation).
	 */
	void setNumTimepoints( int numTimepoints );

	/**
	 * TODO (optional operation).
	 */
	void setCurrentTimepoint( int timepoint );

	/**
	 * TODO (optional operation).
	 */
	void setViewerTransform( AffineTransform3D transform );

	// unclear, most likely
	// optional operation?
	Listeners< ViewerStateChangeListener > changeListeners();

	// unclear...
	IViewerState snapshot();
}
