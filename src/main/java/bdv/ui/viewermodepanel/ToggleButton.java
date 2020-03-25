package bdv.ui.viewermodepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;

class ToggleButton extends JPanel
{
	private final String tooltipText;
	private final String selectedTooltipText;

	private final JToggleButton button;

	public ToggleButton(
			final Icon icon,
			final Icon selectedIcon,
			final String tooltipText,
			final String selectedTooltipText )
	{
		super( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );
		this.tooltipText = tooltipText;
		this.selectedTooltipText = selectedTooltipText;

		button = new JToggleButton( icon );
		button.setSelectedIcon( selectedIcon );
		setLook( button );

		this.setBackground( Color.white );
		this.add( button, "growx, center, wrap" );
	}

	public void setSelected( final boolean selected )
	{
		button.setSelected( selected );
		button.setToolTipText( selected ? selectedTooltipText : tooltipText );
	}

	public boolean isSelected()
	{
		return button.isSelected();
	}

	public void addActionListener( final ActionListener l )
	{
		button.addActionListener( l );
	}

	public void removeActionListener( final ActionListener l )
	{
		button.removeActionListener( l );
	}

	private void setLook( final JToggleButton button )
	{
		button.setMaximumSize( new Dimension( button.getIcon().getIconWidth(), button.getIcon().getIconHeight() ) );
		button.setBackground( Color.white );
		button.setBorderPainted( false );
		button.setFocusPainted( false );
		button.setContentAreaFilled( false );
	}
}
