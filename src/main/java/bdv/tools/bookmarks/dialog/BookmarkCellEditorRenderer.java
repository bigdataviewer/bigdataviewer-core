package bdv.tools.bookmarks.dialog;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import bdv.tools.bookmarks.bookmark.IBookmark;

public class BookmarkCellEditorRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    private static final long serialVersionUID = 1L;
    private BookmarkCellPanel renderer = new BookmarkCellPanel();
    private BookmarkCellPanel editor = new BookmarkCellPanel();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        renderer.setBookmark((IBookmark) value);
        return renderer;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
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
}
