package bdv.zarr;

import java.net.URI;
import java.net.URISyntaxException;

public class DebugUtils
{
	public static URI uri(String s)
	{
		try
		{
			return new URI( s );
		}
		catch ( URISyntaxException e )
		{
			throw new RuntimeException( e );
		}
	}
}
