package bdv.ui;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Example card with high priority.
 *
 * @author Tim-Oliver Buchholz, CSBD / MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 */
@Plugin( type = Card.class, priority = Priority.HIGH )
public class ExampleCardHighPriority extends JPanel implements Card
{
	public ExampleCardHighPriority()
	{
		this.setLayout( new MigLayout( "fillx", "[]", "" ) );
		this.setBackground( Color.white );

		final JLabel content = new JLabel( "My priority is 10" );
		content.setBackground( Color.white );

		this.add( content, "growx" );
	}

	@Override
	public String getName()
	{
		return "Oscar";
	}

	@Override
	public JComponent getComponent()
	{
		return this;
	}

	@Override
	public boolean getDefaultVisibilty()
	{
		return true;
	}
}
