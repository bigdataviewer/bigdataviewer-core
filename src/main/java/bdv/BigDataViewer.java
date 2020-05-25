/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package bdv;

import bdv.viewer.ConverterSetups;
import bdv.viewer.ViewerState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;

import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.cache.CacheControl;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.HelpDialog;
import bdv.tools.InitializeViewerState;
import bdv.tools.RecordMaxProjectionDialog;
import bdv.tools.RecordMovieDialog;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.bookmarks.Bookmarks;
import bdv.tools.bookmarks.BookmarksEditor;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.crop.CropDialog;
import bdv.tools.transformation.ManualTransformation;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.NavigationActions;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;

public class BigDataViewer
{
	protected final ViewerFrame viewerFrame;

	protected final ViewerPanel viewer;

	protected final SetupAssignments setupAssignments;

	protected final ManualTransformation manualTransformation;

	protected final Bookmarks bookmarks;

	protected final BrightnessDialog brightnessDialog;

	protected final CropDialog cropDialog;

	protected final RecordMovieDialog movieDialog;

	protected final RecordMaxProjectionDialog movieMaxProjectDialog;

	protected final VisibilityAndGroupingDialog activeSourcesDialog;

	protected final HelpDialog helpDialog;

	protected final ManualTransformationEditor manualTransformationEditor;

	protected final BookmarksEditor bookmarkEditor;

	protected final JFileChooser fileChooser;

	protected File proposedSettingsFile;

	public void toggleManualTransformation()
	{
		manualTransformationEditor.toggle();
	}

	public void initSetBookmark()
	{
		bookmarkEditor.initSetBookmark();
	}

	public void initGoToBookmark()
	{
		bookmarkEditor.initGoToBookmark();
	}

	public void initGoToBookmarkRotation()
	{
		bookmarkEditor.initGoToBookmarkRotation();
	}

	private static String createSetupName( final BasicViewSetup setup )
	{
		if ( setup.hasName() )
			return setup.getName();

		String name = "";

		final Angle angle = setup.getAttribute( Angle.class );
		if ( angle != null )
			name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

		final Channel channel = setup.getAttribute( Channel.class );
		if ( channel != null )
			name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

		return name;
	}

	/**
	 * Create standard converter from the given {@code type} to ARGB:
	 * <ul>
	 * <li>For {@code RealType}s a {@link RealARGBColorConverter} is
	 * returned.</li>
	 * <li>For {@code ARGBType}s a {@link ScaledARGBConverter.ARGB} is
	 * returned.</li>
	 * <li>For {@code VolatileARGBType}s a
	 * {@link ScaledARGBConverter.VolatileARGB} is returned.</li>
	 * </ul>
	 */
	@SuppressWarnings( "unchecked" )
	public static < T extends NumericType< T > > Converter< T, ARGBType > createConverterToARGB( final T type )
	{
		if ( type instanceof RealType )
		{
			final RealType< ? > t = ( RealType< ? > ) type;
			final double typeMin = Math.max( 0, Math.min( t.getMinValue(), 65535 ) );
			final double typeMax = Math.max( 0, Math.min( t.getMaxValue(), 65535 ) );
			return ( Converter< T, ARGBType > ) RealARGBColorConverter.create( t, typeMin, typeMax );
		}
		else if ( type instanceof ARGBType )
			return ( Converter< T, ARGBType > ) new ScaledARGBConverter.ARGB( 0, 255 );
		else if ( type instanceof VolatileARGBType )
			return ( Converter< T, ARGBType > ) new ScaledARGBConverter.VolatileARGB( 0, 255 );
		else
			throw new IllegalArgumentException( "ImgLoader of type " + type.getClass() + " not supported." );
	}

	/**
	 * Create a {@code ConverterSetup} for the given {@code SourceAndConverter}.
	 * {@link SourceAndConverter#asVolatile() Nested volatile}
	 * {@code SourceAndConverter} are added to the {@code ConverterSetup} if
	 * present. If {@code SourceAndConverter} does not comprise a
	 * {@code ColorConverter}, returns {@code null}.
	 *
	 * @param soc
	 *            {@code SourceAndConverter} for which to create a
	 *            {@code ConverterSetup}
	 * @param setupId
	 *            setupId of the created {@code ConverterSetup}
	 * @return a new {@code ConverterSetup} or {@code null}
	 */
	public static ConverterSetup createConverterSetup( final SourceAndConverter< ? > soc, final int setupId )
	{
		final List< ColorConverter > converters = new ArrayList<>();

		final Converter< ?, ARGBType > c = soc.getConverter();
		if ( c instanceof ColorConverter )
			converters.add( ( ColorConverter ) c );

		final SourceAndConverter< ? extends Volatile< ? > > vsoc = soc.asVolatile();
		if ( vsoc != null )
		{
			final Converter< ?, ARGBType > vc = vsoc.getConverter();
			if ( vc instanceof ColorConverter )
				converters.add( ( ColorConverter ) vc );
		}

		if ( converters.isEmpty() )
			return null;
		else
			return new RealARGBColorConverterSetup( setupId, converters );
	}

	/**
	 * Decorate source with an extra transformation, that can be edited manually
	 * in this viewer. {@link SourceAndConverter#asVolatile() Nested volatile}
	 * {@code SourceAndConverter} are wrapped as well, if present.
	 */
	public static < T, V extends Volatile< T > > SourceAndConverter< T > wrapWithTransformedSource( final SourceAndConverter< T > soc )
	{
		if ( soc.asVolatile() == null )
			return new SourceAndConverter<>( new TransformedSource<>( soc.getSpimSource() ), soc.getConverter() );

		@SuppressWarnings( "unchecked" )
		final SourceAndConverter< V > vsoc = ( SourceAndConverter< V > ) soc.asVolatile();
		final TransformedSource< T > ts = new TransformedSource<>( soc.getSpimSource() );
		final TransformedSource< V > vts = new TransformedSource<>( vsoc.getSpimSource(), ts );
		return new SourceAndConverter<>( ts, soc.getConverter(), new SourceAndConverter<>( vts, vsoc.getConverter() ) );
	}

	private static < T extends NumericType< T >, V extends Volatile< T > & NumericType< V > > void initSetupNumericType(
			final AbstractSpimData< ? > spimData,
			final BasicViewSetup setup,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		final int setupId = setup.getId();
		final ViewerImgLoader imgLoader = ( ViewerImgLoader ) spimData.getSequenceDescription().getImgLoader();
		@SuppressWarnings( "unchecked" )
		final ViewerSetupImgLoader< T, V > setupImgLoader = ( ViewerSetupImgLoader< T, V > ) imgLoader.getSetupImgLoader( setupId );
		final T type = setupImgLoader.getImageType();
		final V volatileType = setupImgLoader.getVolatileImageType();

		if ( ! ( type instanceof NumericType ) )
			throw new IllegalArgumentException( "ImgLoader of type " + type.getClass() + " not supported." );

		final String setupName = createSetupName( setup );

		SourceAndConverter< V > vsoc = null;
		if ( volatileType != null )
		{
			final VolatileSpimSource< V > vs = new VolatileSpimSource<>( spimData, setupId, setupName );
			vsoc = new SourceAndConverter<>( vs, createConverterToARGB( volatileType ) );
		}

		final SpimSource< T > s = new SpimSource<>( spimData, setupId, setupName );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( s, createConverterToARGB( type ), vsoc );
		final SourceAndConverter< T > tsoc = wrapWithTransformedSource( soc );
		sources.add( tsoc );

		final ConverterSetup converterSetup = createConverterSetup( tsoc, setupId );
		if ( converterSetup != null )
			converterSetups.add( converterSetup );
	}

	public static void initSetups(
			final AbstractSpimData< ? > spimData,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		for ( final BasicViewSetup setup : spimData.getSequenceDescription().getViewSetupsOrdered() )
			initSetupNumericType( spimData, setup, converterSetups, sources );
	}

	/**
	 *
	 * @param converterSetups
	 *            list of {@link ConverterSetup} that control min/max and color
	 *            of sources.
	 * @param sources
	 *            list of pairs of source of some type and converter from that
	 *            type to ARGB.
	 * @param spimData
	 *            may be null. The {@link AbstractSpimData} of the dataset (if
	 *            there is one). If it exists, it is used to set up a "Crop"
	 *            dialog.
	 * @param numTimepoints
	 *            the number of timepoints in the dataset.
	 * @param cache
	 *            handle to cache. This is used to control io timing.
	 * @param windowTitle
	 *            title of the viewer window.
	 * @param progressWriter
	 *            a {@link ProgressWriter} to which BDV may report progress
	 *            (currently only used in the "Record Movie" dialog).
	 * @param options
	 *            optional parameters.
	 */
	public BigDataViewer(
			final ArrayList< ConverterSetup > converterSetups,
			final ArrayList< SourceAndConverter< ? > > sources,
			final AbstractSpimData< ? > spimData,
			final int numTimepoints,
			final CacheControl cache,
			final String windowTitle,
			final ProgressWriter progressWriter,
			final ViewerOptions options )
	{
		final InputTriggerConfig inputTriggerConfig = getInputTriggerConfig( options );

		viewerFrame = new ViewerFrame( sources, numTimepoints, cache, options.inputTriggerConfig( inputTriggerConfig ) );
		if ( windowTitle != null )
			viewerFrame.setTitle( windowTitle );
		viewer = viewerFrame.getViewerPanel();

//		final ConverterSetup.SetupChangeListener requestRepaint = s -> viewer.requestRepaint();
//		for ( final ConverterSetup cs : converterSetups )
//			cs.setupChangeListeners().add( requestRepaint );

		manualTransformation = new ManualTransformation( viewer );
		manualTransformationEditor = new ManualTransformationEditor( viewer, viewerFrame.getKeybindings() );

		bookmarks = new Bookmarks();
		bookmarkEditor = new BookmarksEditor( viewer, viewerFrame.getKeybindings(), bookmarks );

		final ConverterSetups setups = viewerFrame.getConverterSetups();
		if ( converterSetups.size() != sources.size() )
			System.err.println( "WARNING! Constructing BigDataViewer, with converterSetups.size() that is not the same as sources.size()." );
		final int numSetups = Math.min( converterSetups.size(), sources.size() );
		for ( int i = 0; i < numSetups; ++i )
		{
			final SourceAndConverter< ? > source = sources.get( i );
			final ConverterSetup setup = converterSetups.get( i );
			if ( setup != null )
				setups.put( source, setup );
		}

		setupAssignments = new SetupAssignments( converterSetups, 0, 65535 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}

		brightnessDialog = new BrightnessDialog( viewerFrame, setupAssignments );

		if (spimData != null )
			viewer.getSourceInfoOverlayRenderer().setTimePointsOrdered( spimData.getSequenceDescription().getTimePoints().getTimePointsOrdered() );

		cropDialog = ( spimData == null ) ? null : new CropDialog( viewerFrame, viewer, spimData.getSequenceDescription() );

		movieDialog = new RecordMovieDialog( viewerFrame, viewer, progressWriter );
		// this is just to get updates of window size:
		viewer.getDisplay().addOverlayRenderer( movieDialog );

		movieMaxProjectDialog = new RecordMaxProjectionDialog( viewerFrame, viewer, progressWriter );
		// this is just to get updates of window size:
		viewer.getDisplay().addOverlayRenderer( movieMaxProjectDialog );

		activeSourcesDialog = new VisibilityAndGroupingDialog( viewerFrame, viewer.state() );

		helpDialog = new HelpDialog( viewerFrame );

		fileChooser = new JFileChooser();
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

		NavigationActions.installActionBindings( viewerFrame.getKeybindings(), viewer, inputTriggerConfig );
		BigDataViewerActions.installActionBindings( viewerFrame.getKeybindings(), this, inputTriggerConfig );

		final JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu( "File" );
		menubar.add( menu );

		final ActionMap actionMap = viewerFrame.getKeybindings().getConcatenatedActionMap();
		final JMenuItem miLoadSettings = new JMenuItem( actionMap.get( BigDataViewerActions.LOAD_SETTINGS ) );
		miLoadSettings.setText( "Load settings" );
		menu.add( miLoadSettings );

		final JMenuItem miSaveSettings = new JMenuItem( actionMap.get( BigDataViewerActions.SAVE_SETTINGS ) );
		miSaveSettings.setText( "Save settings" );
		menu.add( miSaveSettings );

		menu = new JMenu( "Settings" );
		menubar.add( menu );

		final JMenuItem miBrightness = new JMenuItem( actionMap.get( BigDataViewerActions.BRIGHTNESS_SETTINGS ) );
		miBrightness.setText( "Brightness & Color" );
		menu.add( miBrightness );

		final JMenuItem miVisibility = new JMenuItem( actionMap.get( BigDataViewerActions.VISIBILITY_AND_GROUPING ) );
		miVisibility.setText( "Visibility & Grouping" );
		menu.add( miVisibility );

		menu = new JMenu( "Tools" );
		menubar.add( menu );

		if ( cropDialog != null )
		{
			final JMenuItem miCrop = new JMenuItem( actionMap.get( BigDataViewerActions.CROP ) );
			miCrop.setText( "Crop" );
			menu.add( miCrop );
		}

		final JMenuItem miMovie = new JMenuItem( actionMap.get( BigDataViewerActions.RECORD_MOVIE ) );
		miMovie.setText( "Record Movie" );
		menu.add( miMovie );

		final JMenuItem miMaxProjectMovie = new JMenuItem( actionMap.get( BigDataViewerActions.RECORD_MAX_PROJECTION_MOVIE ) );
		miMaxProjectMovie.setText( "Record Max-Projection Movie" );
		menu.add( miMaxProjectMovie );

		final JMenuItem miManualTransform = new JMenuItem( actionMap.get( BigDataViewerActions.MANUAL_TRANSFORM ) );
		miManualTransform.setText( "Manual Transform" );
		menu.add( miManualTransform );

		menu = new JMenu( "Help" );
		menubar.add( menu );

		final JMenuItem miHelp = new JMenuItem( actionMap.get( BigDataViewerActions.SHOW_HELP ) );
		miHelp.setText( "Show Help" );
		menu.add( miHelp );

		viewerFrame.setJMenuBar( menubar );
	}

	public static BigDataViewer open( final AbstractSpimData< ? > spimData, final String windowTitle, final ProgressWriter progressWriter, final ViewerOptions options )
	{
		if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}

		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		initSetups( spimData, converterSetups, sources );

		final AbstractSequenceDescription< ?, ?, ? > seq = spimData.getSequenceDescription();
		final int numTimepoints = seq.getTimePoints().size();
		final CacheControl cache = ( ( ViewerImgLoader ) seq.getImgLoader() ).getCacheControl();

		final BigDataViewer bdv = new BigDataViewer( converterSetups, sources, spimData, numTimepoints, cache, windowTitle, progressWriter, options );

		WrapBasicImgLoader.removeWrapperIfPresent( spimData );

		bdv.viewerFrame.setVisible( true );
		InitializeViewerState.initTransform( bdv.viewer );
		return bdv;
	}

	public static BigDataViewer open( final String xmlFilename, final String windowTitle, final ProgressWriter progressWriter, final ViewerOptions options ) throws SpimDataException
	{
		final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( xmlFilename );
		final BigDataViewer bdv = open( spimData, windowTitle, progressWriter, options );
		if ( !bdv.tryLoadSettings( xmlFilename ) )
			InitializeViewerState.initBrightness( 0.001, 0.999, bdv.viewerFrame );
		return bdv;
	}

	public static BigDataViewer open(
			final ArrayList< ConverterSetup > converterSetups,
			final ArrayList< SourceAndConverter< ? > > sources,
			final int numTimepoints,
			final CacheControl cache,
			final String windowTitle,
			final ProgressWriter progressWriter,
			final ViewerOptions options )
	{
		final BigDataViewer bdv = new BigDataViewer( converterSetups, sources, null, numTimepoints, cache, windowTitle, progressWriter, options );
		bdv.viewerFrame.setVisible( true );
		InitializeViewerState.initTransform( bdv.viewer );
		return bdv;
	}

	public ViewerPanel getViewer()
	{
		return viewer;
	}

	public ViewerFrame getViewerFrame()
	{
		return viewerFrame;
	}

	public ConverterSetups getConverterSetups()
	{
		return viewerFrame.getConverterSetups();
	}

	/**
	 * @deprecated Instead {@code getViewer().state()} returns the {@link ViewerState} that can be modified directly.
	 */
	@Deprecated
	public SetupAssignments getSetupAssignments()
	{
		return setupAssignments;
	}

	public ManualTransformationEditor getManualTransformEditor()
	{
		return manualTransformationEditor;
	}

	public boolean tryLoadSettings( final String xmlFilename )
	{
		proposedSettingsFile = null;
		if( xmlFilename.startsWith( "http://" ) )
		{
			// load settings.xml from the BigDataServer
			final String settings = xmlFilename + "settings";
			{
				try
				{
					loadSettings( settings );
					return true;
				}
				catch ( final FileNotFoundException e )
				{}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		else if ( xmlFilename.endsWith( ".xml" ) )
		{
			final String settings = xmlFilename.substring( 0, xmlFilename.length() - ".xml".length() ) + ".settings" + ".xml";
			proposedSettingsFile = new File( settings );
			if ( proposedSettingsFile.isFile() )
			{
				try
				{
					loadSettings( settings );
					return true;
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public void saveSettings()
	{
		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showSaveDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				saveSettings( proposedSettingsFile.getCanonicalPath() );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	public void saveSettings( final String xmlFilename ) throws IOException
	{
		final Element root = new Element( "Settings" );
		root.addContent( viewer.stateToXml() );
		root.addContent( setupAssignments.toXml() );
		root.addContent( manualTransformation.toXml() );
		root.addContent( bookmarks.toXml() );
		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileWriter( xmlFilename ) );
	}

	/**
	 * If {@code options} doesn't define a {@link InputTriggerConfig}, try to
	 * load it from files in this order:
	 * <ol>
	 * <li>"bdvkeyconfig.yaml" in the current directory.
	 * <li>".bdv/bdvkeyconfig.yaml" in the user's home directory.
	 * <li>legacy "bigdataviewer.keys.properties" in current directory (will be
	 * also written to "bdvkeyconfig.yaml").
	 * </ol>
	 *
	 * @param options
	 * @return
	 */
	public static InputTriggerConfig getInputTriggerConfig( final ViewerOptions options )
	{
		InputTriggerConfig conf = options.values.getInputTriggerConfig();

		// try "bdvkeyconfig.yaml" in current directory
		if ( conf == null && new File( "bdvkeyconfig.yaml" ).isFile() )
		{
			try
			{
				conf = new InputTriggerConfig( YamlConfigIO.read( "bdvkeyconfig.yaml" ) );
			}
			catch ( final IOException e )
			{}
		}

		// try "~/.bdv/bdvkeyconfig.yaml"
		if ( conf == null )
		{
			final String fn = System.getProperty( "user.home" ) + "/.bdv/bdvkeyconfig.yaml";
			if ( new File( fn ).isFile() )
			{
				try
				{
					conf = new InputTriggerConfig( YamlConfigIO.read( fn ) );
				}
				catch ( final IOException e )
				{}
			}
		}

		if ( conf == null )
		{
			conf = new InputTriggerConfig();
		}

		return conf;
	}

	public void loadSettings()
	{
		fileChooser.setSelectedFile( proposedSettingsFile );
		final int returnVal = fileChooser.showOpenDialog( null );
		if ( returnVal == JFileChooser.APPROVE_OPTION )
		{
			proposedSettingsFile = fileChooser.getSelectedFile();
			try
			{
				loadSettings( proposedSettingsFile.getCanonicalPath() );
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	public void loadSettings( final String xmlFilename ) throws IOException, JDOMException
	{
		final SAXBuilder sax = new SAXBuilder();
		final Document doc = sax.build( xmlFilename );
		final Element root = doc.getRootElement();
		viewer.stateFromXml( root );
		setupAssignments.restoreFromXml( root );
		manualTransformation.restoreFromXml( root );
		bookmarks.restoreFromXml( root );
		activeSourcesDialog.update();
		viewer.requestRepaint();
	}

	public void expandAndFocusCardPanel()
	{
		viewerFrame.getSplitPanel().setCollapsed( false );
		viewerFrame.getSplitPanel().getRightComponent().requestFocusInWindow();
	}

	public void collapseCardPanel()
	{
		viewerFrame.getSplitPanel().setCollapsed( true );
		viewer.requestFocusInWindow();
	}

	public static void main( final String[] args )
	{
//		final String fn = "http://tomancak-mac-17.mpi-cbg.de:8080/openspim/";
//		final String fn = "/Users/Pietzsch/Desktop/openspim/datasetHDF.xml";
//		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
//		final String fn = "/Users/Pietzsch/Desktop/spimrec2/dataset.xml";
//		final String fn = "/Users/pietzsch/Desktop/HisYFP-SPIM/dataset.xml";
//		final String fn = "/Users/Pietzsch/Desktop/bdv example/drosophila 2.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/clusterValia/140219-1/valia-140219-1.xml";
//		final String fn = "/Users/Pietzsch/Desktop/data/catmaid.xml";
//		final String fn = "src/main/resources/openconnectome-bock11-neariso.xml";
//		final String fn = "/home/saalfeld/catmaid.xml";
//		final String fn = "/home/saalfeld/catmaid-fafb00-v9.xml";
//		final String fn = "/home/saalfeld/catmaid-fafb00-sample_A_cutout_3k.xml";
//		final String fn = "/home/saalfeld/catmaid-thorsten.xml";
//		final String fn = "/home/saalfeld/knossos-example.xml";
//		final String fn = "/Users/Pietzsch/Desktop/data/catmaid-confocal.xml";
//		final String fn = "/Users/pietzsch/desktop/data/BDV130418A325/BDV130418A325_NoTempReg.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/valia2/valia.xml";
//		final String fn = "/Users/pietzsch/workspace/data/fast fly/111010_weber/combined.xml";
//		final String fn = "/Users/pietzsch/workspace/data/mette/mette.xml";
//		final String fn = "/Users/tobias/Desktop/openspim.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/fibsem.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/fibsem-remote.xml";
//		final String fn = "/Users/pietzsch/Desktop/url-valia.xml";
//		final String fn = "/Users/pietzsch/Desktop/data/clusterValia/140219-1/valia-140219-1.xml";
		final String fn = "/Users/pietzsch/workspace/data/111010_weber_full.xml";
//		final String fn = "/Volumes/projects/tomancak_lightsheet/Mette/ZeissZ1SPIM/Maritigrella/021013_McH2BsGFP_CAAX-mCherry/11-use/hdf5/021013_McH2BsGFP_CAAX-mCherry-11-use.xml";
		try
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "true" );

			final BigDataViewer bdv = open( fn, new File( fn ).getName(), new ProgressWriterConsole(), ViewerOptions.options() );

//			DumpInputConfig.writeToYaml( System.getProperty( "user.home" ) + "/.bdv/bdvkeyconfig.yaml", bdv.getViewerFrame() );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
