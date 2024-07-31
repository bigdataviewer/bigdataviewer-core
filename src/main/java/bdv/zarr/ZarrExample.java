package bdv.zarr;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrWriter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataInstantiationException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.zarr.JsonIoBasicImgLoader;

public class ZarrExample
{
	public static void main( final String[] args ) throws SpimDataException, URISyntaxException
	{
		// load SpimData from xml file
		final SpimData spimData = readFromXml();
		spimData.setBasePathURI( null );
		writeToZarr( spimData );
		final SpimData spimDataRead = readFromZarr();
//		System.out.println( "spimDataRead = " + spimDataRead );
	}

	private static SpimData readFromXml() throws SpimDataException
	{
		final String xmlFilename = "/Users/pietzsch/workspace/data/111010_weber_resave.xml";
		final XmlIoSpimData io = new XmlIoSpimData();
		final SpimData spimData = io.load( xmlFilename );
		return spimData;
	}

	private static void writeToZarr( final SpimData spimData ) throws URISyntaxException
	{
		final String basePath = "/Users/pietzsch/tmp/spimdata.zarr";
		final N5ZarrWriter writer = new N5ZarrWriter(basePath, gsonBuilder());
//		spimData.setBasePathURI( new URI(".") );
//		spimData.setBasePathURI( writer.getURI() );
//		spimData.setBasePathURI( null );
		writer.createGroup( "bak1" );
		spimData.setBasePathURI( new URI("bak1") );
		writer.setAttribute("bak1", "spimdata", spimData );
		writer.close();
	}

	private static SpimData readFromZarr() throws SpimDataException
	{
		final String basePath = "/Users/pietzsch/tmp/spimdata.zarr";
		final N5ZarrReader reader = new N5ZarrReader(basePath, gsonBuilder());

		URI uri = reader.getURI();
		System.out.println( "reader.getURI() = " + uri );

		return reader.getAttribute( ".", "spimdata", SpimData.class );

	}




	static class SpimDataAdapter implements JsonSerializer< SpimData >, JsonDeserializer< SpimData >
	{
		@Override
		public JsonElement serialize( final SpimData src, final Type typeOfSrc, final JsonSerializationContext context )
		{
			JsonObject jsonObject = new JsonObject();


			final URI basePathURI = src.getBasePathURI();
			if ( basePathURI != null )
			{
				jsonObject.addProperty( "BasePath", basePathURI.toString() );
			}
			jsonObject.add( "ImageLoader", serializeBasicImgLoader( src.getSequenceDescription().getImgLoader(), basePathURI, context ) );
			jsonObject.add( "SequenceDescription", context.serialize( src.getSequenceDescription() ) );
//			jsonObject.addProperty("ViewRegistrations", src.getViewRegistrations().toString());
			return jsonObject;
		}

		@Override
		public SpimData deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
		{
			try
			{
				System.out.println( "SpimDataAdapter.deserialize" );
				System.out.println( "json = " + json + ", typeOfT = " + typeOfT + ", context = " + context );
				System.out.println();

				final JsonObject jsonObject = json.getAsJsonObject();
				final JsonElement basePathElement = jsonObject.get( "BasePath" );
				final URI basePathURI = basePathElement == null ? null : new URI( basePathElement.getAsString() );

				final JsonElement element = jsonObject.get( "ImageLoader" );
				deserializeBasicImgLoader( element, basePathURI, context );

				return DEBUG_SPIMDATA_INSTANCE_FROM_XML(); // TODO

			}
			catch ( URISyntaxException e )
			{
				throw new RuntimeException( e );
			}
		}
	}



	private static SpimData DEBUG_SPIMDATA_INSTANCE_FROM_XML()
	{
		try
		{
			return readFromXml();
		}
		catch ( SpimDataException e )
		{
			throw new RuntimeException( e );
		}
	}

	/*
    <ImageLoader format="bdv.n5" version="1.0">
      <n5 type="relative">111010_weber_resave.n5</n5>
    </ImageLoader>

	 */

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	static JsonElement serializeBasicImgLoader( final BasicImgLoader imgLoader, final URI basePathURI, final JsonSerializationContext context )
	// TODO: throws SpimDataInstantiationException ???
	{
		try
		{
			final JsonIoBasicImgLoader io = JsonIoBasicImgLoader.forClass( imgLoader.getClass() );
			return io.serialize( imgLoader, basePathURI, context );
		}
		catch ( SpimDataInstantiationException e )
		{
			throw new RuntimeException( e );
		}
	}

	static BasicImgLoader deserializeBasicImgLoader( final JsonElement json, final URI basePathURI, final JsonDeserializationContext context ) throws JsonParseException
	// TODO: throws SpimDataInstantiationException ???
	{
		System.out.println( "ZarrExample.deserializeBasicImgLoader" );
		System.out.println( "json = " + json + ", basePathURI = " + basePathURI + ", context = " + context );
		System.out.println();

		final JsonObject jsonObject = json.getAsJsonObject();
		final String format = jsonObject.get( "format" ).getAsString();
		try
		{
			final JsonIoBasicImgLoader io = JsonIoBasicImgLoader.forFormat( format );
			return io.deserialize( json, basePathURI, context );
		}
		catch ( SpimDataInstantiationException e )
		{
			throw new RuntimeException( e );
		}
	}

	static class SequenceDescriptionAdapter implements JsonSerializer< SequenceDescription >
	{
		@Override
		public JsonElement serialize( final SequenceDescription src, final Type typeOfSrc, final JsonSerializationContext context )
		{
			JsonObject jsonObject = new JsonObject();
//			jsonObject.add( "ViewSetups", context.serialize( src.getViewSetupsOrdered() ) );
			return jsonObject;
		}
	}


	public static GsonBuilder gsonBuilder()
	{
		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter( SpimData.class, new SpimDataAdapter() );
		gb.registerTypeAdapter( SequenceDescription.class, new SequenceDescriptionAdapter() );
		return gb;
	}

}
