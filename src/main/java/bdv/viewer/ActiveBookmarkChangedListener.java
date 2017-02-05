package bdv.viewer;

import bdv.tools.bookmarks.bookmark.IBookmark;

public interface ActiveBookmarkChangedListener {
	public void activeBookmarkChanged(IBookmark previousBookmark, IBookmark activeBookmark);
}
