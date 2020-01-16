package bdv.ui.convertersetupeditor;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.sourcegrouptree.SourceGroupTree;
import bdv.ui.UIUtils;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import net.imglib2.type.numeric.ARGBType;

class ColorEditor
{
	private final Supplier< List< ConverterSetup > > selectedConverterSetups;

	private final ColorPanel colorPanel;

	private final Color equalColor;

	private final Color notEqualColor;

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

		colorPanel.changeListeners().add( () -> updateConverterSetupColors() );
		converterSetups.listeners().add( s -> updateColorPanel() );

		equalColor = colorPanel.getBackground();
		notEqualColor = UIUtils.mix( equalColor, Color.red, 0.9 );
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
		blockUpdates = true;

		if ( converterSetups == null || converterSetups.isEmpty() )
		{
			colorPanel.setEnabled( false );
			colorPanel.setColor( null );
			colorPanel.setBackground( equalColor );
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
			colorPanel.setEnabled( color != null );
			colorPanel.setColor( color );
			colorPanel.setBackground( allColorsEqual ? equalColor : notEqualColor );
		}

		blockUpdates = false;
	}
}
