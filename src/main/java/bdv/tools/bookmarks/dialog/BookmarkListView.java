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

import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.bookmark.IBookmark;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class BookmarkListView extends JPanel {
    
    private final JScrollPane scrollPane;
    private final JPanel listItemContainer;
    
    private final Bookmarks bookmarks;
    
    private final Map<IBookmark, BookmarkCellPanel> listItems = new HashMap<>();

    public BookmarkListView(Bookmarks bookmarks) {
        this.bookmarks = Objects.requireNonNull(bookmarks, "bookmarks");
        
        this.listItemContainer = new JPanel();
        this.listItemContainer.setLayout(new BoxLayout(listItemContainer, BoxLayout.Y_AXIS));
        
        this.scrollPane = new JScrollPane(this.listItemContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
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
        
        for (IBookmark singleBookmark : this.bookmarks.getAll()) {
            this.listItems.put(singleBookmark, new BookmarkCellPanel(singleBookmark));
        }
        
        for (BookmarkCellPanel itemPanel : listItems.values()) {
            this.listItemContainer.add(itemPanel);
        }
    }
    
}
