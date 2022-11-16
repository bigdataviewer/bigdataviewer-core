package bdv.img.omezarr;

import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.util.volatiles.VolatileViews;
import com.google.gson.JsonArray;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;

import javax.annotation.Nullable;

import java.util.ArrayList;

import static bdv.img.omezarr.Multiscales.MULTI_SCALE_KEY;

public class MultiscaleImage< T extends NativeType< T > & RealType< T >, V extends Volatile< T > & NativeType< V > & RealType< V > >
{
	private final String multiscalePath;

	private final SharedQueue queue;

	private int numResolutions;

	private long[] dimensions;

	private T type;

	private V volatileType;

	private CachedCellImg< T, ? >[] imgs;

	private RandomAccessibleInterval< V >[] vimgs;

	private Multiscales multiscales;

	private int multiscaleArrayIndex = 0; // TODO (see comments within code)

	private DataType dataType;

	private long[][] multiDimensions;

	private DataType[] multiDataType;

	/**
	 * TODO
	 */
	public MultiscaleImage(
			final String multiscalePath,
			@Nullable final SharedQueue queue )
	{
		this.multiscalePath = multiscalePath;
		this.queue = queue;
	}

	private void init()
	{
		if ( imgs != null ) return;

		try
		{
			// FIXME support S3
			final N5ZarrReader n5ZarrReader = new N5ZarrReader( multiscalePath );

			// Fetch metadata
			//
			Multiscales[] multiscalesArray = n5ZarrReader.getAttribute( "", MULTI_SCALE_KEY, Multiscales[].class );

			// In principle the call above would be sufficient.
			// However since we need to support different
			// versions of OME-Zarrr we need to "manually"
			// fix some fields.
			// Thus, we parse the same JSON again and fill in missing
			// information.
			// TODO: could we do this by means of a JsonDeserializer?

			final JsonArray multiscalesJsonArray = n5ZarrReader.getAttributes( "" ).get( MULTI_SCALE_KEY ).getAsJsonArray();
			for ( int i = 0; i < multiscalesArray.length; i++ )
			{
				multiscalesArray[ i ].applyVersionFixes( multiscalesJsonArray.get( i ).getAsJsonObject() );
				multiscalesArray[ i ].init();
			}

			// TODO
			//   From the spec:
			//   "If only one multiscale is provided, use it.
			//   Otherwise, the user can choose by name,
			//   using the first multiscale as a fallback"
			//   Right now, we always only use the first one.
			//   One option would be to add the {@code multiscaleArrayIndex}
			//   array index as a parameter to the constructor
			multiscales = multiscalesArray[ multiscaleArrayIndex ];

			// Here, datasets are single resolution N-D Images.
			// Each dataset represents one resolution layer.
			final Multiscales.Dataset[] datasets = multiscales.getDatasets();
			numResolutions = datasets.length;

			// Set the dimensions and data type
			// from the highest resolution dataset's
			// metadata.

			multiDimensions = new long[numResolutions][];
			multiDataType = new DataType[numResolutions];

			for (int resolution = numResolutions-1; resolution >= 0; --resolution) {
				final DatasetAttributes attributes = n5ZarrReader.getDatasetAttributes(datasets[resolution].path);
				multiDimensions[resolution] = attributes.getDimensions();
				multiDataType[resolution] = attributes.getDataType();
			}
			dimensions = multiDimensions[0];
			dataType = multiDataType[0];
			initTypes( dataType );

			// Initialize the images for all resolutions.
			//
			// TODO only on demand
			imgs = new CachedCellImg[ numResolutions ];
			vimgs = new RandomAccessibleInterval[ numResolutions ];

			for ( int resolution = 0; resolution < numResolutions; ++resolution )
			{
				imgs[ resolution ] = N5Utils.openVolatile( n5ZarrReader, datasets[ resolution ].path );

				if ( queue == null )
					vimgs[ resolution ] = VolatileViews.wrapAsVolatile( imgs[ resolution ] );
				else
					vimgs[ resolution ] = VolatileViews.wrapAsVolatile( imgs[ resolution ], queue );
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	private void initTypes( DataType dataType )
	{
		if ( type != null ) return;

		// TODO JOHN: Does the below code already exists
		//   somewhere in N5?
		switch ( dataType ) {
			case UINT8:
				type = Cast.unchecked( new UnsignedByteType() );
				break;
			case UINT16:
				type = Cast.unchecked( new UnsignedShortType() );
				break;
			case UINT32:
				type = Cast.unchecked( new UnsignedIntType() );
				break;
			case UINT64:
				type = Cast.unchecked( new UnsignedLongType() );
				break;
			case INT8:
				type = Cast.unchecked( new ByteType() );
				break;
			case INT16:
				type = Cast.unchecked( new ShortType() );
				break;
			case INT32:
				type = Cast.unchecked( new IntType() );
				break;
			case INT64:
				type = Cast.unchecked( new LongType() );
				break;
			case FLOAT32:
				type = Cast.unchecked( new FloatType() );
				break;
			case FLOAT64:
				type = Cast.unchecked( new DoubleType() );
				break;
		}

		volatileType = ( V ) VolatileTypeMatcher.getVolatileTypeForType( type );
	}

	public Multiscales getMultiscales()
	{
		return multiscales;
	}

	public long[] dimensions()
	{
		init();
		return dimensions;
	}

	public int numResolutions()
	{
		init();
		return numResolutions;
	}

	public CachedCellImg< T, ? > getImg( final int resolutionLevel )
	{
		init();
		return imgs[ resolutionLevel ];
	}

	public long[] getDimensions(final int level)
	{
		return multiDimensions[level];
	}

	public RandomAccessibleInterval< V > getVolatileImg( final int resolutionLevel )
	{
		init();
		return vimgs[ resolutionLevel ];
	}

	public DataType getDataType()
	{
		return dataType;
	}

	public T getType()
	{
		init();
		return type;
	}

	public V getVolatileType()
	{
		init();
		return volatileType;
	}

	public SharedQueue getSharedQueue()
	{
		return queue;
	}

	public int numDimensions()
	{
		return dimensions.length;
	}

	public static void main( String[] args )
	{
		//final String multiscalePath = "/data1/gabor.kovacs/davidf_sample_dataset/SmartSPIM_617052_sample.zarr";
//		final String multiscalePath = "/home/gabor.kovacs/data/davidf_sample_dataset/SmartSPIM_617052_sample.zarr";
		final String multiscalePath = "/Users/kgabor/data/davidf_sample_dataset/SmartSPIM_617052_sample.zarr";
		final MultiscaleImage< ?, ? > multiscaleImage = new MultiscaleImage<>( multiscalePath, null );
		multiscaleImage.dimensions();

		// Show as imagePlus
//		final ImageJ imageJ = new ImageJ();
//		imageJ.ui().showUI();
//		final DefaultPyramidal5DImageData< ?, ? > dataset = new DefaultPyramidal5DImageData<>( imageJ.context(), "image", multiscaleImage );
//		imageJ.ui().show( dataset.asPyramidalDataset() );
//
//		// Also show the displayed image in BDV
//		imageJ.command().run( OpenInBDVCommand.class, true );
	}
}
