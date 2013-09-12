package mpicbg.spim.data;

import java.io.File;
import java.io.IOException;

import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

public class XmlHelpers
{
	public static Element affineTransform3DElement( final String name, final AffineGet value )
	{
		final Element e = new Element( name );
		final double[] v = value.getRowPackedCopy();
		final String text =
				v[0] + " " + v[1] + " " + v[2] + " " + v[3] + " " +
				v[4] + " " + v[5] + " " + v[6] + " " + v[7] + " " +
				v[8] + " " + v[9] + " " + v[10] + " " + v[11];
		e.setText( text );
		return e;
	}

	public static AffineTransform3D loadAffineTransform3D( final Element elem )
	{
		final String data = elem.getText();
		final String[] fields = data.split( "\\s+" );
		if ( fields.length == 12 )
		{
			final double[] values = new double[ 12 ];
			for ( int i = 0; i < 12; ++i )
				values[ i ] = Double.parseDouble( fields[ i ] );
			final AffineTransform3D a = new AffineTransform3D();
			a.set( values );
			return a;
		}
		else
			throw new NumberFormatException( "Inappropriate parameters for " + AffineTransform3D.class.getCanonicalName() );
	}

	public static Element intElement( final String name, final int value )
	{
		return new Element( name ).addContent( Integer.toString( value ) );
	}

	public static int getInt( final Element parent, final String name )
	{
		return Integer.parseInt( parent.getChildText( name ) );
	}

	public static int getInt( final Element parent, final String name, final int defaultValue )
	{
		final String text = parent.getChildText( name );
		return text == null ? defaultValue : Integer.parseInt( text );
	}

	public static Element doubleElement( final String name, final double value )
	{
		return new Element( name ).addContent( Double.toString( value ) );
	}

	public static double getDouble( final Element parent, final String name )
	{
		return Double.parseDouble( parent.getChildText( name ) );
	}

	public static double getDouble( final Element parent, final String name, final double defaultValue )
	{
		final String text = parent.getChildText( name );
		return text == null ? defaultValue : Double.parseDouble( text );
	}

	public static Element booleanElement( final String name, final boolean value )
	{
		return new Element( name ).addContent( Boolean.toString( value ) );
	}

	public static boolean getBoolean( final Element parent, final String name )
	{
		return Boolean.parseBoolean( parent.getChildText( name ) );
	}

	public static boolean getBoolean( final Element parent, final String name, final boolean defaultValue )
	{
		final String text = parent.getChildText( name );
		return text == null ? defaultValue : Boolean.parseBoolean( text );
	}

	public static String getText( final Element parent, final String name, final String defaultValue )
	{
		final String text = parent.getChildText( name );
		return text == null ? defaultValue : text;
	}

	public static Element textElement( final String name, final String text )
	{
		return new Element( name ).addContent( text );
	}

	public static File loadPath( final Element parent, final String name, final String defaultRelativePath, final File basePath )
	{
		final Element elem = parent.getChild( name );
		final String path = ( elem == null ) ? defaultRelativePath : elem.getText();
		final boolean isRelative = ( elem == null ) ? true : elem.getAttributeValue( "type" ).equals( "relative" );
		if ( isRelative )
		{
			if ( basePath == null )
				return null;
			else
				return new File( basePath + "/" + path );
		}
		else
			return new File( path );
	}

	public static File loadPath( final Element parent, final String name, final File basePath )
	{
		final Element elem = parent.getChild( name );
		if ( elem == null )
			return null;
		final String path = elem.getText();
		final boolean isRelative = elem.getAttributeValue( "type" ).equals( "relative" );
		if ( isRelative )
		{
			if ( basePath == null )
				return null;
			else
				return new File( basePath + "/" + path );
		}
		else
			return new File( path );
	}

	/**
	 * @param basePath if null put the absolute path, otherwise relative to this
	 */
	public static Element pathElement( final String name, final File path, final File basePath )
	{
		final Element e = new Element( name );

		if ( basePath == null )
			e.setText( path.getAbsolutePath() );
		else
		{
			e.setAttribute( "type", "relative" );
			e.setText( getRelativePath( path, basePath ).getPath() );
		}

		return e;
	}

	public static File getRelativePath( final File file, final File relativeToThis )
	{
		try
		{
			return getRelativePath( file.getCanonicalFile(), relativeToThis.getCanonicalFile(), "" );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
			return null;
		}
	}

	private static File getRelativePath( final File file, final File relativeToThis, final String relativeInitial )
	{
		File parent = file;
		String relative = null;
		while( parent != null )
		{
			if ( parent.equals( relativeToThis ) )
			{
				return new File( relativeInitial + ( relative == null ? "." : relative ) );
			}
			relative = parent.getName() + ( relative == null ? "" : "/" + relative );
			parent = parent.getParentFile();
		}
		final File toParent = relativeToThis.getParentFile();
		if ( toParent == null )
			return null;
		else
			return getRelativePath( file, toParent, "../" + relativeInitial );
	}

}
