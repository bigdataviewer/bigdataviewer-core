package viewer.display;

import java.util.HashMap;
import java.util.List;

import net.imglib2.converter.Converter;
import net.imglib2.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;

public class LabelingTypeARGBConverter< L extends Comparable< L >> implements Converter< LabelingType< L >, ARGBType >
{
	private HashMap< List< L >, ARGBType > colorTable;

	public LabelingTypeARGBConverter( final HashMap< List< L >, ARGBType > colorTable )
	{
		this.colorTable = colorTable;
	}

	@Override
	public void convert( final LabelingType< L > input, final ARGBType output )
	{
		output.set( colorTable.get( input.getLabeling() ) );
	}

	public void setColorTable( final HashMap< List< L >, ARGBType > colorTable )
	{
		this.colorTable = colorTable;
	}
}
