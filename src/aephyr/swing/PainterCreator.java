package aephyr.swing;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.*;

import java.lang.reflect.*;
import java.util.List;
import java.util.*;

public class PainterCreator extends JComponent {
	private static final String[] HEADERS = {"Variable", "Method", "Arguments"};
	private static Font MONOSPACED = Font.decode(Font.MONOSPACED + ' ' + 12);
	private static Font MONOSPACED_BOLD = MONOSPACED.deriveFont(Font.BOLD);
	
	private TableColumn createTableColumn(int idx, TableCellEditor editor) {
		TableColumn col = new TableColumn(idx, (idx+1)*100, null, editor);
		col.setHeaderValue(HEADERS[idx]);
		return col;
	}
	
	private JLabel createLabel(String text, boolean editable) {
		JLabel l = new JLabel(text);
		l.setFont(editable ? MONOSPACED_BOLD : MONOSPACED);
		return l;
	}
	
	public PainterCreator() {
		TableCellEditor editor = new Editor();
		DefaultTableColumnModel columns = new DefaultTableColumnModel();
		columns.addColumn(createTableColumn(0, editor));
		columns.addColumn(createTableColumn(1, editor));
		columns.addColumn(createTableColumn(2, null));
		table = new JTable(new Model(), columns);
		JLabel lbl1 = createLabel("paint(Graphics2D", false);
		graphicsLabel = createLabel("g", true);
		JLabel lbl2 = createLabel(",  ", false);
		genericLabel = createLabel("T", false);
		contextLabel = createLabel("c", true);
		JLabel lbl3 = createLabel(",  int", false);
		widthLabel = createLabel("width", true);
		JLabel lbl4 = createLabel(",  int", false);
		heightLabel = createLabel("height", true);

		JScrollPane tbl = new JScrollPane(table);
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		int gap = lbl1.getFontMetrics(MONOSPACED).charWidth(' ')*3/2;
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
						.addComponent(lbl1).addGap(gap).addComponent(graphicsLabel)
						.addComponent(lbl2).addComponent(genericLabel).addGap(gap).addComponent(contextLabel)
						.addComponent(lbl3).addGap(gap).addComponent(widthLabel)
						.addComponent(lbl4).addGap(gap).addComponent(heightLabel))
				.addComponent(tbl));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createBaselineGroup(false, true)
						.addComponent(lbl1).addComponent(graphicsLabel).addComponent(lbl2)
						.addComponent(genericLabel).addComponent(contextLabel).addComponent(lbl3)
						.addComponent(widthLabel).addComponent(lbl4).addComponent(heightLabel))
				.addComponent(tbl));
		
	}
	
	JTable table;
	JLabel graphicsLabel;
	JLabel genericLabel;
	JLabel contextLabel;
	JLabel widthLabel;
	JLabel heightLabel;

	
	
	
	
	
	
	
//	static class Border extends AbstractBorder {
//		Border() {
//			font = Font.decode(Font.SANS_SERIF + ' ' + 10).deriveFont(Font.BOLD);
//		}
//		Font font;
//		
//		@Override
//		public Insets getBorderInsets(Component c, Insets insets) {
//			insets.left = insets.right = insets.bottom = 5;
//			insets.top = 1;
//			return insets;
//		}
//		@Override
//		public Insets getBorderInsets(Component c) {
//			return getBorderInsets(c, new Insets(0, 0, 0, 0));
//		}
//		@Override
//		public void paintBorder(Component c, Graphics g, int x, int y,
//				int width, int height) {
//			Graphics2D g2 = (Graphics2D)g.create();
//			g2.setColor(Color.gray);
////			g2.setColor(UIManager.getColor("nimbusBorder"));
//			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
////				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
//			g2.drawRoundRect(x+1, y+1, width-2, height-2, 10, 10);
//			g2.dispose();
//		}
//	}
	
	
	private static class VariableChooser {
		
		VariableChooser() {
			variables = new JList(new DefaultListModel());
			char[] prototype = new char[16];
			for (int i=prototype.length; --i>=0;)
				prototype[i] = 'w';
			variables.setPrototypeCellValue(new String(prototype));
			name = new JTextField(10);
			List<String> list = new ArrayList<String>();
			list.add("java.awt");
			list.add("java.awt.geom");
			pkg = new SpinnerListModel(list);
			JSpinner spin = new JSpinner(pkg);
			cls = new JList();
			
			JScrollPane var = new JScrollPane(variables);
			JScrollPane cl = new JScrollPane(cls);
			pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addComponent(var)
					.addGroup(layout.createParallelGroup()
							.addComponent(name, 250, 250, 250)
							.addComponent(spin, 250, 250, 250)
							.addComponent(cl, 250, 250, 250)));
			layout.setVerticalGroup(layout.createParallelGroup()
					.addComponent(var)
					.addGroup(layout.createSequentialGroup()
							.addComponent(name).addComponent(spin).addComponent(cl)));
			
		}
		
		JComponent pane;
		JList variables;
		JTextField name;
		SpinnerListModel pkg;
		JList cls;
		
	}
	
	

	private static class Editor extends AbstractCellEditor implements TableCellEditor {

		Editor() {
			
			
		}
		
		JPopupMenu popup;
		VariableChooser variable;
		
		
		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			if (column == 0) {
				showPopup(variable.pane);
			} else if (column == 1) {
				
			} else {
				
			}
			return null;
		}
		
		private void showPopup(JComponent c) {
			BorderLayout layout = (BorderLayout)popup.getLayout();
			Component cur = layout.getLayoutComponent(BorderLayout.CENTER);
			if (cur != c) {
				if (cur != null)
					popup.remove(cur);
				popup.add(c, BorderLayout.CENTER);
			}
//			popup.show(invoker, x, y)
		}

		@Override
		public Object getCellEditorValue() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	
	private static class Element {
		Element(String nm, Class<?> cls) {
			name = nm;
			clss = cls;
		}
		
		String name;
		Class<?> clss;
		Method method;
		Object args;
		
//		void invoke(Object obj) throws IllegalAccessException, InvocationTargetException {
//			method.invoke(obj, args);
//		}
	}
	
	private static class Model extends AbstractTableModel {

		List<Element> rows = new ArrayList<Element>();

		
		void insert(int index, String nm, Class<?> cls) {

			rows.add(index, new Element(nm, cls));
			fireTableRowsInserted(index, index);
		}
		
		
		@Override
		public String getColumnName(int col) {
			switch (col) {
			case 0:
			case 1:
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			Element e = rows.get(row);
			switch (col) {
			case 0: return e.name;
			case 1: return e.method.getName();
			case 2: return e.args == null ? "" : e.args;
			}
			return null;
		}
		
		@Override
		public void setValueAt(Object aValue, int row, int col) {

		}

	}
	
	
	
	
	Graphics2D junkA;
}


