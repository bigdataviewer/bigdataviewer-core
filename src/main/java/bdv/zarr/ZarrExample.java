package bdv.zarr;

import static bdv.zarr.DebugUtils.uri;
import static mpicbg.spim.data.zarr.JsonKeys.BASEPATH;
import static mpicbg.spim.data.zarr.JsonKeys.SEQUENCEDESCRIPTION;
import static mpicbg.spim.data.zarr.JsonKeys.SPIMDATA;

import java.io.File;
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
	public static void main( final String[] args ) throws SpimDataException
	{
		// load SpimData from xml file
//		final SpimData spimData = readFromXml();
//		writeToZarr( spimData );
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

	private static void writeToZarr( final SpimData spimData )
	{
		final String basePath = "/Users/pietzsch/workspace/data/spimdata.zarr";
		final N5ZarrWriter writer = new N5ZarrWriter( basePath, gsonBuilder( new File( basePath ).toURI() ) );
//		spimData.setBasePathURI( uri( "." ) );
//		spimData.setBasePathURI( writer.getURI() );
//		spimData.setBasePathURI( null );
		spimData.setBasePathURI( uri( "../" ) );
		writer.setAttribute( ".", SPIMDATA, spimData );
		writer.close();
	}

	private static SpimData readFromZarr() throws SpimDataException
	{
		final String basePath = "/Users/pietzsch/workspace/data/spimdata.zarr";
		final N5ZarrReader reader = new N5ZarrReader( basePath, gsonBuilder( new File( basePath ).toURI() ) );
		return reader.getAttribute( ".", SPIMDATA, SpimData.class );
	}




	static class SpimDataAdapter implements JsonSerializer< SpimData >, JsonDeserializer< SpimData >
	{
		private final URI containerURI;

//		private final URI basePathURI;

		SpimDataAdapter(final URI containerURI)
			// TODO	,final URI basePathURI)
			//   Consider adding a basePathURI argument that overrides SpimData.getBasePathURI().
			//   SpimData.getBasePathURI() is assumed to be an absolute URI in SpimData usage (and for good reason, probably).
			//   So I would not touch that to set it to a relative URI (or null) for saving.
			//   Better to remove ambiguity and define that SpimData.getBasePathURI() should be an absolute URI always.
			// TODO
			//   The idea then would be to serialize with overriding basePathURI = "." or "../", typically,
			//   where the relative URI is relative wrt containerURI.
		{
			this.containerURI = containerURI;
//			this.basePathURI = basePathURI;
		}

		@Override
		public JsonElement serialize( final SpimData src, final Type typeOfSrc, final JsonSerializationContext context )
		{
			JsonObject jsonObject = new JsonObject();

			final URI absoluteBasePathURI;
			{
				final URI basePathURI = src.getBasePathURI(); // TODO maybe overridden by this.basePathURI ...
				if ( basePathURI != null )
				{
					absoluteBasePathURI = containerURI.resolve( basePathURI );
					final URI uri = containerURI.relativize( absoluteBasePathURI );
					if ( !uri.toString().isEmpty() )
					{
						jsonObject.addProperty( BASEPATH, basePathURI.toString() );
					}
				}
				else
				{
					absoluteBasePathURI = containerURI;
				}
			}

			jsonObject.add( "ImageLoader", serializeBasicImgLoader( src.getSequenceDescription().getImgLoader(), absoluteBasePathURI, context ) );
			jsonObject.add( SEQUENCEDESCRIPTION, context.serialize( src.getSequenceDescription() ) );
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
				final URI absoluteBasePathURI;
				{
					final JsonElement basePathElement = jsonObject.get( BASEPATH );
					if ( basePathElement != null )
					{
						final URI basePathURI = new URI( basePathElement.getAsString() ); // TODO maybe overridden by this.basePathURI? How would this be useful for reading?
						absoluteBasePathURI = containerURI.resolve( basePathURI );
					}
					else
					{
						absoluteBasePathURI = containerURI;
					}
				}
				final BasicImgLoader imgLoader = deserializeBasicImgLoader( jsonObject.get( "ImageLoader" ), absoluteBasePathURI, context );
				final SequenceDescription sequenceDescription = context.deserialize( jsonObject.get( SEQUENCEDESCRIPTION ), SequenceDescription.class );


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


	public static GsonBuilder gsonBuilder(final URI containerURI)
	{
		final GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter( SpimData.class, new SpimDataAdapter(containerURI) );
		gb.registerTypeAdapter( SequenceDescription.class, new SequenceDescriptionAdapter() );
		return gb;
	}

}
