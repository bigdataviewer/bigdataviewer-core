package bdv.ui;

import org.scijava.command.Command;

/**
 * A {@link DiscoverableCard} can have {@link org.scijava.plugin.Parameter} annotations
 * and is discoverable by the {@link org.scijava.command.CommandService}.
 *
 * @author Tim-Oliver Buchholz, CSBD / MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD / MPI-CBG, Dresden
 * @author Tobias Pietzsch, CSBD / MPI-CBG, Dresden
 */
public interface DiscoverableCard extends Card, Command
{
	Card getCard();
}
