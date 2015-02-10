package net.imglib2.display;

import net.imglib2.type.numeric.ARGBType;

public interface ColorConverter extends LinearRange
{
	public ARGBType getColor();

	public void setColor( final ARGBType c );

	public boolean supportsColor();
}
