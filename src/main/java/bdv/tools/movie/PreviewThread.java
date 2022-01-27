package bdv.tools.movie;

import bdv.viewer.Interpolation;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

import javax.swing.*;

public class PreviewThread extends VNCMovie implements Runnable {

    private final ViewerPanel viewer;
    private final AffineTransform3D[] transforms;
    private final int[] frames;
    private final int[] accel;
    private final int sleep;
    private final int down;
    public Thread t;

    boolean suspended = false;

    PreviewThread(
            final ViewerPanel viewer,
            final AffineTransform3D[] transforms,
            final int[] frames,
            final int[] accel,
            final int sleep,
            final int down) {
        this.viewer = viewer;
        this.transforms = transforms;
        this.frames = frames;
        this.accel = accel;
        this.sleep = sleep;
        this.down = down;
    }

    public void run() {
        try {
            viewer.setInterpolation(Interpolation.NLINEAR);
            for (int k = 1; k < transforms.length; ++k) {
                synchronized (this) {
                    if (suspended) {
                        wait();
                    }
                }
                    final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(
                            transforms[k - 1],
                            transforms[k],
                            0,
                            0,
                            0);
                    int downFrames = frames[k] / down;
                    for (int d = 0; d < downFrames; ++d) {
                        System.out.println(k + "-" + d);
                        final AffineTransform3D tkd = animator.get(accel((double) d / (double) downFrames, accel[k]));
                        System.out.println(tkd.toString());
                        SwingUtilities.invokeLater(() -> {
                            viewer.state().setViewerTransform(tkd);
                            viewer.repaint();
                        });
                        Thread.sleep(sleep);
                    }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.start();
    }

    public boolean getStatus() {
        return t == null;
    }

    public boolean isDone(){
        return (t.getState()==Thread.State.TERMINATED);
    }

    public void start() {
        if (getStatus()) {
            t = new Thread(this);
            t.start();
        }
    }

    public void suspend() {
        suspended = true;
    }

    public boolean isSuspended() {
        return suspended;
    }

    synchronized void resume() {
        suspended = false;
        this.notify();
    }

}

