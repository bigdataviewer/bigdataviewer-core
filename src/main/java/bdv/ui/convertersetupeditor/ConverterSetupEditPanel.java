package bdv.ui.convertersetupeditor;

import bdv.viewer.ConverterSetupBounds;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import bdv.viewer.ConverterSetups;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.sourcegrouptree.SourceGroupTree;

/**
 * A {@code JPanel} containing a {@link ColorPanel} and a
 * {@link BoundedRangePanel}. It can be constructed for a {@link SourceTable} or
 * a {@link SourceGroupTree}, and will be set up to edit the selected
 * source/groups respectively.
 *
 * @author Tobias Pietzsch
 */
public class ConverterSetupEditPanel extends JPanel
{
	private final ColorPanel colorPanel;

	private final BoundedRangePanel rangePanel;

	public ConverterSetupEditPanel(
			final SourceGroupTree tree,
			final ConverterSetups converterSetups )
	{
		this();
		new BoundedRangeEditor( tree, converterSetups, rangePanel, converterSetups.getBounds() );
		new ColorEditor( tree, converterSetups, colorPanel );
	}

	public ConverterSetupEditPanel(
			final SourceTable table,
			final ConverterSetups converterSetups )
	{
		this();
		new BoundedRangeEditor( table, converterSetups, rangePanel, converterSetups.getBounds() );
		new ColorEditor( table, converterSetups, colorPanel );
	}

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
