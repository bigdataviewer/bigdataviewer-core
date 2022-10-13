package bdv.img.omezarr;


import bdv.BigDataViewer;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.export.ProgressWriterConsole;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.ViewerOptions;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import org.jdom2.Element;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import bdv.img.omezarr.ZarrImageLoader;
import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "bdv.multimg.zarr", type = ZarrImageLoader.class )
public class XmlIoZarrImageLoader implements XmlIoBasicImgLoader<ZarrImageLoader>
{
    @Override
    public Element toXml(final ZarrImageLoader imgLoader, final File basePath )
    {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "bdv.multimg.zarr" );
        elem.setAttribute( "version", "1.0" );
        // TODO (?)
//			elem.addContent( XmlHelpers.pathElement( "n5", imgLoader.getN5File(), basePath ) );
        return elem;
    }

    @Override
    public ZarrImageLoader fromXml(final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
    {
//            final String version = elem.getAttributeValue( "version" );
        final File zpath = loadPath( elem, "zarr", basePath );
        final Element zgroupsElem = elem.getChild( "zgroups" );
        final TreeMap<ViewId, String > zgroups = new TreeMap<>();
        // TODO validate that sequenceDescription and zgroups have the same entries
        for ( final Element c : zgroupsElem.getChildren( "zgroup" ) )
        {
            final int timepointId = Integer.parseInt( c.getAttributeValue( "timepoint" ) );
            final int setupId = Integer.parseInt( c.getAttributeValue( "setup" ) );
            final String path = c.getChild( "path" ).getText();
            zgroups.put( new ViewId( timepointId,setupId ), path );
        }

        return new ZarrImageLoader(zpath.getAbsolutePath(), zgroups, sequenceDescription);
    }

    public static void main( String[] args ) throws SpimDataException
    {
        final String fn = "/home/gkovacs/data/davidf_zarr_dataset.xml";
//        final String fn = "/home/gabor.kovacs/data/davidf_zarr_dataset.xml";
//        final String fn = "/Users/kgabor/data/davidf_zarr_dataset.xml";
        final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( fn );
        final ViewerImgLoader imgLoader = ( ViewerImgLoader ) spimData.getSequenceDescription().getImgLoader();
        final ViewerSetupImgLoader<?, ?> setupImgLoader = imgLoader.getSetupImgLoader(0);
        setupImgLoader.getMipmapResolutions();
        BigDataViewer.open(spimData, "BigDataViewer Zarr Example", new ProgressWriterConsole(), ViewerOptions.options());
        System.out.println( "imgLoader = " + imgLoader );
        System.out.println( "setupimgLoader = " + setupImgLoader );
    }
}


