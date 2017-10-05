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
package bdv.viewer;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bdv.tools.bookmarks.bookmark.KeyFrame;
import bdv.tools.bookmarks.editor.BookmarksEditor;

/**
 * Context-Menu for the {@link KeyFramePanel}-Component.
 *
 */
public final class KeyFramePopupMenu extends JPopupMenu
{

	private static final long serialVersionUID = 1L;

	private BookmarksEditor bookmarksEditor;

	private final JMenuItem itemCopyKF;

	private final JMenuItem itemSetTransformKF;

	private final JMenuItem itemRemoveKF;

	/**
	 * User-selected {@link KeyFrame} or {@code null} if no frame is selected.
	 */
	private volatile KeyFrame selectedKeyFrame = null;

	KeyFramePopupMenu()
	{
		itemCopyKF = new JMenuItem( "Clone" );
		itemCopyKF.addActionListener( e -> {
			if ( bookmarksEditor != null && selectedKeyFrame != null )
				bookmarksEditor.copyKeyFrame( selectedKeyFrame );
		} );

		itemSetTransformKF = new JMenuItem( "Set transformation" );
		itemSetTransformKF.addActionListener( e -> {
			if ( bookmarksEditor != null && selectedKeyFrame != null )
				bookmarksEditor.setTransformationToKeyframe( selectedKeyFrame );
		} );

		itemRemoveKF = new JMenuItem( "Remove" );
		itemRemoveKF.addActionListener( e -> {
			if ( bookmarksEditor != null && selectedKeyFrame != null )
				bookmarksEditor.removeKeyframe( selectedKeyFrame );
		} );

		add( itemCopyKF );
		add( itemSetTransformKF );
		add( itemRemoveKF );
	}

	void setKeyFrameFlagSelected( final KeyFrame keyFrame )
	{
		this.selectedKeyFrame = keyFrame;
	}

	public void setBookmarksEditor( final BookmarksEditor bookmarksEditor )
	{
		this.bookmarksEditor = bookmarksEditor;
	}
}
