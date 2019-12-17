package bdv.ui;

import javax.swing.JComponent;
import org.scijava.plugin.SciJavaPlugin;

/**
 * Interface for cards which are discoverable by {@link CardPanel}.
 *
 * @author Tim-Oliver Buchholz, CSBD / MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 */
public interface Card extends SciJavaPlugin
{
	/**
	 * This name is displayed in the card header.
	 *
	 * @return card name
	 */
	String getName();

	/**
	 * @return the component to wrap in the card
	 */
	JComponent getComponent();

	/**
	 * {@link CardPanel} cards have an initial visibility.
	 * If this is true, then the card is open, hence the component is visible.
	 *
	 * @return is visible
	 */
	boolean getDefaultVisibilty();
}
