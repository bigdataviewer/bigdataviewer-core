package bdv.ui.convertersetupeditor;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * A {@code JPanel} containing a {@link ColorPanel} and a
 * {@link BoundedRangePanel}. It can be constructed for a {@code SourceTable} or
 * a {@code SourceGroupTree}, and will be set up to edit the selected
 * source/groups respectively.
 *
 * @author Tobias Pietzsch
 */
public class ConverterSetupEditPanel extends JPanel
{
	private final ColorPanel colorPanel;

	private final BoundedRangePanel rangePanel;

	public ConverterSetupEditPanel()
	{
		super( new MigLayout( "ins 0, fillx, hidemode 3", "[]0[grow]", "" ) );
		colorPanel = new ColorPanel();
		rangePanel = new BoundedRangePanel();

		( ( MigLayout ) rangePanel.getLayout() ).setLayoutConstraints( "ins 5 5 5 10, fillx, filly, hidemode 3" );
		add( colorPanel, "growy" );
		add( rangePanel, "grow" );
	}
}
