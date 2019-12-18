package bdv.ui;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Example card with high priority.
 *
 * @author Tim-Oliver Buchholz, CSBD / MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresden
 */
@Plugin( type = HighPriorityCard.class, priority = Priority.HIGH )
public class ExampleCardHighPriority extends JPanel implements HighPriorityCard
{

	@Parameter
	private Age age;

	@Override
	public Card getCard()
	{
		this.setLayout( new MigLayout( "fillx", "[]", "" ) );
		this.setBackground( Color.white );

		final JLabel content = new JLabel( "My age is " + age.getAge() );
		content.setBackground( Color.white );
		this.add( content, "growx" );
		return this;
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

	static class Age
	{
		private final long age;

		public Age( final long age )
		{
			this.age = age;
		}

		public long getAge()
		{
			return age;
		}
	}
}
