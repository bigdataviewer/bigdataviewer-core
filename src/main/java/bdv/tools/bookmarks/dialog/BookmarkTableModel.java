package bdv.tools.bookmarks.dialog;

import javax.swing.table.DefaultTableModel;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.bookmark.IBookmark;

public class BookmarkTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;

    private final Bookmarks bookmarks;
    
    public BookmarkTableModel(Bookmarks bookmarks){
    	this.bookmarks = bookmarks;
    }
    
    @Override
    public int getColumnCount() {
        return 1;
    }
    
    public Bookmarks getBookmarks(){
    	return this.bookmarks;
    }
    
    public void repaint(){
    	removeAllRows();
    	
    	for(IBookmark b : this.bookmarks.getAll()){
    		addRow(new Object[]{b});
    	}
    	
    	 super.fireTableDataChanged();
    }

    private void removeAllRows(){
    	if (getRowCount() > 0) {
    	    for (int i = getRowCount() - 1; i > -1; i--) {
    	        removeRow(i);
    	    }
    	}
    }
    
}