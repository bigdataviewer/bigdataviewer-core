package bdv.zarr;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataInstantiationException;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.ViewSetup;
import mpicbg.spim.data.sequence.VoxelDimensions;
import mpicbg.spim.data.zarr.JsonIoViewSetupAttribute;
import net.imglib2.util.Cast;

class SequenceDescriptionAdapter implements JsonSerializer< SequenceDescription >
{
	@Override
	public JsonElement serialize( final SequenceDescription src, final Type typeOfSrc, final JsonSerializationContext context )
	{
		final JsonObject jsonObject = new JsonObject();
		try
		{
			final List< ViewSetup > setups = src.getViewSetupsOrdered();
			final ViewSetupAttributes attrs = new ViewSetupAttributes();
			attrs.addAttributes( setups );
			jsonObject.add( "ViewSetups", serialize( setups, context ) );
			jsonObject.add( "Attributes", serialize( attrs, context ) );

			// TODO: TimePoints
			wejhfoiser
					resgjowahergohwaer
					awe iuhgiuweahg
							aw egywe
		}
		catch ( SpimDataException e ) // TODO: exception handling
		{
			throw new RuntimeException( e );
		}
		return jsonObject;
	}

	private static JsonElement serialize( final Collection< ? extends BasicViewSetup > setups, final JsonSerializationContext context )
	{
		final JsonArray jsonArray = new JsonArray();
		setups.forEach( s -> jsonArray.add( serialize( s, context ) ) );
		return jsonArray;
	}

	private static JsonElement serialize( final BasicViewSetup setup, final JsonSerializationContext context )
	{
		final JsonObject jsonObject = new JsonObject();

		jsonObject.addProperty( "id", setup.getId() );

		jsonObject.addProperty( "name", setup.getName() );

		if ( setup.hasSize() )
			jsonObject.add( "size", context.serialize( setup.getSize().dimensionsAsLongArray() ) );

		if (setup.hasVoxelSize())
			jsonObject.add( "voxelSize", serialize( setup.getVoxelSize(), context ) );

		final Map< String, Entity > attributes = setup.getAttributes();
		final JsonObject jsonAttributes = new JsonObject();
		attributes.forEach( ( name, value ) -> jsonAttributes.addProperty( name, value.getId() ) );
		jsonObject.add( "attributes", jsonAttributes );

		return jsonObject;
	}

	private static JsonElement serialize( final VoxelDimensions voxelSize, final JsonSerializationContext context )
	{
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty( "unit", voxelSize.unit() );
		jsonObject.add( "size", context.serialize( voxelSize.dimensionsAsDoubleArray() ) );
		return jsonObject;
	}

	private static JsonElement serialize( final ViewSetupAttributes viewSetupAttributes, final JsonSerializationContext context )
	{
		final JsonObject jsonObject = new JsonObject();
		viewSetupAttributes.attributeNameToValues.forEach( ( name, values ) ->
				jsonObject.add( name, serializeAttributeValues( name, values, context ) ) );
		return jsonObject;
	}

	private static < T extends Entity > JsonArray serializeAttributeValues( final String name, final Map< Integer, Entity > values, final JsonSerializationContext context )
	{
		try
		{
			final JsonArray jsonArray = new JsonArray();
			final JsonIoViewSetupAttribute< T > io = JsonIoViewSetupAttribute.forAttributeName( name );
			values.values().forEach( a -> jsonArray.add( io.serialize( Cast.unchecked( a ), context ) ) );
			return jsonArray;
		}
		catch ( SpimDataInstantiationException e ) // TODO: exception handling
		{
			throw new RuntimeException( e );
		}
	}

	static class ViewSetupAttributes
	{
		// maps attribute name, e.g., "tile" to possible values.
		// possible values is another map that maps int ids to instances of the attribute (with that id).
		// Map< ViewSetupAttributeName, Map< Id, AttributeInstance >
		final Map< String, Map< Integer, Entity > > attributeNameToValues = new LinkedHashMap<>();

		void addAttributes( final Collection< ? extends BasicViewSetup > setups ) throws SpimDataException
		{
			setups.forEach( this::addAttributes );
		}

		void addAttributes( final BasicViewSetup setup )
		{
			setup.getAttributes().forEach( this::addAttribute );
		}

		void addAttribute( final String name, final Entity attribute )
		{
			System.out.println( "ViewSetupAttributes.addAttribute" );
			System.out.println( "name = " + name + ", attribute = " + attribute );
			System.out.println();
			final Map< Integer, Entity > values = attributeNameToValues.computeIfAbsent( name, k -> new LinkedHashMap<>() );
			values.merge( attribute.getId(), attribute, ( a1, a2 ) -> {
				if ( !Objects.equals( a1, a2 ) )
				{
					throw new IllegalArgumentException( "attributes " + a1 + " and " + a2 + " have the same id but are not equal." );
				}
				return a1;
			} );
		}

		@Override
		public String toString()
		{
			final StringBuilder sb = new StringBuilder( "ViewSetupAttributes{" );
			sb.append( "attributeNameToValues=" ).append( attributeNameToValues );
			sb.append( '}' );
			return sb.toString();
		}

	}
}
