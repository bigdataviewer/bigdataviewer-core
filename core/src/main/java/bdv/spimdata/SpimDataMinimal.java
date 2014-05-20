package bdv.spimdata;

import java.io.File;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistrations;

public class SpimDataMinimal extends AbstractSpimData< SequenceDescriptionMinimal >
{
	public SpimDataMinimal( final File basePath, final SequenceDescriptionMinimal sequenceDescription, final ViewRegistrations viewRegistrations )
	{
		super( basePath, sequenceDescription, viewRegistrations );
	}

	/**
	 * create copy of a {@link SpimDataMinimal} with replaced {@link BasicImgLoader}
	 */
	public SpimDataMinimal( final SpimDataMinimal other, final BasicImgLoader< ? > imgLoader )
	{
		super( other.getBasePath(), new SequenceDescriptionMinimal( other.getSequenceDescription(), imgLoader ), other.getViewRegistrations() );
	}

	protected SpimDataMinimal()
	{}
}
