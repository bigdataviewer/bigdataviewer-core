package bdv.tools.bookmarks.dialog;

import bdv.tools.bookmarks.BookmarksCollectionChangedListener;
import bdv.tools.bookmarks.bookmark.IBookmark;
import bdv.tools.bookmarks.editor.BookmarksEditor;
import bdv.viewer.ActiveBookmarkChangedListener;
import bdv.viewer.ViewerFrame;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public class BookmarkManagementDialog extends JDialog {

	private class BookmarkChangeListener implements BookmarksCollectionChangedListener {

		@Override
		public void bookmarksCollectionChanged() {
			repaintBookmark();
		}
	}
	
	private class ActiveBookmarkChangeListener implements ActiveBookmarkChangedListener{

		@Override
		public void activeBookmarkChanged(IBookmark previousBookmark, IBookmark activeBookmark) {
			repaintBookmark();
		}
		
	}

    private final BookmarksEditor bookmarksEditor;
	
	private final BookmarkChangeListener bookmarkChangedListener = new BookmarkChangeListener();
	private final ActiveBookmarkChangeListener activeBookmarkChangeListener = new ActiveBookmarkChangeListener();
    
    private final BookmarkListView bookmarkListView;

	public BookmarkManagementDialog(ViewerFrame owner, BookmarksEditor bookmarksEditor) {
		super(owner, "Bookmark Management", false);
		setSize(new Dimension(400, 500));
        setLocationRelativeTo(owner);
        
		this.bookmarksEditor = bookmarksEditor;
        System.out.println("bookmarksEditor.getBookmarks().size() = " + bookmarksEditor.getBookmarks().getAll().size());
		this.bookmarkListView = new BookmarkListView(bookmarksEditor.getBookmarks());
        
		bookmarksEditor.getBookmarks().addListener(bookmarkChangedListener);
		owner.getViewerPanel().addActiveBookmarkChangedListener(activeBookmarkChangeListener);

		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(new Color(220, 220, 220));
		getContentPane().add(buttonPane, BorderLayout.NORTH);
		buttonPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton newButton = new JButton("Add bookmark");
		buttonPane.add(newButton);
		newButton.setVerticalAlignment(SwingConstants.TOP);
		newButton.setHorizontalAlignment(SwingConstants.LEFT);

		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AddBookmarkDialog addBookmarkDialog = new AddBookmarkDialog(owner, bookmarksEditor);
				addBookmarkDialog.setLocationRelativeTo(BookmarkManagementDialog.this);
				addBookmarkDialog.setVisible(true);
			}
		});
        
        getContentPane().add(bookmarkListView, BorderLayout.CENTER);
		
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setVisible(false);
			}

		};
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), hideKey);
		am.put(hideKey, hideAction);

		//pack();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	public void repaintBookmark() {
        this.bookmarkListView.updateListItems();
	}
	
	public void selectBookmark(IBookmark bookmark){
		bookmarksEditor.recallTransformationOfBookmark(bookmark.getKey());
	}

	public void removeBookmark(IBookmark bookmark) {
		bookmarksEditor.deleteBookmark(bookmark.getKey());
	}
}
