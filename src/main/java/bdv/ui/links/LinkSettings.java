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
package bdv.ui.links;

import static bdv.tools.links.PasteSettings.SourceMatchingMethod.BY_SPEC_LOAD_MISSING;

import org.scijava.listeners.Listeners;

import bdv.tools.links.PasteSettings;
import bdv.tools.links.PasteSettings.RecenterMethod;
import bdv.tools.links.PasteSettings.RescaleMethod;
import bdv.tools.links.PasteSettings.SourceMatchingMethod;

/**
 * Settings for copying and pasting links.
 * <p>
 * Listeners can be registered and will be notified of changes.
 */
public class LinkSettings
{
	private boolean pasteDisplayMode = true;
	private boolean pasteViewerTransform = true;
	private boolean pasteCurrentTimepoint = true;
	private boolean pasteSourceVisibility = true;
	private boolean pasteSourceConverterConfigs = true;
	private boolean pasteSourceConfigs = true;
	private SourceMatchingMethod sourceMatchingMethod = BY_SPEC_LOAD_MISSING;
	private RecenterMethod recenterMethod = RecenterMethod.PANEL_CENTER;
	private RescaleMethod rescaleMethod = RescaleMethod.FIT_PANEL;
	private boolean showLinkSettingsCard = false;

	public interface UpdateListener
	{
		void appearanceChanged();
	}

	private final Listeners.List< UpdateListener > updateListeners;

	public LinkSettings()
	{
		updateListeners = new Listeners.SynchronizedList<>();
	}

	public void set( final LinkSettings other )
	{
		this.pasteDisplayMode = other.pasteDisplayMode;
		this.pasteViewerTransform = other.pasteViewerTransform;
		this.pasteCurrentTimepoint = other.pasteCurrentTimepoint;
		this.pasteSourceVisibility = other.pasteSourceVisibility;
		this.pasteSourceConverterConfigs = other.pasteSourceConverterConfigs;
		this.pasteSourceConfigs = other.pasteSourceConfigs;
		this.sourceMatchingMethod = other.sourceMatchingMethod;
		this.recenterMethod = other.recenterMethod;
		this.rescaleMethod = other.rescaleMethod;
		this.showLinkSettingsCard = other.showLinkSettingsCard;
		notifyListeners();
	}

	private void notifyListeners()
	{
		updateListeners.list.forEach( UpdateListener::appearanceChanged );
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	public boolean pasteDisplayMode()
	{
		return pasteDisplayMode;
	}

	public void setPasteDisplayMode( final boolean b )
	{
		if ( pasteDisplayMode != b )
		{
			pasteDisplayMode = b;
			notifyListeners();
		}
	}

	public boolean pasteViewerTransform()
	{
		return pasteViewerTransform;
	}

	public void setPasteViewerTransform( final boolean b )
	{
		if ( pasteViewerTransform != b )
		{
			pasteViewerTransform = b;
			notifyListeners();
		}
	}

	public boolean pasteCurrentTimepoint()
	{
		return pasteCurrentTimepoint;
	}

	public void setPasteCurrentTimepoint( final boolean b )
	{
		if ( pasteCurrentTimepoint != b )
		{
			pasteCurrentTimepoint = b;
			notifyListeners();
		}
	}

	public boolean pasteSourceVisibility()
	{
		return pasteSourceVisibility;
	}

	public void setPasteSourceVisibility( final boolean b )
	{
		if ( pasteSourceVisibility != b )
		{
			pasteSourceVisibility = b;
			notifyListeners();
		}
	}

	public boolean pasteSourceConverterConfigs()
	{
		return pasteSourceConverterConfigs;
	}

	public void setPasteSourceConverterConfigs( final boolean b )
	{
		if ( pasteSourceConverterConfigs != b )
		{
			pasteSourceConverterConfigs = b;
			notifyListeners();
		}
	}

	public boolean pasteSourceConfigs()
	{
		return pasteSourceConfigs;
	}

	public void setPasteSourceConfigs( final boolean b )
	{
		if ( pasteSourceConfigs != b )
		{
			pasteSourceConfigs = b;
			notifyListeners();
		}
	}

	public SourceMatchingMethod sourceMatchingMethod()
	{
		return sourceMatchingMethod;
	}

	public void setSourceMatchingMethod( final SourceMatchingMethod m )
	{
		if ( sourceMatchingMethod != m )
		{
			sourceMatchingMethod = m;
			notifyListeners();
		}
	}

	public RecenterMethod recenterMethod()
	{
		return recenterMethod;
	}

	public void setRecenterMethod( final RecenterMethod m )
	{
		if ( recenterMethod != m )
		{
			recenterMethod = m;
			notifyListeners();
		}
	}

	public RescaleMethod rescaleMethod()
	{
		return rescaleMethod;
	}

	public void setRescaleMethod( final RescaleMethod m )
	{
		if ( rescaleMethod != m )
		{
			rescaleMethod = m;
			notifyListeners();
		}
	}

	public boolean showLinkSettingsCard()
	{
		return showLinkSettingsCard;
	}

	public void setShowLinkSettingsCard( final boolean b )
	{
		if ( showLinkSettingsCard != b )
		{
			showLinkSettingsCard = b;
			notifyListeners();
		}
	}

	@Override
	public String toString()
	{
		return "LinkSettings{" + "pasteDisplayMode=" + pasteDisplayMode
				+ ", pasteViewerTransform=" + pasteViewerTransform
				+ ", pasteCurrentTimepoint=" + pasteCurrentTimepoint
				+ ", pasteSourceVisibility=" + pasteSourceVisibility
				+ ", pasteSourceConverterConfigs=" + pasteSourceConverterConfigs
				+ ", pasteSourceConfigs=" + pasteSourceConfigs
				+ ", sourceMatchingMethod=" + sourceMatchingMethod
				+ ", showLinkSettingsCard=" + showLinkSettingsCard
				+ '}';
	}

	public PasteSettings pasteSettings()
	{
		// view of this LinkSettings as PasteSettings
		return new PasteSettings()
		{
			@Override
			public boolean pasteViewerTransform()
			{
				return pasteViewerTransform;
			}

			@Override
			public boolean pasteCurrentTimepoint()
			{
				return pasteCurrentTimepoint;
			}

			@Override
			public SourceMatchingMethod sourceMatchingMethod()
			{
				return sourceMatchingMethod;
			}

			@Override
			public RescaleMethod rescaleMethod()
			{
				return rescaleMethod;
			}

			@Override
			public RecenterMethod recenterMethod()
			{
				return recenterMethod;
			}

			@Override
			public boolean pasteSourceConfigs()
			{
				return pasteSourceConfigs;
			}

			@Override
			public boolean pasteDisplayMode()
			{
				return pasteDisplayMode;
			}

			@Override
			public boolean pasteInterpolation()
			{
				return pasteDisplayMode;
			}

			@Override
			public boolean pasteConverterConfigs()
			{
				return pasteSourceConverterConfigs;
			}

			@Override
			public boolean pasteSourceVisibility()
			{
				return pasteSourceVisibility;
			}
		};
	}
}
