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
package bdv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.UIManager;

/**
 * AWT/Swing helpers.
 *
 * @author Tobias Pietzsch
 */
public class UIUtils
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

	/**
	 * Get an approximate UI scaling factor.
	 *
	 * TODO UI scaling depends on the LAF and there is no straight forward way
	 *   to get a consistent scaling factor that works on all platforms.
	 *   However, some LAFs have implemented their own strategy to estimate
	 *   this scaling factor and adjust font sizes and element heights
	 *   accordingly.  The best one I know of is available in th.  We therefore use the default font size compared to a
	 *   hypothesized 'normal' font size of 12 as a surrogate for the UI
	 *   scaling factor.  This is not great.
	 *
	 * @return approximate UI scaling factor
	 */
	public static double getUIScaleFactor()
	{
		return UIManager.getFont( "Panel.font" ).getSize() / 12.0;
	}
}
