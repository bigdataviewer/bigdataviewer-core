/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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

import bdv.ui.UIUtils;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.GroupModel;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.SourceModel;
import com.formdev.flatlaf.ui.FlatUIUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

/**
 * @author Tobias Pietzsch
 */
class SourceGroupTreeCellRenderer implements TreeCellRenderer
{
	/**
	 * Last tree the renderer was painted in.
	 */
	private JTree tree;

	/**
	 * Is the value currently selected.
	 */
	private boolean selected;

	/**
	 * True if has focus.
	 */
	private boolean hasFocus;


	// Color to use for the foreground for selected nodes.
	private Color selectionForeground;

	// TODO
	private Color selectionInactiveForeground;

	// Color to use for the foreground for non-selected nodes.
	private Color foreground;

	// Color to use for the background when the node isn't selected.
	private Color background;

	// Color to use for the background when the node is selected and the tree has focus.
	private Color selectionBackground;

	// Color to use for the background when the node is selected and the tree doesn't have focus.
	private Color selectionInactiveBackground;

	// Whether the tree has focus.
	private boolean treeHasFocus;


	// Color to use for the focus indicator when the node has focus.
	private Color selectionBorderColor;

	// TODO
	private Color dropCellBackground;

	// TODO
	private boolean rendererFillBackground;

	// If true, a dashed line is drawn as the focus indicator.
	private boolean drawDashedFocusIndicator;

	// If drawDashedFocusIndicator is true, the following are used.

	// Background color of the tree.
	private Color treeBGColor;

	// Color to draw the focus indicator in, determined from the background color.
	private Color focusBGColor;

	// actual renderers
	private GroupRenderer groupRenderer = new GroupRenderer();
	private SourceRenderer sourceRenderer = new SourceRenderer();

	// fallback for debugging
	private final DefaultTreeCellRenderer defaultRenderer = new DefaultTreeCellRenderer();

	SourceGroupTreeCellRenderer()
	{
		updateUI();
	}

	void updateUI()
	{
		foreground = UIManager.getColor( "Tree.foreground" ); // TODO: "foreground"?
		background = UIManager.getColor( "Tree.background" ); // TODO: "background"?
		selectionForeground = UIManager.getColor( "Tree.selectionForeground" );
		selectionInactiveForeground = FlatUIUtils.getUIColor(
				"Tree.selectionInactiveForeground",
				UIUtils.mix( selectionForeground, foreground, 0.0 )
		);
		selectionBackground = UIManager.getColor( "Tree.selectionBackground" );
		selectionInactiveBackground = FlatUIUtils.getUIColor(
				"Tree.selectionInactiveBackground",
				UIUtils.mix( selectionBackground, background, 0.5 )
		);
		selectionBorderColor = UIManager.getColor( "Tree.selectionBorderColor" );
		dropCellBackground = FlatUIUtils.getUIColor( "Tree.dropCellBackground", selectionBackground );
		rendererFillBackground = UIUtils.getUIBoolean( "Tree.rendererFillBackground", true );
		drawDashedFocusIndicator = UIUtils.getUIBoolean( "Tree.drawDashedFocusIndicator", false );
		SwingUtilities.updateComponentTreeUI( groupRenderer );
		SwingUtilities.updateComponentTreeUI( sourceRenderer );
	}

	/**
	 * Sets the color to use for the background if node is selected,
	 * depending on whether the tree has focus.
	 */
	public void setTreeHasFocus( final boolean hasFocus )
	{
		treeHasFocus = hasFocus;
	}

	/**
	 * Returns the color to use for the background if node is selected.
	 */
	public Color getBackgroundSelectionColor()
	{
		return treeHasFocus ? selectionBackground : selectionInactiveBackground;
	}

	@Override
	public Component getTreeCellRendererComponent( final JTree tree, final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus )
	{
		this.tree = tree;
		this.hasFocus = hasFocus;
		this.selected = selected;

		if ( value instanceof GroupModel )
			return groupRenderer.getTreeCellRendererComponent( ( GroupModel ) value );
		else if ( value instanceof SourceModel )
			return sourceRenderer.getTreeCellRendererComponent( ( SourceModel ) value );
		else
			return defaultRenderer.getTreeCellRendererComponent( tree, value, selected, expanded, leaf, row, hasFocus );
	}

	public int determineOffset( final Object value )
	{
		if ( value instanceof GroupModel )
			return groupRenderer.getOffset();
		else if ( value instanceof SourceModel )
			return sourceRenderer.getOffset();
		else
			return 0;
	}

	public boolean currentHit( final int x, final int y )
	{
		return groupRenderer.currentHit( x, y );
	}

	public boolean activeHit( final int x, final int y )
	{
		return groupRenderer.activeHit( x, y );
	}

	class TreeLabel extends JLabel
	{
		@Override
		public void setFont( Font font )
		{
			if ( font instanceof FontUIResource )
				font = null;
			super.setFont( font );
		}

		@Override
		public Font getFont()
		{
			Font font = super.getFont();
			if ( font == null && tree != null )
			{
				font = tree.getFont();
			}
			return font;
		}
	}

	private void paintFocus( Graphics g, int x, int y, int w, int h, Color notColor )
	{
		Color bsColor = selectionBorderColor;

		if ( bsColor != null && ( selected || !drawDashedFocusIndicator ) )
		{
			g.setColor( bsColor );
			g.drawRect( x, y, w - 1, h - 1 );
		}
		if ( drawDashedFocusIndicator && notColor != null )
		{
			if ( treeBGColor != notColor )
			{
				treeBGColor = notColor;
				focusBGColor = new Color( ~notColor.getRGB() );
			}
			g.setColor( focusBGColor );
			BasicGraphicsUtils.drawDashedRect( g, x, y, w, h );
		}
	}

	class GroupRenderer extends JPanel
	{
		private final TreeLabel nameLabel;

		private final JRadioButton currentRadioButton;

		private final JCheckBox activeCheckBox;

		GroupRenderer()
		{
			setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
			setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
			nameLabel = new TreeLabel();
//			UIUtils.setPreferredWidth( nameLabel, 100 );
			currentRadioButton = new JRadioButton();
			currentRadioButton.setBorder( new EmptyBorder( 0, 0, 0, 5 ) );
			currentRadioButton.setOpaque( false );
			activeCheckBox = new JCheckBox();
			activeCheckBox.setBorder( new EmptyBorder( 0, 0, 0, 5 ) );
			activeCheckBox.setOpaque( false );
			add( currentRadioButton );
			add( activeCheckBox );
			add( nameLabel );
			setOpaque( false );
			invalidate();
		}

		@Override
		public void setFont( Font font )
		{
			if ( font instanceof FontUIResource )
				font = null;
			super.setFont( font );
		}

		@Override
		public Font getFont()
		{
			Font font = super.getFont();
			if ( font == null && tree != null )
			{
				font = tree.getFont();
			}
			return font;
		}

		public Component getTreeCellRendererComponent( final GroupModel group )
		{
			nameLabel.setText( group.getName() );
			currentRadioButton.setSelected( group.isCurrent() );
			activeCheckBox.setSelected( group.isActive() );

			final Color fg;
			final Color bg;
			if ( selected )
			{
				fg = treeHasFocus ? selectionForeground : selectionInactiveForeground;
				bg = treeHasFocus ? selectionBackground : selectionInactiveBackground;
			}
			else
			{
				fg = foreground;
				bg = background;
			}
			nameLabel.setForeground( fg );
			nameLabel.setBackground( bg );

			final boolean enabled = tree.isEnabled();
			setEnabled( enabled );
			nameLabel.setEnabled( enabled );
			currentRadioButton.setEnabled( enabled );
			activeCheckBox.setEnabled( enabled );

			final ComponentOrientation componentOrientation = tree.getComponentOrientation();
			setComponentOrientation( componentOrientation );
			nameLabel.setComponentOrientation( componentOrientation );
			currentRadioButton.setComponentOrientation( componentOrientation );
			activeCheckBox.setComponentOrientation( componentOrientation );

			invalidate();
			return this;
		}

		// TODO
		private boolean isDropCell = false;

		/**
		 * Paints the value.  The background is filled based on selected.
		 */
		@Override
		public void paint( Graphics g )
		{
			Color bColor;

			if ( isDropCell )
				bColor = dropCellBackground;
			else if ( selected )
				bColor = getBackgroundSelectionColor();
			else
				bColor = background;

			if ( bColor == null )
				bColor = getBackground();

			if ( bColor != null && rendererFillBackground )
			{
				g.setColor( bColor );
				g.fillRect( 0, 0, getWidth(), getHeight() );
			}

			if ( hasFocus )
				paintFocus( g, 0, 0, getWidth(), getHeight(), bColor );

			super.paint( g );
		}

		public int getOffset()
		{
			return nameLabel.getX();
		}

		public boolean currentHit( final int x, final int y )
		{
			return currentRadioButton.getBounds().contains( x, y );
		}

		public boolean activeHit( final int x, final int y )
		{
			return activeCheckBox.getBounds().contains( x, y );
		}
	}

	class SourceRenderer extends TreeLabel
	{
		SourceRenderer()
		{
			setBorder( new EmptyBorder( 0, 35, 0, 0 ) );
			setOpaque( false );
		}

		public Component getTreeCellRendererComponent( final SourceModel source )
		{
			setText( source.getName() );

			final Color fg;
			final Color bg;
			if ( selected )
			{
				fg = treeHasFocus ? selectionForeground : selectionInactiveForeground;
				bg = treeHasFocus ? selectionBackground : selectionInactiveBackground;
			}
			else
			{
				fg = foreground;
				bg = background;
			}
			setForeground( fg );
			setBackground( bg );

			final boolean enabled = tree.isEnabled();
			setEnabled( enabled );

			final ComponentOrientation componentOrientation = tree.getComponentOrientation();
			setComponentOrientation( componentOrientation );

			return this;
		}

		// TODO
		private boolean isDropCell = false;

		/**
		 * Paints the value.  The background is filled based on selected.
		 */
		@Override
		public void paint( Graphics g )
		{
			Color bColor;

			if ( isDropCell )
				bColor = dropCellBackground;
			else if ( selected )
				bColor = getBackgroundSelectionColor();
			else
				bColor = background;

			if ( bColor == null )
				bColor = getBackground();

			if ( bColor != null && rendererFillBackground )
			{
				g.setColor( bColor );
				g.fillRect( 0, 0, getWidth(), getHeight() );
			}

			if ( hasFocus )
				paintFocus( g, 0, 0, getWidth(), getHeight(), bColor );

			super.paint( g );
		}

		public int getOffset()
		{
			return getX();
		}
	}
}
