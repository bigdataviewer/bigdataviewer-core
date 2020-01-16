/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv;

import java.awt.Dialog;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.tools.HelpDialog;
import bdv.tools.RecordMaxProjectionDialog;
import bdv.tools.RecordMovieDialog;
import bdv.tools.ToggleDialogAction;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.crop.CropDialog;
import bdv.tools.transformation.ManualTransformationEditor;

public class BigDataViewerActions extends Actions
{
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String MANUAL_TRANSFORM = "toggle manual transformation";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";
	public static final String EXPAND_CARDS = "expand and focus cards panel";
	public static final String COLLAPSE_CARDS = "collapse cards panel";
	public static final String RECORD_MOVIE = "record movie";
	public static final String RECORD_MAX_PROJECTION_MOVIE = "record max projection movie";
	public static final String SET_BOOKMARK = "set bookmark";
	public static final String GO_TO_BOOKMARK = "go to bookmark";
	public static final String GO_TO_BOOKMARK_ROTATION = "go to bookmark rotation";

	static final String[] BRIGHTNESS_SETTINGS_KEYS         = new String[] { "S" };
	static final String[] VISIBILITY_AND_GROUPING_KEYS     = new String[] { "F6" };
	static final String[] MANUAL_TRANSFORM_KEYS            = new String[] { "T" };
	static final String[] SHOW_HELP_KEYS                   = new String[] { "F1", "H" };
	static final String[] RECORD_MAX_PROJECTION_MOVIE_KEYS = new String[] { "F8" };
	static final String[] CROP_KEYS                        = new String[] { "F9" };
	static final String[] RECORD_MOVIE_KEYS                = new String[] { "F10" };
	static final String[] SAVE_SETTINGS_KEYS               = new String[] { "F11" };
	static final String[] LOAD_SETTINGS_KEYS               = new String[] { "F12" };
	static final String[] EXPAND_CARDS_KEYS                = new String[] { "P" };
	static final String[] COLLAPSE_CARDS_KEYS              = new String[] { "shift P", "shift ESCAPE" };
	static final String[] GO_TO_BOOKMARK_KEYS              = new String[] { "B" };
	static final String[] GO_TO_BOOKMARK_ROTATION_KEYS     = new String[] { "O" };
	static final String[] SET_BOOKMARK_KEYS                = new String[] { "shift B" };

	/**
	 * Create BigDataViewer actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param bdv
	 *            Actions are targeted at this {@link BigDataViewer}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final BigDataViewer bdv,
			final KeyStrokeAdder.Factory keyProperties )
	{
		final BigDataViewerActions actions = new BigDataViewerActions( keyProperties );

		actions.dialog( bdv.brightnessDialog );
		actions.dialog( bdv.activeSourcesDialog );
		actions.dialog( bdv.helpDialog );
		actions.dialog( bdv.cropDialog );
		actions.dialog( bdv.movieDialog );
		actions.dialog( bdv.movieMaxProjectDialog );
		actions.bookmarks( bdv.bookmarkEditor );
		actions.manualTransform( bdv.manualTransformationEditor );
		actions.runnableAction( bdv::loadSettings, LOAD_SETTINGS, LOAD_SETTINGS_KEYS );
		actions.runnableAction( bdv::saveSettings, SAVE_SETTINGS, SAVE_SETTINGS_KEYS );
		actions.runnableAction( bdv::expandAndFocusCardPanel, EXPAND_CARDS, EXPAND_CARDS_KEYS );
		actions.runnableAction( bdv::collapseCardPanel, COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );

		actions.install( inputActionBindings, "bdv" );
	}

	public BigDataViewerActions( final KeyStrokeAdder.Factory keyConfig )
	{
		super( keyConfig, new String[] { "bdv" } );
	}

	public void toggleDialogAction( final Dialog dialog, final String name, final String... defaultKeyStrokes )
	{
		keyStrokeAdder.put( name, defaultKeyStrokes );
		new ToggleDialogAction( name, dialog ).put( getActionMap() );
	}

	public void dialog( final BrightnessDialog brightnessDialog )
	{
		toggleDialogAction( brightnessDialog, BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS );
	}

	public void dialog( final VisibilityAndGroupingDialog visibilityAndGroupingDialog )
	{
		toggleDialogAction( visibilityAndGroupingDialog, VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS );
	}

	public void dialog( final CropDialog cropDialog )
	{
		toggleDialogAction( cropDialog, CROP, CROP_KEYS );
	}

	public void dialog( final HelpDialog helpDialog )
	{
		toggleDialogAction( helpDialog, SHOW_HELP, SHOW_HELP_KEYS );
	}

	public void dialog( final RecordMovieDialog recordMovieDialog )
	{
		toggleDialogAction( recordMovieDialog, RECORD_MOVIE, RECORD_MOVIE_KEYS );
	}

	public void dialog( final RecordMaxProjectionDialog recordMaxProjectionDialog )
	{
		toggleDialogAction( recordMaxProjectionDialog, RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS );
	}

	public void bookmarks( final BookmarksEditor bookmarksEditor )
	{
		runnableAction( bookmarksEditor::initGoToBookmark, GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS );
		runnableAction( bookmarksEditor::initGoToBookmarkRotation, GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS );
		runnableAction( bookmarksEditor::initSetBookmark, SET_BOOKMARK, SET_BOOKMARK_KEYS );
	}

	public void manualTransform( final ManualTransformationEditor manualTransformationEditor )
	{
		runnableAction( manualTransformationEditor::toggle, MANUAL_TRANSFORM, MANUAL_TRANSFORM_KEYS );
	}
}
