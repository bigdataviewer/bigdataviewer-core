package viewer.gui.transformation;

import java.awt.Graphics;
import java.awt.Graphics2D;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.ui.TransformListener;
import viewer.TextOverlayAnimator;
import viewer.render.DisplayMode;
import viewer.render.SourceState;
import viewer.render.TransformedSource;
import viewer.render.ViewerState;

public class ManualTransformationEditor implements TransformListener< AffineTransform3D >, OverlayRenderer
{

	private boolean active = false;

	private final ViewerState state;

	private TextOverlayAnimator animatedOverlay;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	public ManualTransformationEditor( final ViewerState state )
	{
		this.state = state;
		this.frozenTransform = new AffineTransform3D();
		this.liveTransform = new AffineTransform3D();
	}

	public synchronized void toggle()
	{
		if ( !active )
		{ // Enter manual edit mode
			if ( state.getDisplayMode() != DisplayMode.FUSED )
			{
				animatedOverlay = new TextOverlayAnimator( "Can only do manual transformation when in FUSED mode.", 1000 );
				return;
			}
			else
			{

				active = true;
				state.getViewerTransform( frozenTransform );
			}
		}
		else
		{ // Exit manual edit mode.

			final int currentSource = state.getCurrentSource();
			final SourceState< ? > sourceState = state.getSources().get( currentSource );
			final TransformedSource< ? > source = ( TransformedSource< ? > ) sourceState.getSpimSource();
			final AffineTransform3D tmp = new AffineTransform3D();
			source.getIncrementalTransform( frozenTransform );
			source.getFixedTransform( tmp );
			frozenTransform.concatenate( tmp );
			tmp.identity();
			source.setIncrementalTransform( tmp );
			source.setFixedTransform( frozenTransform );
			active = false;
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		if ( !active ) { return; }

		final int currentSource = state.getCurrentSource();
		final SourceState< ? > sourceState = state.getSources().get( currentSource );
		final TransformedSource< ? > source = ( TransformedSource< ? > ) sourceState.getSpimSource();

		state.getViewerTransform( liveTransform );
		liveTransform.preConcatenate( frozenTransform.inverse() );

		source.setIncrementalTransform( liveTransform.inverse() );
	}

	@Override
	public void drawOverlays( final Graphics g )
	{
		if ( animatedOverlay != null )
		{
			animatedOverlay.paint( ( Graphics2D ) g, System.currentTimeMillis() );
			if ( animatedOverlay.isComplete() )
			{
				animatedOverlay = null;
			}
		}
	}

	@Override
	public void setCanvasSize( final int width, final int height )
	{}

}
