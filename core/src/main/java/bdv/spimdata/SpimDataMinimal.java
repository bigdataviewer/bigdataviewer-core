package bdv.spimdata;

import java.io.File;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistrations;

public class SpimDataMinimal extends AbstractSpimData< SequenceDescriptionMinimal >
{
	public SpimDataMinimal( final File basePath, final SequenceDescriptionMinimal sequenceDescription, final ViewRegistrations viewRegistrations )
	{
		super( basePath, sequenceDescription, viewRegistrations );
	}

	protected SpimDataMinimal()
	{}
}
