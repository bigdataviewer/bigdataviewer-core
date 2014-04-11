package mpicbg.spim.data;

import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

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
				Integer.parseInt( elem.getChildText( "timepoint" ) ),
				Integer.parseInt( elem.getChildText( "setup" ) ),
				XmlHelpers.loadAffineTransform3D( elem.getChild( "affine" ) )
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

	public Element toXml()
	{
		final Element elem = new Element( "ViewRegistration" );

		elem.addContent( XmlHelpers.intElement( "timepoint", getTimepointIndex() ) );
		elem.addContent( XmlHelpers.intElement( "setup", getSetupIndex() ) );
		elem.addContent( XmlHelpers.affineTransform3DElement( "affine", getModel() ) );

		return elem;
	}
}
