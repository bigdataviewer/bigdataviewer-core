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