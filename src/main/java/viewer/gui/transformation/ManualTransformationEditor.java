package viewer.gui.transformation;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import viewer.SpimViewer;
import viewer.TextOverlayAnimator;
import viewer.render.DisplayMode;
import viewer.render.Source;
import viewer.render.TransformedSource;
import viewer.render.ViewerState;


// TODO: what happens when the current source, display mode, etc is changed while the editor is active? deactivate?
public class ManualTransformationEditor implements TransformListener< AffineTransform3D >
{

	private boolean active = false;

	private final SpimViewer viewer;

	private TextOverlayAnimator animatedOverlay;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	private final ArrayList< TransformedSource< ? > > sourcesToModify;

	private final ArrayList< TransformedSource< ? > > sourcesToFix;

	private final KeyStroke abortKey;

	private final Action abortAction;

	private final KeyStroke resetKey;

	private final Action resetAction;

	public ManualTransformationEditor( final SpimViewer viewer )
	{
		this.viewer = viewer;
		frozenTransform = new AffineTransform3D();
		liveTransform = new AffineTransform3D();
		sourcesToModify = new ArrayList< TransformedSource< ? > >();
		sourcesToFix = new ArrayList< TransformedSource< ? > >();
		abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		abortAction = new AbstractAction( "abort manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};
		resetKey = KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 );
		resetAction = new AbstractAction( "reset manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				reset();
			}

			private static final long serialVersionUID = 1L;
		};
	}

	public synchronized void abort()
	{
		if ( active )
		{
			final AffineTransform3D tmp = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
				source.setIncrementalTransform( tmp );
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "aborted manual transform" );
			active = false;
		}
	}

	public synchronized void reset()
	{
		if ( active )
		{
			final AffineTransform3D tmp = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				source.setIncrementalTransform( tmp );
				source.setFixedTransform( tmp );
			}
			for ( final TransformedSource< ? > source : sourcesToFix )
			{
				source.setIncrementalTransform( tmp );
				source.setFixedTransform( tmp );
			}
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "reset manual transform" );
		}
	}

	public synchronized void toggle()
	{
		if ( !active )
		{ // Enter manual edit mode
			final ViewerState state = viewer.getState();
			if ( state.getDisplayMode() != DisplayMode.FUSED )
			{
				viewer.showMessage( "Can only do manual transformation when in FUSED mode." );
				return;
			}
			else
			{
				state.getViewerTransform( frozenTransform );
				final List< Integer > indices = Arrays.asList( new Integer( state.getCurrentSource() ) );
				sourcesToModify.clear();
				sourcesToFix.clear();
				final int numSources = state.numSources();
				for ( int i = 0; i < numSources; ++i )
				{
					final Source< ? > source = state.getSources().get( i ).getSpimSource();
					if ( TransformedSource.class.isInstance( source ) )
					{
						if ( indices.contains( i ) )
							sourcesToModify.add( ( TransformedSource< ? > ) source );
						else
							sourcesToFix.add( ( TransformedSource< ? > ) source );
					}
				}
				active = true;
				viewer.addTransformListener( this );
				viewer.addKeyAction( abortKey, abortAction ); // TODO: we must be able to remove this
				viewer.addKeyAction( resetKey, resetAction ); // TODO: we must be able to remove this
				viewer.showMessage( "starting manual transform" );
			}
		}
		else
		{ // Exit manual edit mode.
			active = false;
			viewer.removeTransformListener( this );
			final AffineTransform3D tmp = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				tmp.identity();
				source.setIncrementalTransform( tmp );
				source.getFixedTransform( tmp );
				tmp.preConcatenate( liveTransform );
				source.setFixedTransform( tmp );
			}
			tmp.identity();
			for ( final TransformedSource< ? > source : sourcesToFix )
				source.setIncrementalTransform( tmp );
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "fixed manual transform" );
		}
	}

	@Override
	public void transformChanged( final AffineTransform3D transform )
	{
		if ( !active ) { return; }

		liveTransform.set( transform );
		liveTransform.preConcatenate( frozenTransform.inverse() );

		for ( final TransformedSource< ? > source : sourcesToFix )
			source.setIncrementalTransform( liveTransform.inverse() );
	}

}
