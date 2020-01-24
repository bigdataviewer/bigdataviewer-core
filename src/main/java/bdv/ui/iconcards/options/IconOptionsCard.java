package bdv.ui.iconcards.options;

import java.net.URL;

/**
 * A card storing options in groups.
 * Each option can have multiple choices, each choice linked to an action.
 *
 * @author Deborah Schmidt, CSBD/MPI-CBG, Dresden
 */
public interface IconOptionsCard
{

	void addOptionChoice( String optionGroup, String optionName, String choiceName, Runnable action, URL iconURL );

	void setSelected( String optionGroup, String optionName, String choiceName );

	void removeOption( String optionGroup, String optionName );

	void removeGroup( String optionGroup );

}
