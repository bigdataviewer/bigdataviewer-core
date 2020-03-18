package bdv.ui.viewermodepanel;

import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JLabel;

class LabeledToggleButton extends ToggleButton
{
	private final String text;
	private final String selectedText;

	private final JLabel label;

	public LabeledToggleButton(
			final Icon icon,
			final Icon selectedIcon,
			final String text,
			final String selectedText,
			final String tooltipText,
			final String selectedTooltipText )
	{
		super( icon, selectedIcon, tooltipText, selectedTooltipText );
		this.text = text;
		this.selectedText = selectedText;

		label = new JLabel( text );
		setFont( label );

		this.add( label, "center" );
	}

	public void setSelected( final boolean selected )
	{
		super.setSelected( selected );
		label.setText( selected ? selectedText : text );
	}

	private void setFont( final JLabel label )
	{
		label.setFont( new Font( Font.MONOSPACED, Font.BOLD, 9 ) );
	}
}
