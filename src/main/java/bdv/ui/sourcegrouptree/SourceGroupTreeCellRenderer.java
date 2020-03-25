package bdv.ui.sourcegrouptree;

import bdv.ui.sourcegrouptree.SourceGroupTreeModel.GroupModel;
import bdv.ui.sourcegrouptree.SourceGroupTreeModel.SourceModel;
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
	private final Color textSelectionColor;

	// Color to use for the foreground for non-selected nodes.
	private final Color textNonSelectionColor;

	// Color to use for the background when a node is selected.
	private Color backgroundSelectionColor;

	// Color to use for the background when the node isn't selected.
	private final Color backgroundNonSelectionColor;

	// Color to use for the focus indicator when the node has focus.
	private final Color borderSelectionColor;

	// TODO
	private final Color backgroundDropCellColor;

	// TODO
	private final boolean fillBackground;

	// If true, a dashed line is drawn as the focus indicator.
	private final boolean drawDashedFocusIndicator;

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
		textSelectionColor = getUIColor( "Tree.selectionForeground" );
		textNonSelectionColor = getUIColor( "Tree.textForeground" );
		backgroundSelectionColor = getUIColor( "Tree.selectionBackground" );
		backgroundNonSelectionColor = getUIColor( "Tree.textBackground" );
		borderSelectionColor = getUIColor( "Tree.selectionBorderColor" );
		backgroundDropCellColor = getUIColor( "Tree.dropCellBackground", backgroundSelectionColor );
		fillBackground = getUIBoolean( "Tree.rendererFillBackground", true );
		drawDashedFocusIndicator = getUIBoolean( "Tree.drawDashedFocusIndicator", false );
	}

	/**
	 * Sets the color to use for the background if node is selected.
	 */
	public void setBackgroundSelectionColor( final Color newColor )
	{
		backgroundSelectionColor = newColor;
	}

	/**
	 * Returns the color to use for the background if node is selected.
	 */
	public Color getBackgroundSelectionColor()
	{
		return backgroundSelectionColor;
	}

	private static boolean getUIBoolean( String key, boolean defaultValue )
	{
		final Object value = UIManager.get( key );
		if ( value instanceof Boolean )
			return ( Boolean ) value;
		else
			return defaultValue;
	}

	private static Color getUIColor( String key )
	{
		return getUIColor( key, null );
	}

	private static Color getUIColor( String key, Color defaultValue )
	{
		final Object value = UIManager.get( key );
		if ( value instanceof Color )
			return ( Color ) value;
		else
			return defaultValue;
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
		Color bsColor = borderSelectionColor;

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
			if ( selected )
				fg = textSelectionColor;
			else
				fg = textNonSelectionColor;
			nameLabel.setForeground( fg );

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
				bColor = backgroundDropCellColor;
			else if ( selected )
				bColor = backgroundSelectionColor;
			else
				bColor = backgroundNonSelectionColor;

			if ( bColor == null )
				bColor = getBackground();

			if ( bColor != null && fillBackground )
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
			if ( selected )
				fg = textSelectionColor;
			else
				fg = textNonSelectionColor;
			setForeground( fg );

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
				bColor = backgroundDropCellColor;
			else if ( selected )
				bColor = backgroundSelectionColor;
			else
				bColor = backgroundNonSelectionColor;

			if ( bColor == null )
				bColor = getBackground();

			if ( bColor != null && fillBackground )
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
