package bdv.tools.movie.preview;


import bdv.cache.CacheControl;
import bdv.tools.movie.MovieProducer;
import bdv.tools.movie.panels.ImageFrame;
import bdv.viewer.ViewerPanel;
import bdv.viewer.ViewerState;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bdv.viewer.overlay.ScaleBarOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.render.PainterThread;
import net.imglib2.realtransform.AffineTransform3D;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PreviewRender implements Runnable {

    private final ViewerPanel viewer;
    private final AffineTransform3D[] transforms;
    private final int[] frames;
    private final int[] accel;
    private final int sleep;
    private final int down;
    public Thread t;

    boolean suspended = false;

    public PreviewRender(
            final ViewerPanel viewer,
            final AffineTransform3D[] transforms,
            final int[] frames,
            final int[] accel,
            final int sleep,
            final int down) {
        System.out.println("got "+transforms.length+" transforms");
        this.viewer = viewer;
        this.transforms = transforms;
        this.frames = frames;
        this.accel = accel;
        this.sleep = sleep;
        this.down = down;
    }

    public void run() {
        ImageFrame preview = new ImageFrame();

        int width = 600;
        int height = 600;

        int screenWidth = viewer.getDisplayComponent().getWidth();
        int screenHeight = viewer.getDisplayComponent().getHeight();

        double ratio = Math.min(width * 1.0 / screenWidth, height * 1.0 / screenHeight);
        final AffineTransform3D viewerScale = new AffineTransform3D();
        final AffineTransform3D viewerTranslation = new AffineTransform3D();

        viewerScale.set(
                ratio, 0, 0, 0,
                0, ratio, 0, 0,
                0, 0, 1.0, 0);
//            viewerTranslation.set(
//                    1, 0, 0, 0.5 * screenWidth,
//                    0, 1, 0, 0.5 * screenHeight,
//                    0, 0, 1, 0);


        final ViewerState renderState = viewer.state();
        final ScaleBarOverlayRenderer scalebar = new ScaleBarOverlayRenderer();
        final MultiBoxOverlayRenderer box = new MultiBoxOverlayRenderer(width, height);

        final MovieProducer.Target target = new MovieProducer.Target(width, height);

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


        for (int k = 1; k < transforms.length; ++k) {
            final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(
                    transforms[k - 1],
                    transforms[k],
                    0,
                    0,
                    0);
            int downsampledFrames = frames[k]/down;

            for (int d = 0; d < downsampledFrames; ++d) {
                final AffineTransform3D tkd = animator.get(MovieProducer.accel((double) d / (double) downsampledFrames, accel[k]));
                tkd.preConcatenate(viewerTranslation.inverse());
                tkd.preConcatenate(viewerScale);
                tkd.preConcatenate(viewerTranslation);
                viewer.state().setViewerTransform(tkd);
                renderState.setViewerTransform(tkd);
                renderer.requestRepaint();
                try {
                    renderer.paint(renderState);
                } catch (final Exception e) {
                    e.printStackTrace();
                    return;
                }

                /* clahe */
                final BufferedImage bi = target.renderResult.getBufferedImage();

                final Graphics2D g2 = bi.createGraphics();

                g2.drawImage(bi, 0, 0, null);

                /* scalebar */
                g2.setClip(0, 0, width, height);
                scalebar.setViewerState(renderState);
                scalebar.paint(g2);
                box.setViewerState(renderState);
                box.paint(g2);

                preview.setImage(bi);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

    public boolean getStatus() {
        return t == null;
    }

    public boolean isDone() {
        return (t.getState() == Thread.State.TERMINATED);
    }

    public void start() {
        if (getStatus()) {
            t = new Thread(this);
            t.start();
        }
    }

    public void suspend() {
        t.stop();
        suspended = true;
    }

    public boolean isSuspended() {
        return suspended;
    }

    synchronized void resume() {
        suspended = false;
        t.resume();
        this.notify();
    }


}

