/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2025 BigDataViewer developers.
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

import bdv.viewer.ConverterSetupBounds;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import bdv.viewer.ConverterSetups;
import bdv.ui.sourcetable.SourceTable;
import bdv.ui.sourcegrouptree.SourceGroupTree;

/**
 * A {@code JPanel} containing a {@link ColorPanel} and a
 * {@link BoundedRangePanel}. It can be constructed for a {@link SourceTable} or
 * a {@link SourceGroupTree}, and will be set up to edit the selected
 * source/groups respectively.
 *
 * @author Tobias Pietzsch
 */
public class ConverterSetupEditPanel extends JPanel
{
	private final ColorPanel colorPanel;

	private final BoundedRangePanel rangePanel;

	public ConverterSetupEditPanel(
			final SourceGroupTree tree,
			final ConverterSetups converterSetups )
	{
		this();
		new BoundedRangeEditor( tree, converterSetups, rangePanel, converterSetups.getBounds() );
		new ColorEditor( tree, converterSetups, colorPanel );
	}

	public ConverterSetupEditPanel(
			final SourceTable table,
			final ConverterSetups converterSetups )
	{
		this();
		new BoundedRangeEditor( table, converterSetups, rangePanel, converterSetups.getBounds() );
		new ColorEditor( table, converterSetups, colorPanel );
	}

	public ConverterSetupEditPanel()
	{
		super( new MigLayout( "ins 0, fillx, hidemode 3", "[]0[grow]", "" ) );
		colorPanel = new ColorPanel();
		rangePanel = new BoundedRangePanel();

		( ( MigLayout ) rangePanel.getLayout() ).setLayoutConstraints( "ins 5 5 5 10, fillx, filly, hidemode 3" );
		add( colorPanel, "growy" );
		add( rangePanel, "grow" );
	}
}
