package viewer;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import viewer.KeyProperties.KeyStrokeAdder;
import viewer.gui.InputActionBindings;
import viewer.tools.ToggleDialogAction;
import viewer.util.AbstractNamedAction;
import viewer.util.AbstractNamedAction.NamedActionAdder;

public class SpimViewerActions
{
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String MANUAL_TRANSFORM = "toggle manual transformation";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";
	public static final String RECORD_MOVIE = "record movie";

	/**
	 * Create BigDataViewer actions and install them in the specified
	 * {@link InputActionBindings}.
	 *
	 * @param inputActionBindings
	 *            {@link InputMap} and {@link ActionMap} are installed here.
	 * @param bdv
	 *            Actions are targeted at this {@link ViewRegisteredAngles}.
	 * @param keyProperties
	 *            user-defined key-bindings.
	 */
	public static void installActionBindings(
			final InputActionBindings inputActionBindings,
			final ViewRegisteredAngles bdv,
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

		return inputMap;
	}

	public static ActionMap createActionMap( final ViewRegisteredAngles bdv )
	{
		final ActionMap actionMap = new ActionMap();
		final NamedActionAdder map = new NamedActionAdder( actionMap );

		map.put( new ToggleDialogAction( BRIGHTNESS_SETTINGS, bdv.brightnessDialog ) );
		map.put( new ToggleDialogAction( VISIBILITY_AND_GROUPING, bdv.activeSourcesDialog ) );
		map.put( new ToggleDialogAction( CROP, bdv.cropDialog ) );
		map.put( new ToggleDialogAction( RECORD_MOVIE, bdv.movieDialog ) );
		map.put( new ToggleDialogAction( SHOW_HELP, bdv.helpDialog ) );
		map.put( new ManualTransformAction( bdv ) );
		map.put( new SaveSettingsAction( bdv ) );
		map.put( new LoadSettingsAction( bdv ) );

		return actionMap;
	}

	private static abstract class ViewerAction extends AbstractNamedAction
	{
		protected final ViewRegisteredAngles bdv;

		public ViewerAction( final String name, final ViewRegisteredAngles bdv )
		{
			super( name );
			this.bdv = bdv;
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ManualTransformAction extends ViewerAction
	{
		public ManualTransformAction( final ViewRegisteredAngles bdv )
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

	public static class SaveSettingsAction extends ViewerAction
	{
		public SaveSettingsAction( final ViewRegisteredAngles bdv )
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
		public LoadSettingsAction( final ViewRegisteredAngles bdv )
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

	private SpimViewerActions()
	{}
}
