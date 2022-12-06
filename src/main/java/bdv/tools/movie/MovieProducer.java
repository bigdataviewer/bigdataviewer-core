
/**
 * License: GPL
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package bdv.tools.movie;

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.PainterThread;
import bdv.viewer.render.RenderTarget;
import bdv.viewer.render.awt.BufferedImageRenderResult;
import net.imglib2.realtransform.AffineTransform3D;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * Code was copied from org.janelia.saalfeldlab.hotknife.VNCMovie;
 * Modified and adapted by Marwan Zouinkhi
 */

public class MovieProducer {

    public static class Target implements RenderTarget<BufferedImageRenderResult> {

        public BufferedImageRenderResult renderResult = new BufferedImageRenderResult();

        private final int width;
        private final int height;

        public Target(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public BufferedImageRenderResult getReusableRenderResult() {
            return renderResult;
        }

        @Override
        public BufferedImageRenderResult createRenderResult() {
            return new BufferedImageRenderResult();
        }

        @Override
        public void setRenderResult(final BufferedImageRenderResult renderResult) {
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    /**
     * Cosine shape of linear [0,1]
     */
    protected static double cos(final double x) {

        return 0.5 - 0.5 * Math.cos(Math.PI * x);
    }

    /**
     * Acceleration function for a t in [0,1]:
     * <p>
     * types
     * 0  symmetric
     * 1  slow start
     * 2  slow end
     * 3  soft symmetric
     * 4  soft slow start
     * 5  soft slow end
     * t = current frame / nb frames
     */
    public static double accel(final double t, final int type) {

        switch (type) {
            case 1:        // slow start
                return cos(t * t);
            case 2:        // slow end
                return 1.0 - cos(Math.pow(1.0 - t, 2));
            case 3:        // soft symmetric
                return cos(cos(t));
            case 4:        // soft slow start
                return cos(cos(t * t));
            case 5:        // soft slow end
                return 1.0 - cos(cos(Math.pow(1.0 - t, 2)));
            default:    // symmetric
                return cos(t);
        }
    }

    public static void recordMovie(
            final ViewerPanel viewer,
            final AffineTransform3D[] transforms,
            final int[] frames,
            final int[] accel,
            int width,
            int height,
            final String dir,
            ProgressWriter progressWriter) throws IOException {

        final ViewerState renderState = viewer.state();
        final ScaleBarOverlayRenderer scalebar = new ScaleBarOverlayRenderer();

        int screenWidth = viewer.getDisplayComponent().getWidth();
        int screenHeight = viewer.getDisplayComponent().getHeight();
        double ratio = Math.min(width * 1.0 / screenWidth, height * 1.0 / screenHeight);

        final AffineTransform3D viewerScale = new AffineTransform3D();

        viewerScale.set(
                ratio, 0, 0, 0,
                0, ratio, 0, 0,
                0, 0, 1.0, 0);

        final MultiBoxOverlayRenderer box = new MultiBoxOverlayRenderer(width, height);

        final Target target = new Target(width, height);

        final MultiResolutionRenderer renderer = new MultiResolutionRenderer(
                target,
                new PainterThread(null),
                new double[]{1.0},
                0l,
                12,
                null,
                false,
                viewer.getOptionValues().getAccumulateProjectorFactory(),
                new CacheControl.Dummy());

        int i = 0;

        for (int k = 1; k < transforms.length; ++k) {
            progressWriter.setProgress((k*1.0/transforms.length));
            final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(
                    transforms[k - 1],
                    transforms[k],
                    0,
                    0,
                    0);

            for (int d = 0; d < frames[k]; ++d) {
                final AffineTransform3D tkd = animator.get(accel((double) d / (double) frames[k], accel[k]));
                tkd.preConcatenate(viewerScale);
                viewer.state().setViewerTransform(tkd);
                renderState.setViewerTransform(tkd);
                renderer.requestRepaint();
                try {
                    renderer.paint(renderState);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return;
                }

                final BufferedImage bi = target.renderResult.getBufferedImage();

                final Graphics2D g2 = bi.createGraphics();
                g2.drawImage(bi, 0, 0, null);

                /* scalebar */
                g2.setClip(0, 0, width, height);
                scalebar.setViewerState(renderState);
                scalebar.paint(g2);
                box.setViewerState(renderState);
                box.paint(g2);

                /* save image */
                ImageIO.write(bi, "png", new File(String.format("%s/img-%04d.png", dir, i++)));

                System.out.println(String.format("%s/img-%04d.png", dir, i));
            }
        }
    }



}