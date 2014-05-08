package bdv.spimdata;

import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.XmlIoAbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.XmlIoBasicViewSetups;
import mpicbg.spim.data.registration.XmlIoViewRegistrations;
import mpicbg.spim.data.sequence.XmlIoMissingViews;
import mpicbg.spim.data.sequence.XmlIoTimePoints;

public class XmlIoSpimDataMinimal extends XmlIoAbstractSpimData< SequenceDescriptionMinimal, SpimDataMinimal >
{
	public XmlIoSpimDataMinimal()
	{
		super( SpimDataMinimal.class,
				new XmlIoAbstractSequenceDescription< BasicViewSetup, SequenceDescriptionMinimal >(
						SequenceDescriptionMinimal.class,
						new XmlIoTimePoints(),
						new XmlIoBasicViewSetups< BasicViewSetup >( BasicViewSetup.class ),
						new XmlIoMissingViews() ),
				new XmlIoViewRegistrations() );
	}
}
