package bdv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

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
}
