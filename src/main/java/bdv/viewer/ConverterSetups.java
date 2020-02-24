package bdv.viewer;

import static bdv.viewer.ViewerStateChange.NUM_SOURCES_CHANGED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.ConverterSetup.SetupChangeListener;

/**
 * Provides a mapping between {@link ConverterSetup}s and
 * {@link SourceAndConverter sources}. All {@code setupParametersChanged()}
 * events from ConverterSetup currently in the mapping are forwarded to
 * registered {@link #listeners()}.
 * <p>
 * {@code ConverterSetups} listens to {@code ViewerState} changes. When a source
 * is removed from the {@code ViewerState}, it is also removed from the mapping
 * (as well as the corresponding {@code ConverterSetup}.
 * <p>
 * When <em>adding</em> sources, however, the corresponding
 * {@code ConverterSetup} has to be manually registered using
 * {@link #put(SourceAndConverter, ConverterSetup)}. (This is necessary to avoid
 * breaking existing API.)
 *
 * @author Tobias Pietzsch
 */
public class ConverterSetups implements SourceToConverterSetupBimap
{
	private final Map< ConverterSetup, SourceAndConverter< ? > > setupToSource = new HashMap<>();

	private final Map< SourceAndConverter< ? >, ConverterSetup > sourceToSetup = new HashMap<>();

	private final ViewerState state;

	private final BasicViewerState previousState;

	private final SetupChangeListener converterSetupChangeListener;

	private final Listeners.List< SetupChangeListener > forwardedSetupChangeListeners = new Listeners.SynchronizedList<>();

	private final ConverterSetupBounds bounds;

	public ConverterSetups( final ViewerState state )
	{
		this.state = state;
		previousState = new BasicViewerState( state );
		converterSetupChangeListener = setup -> forwardedSetupChangeListeners.list.forEach( l -> l.setupParametersChanged( setup ) );
		state.changeListeners().add( this::analyzeChanges );
		bounds = new ConverterSetupBounds( this );
	}

	/**
	 * All {@code setupParametersChanged()} events from ConverterSetup currently
	 * in the ViewerState are forwarded to these listeners.
	 */
	public Listeners< SetupChangeListener > listeners()
	{
		return forwardedSetupChangeListeners;
	}

	@Override
	public SourceAndConverter< ? > getSource( final ConverterSetup setup )
	{
		return setupToSource.get( setup );
	}

	@Override
	public ConverterSetup getConverterSetup( final SourceAndConverter< ? > source )
	{
		return sourceToSetup.get( source );
	}

	public synchronized void put( final SourceAndConverter< ? > source, final ConverterSetup setup )
	{
		final ConverterSetup previousSetup = sourceToSetup.put( source, setup );
		final SourceAndConverter< ? > previousSource = setupToSource.put( setup, source );
		setup.setupChangeListeners().add( converterSetupChangeListener );

		// keep mapping one-to-one
		if ( previousSetup != null && previousSetup != setup )
		{
			setupToSource.remove( previousSetup );
			previousSetup.setupChangeListeners().remove( converterSetupChangeListener );
		}
		if ( previousSource != null && previousSource != source )
			sourceToSetup.remove( previousSource );
	}

	public ConverterSetupBounds getBounds()
	{
		return bounds;
	}

	private synchronized void analyzeChanges( final ViewerStateChange change )
	{
		if ( change != NUM_SOURCES_CHANGED )
			return;

		final HashSet< SourceAndConverter< ? > > removedSources = new HashSet<>( previousState.getSources() );
		removedSources.removeAll( state.getSources() );

		// update ConverterSetup listeners
		for ( final SourceAndConverter< ? > source : removedSources )
		{
			final ConverterSetup setup = sourceToSetup.remove( source );
			if ( setup != null )
			{
				setupToSource.remove( setup );
				setup.setupChangeListeners().remove( converterSetupChangeListener );
			}
		}

		previousState.set( state );
	}
}
