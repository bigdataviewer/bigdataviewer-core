/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
package net.imglib2.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * TODO revise interface, are all methods still necessary?
 * TODO revise javadoc
 *
 * Change a transformation in response to user input (mouse events, key events,
 * etc.). Report to a {@link TransformListener} when the transformation changes.
 * The {@link TransformEventHandler} receives notifications about changes of the
 * canvas size (it may react for example by changing the scale of the
 * transformation accordingly).
 *
 * @param <A>
 *            type of transformation.
 *
 * @author Tobias Pietzsch
 */
public interface TransformEventHandler< A >
{
	/**
	 * Install transformation behaviours into the specified {@code behaviours} contrainer.
	 */
	void install( Behaviours behaviours );

	/**
	 * Provides access to the transform which {@code TransformEventHandler} modifies.
	 */
	interface Transform< A >
	{
		A getTransform();

		void setTransform( A transform );
	}

	void setTransformStore( Transform< A > store );

	void setTransformStore( Supplier< A > get, Consumer< A > set );

	/**
	 * This is called, when the screen size of the canvas (the component
	 * displaying the image and generating mouse events) changes. This can be
	 * used to determine screen coordinates to keep fixed while zooming or
	 * rotating with the keyboard, e.g., set these to
	 * <em>(width/2, height/2)</em>. It can also be used to update the current
	 * source-to-screen transform, e.g., to change the zoom along with the
	 * canvas size.
	 *
	 * @param width
	 *            the new canvas width.
	 * @param height
	 *            the new canvas height.
	 * @param updateTransform
	 *            whether the current source-to-screen transform should be
	 *            updated. This will be <code>false</code> for the initial
	 *            update of a new {@link TransformEventHandler} and
	 *            <code>true</code> on subsequent calls. If <code>true</code>,
	 *            an update to its {@link TransformListener} should be
	 *            triggered.
	 */
	void setCanvasSize( int width, int height, boolean updateTransform );

	/**
	 * Set the {@link TransformListener} who will receive updated
	 * transformations.
	 *
	 * @param transformListener
	 *            will receive transformation updates.
	 */
	@Deprecated
	void setTransformListener( TransformListener< A > transformListener );

	@Deprecated
	A getTransform();

	@Deprecated
	void setTransform( A transform );
}
