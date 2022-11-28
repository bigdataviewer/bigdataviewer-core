/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2022 BigDataViewer developers.
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
package bdv.ui.convertersetupeditor;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import net.imglib2.type.numeric.ARGBType;

class ColorEditor
{
	private final Supplier< List< ConverterSetup > > selectedConverterSetups;

	private final ColorPanel colorPanel;

	public ColorEditor(
			final SourceTable table,
			final ConverterSetups converterSetups,
			final ColorPanel colorPanel )
	{
		this( table::getSelectedConverterSetups, converterSetups, colorPanel );
		table.getSelectionModel().addListSelectionListener( e -> updateSelection() );
	}

	public ColorEditor(
			final SourceGroupTree tree,
			final ConverterSetups converterSetups,
			final ColorPanel colorPanel )
	{
		this(
				() -> converterSetups.getConverterSetups( tree.getSelectedSources() ),
				converterSetups, colorPanel );
		tree.getSelectionModel().addTreeSelectionListener( e -> updateSelection() );
		tree.getModel().addTreeModelListener( new TreeModelListener()
		{
			@Override
			public void treeNodesChanged( final TreeModelEvent e )
			{
				updateSelection();
			}

			@Override
			public void treeNodesInserted( final TreeModelEvent e )
			{
				updateSelection();
			}

			@Override
			public void treeNodesRemoved( final TreeModelEvent e )
			{
				updateSelection();
			}

			@Override
			public void treeStructureChanged( final TreeModelEvent e )
			{
				updateSelection();
			}
		} );
	}

	private ColorEditor(
			final Supplier< List< ConverterSetup > > selectedConverterSetups,
			final ConverterSetups converterSetups,
			final ColorPanel colorPanel )
	{
		this.selectedConverterSetups = selectedConverterSetups;
		this.colorPanel = colorPanel;

		colorPanel.changeListeners().add( this::updateConverterSetupColors );
		converterSetups.listeners().add( s -> updateColorPanel() );
	}

	private boolean blockUpdates = false;

	private List< ConverterSetup > converterSetups;

	private synchronized void updateConverterSetupColors()
	{
		if ( blockUpdates || converterSetups == null || converterSetups.isEmpty() )
			return;

		ARGBType color = colorPanel.getColor();

		for ( final ConverterSetup converterSetup : converterSetups )
		{
			if ( converterSetup.supportsColor() )
				converterSetup.setColor( color );
		}

		updateColorPanel();
	}

	private synchronized void updateSelection()
	{
		converterSetups = selectedConverterSetups.get();
		updateColorPanel();
	}

	private synchronized void updateColorPanel()
	{
		if ( converterSetups == null || converterSetups.isEmpty() )
		{
			SwingUtilities.invokeLater( () -> {
				colorPanel.setEnabled( false );
				colorPanel.setColor( null );
				colorPanel.setConsistent( true );
			} );
		}
		else
		{
			ARGBType color = null;
			boolean allColorsEqual = true;
			for ( final ConverterSetup converterSetup : converterSetups )
			{
				if ( converterSetup.supportsColor() )
				{
					if ( color == null )
						color = converterSetup.getColor();
					else
						allColorsEqual &= color.equals( converterSetup.getColor() );
				}
			}
			final ARGBType finalColor = color;
			final boolean isConsistent = allColorsEqual;
			SwingUtilities.invokeLater( () -> {
				synchronized ( ColorEditor.this )
				{
					blockUpdates = true;
					colorPanel.setEnabled( finalColor != null );
					colorPanel.setColor( finalColor );
					colorPanel.setConsistent( isConsistent );
					blockUpdates = false;
				}
			} );
		}
	}
}
