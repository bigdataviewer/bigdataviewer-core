package viewer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import mpicbg.spim.data.SequenceDescription;
import mpicbg.spim.data.View;
import mpicbg.spim.data.ViewRegistration;
import mpicbg.spim.data.ViewRegistrations;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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

	public SequenceViewsLoader( final String xmlFilename ) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		final DocumentBuilder db = dbf.newDocumentBuilder();
		final Document dom = db.parse( xmlFilename );
		final Element root = dom.getDocumentElement();

		final File baseDirectory = new File( xmlFilename ).getParentFile();
		seq = new SequenceDescription( root, baseDirectory != null ? baseDirectory : new File("."), true );
		views = new ArrayList< View >();
		createViews( new ViewRegistrations( root ) );
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
