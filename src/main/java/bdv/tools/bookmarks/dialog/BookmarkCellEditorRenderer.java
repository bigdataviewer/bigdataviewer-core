package bdv.tools.bookmarks.dialog;

import java.awt.Color;
import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import bdv.tools.bookmarks.bookmark.IBookmark;

public class BookmarkCellEditorRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

	private static final long serialVersionUID = 1L;
	
	private final BookmarkCellPanel renderer;
	private final BookmarkCellPanel editor;
	private IBookmark activeBookmark;

	public BookmarkCellEditorRenderer(BookmarkManagementDialog bookmarkManagementDialog) {
		renderer = new BookmarkCellPanel(bookmarkManagementDialog);
		editor = new BookmarkCellPanel(bookmarkManagementDialog);
	}

	public BookmarkCellPanel getRenderer() {
		return renderer;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		IBookmark bookmark = (IBookmark) value;
		renderer.setBookmark(bookmark);

		if (bookmark.equals(activeBookmark)) {
			renderer.setBackground(Color.CYAN);
		}
		else {
			renderer.setBackground(UIManager.getColor("Panel.background"));
		}

		return renderer;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		IBookmark bookmark = (IBookmark) value;
		editor.setBookmark((IBookmark) value);
		return editor;
	}

	@Override
	public Object getCellEditorValue() {
		return editor.getBookmark();
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return false;
	}

	public void setActiveBookmark(IBookmark activeBookmark) {
		this.activeBookmark = activeBookmark;
	}
}
