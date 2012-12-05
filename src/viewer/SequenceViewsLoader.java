package viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import mpicbg.tracking.data.SequenceDescription;
import mpicbg.tracking.data.View;
import mpicbg.tracking.data.ViewRegistration;
import mpicbg.tracking.data.ViewRegistrations;

import org.xml.sax.SAXException;

/**
 * Loads ViewRegistrations and SequenceDescription from XML files.
 * Provides all {@link View views}, see {@link #getView(int, int)}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SequenceViewsLoader
{
	final private SequenceDescription seq;

	final private ArrayList< View > views;

	public SequenceViewsLoader( final String viewRegistrationsFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final ViewRegistrations regs = ViewRegistrations.load( viewRegistrationsFilename );
		seq = SequenceDescription.load( regs.sequenceDescriptionName, true );
		views = new ArrayList< View >();
		createViews( regs );
	}

	public SequenceDescription getSequenceDescription()
	{
		return seq;
	}

	public View getView( final int timepoint, final int setup )
	{
		return views.get( timepoint * seq.numViewSetups() + setup );
	}

	private void createViews( final ViewRegistrations regs )
	{
		views.clear();

		if ( seq.numTimepoints() * seq.numViewSetups() != regs.registrations.size() )
			throw new RuntimeException( "ViewRegistrations does not match SequenceDescription" );

		final ArrayList< ViewRegistration > registrations = new ArrayList< ViewRegistration >( regs.registrations );
		Collections.sort( registrations, new Comparator< ViewRegistration >()
		{
			@Override
			public int compare( final ViewRegistration o1, final ViewRegistration o2 )
			{
				final int dt = o1.getTimepointIndex() - o2.getTimepointIndex();
				if ( dt == 0 )
					return o1.getSetupIndex() - o2.getSetupIndex();
				else
					return dt;
			}
		} );

		final Iterator< ViewRegistration > ri = registrations.iterator();
		for ( int ti = 0; ti < seq.timepoints.length; ++ti )
			for ( int si = 0; si < seq.setups.length; ++si )
			{
				final ViewRegistration reg = ri.next();
				if ( reg.getTimepointIndex() == ti && reg.getSetupIndex() == si )
					views.add( new View( seq, ti, si, reg.getModel() ) );
				else
					throw new RuntimeException( "ViewRegistrations does not match SequenceDescription" );
			}
	}
}
