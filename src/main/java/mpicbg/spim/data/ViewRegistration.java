package mpicbg.spim.data;

import net.imglib2.realtransform.AffineTransform3D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ViewRegistration
{
	/**
	 * The timepoint index.
	 */
	protected final int timepoint;

	/**
	 * The setup index (within the timepoint).
	 */
	protected final int setup;

	/**
	 * The affine registration model of this view mapping local into world
	 * coordinates.
	 */
	protected final AffineTransform3D model;

	public ViewRegistration( final int timepointIndex, final int setupIndex, final AffineTransform3D model )
	{
		this.timepoint = timepointIndex;
		this.setup = setupIndex;
		this.model = model;
	}

	/**
	 * Load a ViewSetup from an XML file.
	 *
	 * @param elem
	 *            The "ViewRegistration" DOM element.
	 */
	public ViewRegistration( final Element elem )
	{
		this(
				Integer.parseInt( elem.getElementsByTagName( "timepoint" ).item( 0 ).getTextContent() ),
				Integer.parseInt( elem.getElementsByTagName( "setup" ).item( 0 ).getTextContent() ),
				XmlHelpers.loadAffineTransform3D( ( Element ) elem.getElementsByTagName( "affine" ).item( 0 ) )
		);
	}

	/**
	 * Get the timepoint index.
	 *
	 * @return timepoint index
	 */
	public int getTimepointIndex()
	{
		return timepoint;
	}

	/**
	 * Get the setup index (within the timepoint).
	 *
	 * @return setup index
	 */
	public int getSetupIndex()
	{
		return setup;
	}

	/**
	 * Get the affine registration model of this view mapping local into world
	 * coordinates.
	 *
	 * @return registration model
	 */
	public AffineTransform3D getModel()
	{
		return model;
	}

	public Element toXml( final Document doc )
	{
		final Element elem = doc.createElement( "ViewRegistration" );

		elem.appendChild( XmlHelpers.intElement( doc, "timepoint", getTimepointIndex() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "setup", getSetupIndex() ) );
		elem.appendChild( XmlHelpers.affineTransform3DElement( doc, "affine", getModel() ) );

		return elem;
	}
}
