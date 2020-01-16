package bdv.ui.convertersetupeditor;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.ConverterSetups;
import bdv.util.BoundedRange;
import bdv.util.Bounds;
import bdv.ui.UIUtils;
import java.awt.Color;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class BoundedRangeEditor
{
	private final Supplier< List< ConverterSetup > > selectedConverterSetups;

	private final BoundedRangePanel rangePanel;

	private final ConverterSetupBounds converterSetupBounds;

	private final Color equalColor;

	private final Color notEqualColor;

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

		equalColor = rangePanel.getBackground();
		notEqualColor = UIUtils.mix( equalColor, Color.red, 0.9 );

		final JPopupMenu menu = new JPopupMenu();
		menu.add( setBoundsItem( "set bounds 0..1", 0, 1 ) );
		menu.add( setBoundsItem( "set bounds 0..255", 0, 255 ) );
		menu.add( setBoundsItem( "set bounds 0..65535", 0, 65535 ) );
		menu.add( runnableItem(  "shrink bounds to selection", rangePanel::shrinkBoundsToRange ) );
		rangePanel.setPopup( () -> menu );
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
		blockUpdates = true;

		if ( converterSetups == null || converterSetups.isEmpty() )
		{
			rangePanel.setEnabled( false );
			rangePanel.setBackground( equalColor );
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
			rangePanel.setEnabled( true );
			rangePanel.setRange( range );
			rangePanel.setBackground( allRangesEqual ? equalColor : notEqualColor );
		}

		blockUpdates = false;
	}
}
