package bdv.ui.convertersetupeditor;

import java.awt.Color;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;

import org.scijava.listeners.Listeners;

import bdv.tools.brightness.ColorIcon;

/**
 * A {@code JPanel} with a color button (for setting {@code ConverterSetup}
 * colors).
 *
 * @author Tobias Pietzsch
 */
class ColorPanel extends JPanel
{
	private final JButton colorButton;

	private final ARGBType color = new ARGBType();

	public interface ChangeListener
	{
		void colorChanged();
	}

	private final Listeners.List< ChangeListener > listeners = new Listeners.SynchronizedList<>();

	public ColorPanel()
	{
		setLayout( new MigLayout( "ins 0, fillx, filly, hidemode 3", "[grow]", "" ) );
		colorButton = new JButton();
		this.add( colorButton, "center" );

		colorButton.addActionListener( e -> chooseColor() );

		colorButton.setBorderPainted( false );
		colorButton.setFocusPainted( false );
		colorButton.setContentAreaFilled( false );
		colorButton.setMinimumSize( new Dimension( 46, 42 ) );
		colorButton.setPreferredSize( new Dimension( 46, 42 ) );
		setColor( null );
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		super.setEnabled( enabled );
		if ( colorButton != null )
			colorButton.setEnabled( enabled );
	}

	private void chooseColor()
	{
		final Color newColor = JColorChooser.showDialog( null, "Set Source Color", new Color( color.get() ) );
		if ( newColor == null )
			return;
		setColor( new ARGBType(  newColor.getRGB() | 0xff000000 ) );
		listeners.list.forEach( ChangeListener::colorChanged );
	};

	public Listeners< ChangeListener > changeListeners()
	{
		return listeners;
	}

	public synchronized void setColor( final ARGBType color )
	{
		if ( color == null )
			this.color.set( 0xffaaaaaa );
		else
			this.color.set( color );
		colorButton.setIcon( new ColorIcon( new Color( this.color.get() ), 30, 30, 10, 10, true ) );
	}

	public ARGBType getColor()
	{
		return color.copy();
	}
}
