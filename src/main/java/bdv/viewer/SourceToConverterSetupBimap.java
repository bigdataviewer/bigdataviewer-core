package bdv.viewer;

import java.util.ArrayList;
import java.util.List;

import bdv.tools.brightness.ConverterSetup;

/**
 * Associates {@code ConverterSetup}s to sources ({@code SourceAndConverter}).
 *
 * @author Tobias Pietzsch
 */
public interface SourceToConverterSetupBimap
{
	SourceAndConverter< ? > getSource( ConverterSetup converterSetup );

	ConverterSetup getConverterSetup( SourceAndConverter< ? > source );

	/**
	 * Returns list of all non-{@code null} ConverterSetups associated to
	 * {@code sources}.
	 */
	default List< ConverterSetup > getConverterSetups( final List< ? extends SourceAndConverter< ? > > sources )
	{
		final List< ConverterSetup > converterSetups = new ArrayList<>();
		for ( final SourceAndConverter< ? > source : sources )
		{
			final ConverterSetup converterSetup = getConverterSetup( source );
			if ( converterSetup != null )
				converterSetups.add( converterSetup );
		}
		return converterSetups;
	}
}
