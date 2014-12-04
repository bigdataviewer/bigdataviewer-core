package bdv.viewer.overlay;

import bdv.util.ModifiableInterval;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;

public class IntervalAndTransform implements MultiBoxOverlay.IntervalAndTransform
{
	protected boolean isVisible;

	protected ModifiableInterval sourceInterval;

	protected AffineTransform3D sourceToViewer;

	public IntervalAndTransform()
	{
		isVisible = false;
		sourceInterval = new ModifiableInterval( 3 );
		sourceToViewer = new AffineTransform3D();
	}

	public void set( final boolean visible, final Interval sourceInterval, final AffineTransform3D sourceToViewer )
	{
		setVisible( visible );
		setSourceInterval( sourceInterval );
		setSourceToViewer( sourceToViewer );
	}

	public void setVisible( final boolean visible )
	{
		isVisible = visible;
	}

	public void setSourceInterval( final Interval interval )
	{
		sourceInterval.set( interval );
	}

	public void setSourceToViewer( final AffineTransform3D t )
	{
		sourceToViewer.set( t );
	}

	@Override
	public boolean isVisible()
	{
		return isVisible;
	}

	@Override
	public Interval getSourceInterval()
	{
		return sourceInterval;
	}

	@Override
	public AffineTransform3D getSourceToViewer()
	{
		return sourceToViewer;
	}
}
