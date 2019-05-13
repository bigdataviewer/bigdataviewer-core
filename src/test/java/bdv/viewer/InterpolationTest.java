package bdv.viewer;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class InterpolationTest {

    @Test
    public void next() {
        Assert.assertEquals( Interpolation.NLINEAR, Interpolation.NEARESTNEIGHBOR.next() );
        Assert.assertEquals( Interpolation.NEARESTNEIGHBOR, Interpolation.NLINEAR.next() );
    }
}
