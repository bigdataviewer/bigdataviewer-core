/*
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
package bdv.tools.boundingbox;

import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.listeners.Listeners;

/**
 * A transformed box that can be modified and notifies listeners about changes.
 * Represented as an interval (defined in subclasses) that is placed into
 * global coordinate system by an {@code AffineTransform3D}.
 */
public abstract class AbstractTransformedBoxModel implements TransformedBox
{
	public interface IntervalChangedListener
	{
		void intervalChanged();
	}

	private final AffineTransform3D transform;

	private final Listeners.List< IntervalChangedListener > listeners;

	public AbstractTransformedBoxModel( final AffineTransform3D transform )
	{
		this.transform = transform;
		listeners = new Listeners.SynchronizedList<>();
	}

	@Override
	public void getTransform( final AffineTransform3D t )
	{
		t.set( transform );
	}

	public Listeners< IntervalChangedListener > intervalChangedListeners()
	{
		return listeners;
	}

	public abstract void setInterval( RealInterval interval );

	protected void notifyIntervalChanged()
	{
		listeners.list.forEach( IntervalChangedListener::intervalChanged );
	}
}
