package bdv.viewer;

import static bdv.viewer.BlendMode.AVG;
import static bdv.viewer.BlendMode.CUSTOM;
import static bdv.viewer.BlendMode.SUM;
import static bdv.viewer.ViewerStateChange.ACCUMULATE_PROJECTOR_CHANGED;

import org.scijava.listeners.Listeners;

import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.AlphaWeightedAccumulateProjectorARGB;
import net.imglib2.type.numeric.ARGBType;

public class BlendModeSwitcher
{
	private final ViewerState state;

	private AccumulateProjectorFactory< ARGBType > customFactory;

	private BlendMode mode;

	public BlendModeSwitcher( final ViewerState state )
	{
		this.state = state;
		listeners = new Listeners.List<>();

		state.changeListeners().add( c ->
		{
			if ( c == ACCUMULATE_PROJECTOR_CHANGED )
				factoryChanged( state.getAccumulateProjectorFactory() );
		} );
		factoryChanged( state.getAccumulateProjectorFactory() );
	}

	/**
	 * {@code BlendModeChangeListener}s are notified about blend mode changes.
	 */
	public interface BlendModeChangeListener
	{
		void blendModeChanged(BlendMode newMode);
	}

	private final Listeners.List< BlendModeChangeListener > listeners;

	public Listeners< BlendModeChangeListener > changeListeners()
	{
		return listeners;
	}

	public BlendMode getCurrentMode()
	{
		return mode;
	}

	public void switchToNextMode()
	{
		mode = next();
		state.setAccumulateProjectorFactory( factory() );
		listeners.list.forEach( l -> l.blendModeChanged( mode ) );
	}

	private BlendMode next()
	{
		switch ( mode )
		{
		case SUM:
			return AVG;
		case AVG:
			return customFactory != null ? CUSTOM : SUM;
		case CUSTOM:
			return SUM;
		default:
			throw new IllegalArgumentException();
		}
	}

	private AccumulateProjectorFactory< ARGBType > factory()
	{
		switch ( mode )
		{
		case SUM:
			return AccumulateProjectorARGB.factory;
		case AVG:
			return AlphaWeightedAccumulateProjectorARGB.factory;
		case CUSTOM:
			return customFactory;
		default:
			throw new IllegalArgumentException();
		}
	}

	private void factoryChanged( final AccumulateProjectorFactory< ARGBType > factory )
	{
		final BlendMode oldMode = mode;
		mode = BlendMode.of( factory );

		if ( mode == CUSTOM )
			customFactory = factory;

		if ( mode != oldMode )
		{
			listeners.list.forEach( l -> l.blendModeChanged( mode ) );
		}
	}
}
