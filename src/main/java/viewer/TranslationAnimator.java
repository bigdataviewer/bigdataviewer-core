package viewer;

import net.imglib2.realtransform.AffineTransform3D;
import viewer.util.AbstractAnimator;

/**
 * An animator that just executes a constant speed translation of the current viewpoint to
 * a target location, keeping all other view parameters constant. 
 * @author Jean-Yves Tinevez
 */
public class TranslationAnimator extends AbstractAnimator {

	private final AffineTransform3D transformStart;
	private final double[] targetTranslation;



	public TranslationAnimator(final AffineTransform3D transformStart, final double[] targetTranslation, long duration) {
		super(duration);
		this.transformStart = transformStart;
		this.targetTranslation = targetTranslation.clone();
	}

	protected AffineTransform3D get( final double t ) {
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set( transformStart );
		
		double sx = transform.get(0, 3);
		double sy = transform.get(1, 3);
		double sz = transform.get(2, 3);
		
		double tx = targetTranslation[0];
		double ty = targetTranslation[1];
		double tz = targetTranslation[2];
		
		transform.set(sx + t * (tx-sx) , 0, 3);
		transform.set(sy + t * (ty-sy) , 1, 3);
		transform.set(sz + t * (tz-sz) , 2, 3);
		
		return transform;
	}
	
}
