package bdv.tools.bookmarks.dialog;

import bdv.tools.bookmarks.bookmark.DynamicBookmark;
import bdv.tools.bookmarks.bookmark.IBookmark;
import bdv.tools.bookmarks.bookmark.SimpleBookmark;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BookmarkCellPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private final JTextField keyField;
    private final JLabel typeLabel;
    
    private final JButton selectButton;
    private final JButton removeButton;

    private IBookmark bookmark;

    BookmarkCellPanel(IBookmark bookmark) {
        this.bookmark = bookmark;
    	
        GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{1, 1, 0};
		gridBagLayout.rowHeights = new int[]{1, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.NORTH;
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.insets = new Insets(5, 5, 0, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0};
		gbl_panel.rowHeights = new int[] {0, 0, 0};
		gbl_panel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0};
		panel.setLayout(gbl_panel);
		
		keyField = new JTextField();
		GridBagConstraints gbc_txtKey = new GridBagConstraints();
		gbc_txtKey.insets = new Insets(0, 0, 5, 0);
		gbc_txtKey.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtKey.gridx = 0;
		gbc_txtKey.gridy = 0;
		panel.add(keyField, gbc_txtKey);
		keyField.setColumns(10);
		
		typeLabel = new JLabel();
		GridBagConstraints gbc_lblType = new GridBagConstraints();
		gbc_lblType.anchor = GridBagConstraints.WEST;
		gbc_lblType.gridx = 0;
		gbc_lblType.gridy = 1;
		panel.add(typeLabel, gbc_lblType);
		
		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(5, 5, 5, 5);
		gbc_panel_1.anchor = GridBagConstraints.NORTHEAST;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 0;
		add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);
		
		selectButton = new JButton("Show");
		GridBagConstraints gbc_btnSelect = new GridBagConstraints();
		gbc_btnSelect.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSelect.insets = new Insets(0, 0, 5, 0);
		gbc_btnSelect.gridx = 0;
		gbc_btnSelect.gridy = 0;
		panel_1.add(selectButton, gbc_btnSelect);
		
		removeButton = new JButton("Remove");
		GridBagConstraints gbc_btnRemove = new GridBagConstraints();
		gbc_btnRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemove.gridx = 0;
		gbc_btnRemove.gridy = 1;
		panel_1.add(removeButton, gbc_btnRemove);
		
		typeLabel.setOpaque(false);
		panel.setOpaque(false);
		panel_1.setOpaque(false);
        
        
        setMinimumSize(new Dimension(450, 70));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        setPreferredSize(new Dimension(450, 70));
    }
   
    public void setBookmark(IBookmark bookmark) {
    	this.bookmark = bookmark;
    	
        keyField.setText(bookmark.getKey());
        if(bookmark instanceof SimpleBookmark){
        	selectButton.setText("Show");
        	typeLabel.setText("Simple bookmark");
        }
        else if(bookmark instanceof DynamicBookmark){
        	selectButton.setText("Select");
        	typeLabel.setText("Dynamic bookmark");
        }
    }
    
    public IBookmark getBookmark(){
    	return bookmark;
    }
}
