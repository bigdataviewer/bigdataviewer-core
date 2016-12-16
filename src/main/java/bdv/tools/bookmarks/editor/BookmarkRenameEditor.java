package bdv.tools.bookmarks.editor;

import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.tools.bookmarks.Bookmarks;
import bdv.viewer.ViewerPanel;

public class BookmarkRenameEditor {

	static enum Mode
	{
		INACTIVE,
		RENAME
	}
	
	public final static String ACTION_INPUT_ID = "bookmarkrenamer";
	
	private Mode mode = Mode.INACTIVE;
	
	private String oldKey;
	
	private final InputActionBindings bindings;
	private final ArrayList< String  > inputMapsToBlock;
	
	private final ActionMap actionMap;
	private final InputMap inputMap;

	private List<BookmarkRenameEditorListener> listeners;
	
	public BookmarkRenameEditor(final ViewerPanel viewer, final InputActionBindings inputActionBindings, final Bookmarks bookmarks){
		
		listeners = new ArrayList<>();
		
		bindings = inputActionBindings;
		inputMapsToBlock = new ArrayList<>( Arrays.asList( "bdv", "navigation" ) );
		
		final KeyStroke abortKey = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 );
		final Action abortAction = new AbstractAction( "abort rename" )
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				abort();
			}

			private static final long serialVersionUID = 1L;
		};
		
		actionMap = new ActionMap();
		inputMap = new InputMap();
		
		actionMap.put( "abort rename", abortAction );
		inputMap.put( abortKey, "abort rename" );
		
		bindings.addActionMap( ACTION_INPUT_ID, actionMap );
		
		viewer.getDisplay().addKeyListener( new KeyAdapter(){
			@Override
			public void keyTyped( final KeyEvent e ){
				if(mode != Mode.INACTIVE){
					final String key = String.valueOf( e.getKeyChar() );
					finish(key);
				}
			}
		});
	}
	
	public void init(String oldKey){
		this.oldKey = oldKey;
		mode = Mode.RENAME;
		bindings.addInputMap( ACTION_INPUT_ID, inputMap, inputMapsToBlock );
	}
	
	public synchronized void abort()
	{
		done();
		
		for(BookmarkRenameEditorListener l : listeners)
			l.bookmarkRenameAborted(oldKey);
	}
	
	public synchronized void finish(String key)
	{
		done();
		
		for(BookmarkRenameEditorListener l : listeners)
			l.bookmarkRenameFinished(oldKey, key);
	}
	
	public synchronized void done()
	{
		mode = Mode.INACTIVE;
		bindings.removeInputMap( ACTION_INPUT_ID );
	}
	
	public void addListener(BookmarkRenameEditorListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(BookmarkRenameEditorListener listener){
		listeners.remove(listener);
	}
}
