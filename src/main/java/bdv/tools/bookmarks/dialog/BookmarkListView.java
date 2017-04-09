/*
 * Copyright (c) 2017, Fiji
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package bdv.tools.bookmarks.dialog;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.editor.BookmarksEditor;

public final class BookmarkListView extends JPanel {

	private final JScrollPane scrollPane;
	private final JPanel listItemContainer;
	
	private final BookmarksEditor bookmarksEditor;
	private final Map<Bookmark, BookmarkCellPanel> listItems = new HashMap<>();

	private Bookmark activeBookmark;
	
	public BookmarkListView(BookmarksEditor bookmarksEditor) {

		this.bookmarksEditor = bookmarksEditor;
		this.listItemContainer = new JPanel();
		this.listItemContainer.setLayout(new BoxLayout(listItemContainer, BoxLayout.Y_AXIS));

		this.scrollPane = new JScrollPane(this.listItemContainer, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.scrollPane.setBorder(BorderFactory.createEmptyBorder());

		initComponents();
		updateListItems();
	}

	private void initComponents() {
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
	}

	public void updateListItems() {
		listItems.clear();
		this.listItemContainer.removeAll();

		List<Bookmark> list = new ArrayList<Bookmark>(this.bookmarksEditor.getAllBookmarks());

		for (Bookmark bookmark : list) {
			BookmarkCellPanel panel = new BookmarkCellPanel(bookmark, bookmarksEditor);
			panel.setActive(bookmark.equals(this.activeBookmark));

			this.listItems.put(bookmark, panel);
			this.listItemContainer.add(panel);
		}

		revalidateListItemContainer();
	}

	private void revalidateListItemContainer() {
		this.listItemContainer.revalidate();
		this.listItemContainer.repaint();
		validate();
	}

	public void setActiveBookmark(Bookmark bookmark) {
		for (BookmarkCellPanel panel : listItems.values()) {
			panel.setActive(false);
		}

		BookmarkCellPanel panel = listItems.get(bookmark);
		if (panel != null) {
			panel.setActive(true);
			this.activeBookmark = bookmark;
		}
	}
}
