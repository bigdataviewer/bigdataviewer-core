/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
