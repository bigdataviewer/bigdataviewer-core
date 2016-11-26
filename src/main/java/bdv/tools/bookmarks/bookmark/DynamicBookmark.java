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

	public DynamicBookmark(final String key) {
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
	
	public AffineTransform3D getTransform(final int timepoint){
		return timepoints.get(timepoint);
	}
	
	public void setTimepoint(final int timepoint, final AffineTransform3D transform){
		timepoints.put(timepoint, transform);
	}
	
	public void removeTimepoint(final int timepoint){
		this.timepoints.remove(timepoints);
	}
	
	@Override
	public Element toXmlNode() {
		final Element elemBookmark = new Element( XML_ELEM_BOOKMARK_NAME );
		elemBookmark.addContent( XmlHelpers.textElement( XML_ELEM_KEY_NAME, this.key ) );

		final Element elemKeyframes = new Element( XML_ELEM_KEYFRAMES_NAME );
		
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
	
	private String restoreKeyFromXml(final Element parent){
		return XmlHelpers.getText( parent, XML_ELEM_KEY_NAME );
	}
	
	private void restoreKeyframesFromXml(final Element parent){
		final Element elemKeyframes = parent.getChild( XML_ELEM_KEYFRAMES_NAME );
		
		for ( final Element elemKeyframe : elemKeyframes.getChildren( XML_ELEM_KEYFRAME_NAME ) )
		{
			final int timepoint = XmlHelpers.getInt( elemKeyframe, XML_ELEM_TIMEPOINT_NAME );
			final AffineTransform3D transform = XmlHelpers.getAffineTransform3D( elemKeyframe, XML_ELEM_TRANSFORM_NAME );
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
	public Integer getPreviousTimepoint(final int timepoint){
		final TreeSet<Integer> timepointTreeSet = new TreeSet<Integer>(timepoints.keySet());
		return timepointTreeSet.floor(timepoint);
	}
	
	/**
	 * Returns the least timepoint strictly greater than the given timepoint,
	 * or null if there is no such timepoint.
	 * @param timepoint the reference value
	 * @return next timepoint which is greater than the given timepoint or null
	 */
	public Integer getNextTimepoint(int timepoint){
		final TreeSet<Integer> timepointTreeSet = new TreeSet<Integer>(timepoints.keySet());
		return timepointTreeSet.higher(timepoint);
	}
	
	/**
	 * Returns the transform from the {@link #getPreviousTimepoint(int) previous timepoint}.
	 * 
	 * @param timepoint the reference value
	 * @return previous transform or null
	 */
	public AffineTransform3D getPreviousTransform(final int timepoint){
		final Integer previousTimepoint = getPreviousTimepoint(timepoint);
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
	public AffineTransform3D getNextTransform(final int timepoint){
		final Integer nextTimepoint = getNextTimepoint(timepoint);
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

		final Integer previousTimepoint = getPreviousTimepoint(timepoint);
		final AffineTransform3D previousTransform;
		// if previous timepoint is null, use default transform as previous transform
		if(previousTimepoint == null){
			previousTransform = new AffineTransform3D();
			//previousTransform.set( previousTransform.get( 0, 3 ) - cX, 0, 3 );
			//previousTransform.set( previousTransform.get( 1, 3 ) - cY, 1, 3 );
			// TODO Should we use the current view transform instead of the default transform?
			// viewer.getState().getViewerTransform( previousTransform );
		}
		else{
			previousTransform = getTransform(previousTimepoint);
		}
		
		final Integer nextTimepoint = getNextTimepoint(timepoint);
		// if next timepoint is null, return previous transform
		if(nextTimepoint == null){
			return previousTransform;
		}
		final AffineTransform3D nextTransform = getTransform(nextTimepoint);

		final SimilarityTransformAnimator transAnimator = new SimilarityTransformAnimator(previousTransform, nextTransform,
				cX, cY, nextTimepoint - previousTimepoint);
		transAnimator.setTime(0);
		final AffineTransform3D targetTransform = transAnimator.getCurrent(timepoint - previousTimepoint);

		return targetTransform;
	}
}
