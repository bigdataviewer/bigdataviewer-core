package creator;

import ij.IJ;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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

	static class ChooseXmlFileListener implements ActionListener
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
			while ( directory != null && !directory.exists() )
				directory = directory.getParentFile();

			final JFileChooser fc = new JFileChooser( directory );
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
				        final int i = s.lastIndexOf('.');
				        if (i > 0 &&  i < s.length() - 1) {
				            final String ext = s.substring(i+1).toLowerCase();
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
				if ( ! f.endsWith( ".xml" ) )
					f += ".xml";
				text.setText( f );
			}
		}
	}

	static int[][] parseResolutionsString( final String s )
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

	public static File createNewPartitionFile( final File xmlSequenceFile ) throws IOException
	{
		final String seqFilename = xmlSequenceFile.getAbsolutePath();
		if ( !seqFilename.endsWith( ".xml" ) )
			throw new IllegalArgumentException();
		final String baseFilename = seqFilename.substring( 0, seqFilename.length() - 4 );
		for ( int i = 0; i < Integer.MAX_VALUE; ++i )
		{
			final File hdf5File = new File( String.format( "%s-%d.h5", baseFilename, i ) );
			if ( ! hdf5File.exists() )
				if ( hdf5File.createNewFile() )
					return hdf5File;
		}
		throw new RuntimeException( "could not generate new partition filename" );
	}



	public static class ProgressListenerIJ implements ProgressListener
	{
		final double min;

		final double scale;

		public ProgressListenerIJ( final double min, final double max )
		{
			this.min = min;
			this.scale = 1.0 / ( max - min );
		}

		@Override
		public ProgressListener createSubTaskProgressListener( final double startCompletionRatio, final double endCompletionRatio )
		{
			return new ProgressListenerIJ( startCompletionRatio, endCompletionRatio );
		}

		@Override
		public ProgressListener createSubTaskProgressListener( final int taskToCompleteTasks, final int numTasks )
		{
			return createSubTaskProgressListener( ( double ) taskToCompleteTasks / numTasks, ( double ) ( taskToCompleteTasks + 1 ) / numTasks );
		}

		@Override
		public void updateProgress( final double completionRatio )
		{
			IJ.showProgress( min + scale * completionRatio );
		}

		@Override
		public void updateProgress( final int numCompletedTasks, final int numTasks )
		{
			updateProgress( ( double ) numCompletedTasks / numTasks );
		}

		@Override
		public void println( final String s )
		{
			IJ.log( s );
		}
	}

	public static class ProgressListenerSysOut implements ProgressListener
	{
		@Override
		public ProgressListener createSubTaskProgressListener( final double startCompletionRatio, final double endCompletionRatio )
		{
			return this;
		}

		@Override
		public ProgressListener createSubTaskProgressListener( final int taskToCompleteTasks, final int numTasks )
		{
			return this;
		}

		@Override
		public void updateProgress( final double completionRatio )
		{
		}

		@Override
		public void updateProgress( final int numCompletedTasks, final int numTasks )
		{
		}

		@Override
		public void println( final String s )
		{
			System.out.println( s );
		}
	}
}
