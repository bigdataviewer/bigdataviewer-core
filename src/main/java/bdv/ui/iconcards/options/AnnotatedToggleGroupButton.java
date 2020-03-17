package bdv.ui.iconcards.options;

import java.awt.Font;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;

public class AnnotatedToggleGroupButton extends JPanel
{

	private final ToggleGroupButton button;

	private final JLabel annotation;

	private final List< String> annotations;

	public AnnotatedToggleGroupButton( final List< Icon > toggleIcons, final List< String > toggleLabels, final List< Runnable > toggleActions, final List< String > annotations )
	{
		this.annotations = annotations;

		button = new ToggleGroupButton( toggleIcons, toggleLabels, toggleActions );
		annotation = new JLabel( annotations.get( button.getCurrent() ) );

		this.setLayout( new MigLayout( "ins 0, fillx, filly", "[]", "[]0lp![]" ) );

		this.add( button, "growx, center, wrap" );
		this.add( annotation, "center" );

		button.addChangeListener( l -> {updateAnnotation();} );
	}

	private void updateAnnotation()
	{
		annotation.setText( annotations.get( button.getCurrent() ) );
//		annotation.invalidate();
		this.update( annotation.getGraphics() );
	}

	public void setAnnotationFont(final Font font)
	{
		annotation.setFont( font );
	}

	public synchronized void next() {
		button.next();
	}

	public synchronized void setCurrent(final int index) {
		button.setCurrent( index );
	}

	public synchronized void setCurrent(final String label) {
		button.setCurrent( label );
	}

	public synchronized int getCurrent() { return button.getCurrent(); }

	public synchronized int getIndexOfLabel(final String label) {
		return button.getIndexOfLabel( label );
	}

	public synchronized void addOption( final Icon icon, final String label, final Runnable action, final String annotation) {
		button.addOption( icon, label, action );
		annotations.add(annotation);
	}

	public synchronized void removeOption(final String label) {
		removeOption( button.getIndexOfLabel( label ) );
	}

	public synchronized void removeOption(final int index) {
		button.removeOption( index );
		annotations.remove( index );
	}
}
