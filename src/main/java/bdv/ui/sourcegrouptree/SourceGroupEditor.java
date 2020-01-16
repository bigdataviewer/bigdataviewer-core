package bdv.ui.sourcegrouptree;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * @author Tobias Pietzsch
 */
class SourceGroupEditor extends DefaultTreeCellEditor
{
	private final SourceGroupTreeCellRenderer sourceGroupRenderer;

	public SourceGroupEditor( final JTree tree, final SourceGroupTreeCellRenderer sourceGroupRenderer )
	{
		// super class uses the DefaultTreeCellRenderer for font, icons, preferred size, etc
		super( tree, new DefaultTreeCellRenderer() );
		this.sourceGroupRenderer = sourceGroupRenderer;
	}

	// Overridden because we don't want to start the editing timer at all
	@Override
	protected void startEditingTimer()
	{
	}

	@Override
	protected void determineOffset( final JTree tree, final Object value, final boolean isSelected, final boolean expanded, final boolean leaf, final int row )
	{
		editingIcon = null;
		offset = sourceGroupRenderer.determineOffset( value );
	}
}
