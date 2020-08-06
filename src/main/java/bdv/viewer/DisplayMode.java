/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package bdv.viewer;

public enum DisplayMode
{
	SINGLE     ( 0, "single-source mode" ),
	GROUP      ( 1, "single-group mode"),
	FUSED      ( 2, "fused mode" ),
	FUSEDGROUP ( 3, "fused group mode" );

	private final int id;
	private final String name;

	private DisplayMode( final int id, final String name )
	{
		this.id = id;
		this.name = name;
	}

	public int id()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public DisplayMode withFused( final boolean activate )
	{
		switch ( this )
		{
		default:
		case SINGLE:
		case FUSED:
			return activate ? FUSED : SINGLE;
		case GROUP:
		case FUSEDGROUP:
			return activate ? FUSEDGROUP : GROUP;
		}
	}

	public DisplayMode withGrouping( final boolean activate )
	{
		switch ( this )
		{
		default:
		case SINGLE:
		case GROUP:
			return activate ? GROUP : SINGLE;
		case FUSED:
		case FUSEDGROUP:
			return activate ? FUSEDGROUP : FUSED;
		}
	}

	public boolean hasFused()
	{
		return this == FUSED || this == FUSEDGROUP;
	}

	public boolean hasGrouping()
	{
		return this == GROUP || this == FUSEDGROUP;
	}

	public static final int length;

	static
	{
		length = values().length;
	}
}
