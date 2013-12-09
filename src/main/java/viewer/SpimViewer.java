package viewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.imglib2.ui.util.GuiUtil;
import viewer.gui.InputActionBindings;
import viewer.hdf5.img.Hdf5GlobalCellCache;
import viewer.render.SourceAndConverter;

public class SpimViewer extends JFrame
{
	final protected ViewerPanel viewer;

	private final InputActionBindings keybindings;

	/**
	 *
	 * @param width
	 *            width of the display window.
	 * @param height
	 *            height of the display window.
	 * @param sources
	 *            the {@link SourceAndConverter sources} to display.
	 * @param numTimePoints
	 *            number of available timepoints.
	 * @param numMipmapLevels
	 *            number of available mipmap levels.
	 */
	public SpimViewer( final int width, final int height, final List< SourceAndConverter< ? > > sources, final int numTimePoints, final Hdf5GlobalCellCache< ? > cache )
	{
//		super( "BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.ARGB_COLOR_MODEL ) );
		super( "BigDataViewer", GuiUtil.getSuitableGraphicsConfiguration( GuiUtil.RGB_COLOR_MODEL ) );
		viewer = new ViewerPanel( sources, numTimePoints, cache );
		keybindings = new InputActionBindings();

		getRootPane().setDoubleBuffered( true );
		setPreferredSize( new Dimension( width, height ) );
		add( viewer, BorderLayout.CENTER );
		pack();
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				viewer.stop();
			}
		} );

		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		new NavigationKeyHandler( this, viewer );
	}

	public void addHandler( final Object handler )
	{
		viewer.addHandler( handler );
		if ( KeyListener.class.isInstance( handler ) )
			addKeyListener( ( KeyListener ) handler );
	}

	// TODO: rename!?
	public ViewerPanel getViewer()
	{
		return viewer;
	}

	public InputActionBindings getKeybindings()
	{
		return keybindings;
	}
}
