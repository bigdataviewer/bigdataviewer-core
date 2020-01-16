package bdv.ui.convertersetupeditor;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.ui.UIUtils;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import net.imglib2.type.numeric.ARGBType;

class ColorEditor
{
	private final Supplier< List< ConverterSetup > > selectedConverterSetups;

	private final ColorPanel colorPanel;

	private final Color equalColor;

	private final Color notEqualColor;

	private ColorEditor(
			final Supplier< List< ConverterSetup > > selectedConverterSetups,
			final ConverterSetups converterSetups,
			final ColorPanel colorPanel )
	{
		this.selectedConverterSetups = selectedConverterSetups;
		this.colorPanel = colorPanel;

		colorPanel.changeListeners().add( () -> updateConverterSetupColors() );
		converterSetups.listeners().add( s -> updateColorPanel() );

		equalColor = colorPanel.getBackground();
		notEqualColor = UIUtils.mix( equalColor, Color.red, 0.9 );
	}

	private boolean blockUpdates = false;

	private List< ConverterSetup > converterSetups;

	private synchronized void updateConverterSetupColors()
	{
		if ( blockUpdates || converterSetups == null || converterSetups.isEmpty() )
			return;

		ARGBType color = colorPanel.getColor();

		for ( final ConverterSetup converterSetup : converterSetups )
		{
			if ( converterSetup.supportsColor() )
				converterSetup.setColor( color );
		}

		updateColorPanel();
	}

	private synchronized void updateSelection()
	{
		converterSetups = selectedConverterSetups.get();
		updateColorPanel();
	}

	private synchronized void updateColorPanel()
	{
		blockUpdates = true;

		if ( converterSetups == null || converterSetups.isEmpty() )
		{
			colorPanel.setEnabled( false );
			colorPanel.setColor( null );
			colorPanel.setBackground( equalColor );
		}
		else
		{
			ARGBType color = null;
			boolean allColorsEqual = true;
			for ( final ConverterSetup converterSetup : converterSetups )
			{
				if ( converterSetup.supportsColor() )
				{
					if ( color == null )
						color = converterSetup.getColor();
					else
						allColorsEqual &= color.equals( converterSetup.getColor() );
				}
			}
			colorPanel.setEnabled( color != null );
			colorPanel.setColor( color );
			colorPanel.setBackground( allColorsEqual ? equalColor : notEqualColor );
		}

		blockUpdates = false;
	}
}
