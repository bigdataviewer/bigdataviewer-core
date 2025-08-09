package bdv.ui.links;

import java.awt.Insets;

import bdv.ui.CardPanel;
import bdv.ui.links.LinkSettingsPage.LinkSettingsPanel;

public class LinkCard
{
	public static final String BDV_LINK_SETTINGS_CARD = "bdv link settings card";

	public static void install( final LinkSettings linkSettings, final CardPanel cards )
	{
		final LinkSettings.UpdateListener update = () -> {
			final boolean show = linkSettings.showLinkSettingsCard();
			final boolean shown = cards.indexOf( BDV_LINK_SETTINGS_CARD ) > 0;
			if ( show && !shown )
				cards.addCard( BDV_LINK_SETTINGS_CARD, "Copy&Paste", new LinkSettingsPanel( linkSettings ), true, new Insets( 3, 20, 0, 0 ) );
			else if ( shown && !show )
				cards.removeCard( BDV_LINK_SETTINGS_CARD );
		};
		linkSettings.updateListeners().add( update );
		update.appearanceChanged();
	}
}
