package bdv.spimdata;

import java.util.Map;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.MissingViews;
import mpicbg.spim.data.sequence.TimePoints;

public class SequenceDescriptionMinimal extends AbstractSequenceDescription< BasicViewSetup, BasicViewDescription< BasicViewSetup >, BasicImgLoader< ? > >
{
	public SequenceDescriptionMinimal( final TimePoints timepoints, final Map< Integer, ? extends BasicViewSetup > setups, final BasicImgLoader< ? > imgLoader, final MissingViews missingViews )
	{
		super( timepoints, setups, imgLoader, missingViews );
	}

	@Override
	protected BasicViewDescription< BasicViewSetup > createViewDescription( final int timepointId, final int setupId )
	{
		return new BasicViewDescription< BasicViewSetup >( timepointId, setupId, true, this );
	}

	protected SequenceDescriptionMinimal()
	{}
}
