package bdv.img.openconnectome;

import java.io.Serializable;

public class OpenConnectomeProject implements Serializable
{
	private static final long serialVersionUID = 3296461682832630651L;

	public String dataset;
	public String dataurl;
	public String dbname;
	public boolean exceptions;
	public String host;
	public int projecttype;
	public boolean readonly;
	public int resolution;
}