package viewer.gui.transformation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

public class XmlIoTransformedSources
{
	public static final String SOURCETRANSFORMS_TAG = "ManualSourceTransforms";

	public static final String SOURCETRANSFORM_TAG = "SourceTransform";
	public static final String SOURCETRANSFORM_TYPE_ATTRIBUTE_NAME = "type";
	public static final String SOURCETRANSFORM_TYPE_VALUE_AFFINE = "affine";
	public static final String SOURCETRANSFORM_AFFINE_TAG = "affine";

	public String getTagName()
	{
		return SOURCETRANSFORMS_TAG;
	}

	public ManualSourceTransforms fromXml( final Element elem )
	{
		final ArrayList< AffineTransform3D > transforms = new ArrayList< AffineTransform3D >();
		for ( final Element t : elem.getChildren( SOURCETRANSFORM_TAG ) )
			transforms.add( fromXmlAffine( t ) );
		return new ManualSourceTransforms( transforms );
	}

	public Element toXml( final ManualSourceTransforms manualSourceTransforms )
	{
		final Element elem = new Element( SOURCETRANSFORMS_TAG );
		final List< AffineTransform3D > transforms = manualSourceTransforms.getTransforms();
		for ( final AffineTransform3D t : transforms )
			elem.addContent( toXmlAffine( t ) );
		return elem;
	}

	protected AffineTransform3D fromXmlAffine( final Element transform )
	{
		final AffineTransform3D affine = XmlHelpers.loadAffineTransform3D( transform.getChild( SOURCETRANSFORM_AFFINE_TAG ) );
		return affine;
	}

	protected Element toXmlAffine( final AffineTransform3D transform )
	{
		final Element elem = new Element( SOURCETRANSFORM_TAG );
		elem.setAttribute( SOURCETRANSFORM_TYPE_ATTRIBUTE_NAME, SOURCETRANSFORM_TYPE_VALUE_AFFINE );
		elem.addContent( XmlHelpers.affineTransform3DElement( SOURCETRANSFORM_AFFINE_TAG, transform ) );
		return elem;
	}
}
