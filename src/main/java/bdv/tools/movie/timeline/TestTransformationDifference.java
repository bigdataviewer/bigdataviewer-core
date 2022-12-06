package bdv.tools.movie.timeline;

import net.imglib2.realtransform.AffineTransform3D;

public class TestTransformationDifference {
    public static void main(String[] args) {
        AffineTransform3D transform3D = new AffineTransform3D();
        transform3D.set(3,2,5,100,
                4,3,2,0,
                8,1,3,0);
        AffineTransform3D transform2 = transform3D.copy();
        transform3D.scale(2.0);
        transform3D.rotate(0,90);
        AffineTransform3D diff = transform3D.copy();

        diff.concatenate(transform2.inverse());

        System.out.println(diff);

    }
}
