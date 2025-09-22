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
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
				final JsonObject obj = json.getAsJsonObject();
				final String jsonType = obj.get( "type" ).getAsString();
				final Type type = JsonIos.typeFor( jsonType );
				return typed( context.deserialize( obj.get( "data" ), type ) );
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
				for ( JsonElement element : json.getAsJsonArray() )
				{
					final Typed< T > typed = context.deserialize( element, Typed.class );
					list.add( typed.get() );
				}
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
				throw new RuntimeException( "could not find JsonIo implementation for " + jsonType );
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
}
