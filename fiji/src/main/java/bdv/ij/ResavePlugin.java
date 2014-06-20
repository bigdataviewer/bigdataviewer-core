package bdv.ij;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.Checkbox;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import bdv.export.ExportMipmapInfo;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
import bdv.ij.util.PluginHelper;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;

public class ResavePlugin implements PlugIn
{
	public static void main( final String[] args )
	{
		new ResavePlugin().run( null );
	}

	public static class Parameters
	{
		boolean setMipmapManual;
		int[][] resolutions;
		int[][] subdivisions;
		File seqFile;
		File hdf5File;

		public Parameters( final boolean setMipmapManual, final int[][] resolutions, final int[][] subdivisions, final File seqFile, final File hdf5File )
		{
			this.setMipmapManual = setMipmapManual;
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
		}
		
		public void setSeqFile( final File seqFile ) { this.seqFile = seqFile; }
		public void setHDF5File( final File hdf5File ) { this.hdf5File = hdf5File; }
		public void setResolutions( final int[][] resolutions ) { this.resolutions = resolutions; }
		public void setSubdivisions( final int[][] subdivisions ) { this.subdivisions = subdivisions; }
		public void setMipmapManual( final boolean setMipmapManual ) { this.setMipmapManual = setMipmapManual; }
		
		public File getSeqFile() { return seqFile; }
		public File getHDF5File() { return hdf5File; }
		public int[][] getResolutions() { return resolutions; }
		public int[][] getSubdivisions() { return subdivisions; }
		public boolean getMipmapManual() { return setMipmapManual; }
	}

	@Override
	public void run( final String arg )
	{
		final File file = getInputXML();
		if ( file == null )
			return;
		
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

		final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo = ProposeMipmaps.proposeMipmaps( spimData.getSequenceDescription() );

		final Parameters params = getParameters( perSetupExportMipmapInfo.get( 0 ), true );
		if ( params == null )
			return;

		final ProgressWriter progressWriter = new ProgressWriterIJ();
		progressWriter.out().println( "starting export..." );

		// write hdf5
		writeHDF5( spimData.getSequenceDescription(), params, perSetupExportMipmapInfo, progressWriter );
		
		// write xml sequence description
		writeXML( spimData, io, params.seqFile, params.hdf5File, progressWriter );
	}
	
	public static void writeHDF5( final AbstractSequenceDescription< ?, ?, ? > seq, final Parameters params, final Map< Integer, ExportMipmapInfo > perSetupExportMipmapInfo, final ProgressWriter progressWriter )
	{
		final boolean setMipmapManual = params.setMipmapManual;
		final int[][] manualResolutions = params.resolutions;
		final int[][] manualSubdivisions = params.subdivisions;

		if ( setMipmapManual )
			WriteSequenceToHdf5.writeHdf5File( seq, manualResolutions, manualSubdivisions, params.hdf5File, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
		else
			WriteSequenceToHdf5.writeHdf5File( seq, perSetupExportMipmapInfo, params.hdf5File, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
	}
	
	public static < T extends AbstractSpimData< A >, A extends AbstractSequenceDescription< ?, ?, BasicImgLoader< ? > > > void writeXML(
			final T spimData,
			final XmlIoAbstractSpimData< A, T > io,
			final File seqFile,
			final File hdf5File,
			final ProgressWriter progressWriter )
	{
		final A seq = spimData.getSequenceDescription();
		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, null, null, false );
		seq.setImgLoader( hdf5Loader );
		spimData.setBasePath( seqFile.getParentFile() );

		try
		{
			io.save( spimData, seqFile.getAbsolutePath() );
		}
		catch ( final SpimDataException e )
		{
			throw new RuntimeException( e );
		}
		
		progressWriter.setProgress( 1.0 );
		progressWriter.out().println( "done" );
	}

	static boolean lastSetMipmapManual = false;

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,2}";

	static String lastChunkSizes = "{16,16,16}, {16,16,16}, {16,16,16}";

	static String lastExportPath = "/Users/pietzsch/Desktop/spimrec2.xml";

	public static File getInputXML()
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

		if ( fileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
			return fileChooser.getSelectedFile();
		else
			return null;
	}
	
	public static Parameters getParameters( final ExportMipmapInfo autoMipmapSettings, final boolean askForXMLPath )
	{
		while ( true )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "Export for BigDataViewer" );

			gd.addCheckbox( "manual mipmap setup", lastSetMipmapManual );
			final Checkbox cManualMipmap = ( Checkbox ) gd.getCheckboxes().lastElement();
			gd.addStringField( "Subsampling factors", lastSubsampling, 25 );
			final TextField tfSubsampling = ( TextField ) gd.getStringFields().lastElement();
			gd.addStringField( "Hdf5 chunk sizes", lastChunkSizes, 25 );
			final TextField tfChunkSizes = ( TextField ) gd.getStringFields().lastElement();

			if ( askForXMLPath )
				PluginHelper.addSaveAsFileField( gd, "Export path", lastExportPath, 25 );

			final String autoSubsampling = ProposeMipmaps.getArrayString( autoMipmapSettings.getExportResolutions() );
			final String autoChunkSizes = ProposeMipmaps.getArrayString( autoMipmapSettings.getSubdivisions() );
			gd.addDialogListener( new DialogListener()
			{
				@Override
				public boolean dialogItemChanged( final GenericDialog dialog, final AWTEvent e )
				{
					if ( e instanceof ItemEvent && e.getID() == ItemEvent.ITEM_STATE_CHANGED && e.getSource() == cManualMipmap )
					{
						final boolean useManual = cManualMipmap.getState();
						tfSubsampling.setEnabled( useManual );
						tfChunkSizes.setEnabled( useManual );
						if ( !useManual )
						{
							tfSubsampling.setText( autoSubsampling );
							tfChunkSizes.setText( autoChunkSizes );
						}
					}
					return true;
				}
			} );

			tfSubsampling.setEnabled( lastSetMipmapManual );
			tfChunkSizes.setEnabled( lastSetMipmapManual );
			if ( !lastSetMipmapManual )
			{
				tfSubsampling.setText( autoSubsampling );
				tfChunkSizes.setText( autoChunkSizes );
			}

			gd.showDialog();
			if ( gd.wasCanceled() )
				return null;

			lastSetMipmapManual = gd.getNextBoolean();
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

			final File seqFile, hdf5File;

			if ( askForXMLPath )
			{
				String seqFilename = lastExportPath;
				if ( !seqFilename.endsWith( ".xml" ) )
					seqFilename += ".xml";
				seqFile = new File( seqFilename );
				final File parent = seqFile.getParentFile();
				if ( parent == null || !parent.exists() || !parent.isDirectory() )
				{
					IJ.showMessage( "Invalid export filename " + seqFilename );
					continue;
				}
				final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
				hdf5File = new File( hdf5Filename );
			}
			else
			{
				seqFile = hdf5File = null;
			}

			return new Parameters( lastSetMipmapManual, resolutions, subdivisions, seqFile, hdf5File );
		}
	}
}
