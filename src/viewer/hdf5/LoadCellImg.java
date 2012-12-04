package viewer.hdf5;

import ij.ImageJ;

import java.io.File;

import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import viewer.hdf5.img.Hdf5Cell;
import viewer.hdf5.img.Hdf5CellImgFactory;

public class LoadCellImg
{
	public static void main( final String[] args )
	{
		final File hdf5CellsFile = new File( "/home/tobias/Desktop/cells.h5" );
		if ( ! hdf5CellsFile.exists() )
			throw new RuntimeException( "cells file not found" );
		final CellImg< UnsignedShortType, ShortArray, Hdf5Cell< ShortArray > > img = Hdf5CellImgFactory.createUnsignedShort( hdf5CellsFile );
		new ImageJ();
		ImageJFunctions.show( img );
	}
}
