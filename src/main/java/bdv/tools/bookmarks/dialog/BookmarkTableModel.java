package bdv.tools.bookmarks.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.bookmark.Bookmark;

public class BookmarkTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1L;

    private final Bookmarks bookmarks;
    
    private Comparator<Bookmark> bookmarkComparator = new Comparator< Bookmark >( ){

		@Override
		public int compare(Bookmark arg0, Bookmark arg1) {
			return arg0.getKey().compareTo(arg1.getKey());
		}
		
	};
    
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
    	
    	List<Bookmark> bookmarkCollection = new ArrayList<>(this.bookmarks.getAll());
    	Collections.sort( bookmarkCollection, bookmarkComparator);
    	
    	for(Bookmark b : bookmarkCollection){
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