package bdv.ij;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import mpicbg.spim.data.SpimDataException;
import bdv.export.ProgressWriter;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class ResavePlugin implements PlugIn
{
	public static void main( final String[] args )
	{
		new ResavePlugin().run( null );
	}

	protected static class Parameters
	{
		final int[][] resolutions;

		final int[][] subdivisions;

		final File seqFile;

		final File hdf5File;

		public Parameters( final int[][] resolutions, final int[][] subdivisions, final File seqFile, final File hdf5File )
		{
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
		}
	}

	@Override
	public void run( final String arg )
	{
		final JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter( new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "xml files";
			}

			@Override
			public boolean accept( final File f )
			{
				if ( f.isDirectory() )
					return true;
				if ( f.isFile() )
				{
			        final String s = f.getName();
			        final int i = s.lastIndexOf('.');
			        if (i > 0 &&  i < s.length() - 1) {
			            final String ext = s.substring(i+1).toLowerCase();
			            return ext.equals( "xml" );
			        }
				}
				return false;
			}
		} );

		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			final File file = fileChooser.getSelectedFile();
			final XmlIoSpimDataMinimal io = new XmlIoSpimDataMinimal();
			SpimDataMinimal spimData;
			try
			{
				spimData = io.load( file.getAbsolutePath() );
			}
			catch ( final SpimDataException e )
			{
				throw new RuntimeException( e );
			}

			final Parameters params = getParameters();
			if ( params == null )
				return;

			final ProgressWriter progressWriter = new ProgressWriterIJ();
			progressWriter.out().println( "starting export..." );

			// write hdf5
			final File seqFile = params.seqFile;
			final File hdf5File = params.hdf5File;
			final int[][] resolutions = params.resolutions;
			final int[][] subdivisions = params.subdivisions;

			final SequenceDescriptionMinimal seq = spimData.getSequenceDescription();
			WriteSequenceToHdf5.writeHdf5File( seq, resolutions, subdivisions, hdf5File, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );

			// write xml sequence description
			final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, null, null, false );
			seq.setImgLoader( hdf5Loader );

			final File basePath = seqFile.getParentFile();
			final SpimDataMinimal spimData2 = new SpimDataMinimal( basePath, seq, spimData.getViewRegistrations() );

			try
			{
				io.save( spimData2, seqFile.getAbsolutePath() );
			}
			catch ( final SpimDataException e )
			{
				throw new RuntimeException( e );
			}
			progressWriter.setProgress( 1.0 );
			progressWriter.out().println( "done" );
		}
	}

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

	static String lastChunkSizes = "{16,16,16}, {16,16,16}, {16,16,16}";

	static String lastExportPath = "./export.xml";

	protected Parameters getParameters()
	{
		while ( true )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

			gd.addStringField( "Subsampling factors", lastSubsampling, 25 );
			gd.addStringField( "Hdf5 chunk sizes", lastChunkSizes, 25 );

			PluginHelper.addSaveAsFileField( gd, "Export path", lastExportPath, 25 );

			gd.showDialog();
			if ( gd.wasCanceled() )
				return null;

			lastSubsampling = gd.getNextString();
			lastChunkSizes = gd.getNextString();
			lastExportPath = gd.getNextString();

			// parse mipmap resolutions and cell sizes
			final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );
			if ( resolutions.length == 0 )
			{
				IJ.showMessage( "Cannot parse subsampling factors " + lastSubsampling );
				continue;
			}
			if ( subdivisions.length == 0 )
			{
				IJ.showMessage( "Cannot parse hdf5 chunk sizes " + lastChunkSizes );
				continue;
			}
			else if ( resolutions.length != subdivisions.length )
			{
				IJ.showMessage( "subsampling factors and hdf5 chunk sizes must have the same number of elements" );
				continue;
			}

			String seqFilename = lastExportPath;
			if ( !seqFilename.endsWith( ".xml" ) )
				seqFilename += ".xml";
			final File seqFile = new File( seqFilename );
			final File parent = seqFile.getParentFile();
			if ( parent == null || !parent.exists() || !parent.isDirectory() )
			{
				IJ.showMessage( "Invalid export filename " + seqFilename );
				continue;
			}
			final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
			final File hdf5File = new File( hdf5Filename );

			return new Parameters( resolutions, subdivisions, seqFile, hdf5File );
		}
	}
}
