package creator;

import mpicbg.spim.data.ImgLoader;
import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.ViewSetup;

/**
 * A copy of a {@link ViewSetup} with another id.
 * Stores the {@link ViewSetup setup}'s original id and {@link SequenceDescription}.
 * For example, this can be used to access the original {@link ImgLoader}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ViewSetupWrapper extends ViewSetup
{
	private final SequenceDescription sourceSequence;

	private final int sourceSetupIndex;

	protected ViewSetupWrapper( final int id, final SequenceDescription sourceSequence, final ViewSetup sourceSetup )
	{
		super( id, sourceSetup.getAngle(), sourceSetup.getIllumination(), sourceSetup.getChannel(), sourceSetup.getWidth(), sourceSetup.getHeight(), sourceSetup.getDepth(), sourceSetup.getPixelWidth(), sourceSetup.getPixelHeight(), sourceSetup.getPixelDepth() );
		this.sourceSequence = sourceSequence;
		this.sourceSetupIndex = sourceSetup.getId();
	}

	public SequenceDescription getSourceSequence()
	{
		return sourceSequence;
	}

	public int getSourceSetupIndex()
	{
		return sourceSetupIndex;
	}
}