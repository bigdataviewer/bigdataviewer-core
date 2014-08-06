package bdv.ij.util;

import ij.Prefs;

import java.awt.Button;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import fiji.util.gui.GenericDialogPlus;

public class PluginHelper
{
	public static void addSaveAsFileField( final GenericDialogPlus dialog, final String label, final String defaultPath, final int columns) {
		dialog.addStringField( label, defaultPath, columns );

		final TextField text = ( TextField ) dialog.getStringFields().lastElement();
		final GridBagLayout layout = ( GridBagLayout ) dialog.getLayout();
		final GridBagConstraints constraints = layout.getConstraints( text );

		final Button button = new Button( "Browse..." );
		final ChooseXmlFileListener listener = new ChooseXmlFileListener( text );
		button.addActionListener( listener );
		button.addKeyListener( dialog );

		final Panel panel = new Panel();
		panel.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
		panel.add( text );
		panel.add( button );

		layout.setConstraints( panel, constraints );
		dialog.add( panel );
	}

	public static boolean useFileDialog = true;

	public static class ChooseXmlFileListener implements ActionListener
	{
		TextField text;

		public ChooseXmlFileListener( final TextField text )
		{
			this.text = text;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			File directory = new File( text.getText() );
			final String fn = directory.getName();
			while ( directory != null && !directory.exists() )
				directory = directory.getParentFile();

			if ( Prefs.useJFileChooser )
			{
				final JFileChooser fc = new JFileChooser( directory );
				fc.setSelectedFile( new File( fn ) );
				fc.setFileFilter( new FileFilter()
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
							final int i = s.lastIndexOf( '.' );
							if ( i > 0 && i < s.length() - 1 )
							{
								final String ext = s.substring( i + 1 ).toLowerCase();
								return ext.equals( "xml" );
							}
						}
						return false;
					}
				} );

				fc.setFileSelectionMode( JFileChooser.FILES_ONLY );

				final int returnVal = fc.showSaveDialog( null );
				if ( returnVal == JFileChooser.APPROVE_OPTION )
				{
					String f = fc.getSelectedFile().getAbsolutePath();
					if ( !f.endsWith( ".xml" ) )
						f += ".xml";
					text.setText( f );
				}
			}
			else // use FileDialog
			{
				final FileDialog fd = new FileDialog( ( Frame ) null, "Save", FileDialog.SAVE );
				fd.setDirectory( directory.getAbsolutePath() );
				fd.setFile( fn );
				fd.setFilenameFilter( new FilenameFilter()
				{
					@Override
					public boolean accept( final File dir, final String name )
					{
						final int i = name.lastIndexOf( '.' );
						if ( i > 0 && i < name.length() - 1 )
						{
							final String ext = name.substring( i + 1 ).toLowerCase();
							return ext.equals( "xml" );
						}
						return false;
					}
				} );
				fd.setVisible( true );
				final String filename = fd.getFile();
				if ( filename != null )
				{
					String f = new File( fd.getDirectory() + filename ).getAbsolutePath();
					if ( !f.endsWith( ".xml" ) )
						f += ".xml";
					text.setText( f );
				}
			}
		}
	}

	public static int[][] parseResolutionsString( final String s )
	{
		final String regex = "\\{\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\}";
		final Pattern pattern = Pattern.compile( regex );
		final Matcher matcher = pattern.matcher( s );

		final ArrayList< int[] > tmp = new ArrayList< int[] >();
		while ( matcher.find() )
		{
			final int[] resolution = new int[] { Integer.parseInt( matcher.group( 1 ) ), Integer.parseInt( matcher.group( 2 ) ), Integer.parseInt( matcher.group( 3 ) ) };
			tmp.add( resolution );
		}
		final int[][] resolutions = new int[ tmp.size() ][];
		for ( int i = 0; i < resolutions.length; ++i )
			resolutions[ i ] = tmp.get( i );

		return resolutions;
	}

	public static File createNewPartitionFile( final String baseFilename ) throws IOException
	{
		File hdf5File = new File( String.format( "%s.h5", baseFilename ) );
		if ( ! hdf5File.exists() )
			if ( hdf5File.createNewFile() )
				return hdf5File;

		for ( int i = 0; i < Integer.MAX_VALUE; ++i )
		{
			hdf5File = new File( String.format( "%s-%d.h5", baseFilename, i ) );
			if ( ! hdf5File.exists() )
				if ( hdf5File.createNewFile() )
					return hdf5File;
		}

		throw new RuntimeException( "could not generate new partition filename" );
	}
}
