package bdv.tools.links;

public interface ResourceSpec< T >
{
	/**
	 * Creates the specified resource. Resources for nested specs are
	 * retrieved from a {@code Resources} map, or created and put into the map.
	 *
	 * @throws ResourceCreationException
	 * 		if the resource could not be created
	 */
	T create( ResourceManager resources ) throws ResourceCreationException;

	/**
	 * Create a {@code ResourceConfig} corresponding to this {@code ResourceSpec}.
	 * <p>
	 * A typical implementation gets the resource corresponding to this {@code
	 * ResourceSpec} from {@code resources}, extracts dynamic properties (such
	 * as the current transform of a {@code TransformedSource}), and builds the
	 * config.
	 *
	 * @param resources
	 * 		maps specs to resources
	 *
	 * @return the current {@code ResourceConfig} of the resource corresponding to this spec
	 */
	ResourceConfig getConfig( ResourceManager resources );
}
