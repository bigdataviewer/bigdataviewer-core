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
package bdv.ui.sourcetable;

import bdv.tools.brightness.ColorIcon;
import bdv.ui.UIUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import net.imglib2.type.numeric.ARGBType;

import static bdv.ui.sourcetable.SourceTableModel.COLOR_COLUMN;

/**
 * @author Tobias Pietzsch
 */
class ColorRenderer extends JLabel implements TableCellRenderer
{
	private final NoColorIcon noColorIcon;

	public ColorRenderer()
	{
		setBorder( new EmptyBorder( 0, 0, 0, 0 ) );
		setOpaque( true );
		setHorizontalAlignment( SwingConstants.CENTER );
		noColorIcon = new NoColorIcon( 14, 14 );
	}

	@Override
	public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column )
	{
		setBackground( isSelected ? table.getSelectionBackground() : table.getBackground() );
		if ( value == null )
		{
			setIcon( noColorIcon );
		}
		else
		{
//			setBackground( new Color( ( ( ARGBType ) value ).get() ) );
			final Color c = new Color( ( ( ARGBType ) value ).get() );
			final int w = table.getColumnModel().getColumn( COLOR_COLUMN ).getWidth() - 6;
			final int h = table.getRowHeight() - 6;
			final Color outlineColor = UIUtils.mix(
					c, isSelected ? table.getSelectionForeground() : table.getForeground(), 0.5 );
			setIcon( new ColorIcon( c, w, h, 5, 5, true, outlineColor ) );
		}
		return this;
	}

	private static class NoColorIcon implements Icon
	{
		private final int width;
		private final int height;

		private final int size; // == min(width, height)
		private final int ox;
		private final int oy;
		private final int lox0;
		private final int loy0;
		private final int lox1;
		private final int loy1;

		private final Color color = new Color( 0xBBBBBB );
		private final BasicStroke stroke = new BasicStroke( 2 );

		public NoColorIcon( final int width, final int height )
		{
			this.width = width;
			this.height = height;

			size = Math.min( width, height );
			ox = ( width - size ) / 2;
			oy = ( height - size ) / 2;
			lox0 = ( int ) ( ox + size * ( 0.5 * ( 1 - Math.sqrt( 0.5 ) ) ) );
			loy0 = ( int ) ( oy + size * ( 0.5 * ( 1 - Math.sqrt( 0.5 ) ) ) );
			lox1 = ( int ) ( ox + size * ( 0.5 * ( 1 + Math.sqrt( 0.5 ) ) ) );
			loy1 = ( int ) ( oy + size * ( 0.5 * ( 1 + Math.sqrt( 0.5 ) ) ) );
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setColor( color );
			g2d.setStroke( stroke );

			final int lx0 = x + lox0;
			final int ly0 = y + loy0;
			final int lx1 = x + lox1;
			final int ly1 = y + loy1;
			g2d.drawLine( lx0, ly0, lx1, ly1 );

			final int x0 = x + ox;
			final int y0 = y + oy;
			g2d.drawOval( x0, y0, size, size );
		}

		@Override
		public int getIconWidth()
		{
			return width;
		}

		@Override
		public int getIconHeight()
		{
			return height;
		}
	}
}
