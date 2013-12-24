package bdv.tools.transformation;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import bdv.viewer.InputActionBindings;
import bdv.viewer.Source;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;


// TODO: what happens when the current source, display mode, etc is changed while the editor is active? deactivate?
public class ManualTransformationEditor implements TransformListener< AffineTransform3D >
{

	private boolean active = false;

	private final InputActionBindings bindings;

	private final ViewerPanel viewer;

	private final AffineTransform3D frozenTransform;

	private final AffineTransform3D liveTransform;

	private final ArrayList< TransformedSource< ? > > sourcesToModify;

	private final ArrayList< TransformedSource< ? > > sourcesToFix;

	private final ActionMap actionMap;

	private final InputMap inputMap;

	public ManualTransformationEditor( final ViewerPanel viewer, final InputActionBindings inputActionBindings )
	{
		this.viewer = viewer;
		bindings = inputActionBindings;
		frozenTransform = new AffineTransform3D();
		liveTransform = new AffineTransform3D();
		sourcesToModify = new ArrayList< TransformedSource< ? > >();
		sourcesToFix = new ArrayList< TransformedSource< ? > >();

		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new AbstractAction( "abort manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};
		final KeyStroke resetKey = KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 );
		final Action resetAction = new AbstractAction( "reset manual transformation" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				reset();
			}

			private static final long serialVersionUID = 1L;
		};
		actionMap = new ActionMap();
		inputMap = new InputMap();
		actionMap.put( "abort manual transformation", abortAction );
		inputMap.put( abortKey, "abort manual transformation" );
		actionMap.put( "reset manual transformation", resetAction );
		inputMap.put( resetKey, "reset manual transformation" );
		bindings.addActionMap( "manual transform", actionMap );
	}

	public synchronized void abort()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
				source.setIncrementalTransform( identity );
			viewer.setCurrentViewerTransform( frozenTransform );
			viewer.showMessage( "aborted manual transform" );
			active = false;
		}
	}

	public synchronized void reset()
	{
		if ( active )
		{
			final AffineTransform3D identity = new AffineTransform3D();
			for ( final TransformedSource< ? > source : sourcesToModify )
			{
				source.setIncrementalTransform( identity );
				source.setFixedTransform( identity );
			}
			for ( final TransformedSource< ? > source : sourcesToFix )
			{
				source.setIncrementalTransform( identity );
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
			final List< Integer > indices = new ArrayList< Integer >();
			switch ( state.getDisplayMode() )
			{
			case FUSED:
				indices.add( state.getCurrentSource() );
				break;
			case FUSEDGROUP:
				final SourceGroup group = state.getSourceGroups().get( state.getCurrentGroup() );
				indices.addAll( group.getSourceIds() );
				break;
			default:
				viewer.showMessage( "Can only do manual transformation when in FUSED mode." );
				return;
			}
			state.getViewerTransform( frozenTransform );
			sourcesToModify.clear();
			sourcesToFix.clear();
			final int numSources = state.numSources();
			for ( int i = 0; i < numSources; ++i )
			{
				final Source< ? > source = state.getSources().get( i ).getSpimSource();
				if ( TransformedSource.class.isInstance( source ) )
				{
					if ( indices.contains( i ) )
						sourcesToModify.add( (bdv.tools.transformation.TransformedSource< ? > ) source );
					else
						sourcesToFix.add( (bdv.tools.transformation.TransformedSource< ? > ) source );
				}
			}
			active = true;
			viewer.addTransformListener( this );
			bindings.addInputMap( "manual transform", inputMap );
			viewer.showMessage( "starting manual transform" );
		}
		else
		{ // Exit manual edit mode.
			active = false;
			viewer.removeTransformListener( this );
			bindings.removeInputMap( "manual transform" );
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
