/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2023 BigDataViewer developers.
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
package bdv.viewer.location;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.scijava.listeners.Listeners;

import bdv.ui.BdvDefaultCards;
import bdv.ui.CardPanel;
import bdv.ui.appearance.Appearance;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.splitpanel.SplitPanel;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import bdv.viewer.overlay.SourceInfoOverlayRenderer;
import net.imglib2.Interval;
import net.imglib2.RealPoint;
import net.imglib2.util.Intervals;

/**
 * Helper class to set up the highly-coupled SourceInfoToolBar and LocationPanel UI components.
 *
 * @author Eric Trautman
 */
public class SourceInfoToolbarAndLocationCardManager implements ViewerStateChangeListener {
	private final AppearanceManager appearanceManager;
	private final ViewerPanel viewer;
	private final SourceInfoToolBar sourceInfoToolBar;
	private final LocationPanel locationPanel;

	public SourceInfoToolbarAndLocationCardManager(final AppearanceManager appearanceManager,
												   final ViewerPanel viewer) {
		this.appearanceManager = appearanceManager;
		this.viewer = viewer;

		this.sourceInfoToolBar = new SourceInfoToolBar();
		sourceInfoToolBar.setVisible(showSourceInfoToolBar());

		final Listeners<Appearance.UpdateListener> updateListeners = appearanceManager.appearance().updateListeners();
		updateListeners.add(() -> sourceInfoToolBar.setVisible(showSourceInfoToolBar()));

		// create card locationPanel and connect it to sourceInfoToolBar
		this.locationPanel = new LocationPanel(getCurrentSourceInterval());
		locationPanel.setDimensionValueChangeListener(e -> {
			final DimensionCoordinateComponents coordinateComponents = (DimensionCoordinateComponents) e.getSource();
			viewer.centerViewAt(coordinateComponents.getPosition(),
								coordinateComponents.getDimension());
			final double[] gCenterPos = new double[3];
			viewer.state().getViewerTransform().applyInverse(gCenterPos, viewer.getDisplayCenterCoordinates());
			sourceInfoToolBar.setCenterPosition(gCenterPos);
			sourceInfoToolBar.revalidate();
		});

		// register for (source) state change updates
		viewer.state().changeListeners().add(this);

		// register for mouse events
		addViewerMouseListeners();

		// populate everything with starting location info
		updateSourceInfo();
		updateCenterPosition();
		updateMousePosition();
	}

	public void addToolbarToViewerFrame(final ViewerFrame viewerFrame) {
		viewerFrame.add(sourceInfoToolBar, BorderLayout.NORTH);
	}

	public void addLocationCardToSplitPanel(final SplitPanel splitPanel,
											final CardPanel cards) {

		cards.addCard(BdvDefaultCards.DEFAULT_LOCATIONS_CARD,
					  "Locations",
					  locationPanel,
					  false,
					  new Insets(0, 4, 0, 0));

		// add hook to expand card panel and locations card when edit button in source info toolbar is clicked
		sourceInfoToolBar.setEditActionListener(e -> {
			// expand card panel and location card
			splitPanel.setCollapsed(false);
			cards.setCardExpanded(BdvDefaultCards.DEFAULT_SOURCES_CARD, false);
			cards.setCardExpanded(BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD, false);
			cards.setCardExpanded(BdvDefaultCards.DEFAULT_LOCATIONS_CARD, true);
			locationPanel.requestFocusOnFirstComponent();
		});
	}

	@Override
	public void viewerStateChanged(final ViewerStateChange change) {
		switch (change) {
			case CURRENT_SOURCE_CHANGED:
			case GROUP_NAME_CHANGED:
			case CURRENT_GROUP_CHANGED:
			case CURRENT_TIMEPOINT_CHANGED:
			case NUM_TIMEPOINTS_CHANGED:
				updateSourceInfo();
				locationPanel.setSourceInterval(getCurrentSourceInterval());
				break;
			case VIEWER_TRANSFORM_CHANGED:
				updateCenterPosition();
				updateMousePosition();
		}
	}

	private Interval getCurrentSourceInterval() {
		Interval interval = null;
		final SourceAndConverter<?> currentSource = viewer.state().getCurrentSource();
		if (currentSource != null) {
			final Source<?> spimSource = currentSource.getSpimSource();
			if (spimSource != null) {
				final int timePoint = viewer.state().getCurrentTimepoint();
				interval = spimSource.getSource(timePoint, 0);
			}
		}
		if (interval == null) {
			interval = Intervals.createMinMax(0, 0, 0, 0, 0, 0);
		}
		return interval;
	}
	
	private boolean showSourceInfoToolBar() {
		return appearanceManager.appearance().showSourceInfoToolBar();
	}

	private void updateSourceInfo() {
		if (showSourceInfoToolBar()) {
			final SourceInfoOverlayRenderer sourceInfoOverlayRenderer = viewer.getSourceInfoOverlayRenderer();
			sourceInfoOverlayRenderer.setViewerState(viewer.state());
			sourceInfoToolBar.setSourceNamesAndTimepoint(sourceInfoOverlayRenderer.getSourceName(),
														 sourceInfoOverlayRenderer.getGroupName(),
														 sourceInfoOverlayRenderer.getTimepointString());
		}
	}

	private void updateCenterPosition() {
		final double[] gCenterPos = new double[3];
		viewer.state().getViewerTransform().applyInverse(gCenterPos, viewer.getDisplayCenterCoordinates());
		locationPanel.setCenterPosition(gCenterPos);
		if (showSourceInfoToolBar()) {
			sourceInfoToolBar.setCenterPosition(gCenterPos);
		}
	}

	private void updateMousePosition() {
		if (showSourceInfoToolBar()) {
			final double[] gMousePos = new double[3];
			viewer.getGlobalMouseCoordinates(RealPoint.wrap(gMousePos));
			sourceInfoToolBar.setMousePosition(gMousePos);
		}
	}

	private void addViewerMouseListeners() {

		final Component viewerDisplay = viewer.getDisplayComponent();

		viewerDisplay.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				updateMousePosition();
			}
			@Override
			public void mouseDragged(final MouseEvent e) {
				updateMousePosition();
			}
		});

		viewerDisplay.addMouseWheelListener(e -> {
			updateCenterPosition();
			updateMousePosition();
		});

		viewerDisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(final MouseEvent e) {
				sourceInfoToolBar.setMouseCoordinatesVisible(true);
			}
			@Override
			public void mouseExited(final MouseEvent e) {
				sourceInfoToolBar.setMouseCoordinatesVisible(false);
			}
			@Override
			public void mouseReleased(final MouseEvent e) {
				updateCenterPosition();
				updateMousePosition();
			}
		});
	}
}
