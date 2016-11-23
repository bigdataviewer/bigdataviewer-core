package bdv.tools.bookmarks.bookmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.jdom2.Element;

import bdv.viewer.animate.SimilarityTransformAnimator;
import mpicbg.spim.data.XmlHelpers;
import net.imglib2.realtransform.AffineTransform3D;

public class DynamicBookmark implements IBookmark{

	public static final String XML_ELEM_BOOKMARK_NAME = "DynamicBookmark" ;
	public static final String XML_ELEM_KEY_NAME = "key" ;
	public static final String XML_ELEM_KEYFRAMES_NAME = "keyframes" ;
	public static final String XML_ELEM_KEYFRAME_NAME = "keyframe" ;
	public static final String XML_ELEM_TIMEPOINT_NAME = "timepoint" ;
	public static final String XML_ELEM_TRANSFORM_NAME = "transform" ;
	
	private final String key;
	private final HashMap<Integer, AffineTransform3D> timepoints;

	public DynamicBookmark(String key) {
		this.key = key;
		this.timepoints = new HashMap<>();
	}
	
	public DynamicBookmark(Element element){
		this.key = restoreKeyFromXml(element);
		this.timepoints = new HashMap<>();
		
		restoreKeyframesFromXml(element);
	}

	@Override
	public String getKey() {
		return this.key;
	}
	
	public AffineTransform3D getTransform(int timepoint){
		return timepoints.get(timepoint);
	}
	
	public void setTimepoint(int timepoint, AffineTransform3D transform){
		timepoints.put(timepoint, transform);
	}
	
	public void removeTimepoint(int timepoint){
		this.timepoints.remove(timepoints);
	}
	
	@Override
	public Element toXmlNode() {
		final Element elemBookmark = new Element( XML_ELEM_BOOKMARK_NAME );
		elemBookmark.addContent( XmlHelpers.textElement( XML_ELEM_KEY_NAME, this.key ) );

		Element elemKeyframes = new Element( XML_ELEM_KEYFRAMES_NAME );
		
		for ( final Entry< Integer, AffineTransform3D> entry : timepoints.entrySet() )
		{
			final Element elemKeyframe = new Element( XML_ELEM_KEYFRAME_NAME );
			elemKeyframe.addContent( XmlHelpers.intElement(XML_ELEM_TIMEPOINT_NAME, entry.getKey()));
			elemKeyframe.addContent( XmlHelpers.affineTransform3DElement(XML_ELEM_TRANSFORM_NAME, entry.getValue()));
			elemKeyframes.addContent(elemKeyframe);
		}
		
		elemBookmark.addContent(elemKeyframes);
		
		return elemBookmark;
	}
	
	private String restoreKeyFromXml(Element parent){
		return XmlHelpers.getText( parent, XML_ELEM_KEY_NAME );
	}
	
	private void restoreKeyframesFromXml(Element parent){
		final Element elemKeyframes = parent.getChild( XML_ELEM_KEYFRAMES_NAME );
		
		for ( final Element elemKeyframe : elemKeyframes.getChildren( XML_ELEM_KEYFRAME_NAME ) )
		{
			int timepoint = XmlHelpers.getInt( elemKeyframe, XML_ELEM_TIMEPOINT_NAME );
			AffineTransform3D transform = XmlHelpers.getAffineTransform3D( elemKeyframe, XML_ELEM_TRANSFORM_NAME );
			setTimepoint(timepoint, transform);
		}
	}
	/**
	 * Returns the greatest timepoint less than or equal to the given timepoinr,
	 * or null if there is no such timepoint.
	 * 
	 * @param timepoint the reference value
	 * @return previous timepoint which is less or equals the given timepoint or null
	 */
	public Integer getPreviousTimepoint(int timepoint){
		TreeSet<Integer> timepointTreeSet = new TreeSet<Integer>(timepoints.keySet());
		return timepointTreeSet.floor(timepoint);
	}
	
	/**
	 * Returns the least timepoint strictly greater than the given timepoint,
	 * or null if there is no such timepoint.
	 * @param timepoint the reference value
	 * @return next timepoint which is greater than the given timepoint or null
	 */
	public Integer getNextTimepoint(int timepoint){
		TreeSet<Integer> timepointTreeSet = new TreeSet<Integer>(timepoints.keySet());
		return timepointTreeSet.higher(timepoint);
	}
	
	/**
	 * Returns the transform from the {@link #getPreviousTimepoint(int) previous timepoint}.
	 * 
	 * @param timepoint the reference value
	 * @return previous transform or null
	 */
	public AffineTransform3D getPreviousTransform(int timepoint){
		Integer previousTimepoint = getPreviousTimepoint(timepoint);
		if(previousTimepoint == null)
			return null;
		
		return timepoints.get(previousTimepoint);
	}
	
	/**
	 * Returns the transform from the {@link #getNextTimepoint(int) next timepoint}.
	 * 
	 * @param timepoint the reference value
	 * @return next transform or null
	 */
	public AffineTransform3D getNextTransform(int timepoint){
		Integer nextTimepoint = getNextTimepoint(timepoint);
		if(nextTimepoint == null)
			return null;
		
		return timepoints.get(nextTimepoint);
	}
	
	/**
	 * Returns interpolated transform between {@link #getPreviousTransform(int) previous} and {@link #getNextTransform(int) next} transform for given timepoint.
	 * 
	 * @param timepoint 
	 * @param cX
	 * @param cY
	 * @return
	 */
	public AffineTransform3D getInterpolatedTransform(final int timepoint, final double cX, final double cY) {

		// get previous transform. if null, use default transform
		AffineTransform3D previousTransform = getPreviousTransform(timepoint);
		if (previousTransform == null) {
			previousTransform = new AffineTransform3D();

			// TODO Should we use the current view transform instead of the
			// default transform?
			// viewer.getState().getViewerTransform( previousTransform );
		}

		// get next transform
		// if null (there is no next transform), return previous transform
		AffineTransform3D nextTransform = getNextTransform(timepoint);
		if (nextTransform == null) {
			return previousTransform;
		}

		// get previous timepoint, if null use 0
		Integer previousTimepoint = getPreviousTimepoint(timepoint);
		if (previousTimepoint == null) {
			previousTimepoint = 0;
		}

		// get next timepoint. Cannot be null, since there is no nextTransform.
		Integer nextTimepoint = getNextTimepoint(timepoint);
		if (nextTimepoint == null) {
			nextTimepoint = previousTimepoint;
		}

		SimilarityTransformAnimator transAnimator = new SimilarityTransformAnimator(previousTransform, nextTransform,
				cX, cY, nextTimepoint - previousTimepoint);
		transAnimator.setTime(0);
		AffineTransform3D targetTransform = transAnimator.getCurrent(timepoint - previousTimepoint);

		targetTransform.set( targetTransform.get( 0, 3 ) - cX, 0, 3 );
		targetTransform.set( targetTransform.get( 1, 3 ) - cY, 1, 3 );
		
		return targetTransform;
	}
}
