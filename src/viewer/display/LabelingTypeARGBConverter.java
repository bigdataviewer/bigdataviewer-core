package viewer.display;

import java.util.HashMap;
import java.util.List;

import net.imglib2.converter.Converter;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

public class LabelingTypeARGBConverter< L extends Comparable< L >> implements Converter< LabelingType< L >, ARGBType >
{
	private volatile HashMap< List< L >, ARGBType > colorTable;

	public LabelingTypeARGBConverter( final HashMap< List< L >, ARGBType > colorTable )
	{
		this.colorTable = colorTable;
	}

	@Override
	public void convert( final LabelingType< L > input, final ARGBType output )
	{
		final ARGBType t = colorTable.get( input.getLabeling() );
		if ( t != null )
			output.set( t );
	}

	public void setColorTable( final HashMap< List< L >, ARGBType > colorTable )
	{
		this.colorTable = colorTable;
	}
}
