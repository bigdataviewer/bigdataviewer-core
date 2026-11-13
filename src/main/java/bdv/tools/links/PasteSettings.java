package bdv.tools.links;

public interface PasteSettings
{
	enum SourceMatchingMethod
	{
		BY_SPEC_LOAD_MISSING,
		BY_SPEC,
		BY_INDEX
	}

	enum RescaleMethod
	{
		/**
		 * Do not rescale transform.
		 */
		NONE,
		/**
		 * Compute scale factors from {@link BdvPropertiesV0#panelsize()
		 * recorded} to current panel width, and recorded to current panel
		 * height. Use the smaller of those scale factors.
		 */
		FIT_PANEL,
		/**
		 * Compute scale factors from {@link BdvPropertiesV0#panelsize()
		 * recorded} to current panel width, and recorded to current panel
		 * height. Use the larger of those scale factors.
		 */
		FILL_PANEL
	}

	enum RecenterMethod
	{
		/**
		 * Do not recenter. That means, the world coordinate mapping to the
		 * recorded panel min corner is mapped to the current panel min corner.
		 */
		NONE,
		/**
		 * Shift such the world coordinate mapping to the {@link
		 * BdvPropertiesV0#panelsize() recorded} panel center is mapped to the
		 * current panel center.
		 */
		PANEL_CENTER,
		/**
		 * Shift such the world coordinate mapping to the {@link
		 * BdvPropertiesV0#mousepos() recorded} mouse position is mapped to the
		 * current panel center.
		 */
		MOUSE_POS
	}

	boolean pasteViewerTransform();

	boolean pasteCurrentTimepoint();

	SourceMatchingMethod sourceMatchingMethod();

	RescaleMethod rescaleMethod();

	RecenterMethod recenterMethod();

	boolean pasteSourceConfigs();

	boolean pasteDisplayMode();

	boolean pasteInterpolation();

	boolean pasteConverterConfigs();

	boolean pasteSourceVisibility();
}
