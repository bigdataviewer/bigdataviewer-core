package bdv.viewer;

import bdv.tools.bookmarks.bookmark.Bookmark;

public interface ActiveBookmarkChangedListener {
	public void activeBookmarkChanged(Bookmark previousBookmark, Bookmark activeBookmark);
}
