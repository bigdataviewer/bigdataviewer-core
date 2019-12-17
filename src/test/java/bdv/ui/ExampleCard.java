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
 */
@Plugin( type = Card.class, priority = Priority.NORMAL )
public class ExampleCard extends JPanel implements Card
{
	public ExampleCard()
	{
		this.setLayout( new MigLayout( "fillx", "[]", "" ) );
		this.setBackground( Color.white );

		final JLabel content = new JLabel( "Thank you @frauzufall!!" );
		content.setBackground( Color.white );

		this.add( content, "growx" );
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
}
