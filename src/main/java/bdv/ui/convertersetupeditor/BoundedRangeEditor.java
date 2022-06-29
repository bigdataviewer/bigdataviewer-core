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
import bdv.viewer.ConverterSetupBounds;
import bdv.viewer.ConverterSetups;
import bdv.ui.sourcetable.SourceTable;
import bdv.util.BoundedRange;
import bdv.util.Bounds;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

class BoundedRangeEditor
{
	private final Supplier< List< ConverterSetup > > selectedConverterSetups;

	private final BoundedRangePanel rangePanel;

	private final ConverterSetupBounds converterSetupBounds;

	public BoundedRangeEditor(
			final SourceTable table,
			final ConverterSetups converterSetups,
			final BoundedRangePanel rangePanel,
			final ConverterSetupBounds converterSetupBounds )
	{
		this( table::getSelectedConverterSetups, converterSetups, rangePanel, converterSetupBounds );
		table.getSelectionModel().addListSelectionListener( e -> updateSelection() );
	}

	public BoundedRangeEditor(
			final SourceGroupTree tree,
			final ConverterSetups converterSetups,
			final BoundedRangePanel rangePanel,
			final ConverterSetupBounds converterSetupBounds )
	{
		this(
				() -> converterSetups.getConverterSetups( tree.getSelectedSources() ),
				converterSetups, rangePanel, converterSetupBounds );
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

	private BoundedRangeEditor(
			final Supplier< List< ConverterSetup > > selectedConverterSetups,
			final ConverterSetups converterSetups,
			final BoundedRangePanel rangePanel, final ConverterSetupBounds converterSetupBounds )
	{
		this.selectedConverterSetups = selectedConverterSetups;
		this.rangePanel = rangePanel;
		this.converterSetupBounds = converterSetupBounds;

		rangePanel.changeListeners().add( this::updateConverterSetupRanges );

		converterSetups.listeners().add( s -> updateRangePanel() );

		final JPopupMenu menu = new JPopupMenu();
		menu.add( runnableItem(  "set bounds ...", rangePanel::setBoundsDialog ) );
		menu.add( setBoundsItem( "set bounds 0..1", 0, 1 ) );
		menu.add( setBoundsItem( "set bounds 0..255", 0, 255 ) );
		menu.add( setBoundsItem( "set bounds 0..65535", 0, 65535 ) );
		menu.add( runnableItem(  "shrink bounds to selection", rangePanel::shrinkBoundsToRange ) );
		rangePanel.setPopup( () -> menu );

		updateRangePanel();
	}

	private JMenuItem setBoundsItem( final String text, final double min, final double max )
	{
		final JMenuItem item = new JMenuItem( text );
		item.addActionListener( e -> setBounds( new Bounds( min, max ) ) );
		return item;
	}

	private JMenuItem runnableItem( final String text, final Runnable action )
	{
		final JMenuItem item = new JMenuItem( text );
		item.addActionListener( e -> action.run() );
		return item;
	}

	private boolean blockUpdates = false;

	private List< ConverterSetup > converterSetups;

	private synchronized void setBounds( final Bounds bounds )
	{
		if ( converterSetups == null || converterSetups.isEmpty() )
			return;

		for ( final ConverterSetup converterSetup : converterSetups )
		{
			converterSetupBounds.setBounds( converterSetup, bounds );
		}

		updateRangePanel();
	}

	private synchronized void updateConverterSetupRanges()
	{
		if ( blockUpdates || converterSetups == null || converterSetups.isEmpty() )
			return;

		final BoundedRange range = rangePanel.getRange();

		for ( final ConverterSetup converterSetup : converterSetups )
		{
			converterSetup.setDisplayRange( range.getMin(), range.getMax() );
			converterSetupBounds.setBounds( converterSetup, range.getBounds() );
		}

		updateRangePanel();
	}

	private synchronized void updateSelection()
	{
		converterSetups = selectedConverterSetups.get();
		updateRangePanel();
	}

	private synchronized void updateRangePanel()
	{
		if ( converterSetups == null || converterSetups.isEmpty() )
		{
			SwingUtilities.invokeLater( () -> {
				rangePanel.setEnabled( false );
				rangePanel.setConsistent( true );
			} );
		}
		else
		{
			BoundedRange range = null;
			boolean allRangesEqual = true;
			for ( final ConverterSetup converterSetup : converterSetups )
			{
				final Bounds bounds = converterSetupBounds.getBounds( converterSetup );
				final double minBound = bounds.getMinBound();
				final double maxBound = bounds.getMaxBound();
				final double min = converterSetup.getDisplayRangeMin();
				final double max = converterSetup.getDisplayRangeMax();

				final BoundedRange converterSetupRange = new BoundedRange( minBound, maxBound, min, max );
				if ( range == null )
					range = converterSetupRange;
				else
				{
					allRangesEqual &= range.equals( converterSetupRange );
					range = range.join( converterSetupRange );
				}
			}
			final BoundedRange finalRange = range;
			final boolean isConsistent = allRangesEqual;
			SwingUtilities.invokeLater( () -> {
				synchronized ( BoundedRangeEditor.this )
				{
					blockUpdates = true;
					rangePanel.setEnabled( true );
					rangePanel.setRange( finalRange );
					rangePanel.setConsistent( isConsistent );
					blockUpdates = false;
				}
			} );
		}
	}
}
