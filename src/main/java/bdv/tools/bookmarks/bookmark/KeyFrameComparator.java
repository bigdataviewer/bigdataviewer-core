package bdv.tools.bookmarks.bookmark;

import java.util.Comparator;

public class KeyFrameComparator implements Comparator<KeyFrame> {

	@Override
	public int compare(KeyFrame k1, KeyFrame k2) {

		return Integer.compare(k1.getTimepoint(), k2.getTimepoint());

	}

}
