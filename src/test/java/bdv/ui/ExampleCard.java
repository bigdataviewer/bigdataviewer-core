package bdv.ui;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

/**
 * Example card with normal priority.
 *
 * @author Tim-Oliver Buchholz, CSBD / MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresden
 */
@Plugin( type = DiscoverableCard.class, priority = Priority.NORMAL )
public class ExampleCard extends JPanel implements DiscoverableCard
{

	@Override
	public Card getCard()
	{
		this.setLayout( new MigLayout( "fillx", "[]", "" ) );
		this.setBackground( Color.white );

		final JLabel content = new JLabel( "Thank you @frauzufall!!" );
		content.setBackground( Color.white );

		this.add( content, "growx" );
		return this;
	}

	@Override
	public String getName()
	{
		return "Bruno";
	}

	@Override
	public JComponent getComponent()
	{
		return this;
	}

	@Override
	public boolean getDefaultVisibilty()
	{
		return false;
	}

	@Override
	public void run()
	{
		// nothing
	}
}
