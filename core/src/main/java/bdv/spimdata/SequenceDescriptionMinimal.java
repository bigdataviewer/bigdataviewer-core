package bdv.spimdata;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;

public class SequenceDescriptionMinimal extends AbstractSequenceDescription< BasicViewSetup, BasicViewDescription< BasicViewSetup >, BasicImgLoader< ? > >
{
	@Override
	protected BasicViewDescription< BasicViewSetup > createViewDescription( final int timepointId, final int setupId )
	{
		return new BasicViewDescription< BasicViewSetup >( timepointId, setupId, true, this );
	}

	protected SequenceDescriptionMinimal()
	{}
}
