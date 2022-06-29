/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
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
	public static final String PREFERENCES_DIALOG = "Preferences";

	public static final String[] BRIGHTNESS_SETTINGS_KEYS         = new String[] { "S" };
	public static final String[] VISIBILITY_AND_GROUPING_KEYS     = new String[] { "F6" };
	public static final String[] SHOW_HELP_KEYS                   = new String[] { "F1", "H" };
	public static final String[] CROP_KEYS                        = new String[] { "F9" };
	public static final String[] MANUAL_TRANSFORM_KEYS            = new String[] { "T" };
	public static final String[] SAVE_SETTINGS_KEYS               = new String[] { "F11" };
	public static final String[] LOAD_SETTINGS_KEYS               = new String[] { "F12" };
	public static final String[] EXPAND_CARDS_KEYS                = new String[] { "P" };
	public static final String[] COLLAPSE_CARDS_KEYS              = new String[] { "shift P", "shift ESCAPE" };
	public static final String[] RECORD_MOVIE_KEYS                = new String[] { "F10" };
	public static final String[] RECORD_MAX_PROJECTION_MOVIE_KEYS = new String[] { "F8" };
	public static final String[] SET_BOOKMARK_KEYS                = new String[] { "shift B" };
	public static final String[] GO_TO_BOOKMARK_KEYS              = new String[] { "B" };
	public static final String[] GO_TO_BOOKMARK_ROTATION_KEYS     = new String[] { "O" };
	public static final String[] PREFERENCES_DIALOG_KEYS          = new String[] { "meta COMMA", "ctrl COMMA" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.BIGDATAVIEWER, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( BRIGHTNESS_SETTINGS,BRIGHTNESS_SETTINGS_KEYS, "Show the Brightness&Colors dialog." );
			descriptions.add( VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS, "Show the Visibility&Grouping dialog." );
			descriptions.add( SHOW_HELP, SHOW_HELP_KEYS, "Show the Help dialog." );
			descriptions.add( CROP, CROP_KEYS, "Show the Crop dialog." );
			descriptions.add( MANUAL_TRANSFORM, MANUAL_TRANSFORM_KEYS, "Toggle manual transformation mode." );
			descriptions.add( SAVE_SETTINGS, SAVE_SETTINGS_KEYS, "Save the BigDataViewer settings to a settings.xml file." );
			descriptions.add( LOAD_SETTINGS, LOAD_SETTINGS_KEYS, "Load the BigDataViewer settings from a settings.xml file." );
			descriptions.add( EXPAND_CARDS, EXPAND_CARDS_KEYS, "Expand and focus the BigDataViewer card panel" );
			descriptions.add( COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS, "Collapse the BigDataViewer card panel" );
			descriptions.add( RECORD_MOVIE, RECORD_MOVIE_KEYS, "Show the Record Movie dialog." );
			descriptions.add( RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS, "Show the Record Max Projection Movie dialog." );
			descriptions.add( SET_BOOKMARK, SET_BOOKMARK_KEYS, "Set a labeled bookmark at the current location." );
			descriptions.add( GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS, "Retrieve a labeled bookmark location." );
			descriptions.add( GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS, "Retrieve a labeled bookmark, set only the orientation." );
			descriptions.add( PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS, "Show the Preferences dialog." );
		}
	}

	/**
	 * Create BigDataViewer actions and install them in the specified
	 * {@link Actions}.
	 *
	 * @param actions
	 *            navigation actions are installed here.
	 * @param bdv
	 *            Actions are targeted at this {@link BigDataViewer}.
	 */
	public static void install( final Actions actions, final BigDataViewer bdv )
	{
		toggleDialogAction( actions, bdv.brightnessDialog, BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS );
		toggleDialogAction( actions, bdv.activeSourcesDialog, VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS );
		toggleDialogAction( actions, bdv.helpDialog, SHOW_HELP, SHOW_HELP_KEYS );
		toggleDialogAction( actions, bdv.cropDialog, CROP, CROP_KEYS );
		toggleDialogAction( actions, bdv.movieDialog, RECORD_MOVIE, RECORD_MOVIE_KEYS );
		toggleDialogAction( actions, bdv.movieMaxProjectDialog, RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS );
		toggleDialogAction( actions, bdv.preferencesDialog, PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS );
		bookmarks( actions, bdv.bookmarkEditor );
		manualTransform( actions, bdv.manualTransformationEditor );
		actions.runnableAction( bdv::loadSettings, LOAD_SETTINGS, LOAD_SETTINGS_KEYS );
		actions.runnableAction( bdv::saveSettings, SAVE_SETTINGS, SAVE_SETTINGS_KEYS );
		actions.runnableAction( bdv::expandAndFocusCardPanel, EXPAND_CARDS, EXPAND_CARDS_KEYS );
		actions.runnableAction( bdv::collapseCardPanel, COLLAPSE_CARDS, COLLAPSE_CARDS_KEYS );
	}

	public static void toggleDialogAction( final Actions actions, final Dialog dialog, final String name, final String... defaultKeyStrokes )
	{
		actions.namedAction( new ToggleDialogAction( name, dialog ), defaultKeyStrokes );
	}

	public static void dialog( final Actions actions, final CropDialog cropDialog )
	{
		toggleDialogAction( actions, cropDialog, CROP, CROP_KEYS );
	}

	public static void dialog( final Actions actions, final HelpDialog helpDialog )
	{
		toggleDialogAction( actions, helpDialog, SHOW_HELP, SHOW_HELP_KEYS );
	}

	public static void dialog( final Actions actions, final RecordMovieDialog recordMovieDialog )
	{
		toggleDialogAction( actions, recordMovieDialog, RECORD_MOVIE, RECORD_MOVIE_KEYS );
	}

	public static void dialog( final Actions actions, final RecordMaxProjectionDialog recordMaxProjectionDialog )
	{
		toggleDialogAction( actions, recordMaxProjectionDialog, RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS );
	}

	public static void bookmarks( final Actions actions, final BookmarksEditor bookmarksEditor )
	{
		actions.runnableAction( bookmarksEditor::initGoToBookmark, GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS );
		actions.runnableAction( bookmarksEditor::initGoToBookmarkRotation, GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS );
		actions.runnableAction( bookmarksEditor::initSetBookmark, SET_BOOKMARK, SET_BOOKMARK_KEYS );
	}

	public static void manualTransform( final Actions actions, final ManualTransformationEditor manualTransformationEditor )
	{
		actions.runnableAction( manualTransformationEditor::toggle, MANUAL_TRANSFORM, MANUAL_TRANSFORM_KEYS );
	}

	// -- deprecated API --

	@Deprecated
	public static void dialog( final Actions actions, final BrightnessDialog brightnessDialog )
	{
		toggleDialogAction( actions, brightnessDialog, BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS );
	}

	@Deprecated
	public static void dialog( final Actions actions, final VisibilityAndGroupingDialog visibilityAndGroupingDialog )
	{
		toggleDialogAction( actions, visibilityAndGroupingDialog, VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS );
	}

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
	@Deprecated
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

	@Deprecated
	public BigDataViewerActions( final KeyStrokeAdder.Factory keyConfig )
	{
		super( keyConfig, new String[] { "bdv" } );
	}

	@Deprecated
	public void dialog( final BrightnessDialog brightnessDialog )
	{
		toggleDialogAction( brightnessDialog, BRIGHTNESS_SETTINGS, BRIGHTNESS_SETTINGS_KEYS );
	}

	@Deprecated
	public void dialog( final VisibilityAndGroupingDialog visibilityAndGroupingDialog )
	{
		toggleDialogAction( visibilityAndGroupingDialog, VISIBILITY_AND_GROUPING, VISIBILITY_AND_GROUPING_KEYS );
	}

	@Deprecated
	public void toggleDialogAction( final Dialog dialog, final String name, final String... defaultKeyStrokes )
	{
		keyStrokeAdder.put( name, defaultKeyStrokes );
		new ToggleDialogAction( name, dialog ).put( getActionMap() );
	}

	@Deprecated
	public void dialog( final CropDialog cropDialog )
	{
		toggleDialogAction( cropDialog, CROP, CROP_KEYS );
	}

	@Deprecated
	public void dialog( final HelpDialog helpDialog )
	{
		toggleDialogAction( helpDialog, SHOW_HELP, SHOW_HELP_KEYS );
	}

	@Deprecated
	public void dialog( final RecordMovieDialog recordMovieDialog )
	{
		toggleDialogAction( recordMovieDialog, RECORD_MOVIE, RECORD_MOVIE_KEYS );
	}

	@Deprecated
	public void dialog( final RecordMaxProjectionDialog recordMaxProjectionDialog )
	{
		toggleDialogAction( recordMaxProjectionDialog, RECORD_MAX_PROJECTION_MOVIE, RECORD_MAX_PROJECTION_MOVIE_KEYS );
	}

	@Deprecated
	public void bookmarks( final BookmarksEditor bookmarksEditor )
	{
		runnableAction( bookmarksEditor::initGoToBookmark, GO_TO_BOOKMARK, GO_TO_BOOKMARK_KEYS );
		runnableAction( bookmarksEditor::initGoToBookmarkRotation, GO_TO_BOOKMARK_ROTATION, GO_TO_BOOKMARK_ROTATION_KEYS );
		runnableAction( bookmarksEditor::initSetBookmark, SET_BOOKMARK, SET_BOOKMARK_KEYS );
	}

	@Deprecated
	public void manualTransform( final ManualTransformationEditor manualTransformationEditor )
	{
		runnableAction( manualTransformationEditor::toggle, MANUAL_TRANSFORM, MANUAL_TRANSFORM_KEYS );
	}
}
