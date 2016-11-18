package bdv.tools.bookmarks.bookmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom2.Element;

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
	
	public int getPreviousTimepoint(int currentTimepoint){
		if( timepoints.get(currentTimepoint) != null){
			return currentTimepoint;
		}
		
		List<Integer> list = new ArrayList<>(timepoints.keySet());
		Collections.sort(list);
		
		int closest = 0;
		int lastDiff = Integer.MAX_VALUE;
		
		for (int element : list) {
	       int diff = currentTimepoint - element;
	       if(diff >= 0 && diff < lastDiff){
	    	   closest = element;
	    	   lastDiff = diff;
	       }
	    }
		
		return closest;
	}
	
	public int getNextTimepoint(int currentTimepoint){
		List<Integer> list = new ArrayList<>(timepoints.keySet());
		Collections.sort(list);
		
		int closest = 0;
		int lastDiff = Integer.MAX_VALUE;
		
		for (int element : list) {
	       int diff = element - currentTimepoint;
	       if(diff > 0 && diff < lastDiff){
	    	   closest = element;
	    	   lastDiff = diff;
	       }
	    }
		
		return closest;
	}
	
	public AffineTransform3D getPreviousTransform(int currentTimepoint){
		int previousTimepoint = getPreviousTimepoint(currentTimepoint);
		return timepoints.get(previousTimepoint);
	}
	
	public AffineTransform3D getNextTransform(int currentTimepoint){
		int nextTimepoint = getNextTimepoint(currentTimepoint);
		return timepoints.get(nextTimepoint);
	}
}
