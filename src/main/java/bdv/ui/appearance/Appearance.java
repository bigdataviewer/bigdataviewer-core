package bdv.ui.appearance;

import bdv.util.Prefs;
import bdv.util.Prefs.OverlayPosition;
import java.util.Objects;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.scijava.listeners.Listeners;

import static bdv.util.Prefs.OverlayPosition.TOP_CENTER;

/**
 * Appearance settings.
 * This comprises everything in {@link Prefs} and the LookAndFeel.
 * <p>
 * Listeners can be registered and will be notified of changes.
 */
public class Appearance
{
	public static LookAndFeelInfo DONT_MODIFY_LOOK_AND_FEEL = new LookAndFeelInfo( "don't modify", null );

	private boolean showScaleBar = false;
	private boolean showScaleBarInMovie = false;
	private boolean showMultibox = true;
	private boolean showTextOverlay = true;
	private OverlayPosition sourceNameOverlayPosition = TOP_CENTER;
	private int scaleBarColor = 0xffffffff;
	private int scaleBarBgColor = 0x88000000;
	private LookAndFeelInfo lookAndFeel = DONT_MODIFY_LOOK_AND_FEEL;

	public interface UpdateListener
	{
		void appearanceChanged();
	}

	private final Listeners.List< UpdateListener > updateListeners;

	public Appearance()
	{
		updateListeners = new Listeners.SynchronizedList<>();
	}

	public void set( final Appearance other )
	{
		this.showScaleBar = other.showScaleBar;
		this.showScaleBarInMovie = other.showScaleBarInMovie;
		this.showMultibox = other.showMultibox;
		this.showTextOverlay = other.showTextOverlay;
		this.sourceNameOverlayPosition = other.sourceNameOverlayPosition;
		this.scaleBarColor = other.scaleBarColor;
		this.scaleBarBgColor = other.scaleBarBgColor;
		this.lookAndFeel = other.lookAndFeel;
		notifyListeners();
	}

	private void notifyListeners()
	{
		updateListeners.list.forEach( UpdateListener::appearanceChanged );
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	public boolean showScaleBar()
	{
		return showScaleBar;
	}

	public void setShowScaleBar( final boolean showScaleBar )
	{
		if ( this.showScaleBar != showScaleBar )
		{
			this.showScaleBar = showScaleBar;
			notifyListeners();
		}
	}

	public boolean showScaleBarInMovie()
	{
		return showScaleBarInMovie;
	}

	public void setShowScaleBarInMovie( final boolean showScaleBarInMovie )
	{
		if ( this.showScaleBarInMovie != showScaleBarInMovie )
		{
			this.showScaleBarInMovie = showScaleBarInMovie;
			notifyListeners();
		}
	}

	public boolean showMultibox()
	{
		return showMultibox;
	}

	public void setShowMultibox( final boolean showMultibox )
	{
		if ( this.showMultibox != showMultibox )
		{
			this.showMultibox = showMultibox;
			notifyListeners();
		}
	}

	public boolean showTextOverlay()
	{
		return showTextOverlay;
	}

	public void setShowTextOverlay( final boolean showTextOverlay )
	{
		if ( this.showTextOverlay != showTextOverlay )
		{
			this.showTextOverlay = showTextOverlay;
			notifyListeners();
		}
	}

	public OverlayPosition sourceNameOverlayPosition()
	{
		return sourceNameOverlayPosition;
	}

	public void setSourceNameOverlayPosition( final OverlayPosition sourceNameOverlayPosition )
	{
		if ( this.sourceNameOverlayPosition != sourceNameOverlayPosition )
		{
			this.sourceNameOverlayPosition = sourceNameOverlayPosition;
			notifyListeners();
		}
	}

	public int scaleBarColor()
	{
		return scaleBarColor;
	}

	public void setScaleBarColor( final int scaleBarColor )
	{
		if ( this.scaleBarColor != scaleBarColor )
		{
			this.scaleBarColor = scaleBarColor;
			notifyListeners();
		}
	}

	public int scaleBarBgColor()
	{
		return scaleBarBgColor;
	}

	public void setScaleBarBgColor( final int scaleBarBgColor )
	{
		if ( this.scaleBarBgColor != scaleBarBgColor )
		{
			this.scaleBarBgColor = scaleBarBgColor;
			notifyListeners();
		}
	}

	public LookAndFeelInfo lookAndFeel()
	{
		return lookAndFeel;
	}

	public void setLookAndFeel( final LookAndFeelInfo lookAndFeel )
	{
		if ( !Objects.equals( this.lookAndFeel, lookAndFeel ) )
		{
			this.lookAndFeel = lookAndFeel;
			notifyListeners();
		}
	}

	public static LookAndFeelInfo currentLookAndFeelInfo()
	{
		return lookAndFeelInfoForName( UIManager.getLookAndFeel().getName() );
	}

	public static LookAndFeelInfo lookAndFeelInfoForName( final String name )
	{
		if ( name != null )
			for ( UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels() )
			{
				if ( info.getName().equals( name ) )
					return info;
			}
		return DONT_MODIFY_LOOK_AND_FEEL;
	}

	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer( "Appearance{" );
		sb.append( "showScaleBar=" ).append( showScaleBar );
		sb.append( ", showScaleBarInMovie=" ).append( showScaleBarInMovie );
		sb.append( ", showMultibox=" ).append( showMultibox );
		sb.append( ", showTextOverlay=" ).append( showTextOverlay );
		sb.append( ", sourceNameOverlayPosition=" ).append( sourceNameOverlayPosition );
		sb.append( ", scaleBarColor=" ).append( scaleBarColor );
		sb.append( ", scaleBarBgColor=" ).append( scaleBarBgColor );
		sb.append( ", lookAndFeel=" ).append( lookAndFeel );
		sb.append( '}' );
		return sb.toString();
	}
}
