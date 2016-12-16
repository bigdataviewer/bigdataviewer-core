package bdv.tools.bookmarks.editor;

public interface BookmarkRenameEditorListener {
	
	void bookmarkRenameFinished(String oldKey, String newKey);
	
	void bookmarkRenameAborted(String oldKey);
}
