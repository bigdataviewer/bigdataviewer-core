package viewer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

public class SpimViewerActions
{
	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String MANUAL_TRANSFORM = "toggle manual transformation";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";

	public static ActionMap createActionMap( final ViewRegisteredAngles bdv )
	{
		final ActionMap map = new ActionMap();
		addToActionMap( map, bdv );
		return map;
	}

	public static void addToActionMap( final ActionMap map, final ViewRegisteredAngles bdv )
	{
		put( map, new BrightnessSettingsAction( bdv ) );
		put( map, new VisibilityAndGroupingSettingsAction( bdv ) );
		put( map, new ShowHelpAction( bdv ) );
		put( map, new CropAction( bdv ) );
		put( map, new ManualTransformAction( bdv ) );
		put( map, new SaveSettingsAction( bdv ) );
		put( map, new LoadSettingsAction( bdv ) );
//		put( map, new ( bdv ) );
	}

	private static abstract class ViewerAction extends AbstractAction
	{
		protected final ViewRegisteredAngles bdv;

		public ViewerAction( final String name, final ViewRegisteredAngles bdv )
		{
			super( name );
			this.bdv = bdv;
		}

		public String name()
		{
			return ( String ) getValue( NAME );
		}

		private static final long serialVersionUID = 1L;
	}

	private static void put( final ActionMap map, final ViewerAction a )
	{
		map.put( a.name(), a );
	}

	public static class BrightnessSettingsAction extends ViewerAction
	{
		public BrightnessSettingsAction( final ViewRegisteredAngles bdv )
		{
			super( BRIGHTNESS_SETTINGS, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.toggleBrightnessDialog();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class VisibilityAndGroupingSettingsAction extends ViewerAction
	{
		public VisibilityAndGroupingSettingsAction( final ViewRegisteredAngles bdv )
		{
			super( VISIBILITY_AND_GROUPING, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.toggleActiveSourcesDialog();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class ShowHelpAction extends ViewerAction
	{
		public ShowHelpAction( final ViewRegisteredAngles bdv )
		{
			super( SHOW_HELP, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.showHelp();
		}

		private static final long serialVersionUID = 1L;
	}

	public static class CropAction extends ViewerAction
	{
		public CropAction( final ViewRegisteredAngles bdv )
		{
			super( CROP, bdv );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			bdv.crop();
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
}
