package bdv.tools.bookmarks.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bdv.MaxLengthTextDocument;
import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import bdv.tools.bookmarks.editor.BookmarksEditor;

public class BookmarkCellPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private final static Color ACTIVE_COLOR = Color.decode( "#6495ED" );

	private final Bookmark bookmark;

	private final BookmarksEditor bookmarksEditor;

	private final JLabel lblKey;

	private final JLabel typeLabel;

	private final JTextField keyField;

	private final JTextField titleField;

	private final JButton selectButton;

	private final JButton removeButton;

	private boolean isActive = false;

	private JTextArea descriptionTextArea;

	private JScrollPane scrollPane;

	private JLabel lblDescription;

	BookmarkCellPanel( Bookmark bookmark, BookmarksEditor bookmarksEditor )
	{
		this.bookmark = bookmark;
		this.bookmarksEditor = bookmarksEditor;

		setMinimumSize( new Dimension( 300, 125 ) );
		setMaximumSize( new Dimension( 2147483647, 125 ) );
		setPreferredSize( new Dimension( 267, 126 ) );

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 1, 95, 0 };
		gridBagLayout.rowHeights = new int[] { 1, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		JPanel panelInfo = new JPanel();
		GridBagConstraints gbc_panelInfo = new GridBagConstraints();
		gbc_panelInfo.anchor = GridBagConstraints.NORTH;
		gbc_panelInfo.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelInfo.insets = new Insets( 5, 5, 5, 5 );
		gbc_panelInfo.gridx = 0;
		gbc_panelInfo.gridy = 0;
		add( panelInfo, gbc_panelInfo );
		GridBagLayout gbl_panelInfo = new GridBagLayout();
		gbl_panelInfo.columnWidths = new int[] { 100, 30, 0 };
		gbl_panelInfo.rowHeights = new int[] { 0, 0, 0, 50 };
		gbl_panelInfo.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_panelInfo.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0 };
		panelInfo.setLayout( gbl_panelInfo );

		typeLabel = new JLabel( "Bookmark type" );
		GridBagConstraints gbc_typeLabel = new GridBagConstraints();
		gbc_typeLabel.anchor = GridBagConstraints.WEST;
		gbc_typeLabel.insets = new Insets( 0, 0, 5, 5 );
		gbc_typeLabel.gridx = 0;
		gbc_typeLabel.gridy = 0;
		panelInfo.add( typeLabel, gbc_typeLabel );

		lblKey = new JLabel( "Key" );
		GridBagConstraints gbc_lblKey = new GridBagConstraints();
		gbc_lblKey.anchor = GridBagConstraints.WEST;
		gbc_lblKey.insets = new Insets( 0, 0, 5, 0 );
		gbc_lblKey.gridx = 1;
		gbc_lblKey.gridy = 0;
		panelInfo.add( lblKey, gbc_lblKey );

		titleField = new JTextField();
		titleField.setText( ( String ) null );
		titleField.setColumns( 10 );
		GridBagConstraints gbc_titleField = new GridBagConstraints();
		gbc_titleField.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleField.insets = new Insets( 0, 0, 5, 5 );
		gbc_titleField.gridx = 0;
		gbc_titleField.gridy = 1;
		panelInfo.add( titleField, gbc_titleField );

		keyField = new JTextField();
		keyField.setMaximumSize( new Dimension( 30, 2147483647 ) );
		GridBagConstraints gbc_txtKey = new GridBagConstraints();
		gbc_txtKey.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtKey.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtKey.gridx = 1;
		gbc_txtKey.gridy = 1;
		panelInfo.add( keyField, gbc_txtKey );
		keyField.setColumns( 10 );
		keyField.setDocument( new MaxLengthTextDocument( 1 ) );
		keyField.addFocusListener( new FocusAdapter()
		{

			@Override
			public void focusLost( FocusEvent e )
			{
				if ( keyField.getText().length() == 0 )
					keyField.setText( bookmark.getKey() );
			}
		} );

		lblDescription = new JLabel( "Description" );
		GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.anchor = GridBagConstraints.WEST;
		gbc_lblDescription.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 2;
		panelInfo.add( lblDescription, gbc_lblDescription );

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridwidth = 2;
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 3;
		panelInfo.add( scrollPane, gbc_scrollPane );

		descriptionTextArea = new JTextArea();
		scrollPane.setViewportView( descriptionTextArea );
		descriptionTextArea.setText( ( String ) null );
		descriptionTextArea.setLineWrap( true );
		descriptionTextArea.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null );
		descriptionTextArea.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null );

		JPanel panelButton = new JPanel();
		GridBagConstraints gbc_panelButton = new GridBagConstraints();
		gbc_panelButton.insets = new Insets( 5, 5, 5, 5 );
		gbc_panelButton.anchor = GridBagConstraints.NORTHEAST;
		gbc_panelButton.gridx = 1;
		gbc_panelButton.gridy = 0;
		add( panelButton, gbc_panelButton );
		panelButton.setLayout( new BoxLayout( panelButton, BoxLayout.Y_AXIS ) );

		selectButton = new JButton( "Show" );
		selectButton.setMinimumSize( new Dimension( 95, 23 ) );
		selectButton.setMaximumSize( new Dimension( 95, 23 ) );
		selectButton.setPreferredSize( new Dimension( 95, 23 ) );
		panelButton.add( selectButton );
		selectButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( bookmark != null )
				{
					if ( isActive )
					{
						bookmarksEditor.deselectBookmark();
						if ( bookmark instanceof DynamicBookmark )
							selectButton.setText( "Select" );
					}
					else
					{
						bookmarksEditor.recallTransformationOfBookmark( bookmark.getKey() );
						if ( bookmark instanceof DynamicBookmark )
							selectButton.setText( "Deselect" );
					}
				}
			}
		} );

		panelButton.add( Box.createRigidArea( new Dimension( 5, 5 ) ) );

		removeButton = new JButton( "Remove" );
		removeButton.setPreferredSize( new Dimension( 95, 23 ) );
		removeButton.setMaximumSize( new Dimension( 95, 23 ) );
		removeButton.setMinimumSize( new Dimension( 95, 23 ) );
		panelButton.add( removeButton );
		removeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( bookmark != null )
				{
					bookmarksEditor.removeBookmark( bookmark.getKey() );
					setActive( false );
				}
			}
		} );

		displayBookmarkInfo();
		addBookmarkInfoDocumentListener();
	}

	private void displayBookmarkInfo()
	{
		keyField.setText( bookmark.getKey() );
		titleField.setText( bookmark.getTitle() );
		descriptionTextArea.setText( bookmark.getDescription() );

		if ( bookmark instanceof SimpleBookmark )
		{
			selectButton.setText( "Show" );
			typeLabel.setText( "Simple bookmark" );
		}
		else if ( bookmark instanceof DynamicBookmark )
		{
			selectButton.setText( "Select" );
			typeLabel.setText( "Dynamic bookmark" );
		}
	}

	public Bookmark getBookmark()
	{
		return bookmark;
	}

	public void setActive( boolean active )
	{
		this.isActive = active;

		if ( active )
		{
			setBorder( BorderFactory.createMatteBorder( 3, 3, 3, 3, ACTIVE_COLOR ) );
		}
		else
		{
			setBorder( new CompoundBorder( BorderFactory.createEmptyBorder( 3, 3, 2, 3 ),
					BorderFactory.createMatteBorder( 0, 0, 1, 0, Color.BLACK ) ) );
		}
	}

	public boolean isActive()
	{
		return this.isActive;
	}

	private void addBookmarkInfoDocumentListener()
	{
		keyField.getDocument().addDocumentListener( new DocumentListener()
		{

			@Override
			public void changedUpdate( DocumentEvent arg0 )
			{
				changeKey();
			}

			@Override
			public void insertUpdate( DocumentEvent arg0 )
			{
				changeKey();
			}

			@Override
			public void removeUpdate( DocumentEvent arg0 )
			{
				changeKey();
			}

			private void changeKey()
			{
				final String oldKey = bookmark.getKey();
				final String newKey = keyField.getText();

				if ( oldKey.equals( newKey ) || newKey.length() == 0 )
					return;

				if ( bookmarksEditor.containsBookmark( newKey ) )
				{
					final String message = "The key '" + newKey
							+ "' is already given to another bookmark. Please choose a different key.";
					JOptionPane.showMessageDialog( BookmarkCellPanel.this, message, "Key is already in use",
							JOptionPane.INFORMATION_MESSAGE );
					keyField.setText( oldKey );
				}
				else
				{
					bookmarksEditor.renameBookmark( oldKey, newKey );
				}
			}
		} );

		titleField.getDocument().addDocumentListener( new DocumentListener()
		{

			@Override
			public void changedUpdate( DocumentEvent arg0 )
			{
				bookmark.setTitle( titleField.getText() );
			}

			@Override
			public void insertUpdate( DocumentEvent arg0 )
			{
				bookmark.setTitle( titleField.getText() );
			}

			@Override
			public void removeUpdate( DocumentEvent arg0 )
			{
				bookmark.setTitle( titleField.getText() );
			}
		} );

		descriptionTextArea.getDocument().addDocumentListener( new DocumentListener()
		{

			@Override
			public void changedUpdate( DocumentEvent arg0 )
			{
				bookmark.setDescription( descriptionTextArea.getText() );
			}

			@Override
			public void insertUpdate( DocumentEvent arg0 )
			{
				bookmark.setDescription( descriptionTextArea.getText() );
			}

			@Override
			public void removeUpdate( DocumentEvent arg0 )
			{
				bookmark.setDescription( descriptionTextArea.getText() );
			}
		} );
	}
}
