package bdv.tools.links;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.janelia.saalfeldlab.n5.DataType;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import bdv.viewer.ViewerState;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;

class BdvPropertiesV0
{
	private final AffineTransform3D transform;

	private final int timepoint;

	private final long[] panelsize;

	private final long[] mousepos;

	private final Anchor anchor;

	public enum Anchor
	{
		CENTER( "center" ),
		MOUSE( "mouse" );

		private final String label;

		Anchor( final String label )
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}

		public static Anchor fromString( final String string )
		{

			for ( final Anchor value : values() )
				if ( value.toString().equals( string ) )
					return value;
			return null;
		}

		static public class JsonAdapter implements JsonDeserializer< Anchor >, JsonSerializer< Anchor >
		{

			@Override
			public Anchor deserialize(
					final JsonElement json,
					final Type typeOfT,
					final JsonDeserializationContext context )
			{

				return Anchor.fromString( json.getAsString() );
			}

			@Override
			public JsonElement serialize(
					final Anchor src,
					final Type typeOfSrc,
					final JsonSerializationContext context )
			{

				return new JsonPrimitive( src.toString() );
			}
		}
	}

	public BdvPropertiesV0()
	{
		transform = new AffineTransform3D();
		timepoint = 0;
		panelsize = new long[ 2 ];
		mousepos = new long[ 2 ];
		anchor = Anchor.CENTER;
	}

	public BdvPropertiesV0( AffineTransform3D transform, final int timepoint, final long[] panelsize, final long[] mousepos )
	{
		this.transform = transform;
		this.timepoint = timepoint;
		this.panelsize = panelsize;
		this.mousepos = mousepos;
		this.anchor = Anchor.CENTER;
	}

	public AffineTransform3D transform()
	{
		return transform;
	}

	public int timepoint()
	{
		return timepoint;
	}

	public Dimensions panelsize()
	{
		return FinalDimensions.wrap( panelsize );
	}

	public Point mouspos()
	{
		return Point.wrap( mousepos );
	}

	@Override
	public String toString()
	{
		return "BdvPropertiesV0{" +
				"transform=" + transform +
				", timepoint=" + timepoint +
				", panelsize=" + Arrays.toString( panelsize ) +
				", mousepos=" + Arrays.toString( mousepos ) +
				", anchor=" + anchor +
				'}';
	}

	static BdvPropertiesV0 create(
			final ViewerState state,
			final Dimensions panelsize,
			final Point mousepos )
	{
		return new BdvPropertiesV0(
				state.getViewerTransform(),
				state.getCurrentTimepoint(),
				panelsize.dimensionsAsLongArray(),
				mousepos.positionAsLongArray());
	}
}
