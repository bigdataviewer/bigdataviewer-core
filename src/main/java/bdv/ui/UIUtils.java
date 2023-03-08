/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.stream.Collectors;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.UIScale;

/**
 * AWT/Swing helpers.
 *
 * @author Tobias Pietzsch
 */
public final class UIUtils
{
	/**
	 * Mix colors {@code c1} and {@code c2} by ratios {@code c1Weight} and {@code (1-c1Weight)}, respectively.
	 */
	public static Color mix( final Color c1, final Color c2, final double c1Weight )
	{
		final double c2Weight = 1.0 - c1Weight;
		return new Color(
				( int ) ( c1.getRed() * c1Weight + c2.getRed() * c2Weight ),
				( int ) ( c1.getGreen() * c1Weight + c2.getGreen() * c2Weight ),
				( int ) ( c1.getBlue() * c1Weight + c2.getBlue() * c2Weight ) );
	}

	/**
	 * Set the preferred width of a component (leaving the preferred height untouched).
	 */
	public static void setPreferredWidth( final Component component, final int preferredWidth )
	{
		component.setPreferredSize( new Dimension( preferredWidth, component.getPreferredSize().height ) );
	}

	/**
	 * Set the preferred height of a component (leaving the preferred width untouched).
	 */
	public static void setPreferredHeight( final Component component, final int preferredHeight )
	{
		component.setPreferredSize( new Dimension( component.getPreferredSize().width, preferredHeight ) );
	}

	/**
	 * Set the minimum width of a component (leaving the minimum height untouched).
	 */
	public static void setMinimumWidth( final Component component, final int minimumWidth )
	{
		component.setMinimumSize( new Dimension( minimumWidth, component.getMinimumSize().height ) );
	}

	/**
	 * Set the minimum height of a component (leaving the minimum width untouched).
	 */
	public static void setMinimumHeight( final Component component, final int minimumHeight )
	{
		component.setMinimumSize( new Dimension( component.getMinimumSize().width, minimumHeight ) );
	}

	public enum TextPosition
	{
		TOP_CENTER,
		TOP_RIGHT
	}

	public static void drawString(
			final Graphics g,
			final TextPosition textPosition,
			final int line,
			final String text )
	{
		final Object key = UIUtils.class;
		final double uiScale = UIUtils.getUIScaleFactor( key );

		final FontMetrics fontMetrics = g.getFontMetrics();
		final Rectangle clipBounds = g.getClipBounds();
		final int spacing = fontMetrics.getHeight();
		final int textWidth = fontMetrics.stringWidth( text );

		final int posX;
		switch ( textPosition )
		{
		case TOP_CENTER:
			posX = ( int ) ( ( clipBounds.getWidth() - textWidth ) / 2 );
			break;
		default:
		case TOP_RIGHT:
			posX = ( int ) ( clipBounds.getWidth() - textWidth - uiScale * 17 );
			break;
		}

		final int posY =  ( line + 1 ) * spacing - 1;

		g.drawString( text, posX, posY );
	}

	/**
	 * cache UI scale factors
	 */
	private static final WeakHashMap< Object, Double > uiScaleFactors = new WeakHashMap<>();

	/**
	 * Get an approximate UI scaling factor.
	 *
	 * TODO UI scaling depends on the LAF and there is no straight forward way
	 *   to get a consistent scaling factor that works on all platforms.
	 *   However, some LAFs have implemented their own strategy to estimate
	 *   this scaling factor and adjust font sizes and element heights
	 *   accordingly.  We therefore use the default font size compared to a
	 *   hypothesized 'normal' font size of 12 as a surrogate for the UI
	 *   scaling factor.  This is not great.
	 *
	 * @return approximate UI scaling factor
	 */
	public static double getUIScaleFactor( final Object key )
	{
		return uiScaleFactors.computeIfAbsent( key, k -> ( double ) UIScale.getUserScaleFactor() );
	}


	private static int getFontSize( final Font referenceFont, final String category )
	{
		final double uiScale = getUIScaleFactor( Runtime.getRuntime() );
		final int referenceFontSize = referenceFont.getSize();

		switch (category)
		{
		case "mini":
			return ( int )Math.round( referenceFontSize - 3 * uiScale );
		case "small":
			return ( int )Math.round( referenceFontSize - 2 * uiScale );
		case "medium":
			return ( int )Math.round( referenceFontSize - uiScale );
		case "large":
			return ( int )Math.round( referenceFontSize + 2 * uiScale );
		default:
			return referenceFontSize;
		}
	}

	public static Font getFont( final String key )
	{
		// TODO: should we cache these?

		// TODO: should we add legacy support for the "h0.font" etc of flatlaf?
		//       (See https://www.formdev.com/flatlaf/typography/#set_style_class)

		Font font = UIManager.getFont( key );

		/* all this bruhaha is for the legacy LAFs that do not have user friendly font definitions */
		if ( font == null )
		{
			final Font defaultFont = UIManager.getFont("Label.font");

			final int fontSize;
			if (key.contains("mini"))
				fontSize = getFontSize(defaultFont, "mini");
			else if (key.contains("small"))
				fontSize = getFontSize(defaultFont, "small");
			else if (key.contains("medium"))
				fontSize = getFontSize(defaultFont, "medium");
			else if (key.contains("large"))
				fontSize = getFontSize(defaultFont, "large");
			else
				fontSize = defaultFont.getSize();

			final String fontName;
			if (key.contains("monospace") || key.contains("Monospace"))
				fontName = Font.MONOSPACED;
			else
				fontName = defaultFont.getName();

			final int fontStyle;
			if (key.contains("bold"))
				fontStyle = Font.BOLD;
			else
				fontStyle = defaultFont.getStyle();

			font = new Font(fontName, fontStyle, fontSize);
		}

		return font;
	}

	/**
	 * Resets the caches for {@link #getUIScaleFactor}.
	 */
	public static void resetPropertyCaches()
	{
		uiScaleFactors.clear();
	}

	/**
	 * Tell if the LAF color specified for the provided key is darker than 50%
	 * gray.  If the property is undefined, it is considered not dark.
	 *
	 * E.g. isDark( "Panel.background" ) is often a good indicator that an LAF
	 * is dark/ inverse.
	 *
	 * @return true for dark colors
	 */
	public static boolean isDark( final String key )
	{
		final Color bg = UIManager.getColor( key );
		if ( bg == null ) return false;
		else return ( bg.getRed() + bg.getGreen() + bg.getBlue() ) / 3.0 < 127;
	}

	/**
	 * Get boolean property with the specified {@code key} from the swing {@link UIManager}.
	 * If the property is not set, return {@code defaultValue} instead.
	 */
	public static boolean getUIBoolean( final String key, final boolean defaultValue )
	{
		final Object value = UIManager.get( key );
		if ( value instanceof Boolean )
			return ( Boolean ) value;
		else
			return defaultValue;
	}

	public static synchronized void installFlatLafInfos()
	{
		final Set< String > lafNames = Arrays
				.stream( UIManager.getInstalledLookAndFeels() )
				.map( UIManager.LookAndFeelInfo::getName )
				.collect( Collectors.toSet() );
		if ( !lafNames.contains( FlatLightLaf.NAME ) )
			FlatLightLaf.installLafInfo();
		if ( !lafNames.contains( FlatDarkLaf.NAME ) )
			FlatDarkLaf.installLafInfo();
		if ( !lafNames.contains( FlatDarculaLaf.NAME ) )
			FlatDarculaLaf.installLafInfo();
		if ( !lafNames.contains( FlatIntelliJLaf.NAME ) )
			FlatIntelliJLaf.installLafInfo();
	}
}
