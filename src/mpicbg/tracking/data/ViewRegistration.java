package mpicbg.tracking.data;

import mpicbg.tracking.data.io.XmlHelpers;
import mpicbg.tracking.transform.AffineModel3D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ViewRegistration
{
	/**
	 * The timepoint index.
	 */
	private final int timepoint;

	/**
	 * The setup index (within the timepoint).
	 */
	private final int setup;

	/**
	 * The affine registration model of this view mapping local into world
	 * coordinates.
	 */
	private final AffineModel3D model;

	public ViewRegistration( final int timepointIndex, final int setupIndex, final AffineModel3D model )
	{
		this.timepoint = timepointIndex;
		this.setup = setupIndex;
		this.model = model;
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
	public AffineModel3D getModel()
	{
		return model;
	}

	public static ViewRegistration fromXml( final Element reg )
	{
		final int timepoint = Integer.parseInt( reg.getElementsByTagName( "timepoint" ).item( 0 ).getTextContent() );
		final int setup = Integer.parseInt( reg.getElementsByTagName( "setup" ).item( 0 ).getTextContent() );
		final AffineModel3D model = AffineModel3D.fromXml( ( Element ) reg.getElementsByTagName( "iict_transform" ).item( 0 ) );

		return new ViewRegistration( timepoint, setup, model );
	}

	public static Element toXml( final Document doc, final ViewRegistration reg )
	{
		final Element elem = doc.createElement( "ViewRegistration" );

		elem.appendChild( XmlHelpers.intElement( doc, "timepoint", reg.getTimepointIndex() ) );
		elem.appendChild( XmlHelpers.intElement( doc, "setup", reg.getSetupIndex() ) );
		elem.appendChild( AffineModel3D.toXml( doc, reg.getModel() ) );

		return elem;
	}
}
