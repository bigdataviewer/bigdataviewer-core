package bdv.ui.sourcegrouptree;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
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
		editingIcon = emptyIcon;
		offset = sourceGroupRenderer.determineOffset( value );
	}

	private final Icon emptyIcon = new Icon()
	{
		private int iconHeight = -1;

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
		}

		@Override
		public int getIconWidth()
		{
			return 1;
		}

		@Override
		public int getIconHeight()
		{
			if ( iconHeight < 0 && renderer != null )
				iconHeight = renderer.getDefaultLeafIcon().getIconHeight();
			return iconHeight < 0 ? 1 : iconHeight;
		}
	};

}
