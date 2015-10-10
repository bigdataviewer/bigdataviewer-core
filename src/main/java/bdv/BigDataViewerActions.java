/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
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

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import bdv.tools.ToggleDialogAction;
import bdv.util.AbstractNamedAction;
import bdv.util.AbstractNamedAction.NamedActionAdder;
import bdv.util.KeyProperties;
import bdv.util.KeyProperties.KeyStrokeAdder;
import bdv.viewer.InputActionBindings;

public class BigDataViewerActions
{
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String MANUAL_TRANSFORM = "toggle manual transformation";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";
	public static final String RECORD_MOVIE = "record movie";
	public static final String SET_BOOKMARK = "set bookmark";
	public static final String GO_TO_BOOKMARK = "go to bookmark";
	public static final String GO_TO_BOOKMARK_ROTATION = "go to bookmark rotation";

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
			final KeyProperties keyProperties )
	{
		inputActionBindings.addActionMap( "bdv", createActionMap( bdv ) );
		inputActionBindings.addInputMap( "bdv", createInputMap( keyProperties ) );
	}

	public static InputMap createInputMap( final KeyProperties keyProperties )
	{
		final InputMap inputMap = new InputMap();
		final KeyStrokeAdder map = keyProperties.adder( inputMap );

		map.put( BRIGHTNESS_SETTINGS, "S" );
		map.put( VISIBILITY_AND_GROUPING, "F6" );
		map.put( MANUAL_TRANSFORM, "T" );
		map.put( SHOW_HELP, "F1", "H" );
		map.put( CROP, "F9" );
		map.put( RECORD_MOVIE, "F10" );
		map.put( SAVE_SETTINGS, "F11" );
		map.put( LOAD_SETTINGS, "F12" );
		map.put( GO_TO_BOOKMARK, "B" );
		map.put( GO_TO_BOOKMARK_ROTATION, "O" );
		map.put( SET_BOOKMARK, "shift B" );

		return inputMap;
	}

	public static ActionMap createActionMap( final BigDataViewer bdv )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS, bdv.brightnessDialog ) );
		map.put( new ToggleDialogAction( VISIBILITY_AND_GROUPING, bdv.activeSourcesDialog ) );
		map.put( new ToggleDialogAction( CROP, bdv.cropDialog ) );
		map.put( new ToggleDialogAction( RECORD_MOVIE, bdv.movieDialog ) );
		map.put( new ToggleDialogAction( SHOW_HELP, bdv.helpDialog ) );
		map.put( new ManualTransformAction( bdv ) );
		map.put( new SetBookmarkAction( bdv ) );
		map.put( new GoToBookmarkAction( bdv ) );
		map.put( new GoToBookmarkRotationAction( bdv ) );
		map.put( new SaveSettingsAction( bdv ) );
		map.put( new LoadSettingsAction( bdv ) );

		return actionMap;
	}

	private static abstract class ViewerAction extends AbstractNamedAction
	{
		protected final BigDataViewer bdv;

		public ViewerAction( final String name, final BigDataViewer bdv )
		{
			super( name );
			this.bdv = bdv;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ManualTransformAction extends ViewerAction
	{
		public ManualTransformAction( final BigDataViewer bdv )
		{
			super( MANUAL_TRANSFORM, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.toggleManualTransformation();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SetBookmarkAction extends ViewerAction
	{
		public SetBookmarkAction( final BigDataViewer bdv )
		{
			super( SET_BOOKMARK, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.initSetBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkAction extends ViewerAction
	{
		public GoToBookmarkAction( final BigDataViewer bdv )
		{
			super( GO_TO_BOOKMARK, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.initGoToBookmark();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class GoToBookmarkRotationAction extends ViewerAction
	{
		public GoToBookmarkRotationAction( final BigDataViewer bdv )
		{
			super( GO_TO_BOOKMARK_ROTATION, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.initGoToBookmarkRotation();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class SaveSettingsAction extends ViewerAction
	{
		public SaveSettingsAction( final BigDataViewer bdv )
		{
			super( SAVE_SETTINGS, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.saveSettings();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class LoadSettingsAction extends ViewerAction
	{
		public LoadSettingsAction( final BigDataViewer bdv )
		{
			super( LOAD_SETTINGS, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.loadSettings();
		}

		private static final long serialVersionUID = 1L;
	}

	private BigDataViewerActions()
	{}
}
