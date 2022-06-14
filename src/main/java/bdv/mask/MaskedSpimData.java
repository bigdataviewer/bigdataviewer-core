package bdv.mask;

import bdv.ViewerImgLoader;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;

public class MaskedSpimData
{
	/**
	 * Wrap the {@link ViewerImgLoader image loader} of {@code spimData} with a
	 * {@link MaskedViewerImgLoader}, unless it is already wrapped.
	 *
	 * @return {@code true} if image loader was replaced with wrapper.
	 */
	public static boolean wrapImgLoaderIfNecessary( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader seqImgLoader = seq.getImgLoader();
		if ( !( seqImgLoader instanceof ViewerImgLoader ) )
			throw new IllegalArgumentException( "expected ViewerImgLoader" );

		final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seqImgLoader;
		if ( imgLoader instanceof MaskedViewerImgLoader )
			return false;

		setImgLoader( seq, new MaskedViewerImgLoader( imgLoader ) );
		return true;
	}

	public static boolean removeWrapperIfPresent( final AbstractSpimData< ? > spimData )
	{
		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final BasicImgLoader imgLoader = seq.getImgLoader();
		if ( imgLoader instanceof MaskedViewerImgLoader )
		{
			setImgLoader( seq, ( ( MaskedViewerImgLoader ) imgLoader ).getWrappedImgLoader() );
			return true;
		}
		else
			return false;
	}

	@SuppressWarnings( "unchecked" )
	private static < L extends BasicImgLoader > void setImgLoader(
			final AbstractSequenceDescription< ?, ?, L > seq,
			final BasicImgLoader newLoader )
	{
		seq.setImgLoader( ( L ) newLoader );
	}
}
