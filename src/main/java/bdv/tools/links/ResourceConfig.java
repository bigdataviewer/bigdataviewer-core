package bdv.tools.links;

public interface ResourceConfig
{
	/**
	 * Apply this config to the resource corresponding to the given {@code
	 * spec}.
	 *
	 * @param spec
	 * 		spec of resource that this config should be applied to
	 * @param resources
	 * 		maps specs to resources
	 */
	void apply( ResourceSpec< ? > spec, ResourceManager resources );
}
