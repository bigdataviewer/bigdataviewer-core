/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2024 BigDataViewer developers.
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
package bdv.util;

import bdv.ui.UIUtils;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.KeymapManager;
import bdv.viewer.ViewerStateChange;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.BigDataViewer;
import bdv.cache.CacheControl.CacheControls;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;

public class BdvHandleFrame extends BdvHandle
{
	private BigDataViewer bdv;

	private final String frameTitle;

	BdvHandleFrame( final BdvOptions options )
	{
		super( options );
		frameTitle = options.values.getFrameTitle();
		bdv = null;
		cacheControls = new CacheControls();
		UIUtils.installFlatLafInfos();
	}

	public BigDataViewer getBigDataViewer()
	{
		return bdv;
	}

	@Override
	public void close()
	{
		if ( bdv != null )
		{
			final ViewerFrame frame = bdv.getViewerFrame();
			frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
			bdv = null;
		}
		super.close();
	}

	@Override
	public ManualTransformationEditor getManualTransformEditor()
	{
		return bdv.getManualTransformEditor();
	}

	@Override
	public KeymapManager getKeymapManager()
	{
		return bdv.getKeymapManager();
	}

	@Override
	public AppearanceManager getAppearanceManager()
	{
		return bdv.getAppearanceManager();
	}

	@Override
	public InputActionBindings getKeybindings()
	{
		return bdv.getViewerFrame().getKeybindings();
	}

	@Override
	public TriggerBehaviourBindings getTriggerbindings()
	{
		return bdv.getViewerFrame().getTriggerbindings();
	}

	@Override
	boolean createViewer(
			final List< ? extends ConverterSetup > converterSetups,
			final List< ? extends SourceAndConverter< ? > > sources,
			final int numTimepoints )
	{
		final ProgressWriter progressWriter = new ProgressWriterConsole();
		final ViewerOptions viewerOptions = bdvOptions.values.getViewerOptions();
		final InputTriggerConfig inputTriggerConfig = BigDataViewer.getInputTriggerConfig( viewerOptions );
		bdv = new BigDataViewer(
				new ArrayList<>( converterSetups ),
				new ArrayList<>( sources ),
				null,
				numTimepoints,
				cacheControls,
				frameTitle,
				progressWriter,
				viewerOptions.inputTriggerConfig( inputTriggerConfig ) );
		viewer = bdv.getViewer();
		cards = bdv.getViewerFrame().getCardPanel();
		splitPanel = bdv.getViewerFrame().getSplitPanel();
		setupAssignments = bdv.getSetupAssignments();
		setups = bdv.getConverterSetups();

		// this triggers repaint when PlaceHolderSources are toggled
		viewer.state().changeListeners().add( change -> {
			if ( change == ViewerStateChange.VISIBILITY_CHANGED )
				viewer.getDisplay().repaint();
		} );

		viewer.setDisplayMode( DisplayMode.FUSED );
		bdv.getViewerFrame().setVisible( true );

		final boolean initTransform = !sources.isEmpty();
		return initTransform;
	}
}
