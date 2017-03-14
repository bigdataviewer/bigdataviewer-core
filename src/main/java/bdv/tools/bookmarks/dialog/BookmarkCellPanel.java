package bdv.tools.bookmarks.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.Bookmark;
import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import bdv.tools.bookmarks.editor.BookmarksEditor;

import javax.swing.JTextArea;
import javax.swing.JScrollPane;

public class BookmarkCellPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final static Color ACTIVE_COLOR = Color.CYAN;

	private final Bookmark bookmark;
	private final BookmarksEditor bookmarksEditor;

	private final JLabel lblKey;
	private final JLabel typeLabel;
	private final JLabel lblTitle;
	private final JLabel lblDescription;

	private final JTextField keyField;
	private final JTextField titleField;
	private final JScrollPane scrollPaneDescription;
	private final JTextArea descriptionArea;

	private final JButton selectButton;
	private final JButton removeButton;

	private boolean active = false;

	BookmarkCellPanel(Bookmark bookmark, BookmarksEditor bookmarksEditor) {
		this.bookmark = bookmark;
		this.bookmarksEditor = bookmarksEditor;

		setBorder(new MatteBorder(0, 0, 1, 0, (Color) new Color(0, 0, 0)));
		setMinimumSize(new Dimension(300, 210));
		setMaximumSize(new Dimension(2147483647, 210));
		setPreferredSize(new Dimension(300, 210));

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 1, 1, 0 };
		gridBagLayout.rowHeights = new int[] { 1, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		JPanel panelInfo = new JPanel();
		GridBagConstraints gbc_panelInfo = new GridBagConstraints();
		gbc_panelInfo.anchor = GridBagConstraints.NORTH;
		gbc_panelInfo.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelInfo.insets = new Insets(5, 5, 0, 5);
		gbc_panelInfo.gridx = 0;
		gbc_panelInfo.gridy = 0;
		add(panelInfo, gbc_panelInfo);
		GridBagLayout gbl_panelInfo = new GridBagLayout();
		gbl_panelInfo.columnWidths = new int[] { 0, 0 };
		gbl_panelInfo.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 70, 0 };
		gbl_panelInfo.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelInfo.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0 };
		panelInfo.setLayout(gbl_panelInfo);

		lblKey = new JLabel("Key");
		GridBagConstraints gbc_lblKey = new GridBagConstraints();
		gbc_lblKey.anchor = GridBagConstraints.WEST;
		gbc_lblKey.insets = new Insets(0, 0, 5, 0);
		gbc_lblKey.gridx = 0;
		gbc_lblKey.gridy = 0;
		panelInfo.add(lblKey, gbc_lblKey);

		keyField = new JTextField();
		GridBagConstraints gbc_txtKey = new GridBagConstraints();
		gbc_txtKey.insets = new Insets(0, 0, 5, 0);
		gbc_txtKey.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtKey.gridx = 0;
		gbc_txtKey.gridy = 1;
		panelInfo.add(keyField, gbc_txtKey);
		keyField.setColumns(10);

		typeLabel = new JLabel("Bookmark type");
		GridBagConstraints gbc_typeLabel = new GridBagConstraints();
		gbc_typeLabel.anchor = GridBagConstraints.WEST;
		gbc_typeLabel.insets = new Insets(0, 0, 5, 0);
		gbc_typeLabel.gridx = 0;
		gbc_typeLabel.gridy = 2;
		panelInfo.add(typeLabel, gbc_typeLabel);

		lblTitle = new JLabel("Title");
		GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.anchor = GridBagConstraints.WEST;
		gbc_lblTitle.insets = new Insets(0, 0, 5, 0);
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 3;
		panelInfo.add(lblTitle, gbc_lblTitle);

		titleField = new JTextField();
		titleField.setText((String) null);
		titleField.setColumns(10);
		GridBagConstraints gbc_titleField = new GridBagConstraints();
		gbc_titleField.insets = new Insets(0, 0, 5, 0);
		gbc_titleField.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleField.gridx = 0;
		gbc_titleField.gridy = 4;
		panelInfo.add(titleField, gbc_titleField);

		lblDescription = new JLabel("Description");
		GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.anchor = GridBagConstraints.WEST;
		gbc_lblDescription.insets = new Insets(0, 0, 5, 0);
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 5;
		panelInfo.add(lblDescription, gbc_lblDescription);

		JPanel panelButton = new JPanel();
		GridBagConstraints gbc_panelButton = new GridBagConstraints();
		gbc_panelButton.insets = new Insets(5, 5, 5, 5);
		gbc_panelButton.anchor = GridBagConstraints.NORTHEAST;
		gbc_panelButton.gridx = 1;
		gbc_panelButton.gridy = 0;
		add(panelButton, gbc_panelButton);
		GridBagLayout gbl_panelButton = new GridBagLayout();
		gbl_panelButton.columnWidths = new int[] { 0, 0 };
		gbl_panelButton.rowHeights = new int[] { 0, 0, 0 };
		gbl_panelButton.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_panelButton.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		panelButton.setLayout(gbl_panelButton);

		selectButton = new JButton("Show");
		GridBagConstraints gbc_btnSelect = new GridBagConstraints();
		gbc_btnSelect.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSelect.insets = new Insets(0, 0, 5, 0);
		gbc_btnSelect.gridx = 0;
		gbc_btnSelect.gridy = 0;
		panelButton.add(selectButton, gbc_btnSelect);

		removeButton = new JButton("Remove");
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemove.gridx = 0;
		gbc_btnRemove.gridy = 1;
		panelButton.add(removeButton, gbc_btnRemove);

		panelInfo.setOpaque(false);

		scrollPaneDescription = new JScrollPane();
		GridBagConstraints gbc_scrollPaneDescription = new GridBagConstraints();
		gbc_scrollPaneDescription.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPaneDescription.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneDescription.gridx = 0;
		gbc_scrollPaneDescription.gridy = 6;
		panelInfo.add(scrollPaneDescription, gbc_scrollPaneDescription);

		descriptionArea = new JTextArea();
		descriptionArea.setLineWrap(true);
		descriptionArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		descriptionArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		scrollPaneDescription.setViewportView(descriptionArea);
		panelButton.setOpaque(false);

		displayBookmarkInfo();
		addBookmarkInfoDocumentListener();
	}

	private void displayBookmarkInfo() {
		keyField.setText(bookmark.getKey());
		titleField.setText(bookmark.getTitle());
		descriptionArea.setText(bookmark.getDescription());

		if (bookmark instanceof SimpleBookmark) {
			selectButton.setText("Show");
			typeLabel.setText("Simple bookmark");
		} else if (bookmark instanceof DynamicBookmark) {
			selectButton.setText("Select");
			typeLabel.setText("Dynamic bookmark");
		}
	}

	public Bookmark getBookmark() {
		return bookmark;
	}

	public void addSelectActionListener(ActionListener actionListener) {
		selectButton.addActionListener(actionListener);
	}

	public void addRemoveActionListener(ActionListener actionListener) {
		removeButton.addActionListener(actionListener);
	}

	public void setActive(boolean active) {
		this.active = active;

		if (active) {
			setBackground(ACTIVE_COLOR);
		} else {
			setBackground(UIManager.getColor("Panel.background"));
		}
	}

	public boolean getActive() {
		return this.active;
	}

	private void addBookmarkInfoDocumentListener() {
		keyField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				changeKey();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				changeKey();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				changeKey();
			}

			private void changeKey() {
				final String oldKey = bookmark.getKey();
				final String newKey = keyField.getText();

				if (oldKey.equals(newKey) || newKey.length() == 0)
					return;

				if (bookmarksEditor.containsBookmark(newKey)) {
					final String message = "The key '" + newKey
							+ "' is already given to another bookmark. Please choose a different key.";
					JOptionPane.showMessageDialog(BookmarkCellPanel.this, message, "Key is already in use",
							JOptionPane.INFORMATION_MESSAGE);
				} else {
					bookmarksEditor.renameBookmark(oldKey, newKey);
				}
			}
		});

		titleField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				bookmark.setTitle(titleField.getText());
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				bookmark.setTitle(titleField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				bookmark.setTitle(titleField.getText());
			}
		});

		descriptionArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				bookmark.setDescription(descriptionArea.getText());
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				bookmark.setDescription(descriptionArea.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				bookmark.setDescription(descriptionArea.getText());
			}
		});
	}
}
