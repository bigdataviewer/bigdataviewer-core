package creator;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import creator.tiles.CellVoyagerDataExporter;

public class ExportCellVoyagerPlugIn implements PlugIn
{

	protected static class Parameters
	{
		final int[][] resolutions;

		final int[][] subdivisions;

		final File seqFile;

		final File hdf5File;

		final File sourceFolder;

		public Parameters( final int[][] resolutions, final int[][] subdivisions, final File sourceFile, final File seqFile, final File hdf5File )
		{
			this.resolutions = resolutions;
			this.subdivisions = subdivisions;
			this.sourceFolder = sourceFile;
			this.seqFile = seqFile;
			this.hdf5File = hdf5File;
		}
	}

	static String lastSubsampling = "{1,1,1}, {2,2,1}, {4,4,1}, {8,8,2}";

	static String lastChunkSizes = "{32,32,4}, {16,16,8}, {8,8,4}, {16, 16, 16}";

	static String lastExportPath;

	static String sourceFolderStr;

	@Override
	public void run( final String arg )
	{

		final Parameters params = getParameters( arg );
		if ( params == null )
			return;

		final File measurementFolder = params.sourceFolder;
		final File imageIndexFile = new File( measurementFolder, "ImageIndex.xml" );
		final File measurementSettingFile = new File( measurementFolder, "MeasurementSetting.xml" );

		final CellVoyagerDataExporter exporter = new CellVoyagerDataExporter( measurementSettingFile, imageIndexFile );
		final ProgressListener progressListener = new PluginHelper.ProgressListenerIJ( 0, 1 );
		exporter.export( params.seqFile, params.hdf5File, params.resolutions, params.subdivisions, progressListener );

	}

	protected Parameters getParameters( final String sourcePathStr )
	{
		while ( true )
		{
			final GenericDialogPlus gd = new GenericDialogPlus( "CellVoyager Import" );

			gd.addStringField( "Subsampling factors", lastSubsampling, 25 );
			gd.addStringField( "Hdf5 chunk sizes", lastChunkSizes, 25 );
			gd.addMessage( "" );

			if ( null == sourcePathStr )
			{
				if ( null == sourceFolderStr )
				{
					final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
					sourceFolderStr = folder.getAbsolutePath();
				}
			}
			else
			{
				sourceFolderStr = sourcePathStr;
			}
			addBrowseToCellVoyagerFolder( gd, "Measurement folder", sourceFolderStr, 25 );

			if ( null == lastExportPath )
			{
				final File sourceFile = new File( sourceFolderStr );
				final String parentFolder = sourceFile.getParent();
				lastExportPath = new File( parentFolder, "export.xml" ).getAbsolutePath();
			}
			PluginHelper.addSaveAsFileField( gd, "Export to", lastExportPath, 25 );

			gd.showDialog();
			if ( gd.wasCanceled() )
				return null;

			lastSubsampling = gd.getNextString();
			lastChunkSizes = gd.getNextString();
			sourceFolderStr = gd.getNextString();
			lastExportPath = gd.getNextString();

			// parse mipmap resolutions and cell sizes
			final int[][] resolutions = PluginHelper.parseResolutionsString( lastSubsampling );
			final int[][] subdivisions = PluginHelper.parseResolutionsString( lastChunkSizes );
			if ( resolutions.length == 0 )
			{
				IJ.showMessage( "Cannot parse subsampling factors: " + lastSubsampling );
				continue;
			}
			if ( subdivisions.length == 0 )
			{
				IJ.showMessage( "Cannot parse hdf5 chunk sizes: " + lastChunkSizes );
				continue;
			}
			else if ( resolutions.length != subdivisions.length )
			{
				IJ.showMessage( "Subsampling factors and hdf5 chunk sizes must have the same number of elements." );
				continue;
			}

			final File sourceFolder = new File( sourceFolderStr );

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

			return new Parameters( resolutions, subdivisions, sourceFolder, seqFile, hdf5File );
		}
	}

	/*
	 * STATIC METHODS
	 */

	public static void addBrowseToCellVoyagerFolder( final GenericDialogPlus dialog, final String label, final String defaultPath, final int columns )
	{
		dialog.addStringField( label, defaultPath, columns );

		final TextField text = ( TextField ) dialog.getStringFields().lastElement();
		final GridBagLayout layout = ( GridBagLayout ) dialog.getLayout();
		final GridBagConstraints constraints = layout.getConstraints( text );

		final Button button = new Button( "Browse..." );
		final ChooseCellVoyagerDirListener listener = new ChooseCellVoyagerDirListener( text );
		button.addActionListener( listener );
		button.addKeyListener( dialog );

		final Panel panel = new Panel();
		panel.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
		panel.add( text );
		panel.add( button );

		layout.setConstraints( panel, constraints );
		dialog.add( panel );
	}

	/*
	 * STATIC CLASSES
	 */

	private static class ChooseCellVoyagerDirListener implements ActionListener
	{
		TextField text;

		public ChooseCellVoyagerDirListener( final TextField text )
		{
			this.text = text;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			File directory = new File( text.getText() );
			while ( directory != null && !directory.exists() )
				directory = directory.getParentFile();

			final JFileChooser fc = new JFileChooser( directory );
			fc.setFileFilter( new FileFilter()
			{
				@Override
				public String getDescription()
				{
					return "CellVoyager measurement folder";
				}

				@Override
				public boolean accept( final File f )
				{
					if ( f.isDirectory() )
					{
						final File[] files = f.listFiles( new FilenameFilter()
						{

							@Override
							public boolean accept( final File dir, final String name )
							{
								return name.equals( "MeasurementSetting.xml" );
							}
						} );
						return files.length > 0;
					}
					else
					{
						return false;
					}
				}
			} );

			fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );

			final int returnVal = fc.showOpenDialog( null );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
			{
				final String f = fc.getSelectedFile().getAbsolutePath();
				text.setText( f );
			}
		}
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		final ExportCellVoyagerPlugIn plugin = new ExportCellVoyagerPlugIn();
		final File file = new File( "/Users/tinevez/Desktop/Data/1_7_6_1_2/20130703T145244/" );
		plugin.run( file.getAbsolutePath() );
	}
}
