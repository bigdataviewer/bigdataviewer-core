package viewer.hdf5;

import ij.ImageJ;

import java.io.File;

import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5CellImgFactory;

public class LoadCellImg
{
	public static void main( final String[] args )
	{
		final File hdf5CellsFile = new File( "/home/tobias/Desktop/cells.h5" );
		if ( ! hdf5CellsFile.exists() )
			throw new RuntimeException( "cells file not found" );
		final CellImg< FloatType, FloatArray, Hdf5Cell< FloatArray > > img = Hdf5CellImgFactory.create( hdf5CellsFile );
		new ImageJ();
		ImageJFunctions.show( img );
	}
}
