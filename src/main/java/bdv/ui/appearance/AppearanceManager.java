package bdv.ui.appearance;

import bdv.util.Prefs;
import java.awt.Component;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Manages the {@link Appearance} (save/load config file, propagate changes to
 * {@link Prefs} and LookAndFeel).
 */
public class AppearanceManager
{
	private static final String STYLE_FILE = System.getProperty( "user.home" ) + "/.bdv/appearance.yaml";

	private final List< WeakReference< Component > > components = new CopyOnWriteArrayList<>();

	/**
	 * The managed Appearance. This will be updated with changes from the
	 * Preferences (on "Apply" or "Ok").
	 */
	private final Appearance appearance;

	public AppearanceManager()
	{
		appearance = new Appearance();
		fromPrefs();
		load();
		toPrefs();
	}

	public Appearance appearance()
	{
		return appearance;
	}

	/**
	 * Add components here that should be notified via {@cpde
	 * SwingUtilities.updateComponentTreeUI()} when the Look-And-Feel is
	 * changed.
	 */
	public void addLafComponent( Component component )
	{
		components.add( new WeakReference<>( component ) );
	}

	public void updateLookAndFeel()
	{
		final UIManager.LookAndFeelInfo laf = appearance.lookAndFeel();
		if ( laf == Appearance.DONT_MODIFY_LOOK_AND_FEEL )
			return;
		if ( UIManager.getLookAndFeel().getName().equals( laf.getName() ) )
			return;
		try
		{
			UIManager.setLookAndFeel( laf.getClassName() );

			final List< WeakReference< Component > > remove = new ArrayList<>();
			for ( WeakReference< Component > ref : components )
			{
				final Component component = ref.get();
				if ( component == null )
					remove.add( ref );
				else
					SwingUtilities.updateComponentTreeUI( component );
			}
			components.removeAll( remove );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
	}

	void load()
	{
		load( STYLE_FILE );
	}

	void load( final String filename )
	{
		try
		{
			final Appearance a = AppearanceIO.load( filename );
			appearance.set( a );
		}
		catch ( final FileNotFoundException e )
		{
			System.out.println( "Appearance settings file " + filename + " not found. Using defaults." );
		}
		catch ( final Exception e )
		{
			System.out.println( "Error while reading appearance settings file " + filename + ". Using defaults." );
		}
	}

	void save()
	{
		save( STYLE_FILE );
	}

	void save( final String filename )
	{
		try
		{
			AppearanceIO.save( appearance, filename );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			System.out.println( "Error while reading appearance settings file " + filename + ". Using defaults." );
		}
	}

	/**
	 * @deprecated Prefs will be replaced eventually by directly using {@code Appearance} in BDV
	 */
	@Deprecated
	public void toPrefs()
	{
		Prefs.showScaleBar( appearance.showScaleBar() );
		Prefs.showScaleBarInMovie( appearance.showScaleBarInMovie() );
		Prefs.showMultibox( appearance.showMultibox() );
		Prefs.showTextOverlay( appearance.showTextOverlay() );
		Prefs.sourceNameOverlayPosition( appearance.sourceNameOverlayPosition() );
		Prefs.scaleBarColor( appearance.scaleBarColor() );
		Prefs.scaleBarBgColor( appearance.scaleBarBgColor() );
	}

	/**
	 * @deprecated Prefs will be replaced eventually by directly using {@code Appearance} in BDV
	 */
	@Deprecated
	public void fromPrefs()
	{
		appearance.setShowScaleBar( Prefs.showScaleBar() );
		appearance.setShowScaleBarInMovie( Prefs.showScaleBarInMovie() );
		appearance.setShowMultibox( Prefs.showMultibox() );
		appearance.setShowTextOverlay( Prefs.showTextOverlay() );
		appearance.setSourceNameOverlayPosition( Prefs.sourceNameOverlayPosition() );
		appearance.setScaleBarColor( Prefs.scaleBarColor() );
		appearance.setScaleBarBgColor( Prefs.scaleBarBgColor() );
	}
}
