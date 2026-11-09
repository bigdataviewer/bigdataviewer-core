package bdv.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.scijava.Priority;
import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.annotations.Indexable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.imglib2.util.Cast;

public class JsonUtils
{
	public static Gson gson()
	{
		GsonBuilder gsonBuilder = new GsonBuilder();
		JsonIos.registerTypeAdapters( gsonBuilder );
		return gsonBuilder.create();
	}

	/**
	 * Annotation for classes that de/serialize instances of {@code T} from/to JSON.
	 */
	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.TYPE )
	@Indexable
	public @interface JsonIo
	{
		/**
		 * The value of the "type" attribute in serialized instances.
		 * <p>
		 * This should be unique, because it is used to pick the correct {@code
		 * JsonIo} for deserialization of polymorphic {@link Typed} attributes.
		 */
		String jsonType();

		/**
		 * The class of un-serialized instances.
		 */
		Class< ? > type();

		double priority() default Priority.NORMAL;
	}

	/**
	 * {@code Typed<T>} instances of any {@code T} with a {@code @JsonIo}-
	 * annotated adapter are serialized as json objects with a "type" attribute
	 * with the value of the {@link JsonIo#type()} annotation, and an "obj"
	 * attribute, which is serialized using the annotated adapter. For
	 * deserialization, the correct adapter is looked up via the "type"
	 * attribute.
	 */
	public static class Typed< T >
	{
		private final T obj;

		Typed( T obj )
		{
			Objects.requireNonNull( obj );
			this.obj = obj;
		}

		public T get()
		{
			return obj;
		}

		@Override
		public String toString()
		{
			return "Typed{" +
					"obj=" + obj +
					'}';
		}

		@JsonIo( jsonType = "JsonUtils.Typed", type = Typed.class )
		static class JsonAdapter implements JsonSerializer< Typed< ? > >, JsonDeserializer< Typed< ? > >
		{
			@Override
			public Typed< ? > deserialize(
					final JsonElement json,
					final Type typeOfT,
					final JsonDeserializationContext context )
			{
				final JsonObject obj = asObject( json );
				final String jsonType = getString( obj, "type" );
				final Type type = JsonIos.typeFor( jsonType );
				return typed( context.deserialize( getMember( obj, "data" ), type ) );
			}

			@Override
			public JsonElement serialize(
					final Typed< ? > src,
					final Type typeOfSrc,
					final JsonSerializationContext context )
			{
				final JsonObject obj = new JsonObject();
				final String jsonType = JsonIos.jsonTypeFor( src.get().getClass() );
				obj.addProperty( "type", jsonType );
				obj.add( "data", context.serialize( src.get() ) );
				return obj;
			}
		}
	}

	public static class TypedList< T >
	{
		private final List< T > list;

		public TypedList()
		{
			list = new ArrayList<>();
		}

		public TypedList( final List< T > list )
		{
			this.list = list;
		}

		public List< T > list()
		{
			return list;
		}

		@Override
		public String toString()
		{
			return "TypedList{" + list + '}';
		}


		@JsonUtils.JsonIo( jsonType = "TypedList", type = TypedList.class )
		static class JsonAdapter implements JsonDeserializer< TypedList< ? > >, JsonSerializer< TypedList< ? > >
		{
			@Override
			public TypedList< ? > deserialize(
					final JsonElement json,
					final Type typeOfT,
					final JsonDeserializationContext context )
			{
				return deserializeT( json, typeOfT, context );
			}

			private < T > TypedList< T > deserializeT(
					final JsonElement json,
					final Type typeOfT,
					final JsonDeserializationContext context )
			{
				final List< T > list = new ArrayList<>();
				for ( final JsonElement element : asArray( json ) )
					list.add( asTyped( context, element ) );
				return new TypedList<>( list );
			}

			@Override
			public JsonElement serialize(
					final TypedList< ? > src,
					final Type typeOfSrc,
					final JsonSerializationContext context )
			{
				final JsonArray array = new JsonArray();
				for ( final Object spec : src.list() ) {
					array.add( context.serialize( typed( spec ) ) );
				}
				return array;
			}
		}
	}

	/**
	 * Wrap {@code obj} into a {@code Typed<T>} for serialization of polymorphic objects.
	 */
	public static < T > Typed< T > typed( T obj )
	{
		return new Typed<>( obj );
	}

	static class JsonIos
	{
		static void registerTypeAdapters( final GsonBuilder gsonBuilder )
		{
			build();
			type_to_JsonAdapterClassName.forEach( ( type, adapterClassName ) -> {
				if ( adapterClassName == null )
				{
					throw new RuntimeException( "could not find JsonAdapter for " + type );
				}

				final Object adapter;
				try
				{
					adapter = Class.forName( adapterClassName ).newInstance();
				}
				catch ( final Exception e )
				{
					throw new RuntimeException( "could not create \"" + adapterClassName + "\" instance", e );
				}
				if ( adapter instanceof JsonSerializer && adapter instanceof JsonDeserializer )
					gsonBuilder.registerTypeAdapter( type, new GuardedAdapter<>( Cast.unchecked( adapter ) ) );
				else
					gsonBuilder.registerTypeAdapter( type, adapter );
			} );
		}

		private static String jsonTypeFor( final Type type )
		{
			build();
			final String jsonType = type_to_JsonType.get( type );
			if ( jsonType == null )
				throw new RuntimeException( "could not find JsonIo implementation for " + type );
			return jsonType;
		}

		private static Type typeFor( final String jsonType )
		{
			build();
			final Type type = jsonType_to_Type.get( jsonType );
			if ( type == null )
				throw new JsonParseException( "could not find JsonIo implementation for type \"" + jsonType + "\"" );
			return type;
		}

		private static final Map< Type, String > type_to_JsonType = new ConcurrentHashMap<>();
		private static final Map< String, Type > jsonType_to_Type = new ConcurrentHashMap<>();
		private static final Map< Type, String > type_to_JsonAdapterClassName = new ConcurrentHashMap<>();

		private static volatile boolean buildWasCalled = false;

		private static void build()
		{
			if ( !buildWasCalled )
			{
				synchronized ( JsonUtils.class )
				{
					if ( !buildWasCalled )
					{
						try
						{
							final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
							final Index< JsonIo > annotationIndex = Index.load( JsonUtils.JsonIo.class, classLoader );
							for ( final IndexItem< JsonIo > item : annotationIndex )
							{
								final JsonUtils.JsonIo io = item.annotation();
								type_to_JsonType.put( io.type(), io.jsonType() );
								jsonType_to_Type.put( io.jsonType(), io.type() );
								type_to_JsonAdapterClassName.put( io.type(), item.className() );
							}
						}
						catch ( final Exception e )
						{
							throw new RuntimeException( "problem accessing annotation index", e );
						}
						buildWasCalled = true;
					}
				}
			}
		}
	}

	public static String prettyPrint( final JsonElement jsonElement )
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson( jsonElement );
	}

	/**
	 * Wraps a two-way adapter such that any {@code RuntimeException} thrown
	 * during deserialization is re-thrown as {@code JsonParseException}.
	 */
	private static class GuardedAdapter< T, A extends JsonSerializer< T > & JsonDeserializer< T > >
			implements JsonSerializer< T >, JsonDeserializer< T >
	{
		private final A delegate;

		GuardedAdapter( final A delegate )
		{
			this.delegate = delegate;
		}

		@Override
		public T deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
		{
			try
			{
				return delegate.deserialize( json, typeOfT, context );
			}
			catch ( final JsonParseException e )
			{
				throw e;
			}
			catch ( final RuntimeException e )
			{
				throw new JsonParseException( "could not deserialize " + typeOfT + " from " + abbreviate( json ), e );
			}
		}

		@Override
		public JsonElement serialize(
				final T src,
				final Type typeOfSrc,
				final JsonSerializationContext context )
		{
			return delegate.serialize( src, typeOfSrc, context );
		}
	}

	/*
	 * Helpers for implementing JsonDeserializers.
	 *
	 * These check that the input has the expected structure and throw a
	 * JsonParseException with a descriptive message if it does not. (Instead
	 * of the IllegalStateException, NullPointerException, etc., that would
	 * result from using JsonElement.getAsXxx() directly.)
	 */

	/**
	 * Returns the attribute {@code key} of {@code obj}, or {@code null} if the
	 * attribute is missing or {@code null}.
	 */
	public static JsonElement getMemberOptional( final JsonObject obj, final String key )
	{
		final JsonElement element = obj.get( key );
		return ( element == null || element.isJsonNull() ) ? null : element;
	}

	/**
	 * Returns the attribute {@code key} of {@code obj}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or {@code null}
	 */
	public static JsonElement getMember( final JsonObject obj, final String key ) throws JsonParseException
	{
		final JsonElement element = getMemberOptional( obj, key );
		if ( element == null )
			throw new JsonParseException( "missing attribute \"" + key + "\" in " + abbreviate( obj ) );
		return element;
	}

	/**
	 * Returns {@code json} as a {@code JsonObject}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not an object
	 */
	public static JsonObject asObject( final JsonElement json ) throws JsonParseException
	{
		if ( json == null || !json.isJsonObject() )
			throw new JsonParseException( "expected an object (got " + abbreviate( json ) + ")" );
		return json.getAsJsonObject();
	}

	/**
	 * Returns the attribute {@code key} of {@code obj} as a {@code JsonObject}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or not an object
	 */
	public static JsonObject getObject( final JsonObject obj, final String key ) throws JsonParseException
	{
		return asObject( getMember( obj, key ) );
	}

	/**
	 * Returns {@code json} as a {@code JsonArray}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not an array
	 */
	public static JsonArray asArray( final JsonElement json ) throws JsonParseException
	{
		if ( json == null || !json.isJsonArray() )
			throw new JsonParseException( "expected an array (got " + abbreviate( json ) + ")" );
		return json.getAsJsonArray();
	}

	/**
	 * Returns {@code json} as a {@code String}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not a string
	 */
	public static String asString( final JsonElement json ) throws JsonParseException
	{
		if ( json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString() )
			throw new JsonParseException( "expected a string (got " + abbreviate( json ) + ")" );
		return json.getAsString();
	}

	/**
	 * Returns the attribute {@code key} of {@code obj} as a {@code String}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or not a string
	 */
	public static String getString( final JsonObject obj, final String key ) throws JsonParseException
	{
		return asString( getMember( obj, key ) );
	}

	/**
	 * Returns {@code json} as a {@code Number}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not a number
	 */
	public static Number asNumber( final JsonElement json ) throws JsonParseException
	{
		if ( json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber() )
			throw new JsonParseException( "expected a number (got " + abbreviate( json ) + ")" );
		return json.getAsNumber();
	}

	/**
	 * Returns the attribute {@code key} of {@code obj} as a {@code Number}.
	 * Use {@code .intValue()}, {@code .doubleValue()}, etc. on the result.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or not a number
	 */
	public static Number getNumber( final JsonObject obj, final String key ) throws JsonParseException
	{
		return asNumber( getMember( obj, key ) );
	}

	/**
	 * Returns {@code json} as an {@code int}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not a number that can be represented exactly as {@code int}
	 */
	public static int asInt( final JsonElement json ) throws JsonParseException
	{
		asNumber( json );
		try
		{
			return json.getAsBigDecimal().intValueExact();
		}
		catch ( final ArithmeticException e )
		{
			throw new JsonParseException( "expected an integer (got " + abbreviate( json ) + ")" );
		}
	}

	/**
	 * Returns the attribute {@code key} of {@code obj} as an {@code int}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or not a number that can be represented exactly as {@code int}
	 */
	public static int getInt( final JsonObject obj, final String key ) throws JsonParseException
	{
		return asInt( getMember( obj, key ) );
	}

	/**
	 * Returns {@code json} as a {@code double}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} is not a number
	 */
	public static double asDouble( final JsonElement json ) throws JsonParseException
	{
		return asNumber( json ).doubleValue();
	}

	/**
	 * Returns the attribute {@code key} of {@code obj} as a {@code double}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or not a number
	 */
	public static double getDouble( final JsonObject obj, final String key ) throws JsonParseException
	{
		return asDouble( getMember( obj, key ) );
	}

	/**
	 * Deserializes {@code json} as a {@link Typed Typed<T>} and returns the wrapped {@code T}.
	 *
	 * @throws JsonParseException
	 * 		if {@code json} cannot be deserialized
	 */
	public static < T > T asTyped( final JsonDeserializationContext context, final JsonElement json ) throws JsonParseException
	{
		return context.< Typed< T > >deserialize( asObject( json ), Typed.class ).get();
	}

	/**
	 * Deserializes the attribute {@code key} of {@code obj} as a {@link Typed Typed<T>} and returns the wrapped {@code T}.
	 *
	 * @throws JsonParseException
	 * 		if the attribute is missing or cannot be deserialized
	 */
	public static < T > T getTyped( final JsonDeserializationContext context, final JsonObject obj, final String key ) throws JsonParseException
	{
		return asTyped( context, getMember( obj, key ) );
	}

	static String abbreviate( final JsonElement json )
	{
		final String s = String.valueOf( json );
		return s.length() <= 200 ? s : s.substring( 0, 200 ) + "...";
	}
}
