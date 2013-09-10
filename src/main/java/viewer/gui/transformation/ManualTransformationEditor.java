package viewer.gui.transformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.render.DisplayMode;
import viewer.render.Source;
import viewer.render.TransformedSource;
import viewer.render.ViewerState;


// TODO: re-use TextOverlay of SpimViewer (don't implement OverlayRenderer)
// TODO: construct with TransformedSource<?> List to avoid casting every time?
// TODO: what happens when the current source, display mode, etc is changed while the editor is active? deactivate?
public class ManualTransformationEditor implements TransformListener< AffineTransform3D >
{

	private boolean active = false;

	private final SpimViewer viewer;

	private TextOverlayAnimator animatedOverlay;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	private final ArrayList< TransformedSource< ? > > sourcesToModify;

	public ManualTransformationEditor( final SpimViewer viewer )
	{
		this.viewer = viewer;
		frozenTransform = new AffineTransform3D();
		liveTransform = new AffineTransform3D();
		sourcesToModify = new ArrayList< TransformedSource< ? > >();
	}

	public synchronized void toggle()
	{
		if ( !active )
		{ // Enter manual edit mode
			final ViewerState state = viewer.getState();
			if ( state.getDisplayMode() != DisplayMode.FUSED )
			{
// TODO:		animatedOverlay = new TextOverlayAnimator( "Can only do manual transformation when in FUSED mode.", 1000 );
				return;
			}
			else
			{
				state.getViewerTransform( frozenTransform );
				final List< Integer > indices = Arrays.asList( new Integer( state.getCurrentSource() ) );
				sourcesToModify.clear();
				for ( final int i : indices )
				{
					final Source< ? > source = state.getSources().get( i ).getSpimSource();
					if ( TransformedSource.class.isInstance( source ) )
						sourcesToModify.add( ( TransformedSource< ? > ) source );
				}
				active = true;
				viewer.addTransformListener( this );
			}
		}
		else
		{ // Exit manual edit mode.
			active = false;
			viewer.removeTransformListener( this );
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				final AffineTransform3D tmp = new AffineTransform3D();
				source.getIncrementalTransform( frozenTransform );
				source.getFixedTransform( tmp );
				frozenTransform.concatenate( tmp );
				tmp.identity();
				source.setIncrementalTransform( tmp );
				source.setFixedTransform( frozenTransform );
			}
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		if ( !active ) { return; }

		liveTransform.set( transform );
		liveTransform.preConcatenate( frozenTransform.inverse() );

		for ( final TransformedSource< ? > source : sourcesToModify )
			source.setIncrementalTransform( liveTransform.inverse() );
	}

}
