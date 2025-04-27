package bdv.tools.links;

public class ResourceCreationException extends Exception
{
	public ResourceCreationException()
	{
		super();
	}

	public ResourceCreationException( String message )
	{
		super( message );
	}

	public ResourceCreationException( Throwable cause )
	{
		super( cause );
	}

	public ResourceCreationException( String message, Throwable cause )
	{
		super( message, cause );
	}
}
