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
package bdv.tools.links;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import bdv.KeyConfigContexts;
import bdv.KeyConfigScopes;
import bdv.viewer.ViewerFrame;

public class LinkActions
{
	public static final String COPY_VIEWER_STATE = "copy viewer state";
	public static final String PASTE_VIEWER_STATE = "paste viewer state";

	public static final String[] COPY_VIEWER_STATE_KEYS = new String[] { "ctrl C", "meta C" };
	public static final String[] PASTE_VIEWER_STATE_KEYS = new String[] { "ctrl V", "meta V" };

	/*
	 * Command descriptions for all provided commands
	 */
	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigScopes.BIGDATAVIEWER, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( COPY_VIEWER_STATE, COPY_VIEWER_STATE_KEYS, "Copy the current viewer state as a string." );
			descriptions.add( PASTE_VIEWER_STATE, PASTE_VIEWER_STATE_KEYS, "Paste the current viewer state from a string." );
		}
	}

	/**
	 * Install into the specified {@link Actions}.
	 */
	public static void install( final Actions actions, final ViewerFrame viewerFrame )
	{
		actions.runnableAction( () -> System.out.println( COPY_VIEWER_STATE ),
				COPY_VIEWER_STATE, COPY_VIEWER_STATE_KEYS );
		actions.runnableAction( () -> System.out.println( PASTE_VIEWER_STATE ),
				PASTE_VIEWER_STATE, PASTE_VIEWER_STATE_KEYS );
	}
}
