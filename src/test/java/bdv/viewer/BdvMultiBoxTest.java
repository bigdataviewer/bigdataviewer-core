/*-
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2026 BigDataViewer developers.
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

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class BdvMultiBoxTest {

    public static void main(String[] args) {

    	// two 5x5x5 images 
        RandomAccessibleInterval<UnsignedByteType> img1 = ArrayImgs.unsignedBytes(5, 5, 5);
        RandomAccessibleInterval<UnsignedByteType> img2 = ArrayImgs.unsignedBytes(5, 5, 5);

        img1.forEach(pixel -> pixel.set(180));
        img2.forEach(pixel -> pixel.set(255));

        BdvStackSource<?> bdv = BdvFunctions.show(img1, "Image 1");

        AffineTransform3D transform = new AffineTransform3D();
        transform.translate(5.0, 0.0, 0.0);

        BdvFunctions.show(img2, "Image 2 (Shifted)", BdvOptions.options().addTo(bdv).sourceTransform(transform));
        
        //adjust camera view
        ViewerPanel viewer = bdv.getBdvHandle().getViewerPanel();
        AffineTransform3D t = viewer.state().getViewerTransform();
        t.scale( 0.5 );
        t.translate( 60, 150, 0 );
        viewer.state().setViewerTransform( t );
    }
}
