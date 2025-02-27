package bdv.tools.links;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import bdv.tools.links.resource.UnknownResource;
import net.imglib2.util.Cast;

public class DefaultResourceManager implements ResourceManager
{
	private final Map< Object, ResourceSpec< ? > > resourceToSpec = new WeakHashMap<>();

	private final Map< ResourceSpec< ? >, WeakReference< ? > > specToResource = new WeakHashMap<>();

	private final Map< Object, Object > keepAlive = new WeakHashMap<>();

	@Override
	public synchronized < T > void put( final T resource, final ResourceSpec< T > spec )
	{
		resourceToSpec.put( resource, spec );
		specToResource.put( spec, new WeakReference<>( resource ) );
	}

	@Override
	public synchronized < T > ResourceSpec< T > getResourceSpec( final T resource )
	{
		final ResourceSpec< ? > spec = resourceToSpec.get( resource );
		if ( spec == null )
			return new UnknownResource.Spec<>();
		else
			return Cast.unchecked( spec );
	}

	@Override
	public synchronized < T > T getResource( final ResourceSpec< T > spec )
	{
		final WeakReference< ? > ref = specToResource.get( spec );
		return ref == null ? null : Cast.unchecked( ref.get() );
	}

	@Override
	public synchronized < T > T getOrCreateResource( final ResourceSpec< T > spec )  throws ResourceCreationException
	{
		T resource = getResource( spec );
		if ( resource == null )
		{
			resource = spec.create( this );
		}
		return resource;
	}

	@Override
	public synchronized void keepAlive( final Object anchor, final Object object )
	{
		keepAlive.put( anchor, object );
	}

	@Override
	public String toString()
	{
		String result = "DefaultResources{\n";
		result += "  resourceToSpec{\n";
		for ( Map.Entry< Object, ResourceSpec< ? > > entry : resourceToSpec.entrySet() )
		{
			Object key = entry.getKey();
			ResourceSpec< ? > value = entry.getValue();
			result += "    k = " + key + ", v = " + head( 30, value.toString() ) + "\n";
		}
		result += "  }, specToResource{\n";
		for ( Map.Entry< ResourceSpec< ? >, WeakReference< ? > > entry : specToResource.entrySet() )
		{
			final ResourceSpec< ? > key = entry.getKey();
			final WeakReference< ? > value = entry.getValue();
			result += "    k = " + head( 30, key.toString() ) + ", v = " + value.get() + "\n";
		}
		result += "  }\n";
		result += '}';
		return result;
	}

	private static String head(int len, String s) {
		return s.substring( 0, Math.min( len, s.length() ) );
	}
}
