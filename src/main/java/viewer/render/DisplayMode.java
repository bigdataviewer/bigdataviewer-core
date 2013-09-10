package viewer.render;

public enum DisplayMode
{
	SINGLE     ( 0, "single-source mode" ),
	GROUP      ( 1, "single-group mode"),
	FUSED      ( 2, "fused mode" ),
	FUSEDGROUP ( 3, "fused group mode" );

	private final int id;
	private final String name;

	private DisplayMode( final int id, final String name )
	{
		this.id = id;
		this.name = name;
	}

	public int id()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public static final int length;

	static
	{
		length = values().length;
	}
}
