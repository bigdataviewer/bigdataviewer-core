package bdv.tools.movie.preview;

import net.imglib2.realtransform.AffineTransform3D;

import java.io.Serializable;

public class MovieFrameInst implements Serializable {
    private AffineTransform3D transform;

    private int position;
    private int frames;
    private int accel;
    private final static int DEFAULT_FRAMES = 120;
    private final static int DEFAULT_ACCEL = 0;


    public MovieFrameInst(int position, AffineTransform3D transform) {
        this(position, transform, ((position == 0) ? 0 : DEFAULT_FRAMES), ((position == 0) ? 0 : DEFAULT_ACCEL));
    }

    public MovieFrameInst(int position, AffineTransform3D transform, int frames, int accel) {
        this.position = position;
        this.transform = transform;
        this.frames = frames;
        this.accel = accel;
    }

    public AffineTransform3D getTransform() {
        return transform;
    }

    public void setTransform(AffineTransform3D transform) {
        this.transform = transform;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
    }

    public int getAccel() {
        return accel;
    }

    public void setAccel(int accel) {
        this.accel = accel;
    }

    public static int getDefaultFrames() {
        return DEFAULT_FRAMES;
    }

    public static int getDefaultAccel() {
        return DEFAULT_ACCEL;
    }
}
