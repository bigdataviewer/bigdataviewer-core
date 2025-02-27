package bdv.tools.links;

/**
 * Associates resources and {@code ResourceSpec}s for copy&paste between
 * BigDataViewer instances.
 * <p>
 * Resources are for example {@code SpimData} objects, {@code
 * SourceAndConverter} for a particular setup in a {@code SpimData}, opened N5
 * datasets, etc.
 */
public interface ResourceManager
{
	< T > void put( final T resource, final ResourceSpec< T > spec );

	/**
	 * Get ResourceSpec registered for resource.
	 * (Return null if no spec was registered.)
	 */
	< T > ResourceSpec< T > getResourceSpec( T resource );

	/**
	 * If spec is registered, get the corresponding resource.
	 */
	< T > T getResource( ResourceSpec< T > spec );

	< T > T getOrCreateResource( ResourceSpec< T > spec ) throws ResourceCreationException;

	/**
	 * Puts a mapping from {@code anchor} to {@code object} into a {@code WeakHashMap}.
	 * This will keep {@code object} alive while {@code anchor} is strongly referenced.
	 */
	void keepAlive( Object anchor, Object object );
}
