package bdv.ui.iconcards.options;

import bdv.ui.viewermodepanel.ViewerModesModel;
import bdv.viewer.Interpolation;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerState;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.net.URL;

/**
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG, Dresden
 * @author Deborah Schmidt, CSBD/MPI-CBG, Dresden
 */
public class BdvIconOptions implements ViewerModesModel.ViewerModeListener
{

	private final ViewerModesModel viewerModesModel;

	private final IconOptionsCard card;

	public final static String OPTIONS_GROUP_DISPLAYMODES = "Display Modes";

	public final static String OPTIONS_GROUP_NAVIGATION = "Navigation";

	private static String OPTIONS_FUSION = "fusion";

	private static String OPTIONS_FUSION_SINGLE = "Single";

	private static String OPTIONS_FUSION_FUSED = "Fused";

	private static String OPTIONS_GROUPING = "grouping";

	private static String OPTIONS_GROUPING_GROUPED = "Group";

	private static String OPTIONS_GROUPING_SOURCE = "Source";

	private static String OPTIONS_INTERPOLATION = "interpolation";

	private static String OPTIONS_INTERPOLATION_NEAREST = "Nearest";

	private static String OPTIONS_INTERPOLATION_LINEAR = "Linear";

	private static String OPTIONS_TRANSLATION = "translation";

	private static String OPTIONS_TRANSLATION_ON = "translation on";

	private static String OPTIONS_TRANSLATION_OFF = "translation off";

	private static String OPTIONS_ROTATION = "rotation";

	private static String OPTIONS_ROTATION_ON = "rotation on";

	private static String OPTIONS_ROTATION_OFF = "rotation off";

	public BdvIconOptions( final ViewerState state, final TriggerBehaviourBindings triggerBindings, final IconOptionsCard card )
	{
		viewerModesModel = new ViewerModesModel( state, triggerBindings );
		viewerModesModel.addViewerModeListener( this );
		this.card = card;
		createFusionChoices();
		createGroupingChoices();
		createInterpolationChoices();
		createTranslationChoices();
		createRotationChoices();
	}

	public static void addToCard( SynchronizedViewerState state, TriggerBehaviourBindings triggerbindings, DefaultIconOptionsCard card )
	{
		new BdvIconOptions( state, triggerbindings, card );
	}

	private void createRotationChoices()
	{
		final URL rotation_on = this.getClass().getResource( "/bdv/ui/viewermodepanel/rotation_on.png" );
		final URL rotation_off = this.getClass().getResource( "/bdv/ui/viewermodepanel/rotation_off.png" );
		card.addOptionChoice( OPTIONS_GROUP_NAVIGATION, OPTIONS_ROTATION, OPTIONS_ROTATION_OFF, viewerModesModel::blockRotation, rotation_off );
		card.addOptionChoice( OPTIONS_GROUP_NAVIGATION, OPTIONS_ROTATION, OPTIONS_ROTATION_ON, viewerModesModel::unblockRotation, rotation_on );
		card.setSelected( OPTIONS_GROUP_NAVIGATION, OPTIONS_ROTATION, OPTIONS_ROTATION_ON );
	}

	private void createTranslationChoices()
	{
		final URL translation_on = this.getClass().getResource( "/bdv/ui/viewermodepanel/translation_on.png" );
		final URL translation_off = this.getClass().getResource( "/bdv/ui/viewermodepanel/translation_off.png" );
		card.addOptionChoice( OPTIONS_GROUP_NAVIGATION, OPTIONS_TRANSLATION, OPTIONS_TRANSLATION_OFF, viewerModesModel::blockTranslation, translation_off );
		card.addOptionChoice( OPTIONS_GROUP_NAVIGATION, OPTIONS_TRANSLATION, OPTIONS_TRANSLATION_ON, viewerModesModel::unblockTranslation, translation_on );
		card.setSelected( OPTIONS_GROUP_NAVIGATION, OPTIONS_TRANSLATION, OPTIONS_TRANSLATION_ON );
	}

	private void createInterpolationChoices()
	{
		final URL nearest_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/nearest.png" );
		final URL linear_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/linear.png" );
		Runnable linearAction = () -> viewerModesModel.setInterpolation( Interpolation.NLINEAR );
		Runnable nearestAction = () -> viewerModesModel.setInterpolation( Interpolation.NEARESTNEIGHBOR );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_INTERPOLATION, OPTIONS_INTERPOLATION_LINEAR, linearAction, linear_icon );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_INTERPOLATION, OPTIONS_INTERPOLATION_NEAREST, nearestAction, nearest_icon );
		interpolationMode( Interpolation.NLINEAR );
	}

	private void createGroupingChoices()
	{
		final URL grouping_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/grouping_mode.png" );
		final URL source_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/source_mode.png" );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_GROUPING, OPTIONS_GROUPING_SOURCE, () -> viewerModesModel.setGrouping( false ), source_icon );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_GROUPING, OPTIONS_GROUPING_GROUPED, () -> viewerModesModel.setGrouping( true ), grouping_icon );
		sourceMode();
	}

	private void createFusionChoices()
	{
		final URL fusion_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/fusion_mode.png" );
		final URL single_icon = this.getClass().getResource( "/bdv/ui/viewermodepanel/single_mode.png" );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_FUSION, OPTIONS_FUSION_SINGLE, () -> viewerModesModel.setFused( false ), single_icon );
		card.addOptionChoice( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_FUSION, OPTIONS_FUSION_FUSED, () -> viewerModesModel.setFused( true ), fusion_icon );
		singleMode();
	}

	@Override
	public void fusedMode()
	{
		card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_FUSION, OPTIONS_FUSION_FUSED );
	}

	@Override
	public void singleMode()
	{
		card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_FUSION, OPTIONS_FUSION_SINGLE );
	}

	@Override
	public void sourceMode()
	{
		card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_GROUPING, OPTIONS_GROUPING_SOURCE );
	}

	@Override
	public void groupMode()
	{
		card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_GROUPING, OPTIONS_GROUPING_GROUPED );
	}

	@Override
	public void interpolationMode( Interpolation interpolation_mode )
	{
		if ( interpolation_mode.equals( Interpolation.NLINEAR ) )
			card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_INTERPOLATION, OPTIONS_INTERPOLATION_LINEAR );
		if ( interpolation_mode.equals( Interpolation.NEARESTNEIGHBOR ) )
			card.setSelected( OPTIONS_GROUP_DISPLAYMODES, OPTIONS_INTERPOLATION, OPTIONS_INTERPOLATION_NEAREST );
	}
}
