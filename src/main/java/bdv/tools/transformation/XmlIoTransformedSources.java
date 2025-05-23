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
package bdv.tools.transformation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

import org.jdom2.Element;

public class XmlIoTransformedSources
{
	public static final String SOURCETRANSFORMS_TAG = "ManualSourceTransforms";

	public static final String SOURCETRANSFORM_TAG = "SourceTransform";
	public static final String SOURCETRANSFORM_TYPE_ATTRIBUTE_NAME = "type";
	public static final String SOURCETRANSFORM_TYPE_VALUE_AFFINE = "affine";
	public static final String SOURCETRANSFORM_AFFINE_TAG = "affine";

	public String getTagName()
	{
		return SOURCETRANSFORMS_TAG;
	}

	public ManualSourceTransforms fromXml( final Element elem )
	{
		final ArrayList< AffineTransform3D > transforms = new ArrayList<>();
		for ( final Element t : elem.getChildren( SOURCETRANSFORM_TAG ) )
			transforms.add( fromXmlAffine( t ) );
		return new ManualSourceTransforms( transforms );
	}

	public Element toXml( final ManualSourceTransforms manualSourceTransforms )
	{
		final Element elem = new Element( SOURCETRANSFORMS_TAG );
		final List< AffineTransform3D > transforms = manualSourceTransforms.getTransforms();
		for ( final AffineTransform3D t : transforms )
			elem.addContent( toXmlAffine( t ) );
		return elem;
	}

	protected AffineTransform3D fromXmlAffine( final Element transform )
	{
		final AffineTransform3D affine = XmlHelpers.getAffineTransform3D( transform, SOURCETRANSFORM_AFFINE_TAG );
		return affine;
	}

	protected Element toXmlAffine( final AffineTransform3D transform )
	{
		final Element elem = new Element( SOURCETRANSFORM_TAG );
		elem.setAttribute( SOURCETRANSFORM_TYPE_ATTRIBUTE_NAME, SOURCETRANSFORM_TYPE_VALUE_AFFINE );
		elem.addContent( XmlHelpers.affineTransform3DElement( SOURCETRANSFORM_AFFINE_TAG, transform ) );
		return elem;
	}
}
