package bdv.viewer.state.r;

import bdv.viewer.SourceAndConverter;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class ViewerStateTest
{
	@Test
	public void addRemoveSource()
	{
		final DefaultViewerState state = new DefaultViewerState();
		final SourceAndConverter< ? > s = new SourceAndConverter<>( null, null );

		state.getSources().add( s );
		Assert.assertEquals( state.getSources(), Collections.singletonList( s ) );

		state.getSources().remove( s );
		Assert.assertEquals( state.getSources(), Collections.emptyList() );
	}
}
