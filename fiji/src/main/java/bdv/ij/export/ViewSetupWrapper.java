package bdv.ij.export;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.ViewSetup;

/**
 * A copy of a {@link ViewSetup} with another id.
 * Stores the {@link ViewSetup setup}'s original id and {@link SequenceDescription}.
 * For example, this can be used to access the original {@link ImgLoader}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class ViewSetupWrapper extends BasicViewSetup
{
	private final AbstractSequenceDescription< ?, ?, ? > sourceSequence;

	private final int sourceSetupId;

	public ViewSetupWrapper( final int id, final AbstractSequenceDescription< ?, ?, ? > sourceSequence, final BasicViewSetup sourceSetup )
	{
		super( id, sourceSetup.getName(), sourceSetup.getSize(), sourceSetup.getVoxelSize() );
		this.sourceSequence = sourceSequence;
		this.sourceSetupId = sourceSetup.getId();
	}

	public AbstractSequenceDescription< ?, ?, ? > getSourceSequence()
	{
		return sourceSequence;
	}

	public int getSourceSetupId()
	{
		return sourceSetupId;
	}
}
