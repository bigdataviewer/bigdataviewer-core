package bdv.tools.bookmarks.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import bdv.tools.bookmarks.Bookmarks;


public class BookmarkManagementDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6812071634159292336L;

	private final JTable bookmarkTable;
	private final BookmarkTableModel tableModel;
	
	public BookmarkManagementDialog(final Frame owner, Bookmarks bookmarks){
		super(owner, "Bookmark Management", false);
		//setSize(new Dimension(500, 400));
		
		this.tableModel = new BookmarkTableModel(bookmarks);
		
		JPanel buttonPane = new JPanel();
		buttonPane.setBackground(new Color(220, 220, 220));
		getContentPane().add(buttonPane, BorderLayout.NORTH);
		GridBagLayout gbl_buttonPane = new GridBagLayout();
		gbl_buttonPane.columnWidths = new int[]{101, 0};
		gbl_buttonPane.rowHeights = new int[]{0, 23, 0};
		gbl_buttonPane.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_buttonPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		buttonPane.setLayout(gbl_buttonPane);
		
		JButton btnNewButton = new JButton("Add Bookmark");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.anchor = GridBagConstraints.WEST;
		gbc_btnNewButton.fill = GridBagConstraints.VERTICAL;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 1;
		buttonPane.add(btnNewButton, gbc_btnNewButton);
		btnNewButton.setVerticalAlignment(SwingConstants.TOP);
		btnNewButton.setHorizontalAlignment(SwingConstants.LEFT);
		
		bookmarkTable = new JTable(tableModel);
		bookmarkTable.setTableHeader(null);
		BookmarkCellEditorRenderer compCellEditorRenderer = new BookmarkCellEditorRenderer();
        bookmarkTable.setDefaultRenderer(Object.class, compCellEditorRenderer);
        bookmarkTable.setDefaultEditor(Object.class, compCellEditorRenderer);
		
		JScrollPane scrollPane = new JScrollPane(bookmarkTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		this.addWindowFocusListener(new WindowFocusListener() {
			
			@Override
			public void windowLostFocus(WindowEvent e) { }
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
				repaintBookmark();
			}
		});
		
		final ActionMap am = getRootPane().getActionMap();
		final InputMap im = getRootPane().getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		final Object hideKey = new Object();
		final Action hideAction = new AbstractAction()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				setVisible( false );
			}

			private static final long serialVersionUID = 1L;
		};
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), hideKey );
		am.put( hideKey, hideAction );
		
		pack();
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
	}
	
	public void repaintBookmark(){
		tableModel.repaint();
		bookmarkTable.setRowHeight(new BookmarkCellPanel().getPreferredSize().height);
	}
}

