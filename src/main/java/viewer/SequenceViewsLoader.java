package viewer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * Loads SequenceDescription and ViewRegistrations from XML file.
 * Provides all {@link View views}, see {@link #getView(int, int)}.
 *
 * @author Tobias Pietzsch <tobias.pietzsch@gmail.com>
 */
public class SequenceViewsLoader
{
	final private SequenceDescription seq;

	final private ArrayList< View > views;

	public SequenceViewsLoader( final String xmlFilename ) throws JDOMException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();

		final File baseDirectory = new File( xmlFilename ).getParentFile();
		seq = new SequenceDescription( root, baseDirectory != null ? baseDirectory : new File("."), true );
		views = new ArrayList< View >();
		createViews( new ViewRegistrations( root.getChild( "ViewRegistrations" ) ) );
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
		for ( int ti = 0; ti < seq.numTimepoints(); ++ti )
			for ( int si = 0; si < seq.numViewSetups(); ++si )
			{
				final ViewRegistration reg = ri.next();
				if ( reg.getTimepointIndex() == ti && reg.getSetupIndex() == si )
					views.add( new View( seq, ti, si, reg.getModel() ) );
				else
					throw new RuntimeException( "ViewRegistrations does not match SequenceDescription" );
			}
	}
}
