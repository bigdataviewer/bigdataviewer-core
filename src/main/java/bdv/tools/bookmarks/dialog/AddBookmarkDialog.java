package bdv.tools.bookmarks.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import bdv.tools.bookmarks.editor.BookmarksEditor;

public class AddBookmarkDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JTextField keyField;
	private final JRadioButton simpleBookmarkOption;
	private final JRadioButton dynamicBookmarkOption;

	private final BookmarksEditor bookmarksEditor;
	private JTextField titleField;
	private JTextField descriptionField;

	private class MaxLengthTextDocument extends PlainDocument {
		// Store maximum characters permitted
		private int maxChars;

		public MaxLengthTextDocument(int maxChars) {
			this.maxChars = maxChars;
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			if (str != null && (getLength() + str.length() <= maxChars)) {
				super.insertString(offs, str, a);
			}
		}

		// getter e setter omitted
	}

	public AddBookmarkDialog(Frame owner, BookmarksEditor bookmarksEditor) {
		super(owner, "Add bookmark", true);

		this.bookmarksEditor = bookmarksEditor;

		getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel bookmarkPanel = new JPanel();
		bookmarkPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(bookmarkPanel, BorderLayout.CENTER);
		GridBagLayout gbl_bookmarkPanel = new GridBagLayout();
		gbl_bookmarkPanel.columnWidths = new int[] { 250, 0 };
		gbl_bookmarkPanel.rowHeights = new int[] { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 };
		gbl_bookmarkPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_bookmarkPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		bookmarkPanel.setLayout(gbl_bookmarkPanel);

		JLabel lblTitle = new JLabel("Title");
		GridBagConstraints gbc_lblTitle = new GridBagConstraints();
		gbc_lblTitle.anchor = GridBagConstraints.WEST;
		gbc_lblTitle.insets = new Insets(0, 0, 5, 0);
		gbc_lblTitle.gridx = 0;
		gbc_lblTitle.gridy = 0;
		bookmarkPanel.add(lblTitle, gbc_lblTitle);

		titleField = new JTextField();
		titleField.setColumns(10);
		GridBagConstraints gbc_titleField = new GridBagConstraints();
		gbc_titleField.insets = new Insets(0, 0, 5, 0);
		gbc_titleField.fill = GridBagConstraints.HORIZONTAL;
		gbc_titleField.gridx = 0;
		gbc_titleField.gridy = 1;
		bookmarkPanel.add(titleField, gbc_titleField);

		JLabel lblDescription = new JLabel("Description");
		GridBagConstraints gbc_lblDescription = new GridBagConstraints();
		gbc_lblDescription.anchor = GridBagConstraints.WEST;
		gbc_lblDescription.insets = new Insets(0, 0, 5, 0);
		gbc_lblDescription.gridx = 0;
		gbc_lblDescription.gridy = 2;
		bookmarkPanel.add(lblDescription, gbc_lblDescription);

		descriptionField = new JTextField();
		descriptionField.setColumns(10);
		GridBagConstraints gbc_descriptionField = new GridBagConstraints();
		gbc_descriptionField.insets = new Insets(0, 0, 5, 0);
		gbc_descriptionField.fill = GridBagConstraints.HORIZONTAL;
		gbc_descriptionField.gridx = 0;
		gbc_descriptionField.gridy = 3;
		bookmarkPanel.add(descriptionField, gbc_descriptionField);

		JLabel keyLabel = new JLabel("Key*");
		GridBagConstraints gbc_keyLabel = new GridBagConstraints();
		gbc_keyLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_keyLabel.insets = new Insets(0, 0, 5, 0);
		gbc_keyLabel.gridx = 0;
		gbc_keyLabel.gridy = 4;
		bookmarkPanel.add(keyLabel, gbc_keyLabel);

		keyField = new JTextField();
		GridBagConstraints gbc_keyField = new GridBagConstraints();
		gbc_keyField.fill = GridBagConstraints.HORIZONTAL;
		gbc_keyField.insets = new Insets(0, 0, 5, 0);
		gbc_keyField.anchor = GridBagConstraints.NORTH;
		gbc_keyField.gridx = 0;
		gbc_keyField.gridy = 5;
		bookmarkPanel.add(keyField, gbc_keyField);
		keyField.setColumns(10);

		keyField.setDocument(new MaxLengthTextDocument(1));

		JLabel typeLabel = new JLabel("Type");
		GridBagConstraints gbc_typeLabel = new GridBagConstraints();
		gbc_typeLabel.insets = new Insets(0, 0, 5, 0);
		gbc_typeLabel.anchor = GridBagConstraints.WEST;
		gbc_typeLabel.gridx = 0;
		gbc_typeLabel.gridy = 6;
		bookmarkPanel.add(typeLabel, gbc_typeLabel);

		simpleBookmarkOption = new JRadioButton("Simple bookmark");
		simpleBookmarkOption.setSelected(true);
		GridBagConstraints gbc_simpleBookmarkOption = new GridBagConstraints();
		gbc_simpleBookmarkOption.insets = new Insets(0, 0, 5, 0);
		gbc_simpleBookmarkOption.anchor = GridBagConstraints.WEST;
		gbc_simpleBookmarkOption.gridx = 0;
		gbc_simpleBookmarkOption.gridy = 7;
		bookmarkPanel.add(simpleBookmarkOption, gbc_simpleBookmarkOption);

		dynamicBookmarkOption = new JRadioButton("Dynamic bookmark");
		GridBagConstraints gbc_dynamicBookmarkOption = new GridBagConstraints();
		gbc_dynamicBookmarkOption.anchor = GridBagConstraints.WEST;
		gbc_dynamicBookmarkOption.gridx = 0;
		gbc_dynamicBookmarkOption.gridy = 8;
		bookmarkPanel.add(dynamicBookmarkOption, gbc_dynamicBookmarkOption);

		ButtonGroup group = new ButtonGroup();
		group.add(simpleBookmarkOption);
		group.add(dynamicBookmarkOption);

		JPanel buttonPanel = new JPanel();
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

		JButton cancelButton = new JButton("Cancel");
		buttonPanel.add(cancelButton);
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		JButton addButton = new JButton("Add");
		buttonPanel.add(addButton);
		addButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (!isInputValid())
					return;

				if (simpleBookmarkOption.isSelected()) {
					bookmarksEditor.createSimpleBookmark(keyField.getText(), titleField.getText(), descriptionField.getText());
				} else if (dynamicBookmarkOption.isSelected()) {
					bookmarksEditor.createDynamicBookmark(keyField.getText(), titleField.getText(), descriptionField.getText());
				}

				dispose();
			}
		});

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentShown(ComponentEvent e) {
				reset();
			}
		});

		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				setVisible(false);
			}

			private static final long serialVersionUID = 1L;
		};
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), hideKey);
		am.put(hideKey, hideAction);

		pack();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	private boolean isInputValid() {

		String key = keyField.getText();

		if (key.length() == 0) {
			return false;
		} else if (key.length() > 1) {
			JOptionPane.showMessageDialog(this, "Please enter a key with only 1 character.", "Key is too long",
					JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		if (bookmarksEditor.getBookmarks().get(key) != null) {

			String message = "The key '" + key
					+ "' is already given to another bookmark. Please choose a different key.";

			JOptionPane.showMessageDialog(this, message, "Key is already in use", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}

		return true;
	}

	private void reset() {
		titleField.setText("");
		descriptionField.setText("");
		keyField.setText("");
		simpleBookmarkOption.setSelected(true);
	}

}
