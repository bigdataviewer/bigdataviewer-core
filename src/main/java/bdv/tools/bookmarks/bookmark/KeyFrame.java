package bdv.tools.bookmarks.bookmark;

import net.imglib2.realtransform.AffineTransform3D;

public class KeyFrame {

	private int timepoint;
	private AffineTransform3D transform;

	public KeyFrame(int timepoint, AffineTransform3D transform) {
		this.timepoint = timepoint;
		this.transform = transform;
	}
	
	protected KeyFrame(KeyFrame k){
		this.timepoint = k.timepoint;
		this.transform = k.transform.copy();
	}

	public int getTimepoint() {
		return timepoint;
	}

	public void setTimepoint(int timepoint) {
		this.timepoint = timepoint;
	}

	public AffineTransform3D getTransform() {
		return transform;
	}

	public void setTransform(AffineTransform3D transform) {
		this.transform = transform;
	}
	
	public KeyFrame copy(){
		return new KeyFrame(this);
	}
	
	public boolean equals(Object other){
		if (getClass() != other.getClass()) {
			return false;
		}
		
		if(other instanceof KeyFrame){
			KeyFrame k = (KeyFrame) other;
			return this.timepoint == k.getTimepoint();
		}
		return false;
	}
}
