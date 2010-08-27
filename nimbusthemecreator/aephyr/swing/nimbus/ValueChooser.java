package aephyr.swing.nimbus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

abstract class ValueChooser {

	abstract JComponent getComponent();
	
	abstract void setValue(Object value);
	
	abstract Object getValue();
	
	static class BooleanChooser extends ValueChooser {

		BooleanChooser() {
			tru = new JRadioButton(Boolean.TRUE.toString());
			tru.setFont(UIDefaultsRenderer.BOOLEAN_FONT);
			fal = new JRadioButton(Boolean.FALSE.toString());
			fal.setFont(UIDefaultsRenderer.BOOLEAN_FONT);
			ButtonGroup group = new ButtonGroup();
			group.add(tru);
			group.add(fal);
			pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addGap(100, 100, Short.MAX_VALUE)
					.addGroup(layout.createSequentialGroup()
							.addGap(8)
							.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
									.addComponent(tru).addComponent(fal))
							.addGap(8)));
			layout.setVerticalGroup(layout.createSequentialGroup()
							.addGap(2).addComponent(tru).addComponent(fal).addGap(4));
		}
		
		private JComponent pane;
		private JRadioButton tru;
		private JRadioButton fal;
		
		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			if (Boolean.TRUE.equals(value)) {
				tru.setSelected(true);
			} else {
				fal.setSelected(true);
			}
		}

		@Override
		Object getValue() {
			return Boolean.valueOf(tru.isSelected());
		}

	}
	
	static class StringChooser extends ValueChooser {
		
		StringChooser() {
			pane = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
			text = new JTextField(40);
			pane.add(text);
		}
		
		private JComponent pane;
		private JTextField text;

		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			text.setText(value.toString());
		}

		@Override
		Object getValue() {
			return text.getText();
		}

	}
	
	static class ColorChooser extends ValueChooser {
		
		ColorChooser() {
			chooser = new JColorChooser();
		}
		
		private JColorChooser chooser;

		@Override
		JComponent getComponent() {
			return chooser;
		}

		@Override
		void setValue(Object value) {
			chooser.setColor((Color)value);
		}

		@Override
		Object getValue() {
			return chooser.getColor();
		}

	}
	
	
	static class IntegerChooser extends ValueChooser {
		
		IntegerChooser() {
			chooser = new NumberChooser(null, -10, 100);
			pane = NumberChooser.createComponent(null, -1, -1, -1, -1, chooser);
		}
		
		private JComponent pane;
		
		private NumberChooser chooser;

		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			chooser.setValue((Integer)value);
		}

		@Override
		Object getValue() {
			return chooser.getValue();
		}

	}
	
	static class DimensionChooser extends ValueChooser implements ChangeListener {
		
		DimensionChooser() {
			width = new NumberChooser("Width:", 0, 2000);
			height = new NumberChooser("Height:", 0, 2000);
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Dimension;
			width.addChangeListener(this);
			height.addChangeListener(this);
			pane = NumberChooser.createComponent(renderer, 200, Short.MAX_VALUE, 200, 200, width, height);
		}
		
		private JComponent pane;

		private NumberChooser width;
		
		private NumberChooser height;
		
		private UIDefaultsRenderer renderer;
		
		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			Dimension d = (Dimension)value;
			renderer.value = null;
			width.setValue(d.width);
			height.setValue(d.height);
			renderer.value = (Dimension)d.clone();
		}

		@Override
		Object getValue() {
			return renderer.value;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (renderer.value != null) {
				Dimension d = (Dimension)renderer.value;
				d.width = width.getValue();
				d.height = height.getValue();
				renderer.repaint();
			}
		}

	}
	
	static class InsetsChooser extends ValueChooser implements ChangeListener {
		
		InsetsChooser() {
			top = new NumberChooser("Top:", 0, 20);
			left = new NumberChooser("Left:", 0, 20);
			bottom = new NumberChooser("Bottom:", 0, 20);
			right = new NumberChooser("Right:", 0, 20);
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Insets;
			top.addChangeListener(this);
			left.addChangeListener(this);
			bottom.addChangeListener(this);
			right.addChangeListener(this);
			
			pane = NumberChooser.createComponent(renderer, 120, 120, 50, 50, top, left, bottom, right);
		}
		
		private JComponent pane;
		private NumberChooser top;
		private NumberChooser left;
		private NumberChooser bottom;
		private NumberChooser right;
		private UIDefaultsRenderer renderer;

		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			Insets i = (Insets)value;
			renderer.value = null;
			top.setValue(i.top);
			left.setValue(i.left);
			bottom.setValue(i.bottom);
			right.setValue(i.right);
			renderer.value = (Insets)i.clone();
			renderer.repaint();
		}

		@Override
		Object getValue() {
			return renderer.value;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (renderer.value != null) {
				Insets in = (Insets)renderer.value;
				in.top = top.getValue();
				in.left = left.getValue();
				in.bottom = bottom.getValue();
				in.right = right.getValue();
				renderer.repaint();
			}
		}

	}

	private static class NumberChooser implements ChangeListener {
		
		NumberChooser(String nam, int min, int max) {
			name = nam;
			spin = new SpinnerNumberModel(min, min, max, 1);
			spin.addChangeListener(this);
			slide = new JSlider(min, max);
			slide.setMinorTickSpacing((max-min)/10);
			slide.setMajorTickSpacing((max-min)/2);
			slide.setPaintTicks(true);
			slide.addChangeListener(this);
		}
		
		private String name;
		
		private SpinnerNumberModel spin;
		
		private JSlider slide;
		
		JComponent getDefaultFocusComponent() {
			return slide;
		}
		
		@Override
		public void stateChanged(ChangeEvent e) {
			if (spin.getNumber().intValue() != slide.getValue()) {
				if (e.getSource() == slide) {
					spin.setValue(slide.getValue());
				} else {
					slide.setValue(spin.getNumber().intValue());
				}
			}
		}
		
		int getValue() {
			return slide.getValue();
		}
		
		void setValue(int value) {
			slide.setValue(value);
		}
		
		void addChangeListener(ChangeListener l) {
			slide.addChangeListener(l);
		}
		

		static JComponent createComponent(JComponent preview, int prefW, int maxW, int prefH, int maxH, NumberChooser ... choosers) {
			JComponent pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			GroupLayout.ParallelGroup labelX = null;
			GroupLayout.ParallelGroup spinX = layout.createParallelGroup(Alignment.LEADING, false);
			GroupLayout.ParallelGroup slideX = layout.createParallelGroup(Alignment.LEADING, false);
			GroupLayout.SequentialGroup y = layout.createSequentialGroup().addGap(2);
			for (NumberChooser chooser : choosers) {
				JLabel label = chooser.name == null ? null : new JLabel(chooser.name);
				JSpinner spin = new JSpinner(chooser.spin);
				GroupLayout.ParallelGroup baseline = layout.createBaselineGroup(false, true);
				y.addGroup(baseline);
				if (label != null) {
					if (labelX == null)
						labelX = layout.createParallelGroup(Alignment.TRAILING, false);
					labelX.addComponent(label);
					baseline.addComponent(label);
				}
				spinX.addComponent(spin);
				slideX.addComponent(chooser.slide);
				baseline.addComponent(spin).addComponent(chooser.slide);
			}
			GroupLayout.Group x = layout.createSequentialGroup().addGap(8);
			if (labelX != null)
				x.addGroup(labelX).addGap(2);
			x.addGroup(spinX).addGap(2).addGroup(slideX).addGap(8);
			y.addGap(4);
			if (preview != null) {
				y.addComponent(preview, prefH, prefH, maxH);
				y.addGap(4);
				x = layout.createParallelGroup(Alignment.CENTER, false)
						.addGroup(x).addComponent(preview, prefW, prefW, maxW);
			}
			layout.setHorizontalGroup(x);
			layout.setVerticalGroup(y);
			return pane;
		}
	}
	
	static class FontChooser extends ValueChooser implements ChangeListener {

		FontChooser() {
			family = new SpinnerListModel(new Object[]{
				Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF	
			});
			family.addChangeListener(this);
			JSpinner familySpin = new JSpinner(family);
			size = new SpinnerNumberModel(32f, 8f, 32f, 2f);
			size.addChangeListener(this);
			JSpinner sizeSpin = new JSpinner(size);
			bold = new JToggleButton("b");
			bold.addChangeListener(this);
			bold.setFont(bold.getFont().deriveFont(Font.BOLD));
			italic = new JToggleButton("i");
			italic.addChangeListener(this);
			italic.setFont(italic.getFont().deriveFont(Font.ITALIC));
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Font;
			pane = new JPanel(null);
			GroupLayout layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER, true)
					.addGroup(layout.createSequentialGroup()
							.addGap(8).addComponent(familySpin, 150, 150, 150).addGap(2)
							.addComponent(sizeSpin).addGap(2).addComponent(bold)
							.addGap(2).addComponent(italic).addGap(12))
					.addComponent(renderer));
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGap(2)
					.addGroup(layout.createBaselineGroup(false, true)
							.addComponent(familySpin).addComponent(sizeSpin)
							.addComponent(bold).addComponent(italic))
					.addComponent(renderer, 50, 50, 50));
		}
		
		private JComponent pane;
		private SpinnerListModel family;
		private SpinnerNumberModel size;
		private JToggleButton bold;
		private JToggleButton italic;
		private UIDefaultsRenderer renderer;

		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			Font font = (Font)value;
			renderer.value = null;
			family.setValue(font.getFamily());
			size.setValue(font.getSize2D());
			bold.setSelected(font.isBold());
			italic.setSelected(font.isItalic());
			renderer.value = value;
		}

		@Override
		Object getValue() {
			return renderer.value;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			Font font = (Font)renderer.value;
			if (font != null) {
				if (e.getSource() == size) {
					renderer.value = font.deriveFont(size.getNumber().floatValue());
				} else if (e.getSource() == bold) {
					renderer.value = font.deriveFont(bold.isSelected() ?
							font.getStyle() | Font.BOLD : font.getStyle() & ~Font.BOLD);
				} else if (e.getSource() == italic) {
					renderer.value = font.deriveFont(italic.isSelected() ?
							font.getStyle() | Font.ITALIC : font.getStyle() & ~Font.ITALIC);
				} else if (e.getSource() == family) {
					font = Font.decode(family.getValue().toString()+' '+size.getNumber().intValue());
					int style = 0;
					if (bold.isSelected())
						style |= Font.BOLD;
					if (italic.isSelected())
						style |= Font.ITALIC;
					if (style != 0)
						font = font.deriveFont(style);
					renderer.value = font;
				}
				renderer.repaint();
			}
		}

	}
	
	//TODO
	static class PainterChooser extends ValueChooser implements ActionListener, ListSelectionListener, ChangeListener {

		PainterChooser() {
			tablePane = new JScrollPane();
			renderer = new UIDefaultsRenderer();
			renderer.type = Type.Painter;
			editor = new JTextArea(20, 80);
			editor.setText("Not Implemented");
			editor.setEnabled(false);
			editor.setFont(Font.decode(Font.MONOSPACED+' '+12));
			JScrollPane scroller = new JScrollPane(editor);
			JButton update = new JButton("Update");
			update.addActionListener(this);
			update.setEnabled(false);
			JButton toDialog = new JButton("Switch to Dialog");
			toDialog.addActionListener(this);
			JPanel custom = new JPanel(null);
			GroupLayout layout = new GroupLayout(custom);
			custom.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(scroller)
				.addGroup(layout.createSequentialGroup()
						.addComponent(update).addGap(10, 100, Short.MAX_VALUE).addComponent(toDialog)));
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(scroller)
				.addGroup(layout.createBaselineGroup(false, true)
					.addComponent(update).addComponent(toDialog)));
			
			tabs = new JTabbedPane();
			pane = new JPanel(null);
			layout = new GroupLayout(pane);
			pane.setLayout(layout);
			layout.setHorizontalGroup(layout.createParallelGroup()
					.addComponent(tabs)
					.addComponent(renderer, Alignment.CENTER, 100, 100, Short.MAX_VALUE));
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(tabs).addComponent(renderer, 25, 25, 25));
			tabs.add("Custom", custom);
			tabs.add("Nimbus Painters", tablePane);
			tabs.addChangeListener(this);
		}
		
		private JComponent pane;
		private JTabbedPane tabs;
		private JScrollPane tablePane;
		private JTable table;
		private JTextArea editor;
		private UIDefaultsRenderer renderer;
		private Object value;
		private JDialog dialog;

		@Override
		JComponent getComponent() {
			return pane;
		}

		@Override
		void setValue(Object value) {
			this.value = null;
			if (table != null)
				table.changeSelection(-1, -1, false, false);
			this.value = value;
			renderer.value = value;
		}

		@Override
		Object getValue() {
			return value;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (value == null || e.getValueIsAdjusting())
				return;
			int row = table.getSelectedRow();
			renderer.value = row < 0 ? value : 
					UIManager.getLookAndFeelDefaults().get(
							table.getValueAt(row, 0));
			renderer.repaint();
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (tabs.getSelectedComponent() == tablePane && table == null) {
				DefaultTableColumnModel columns = new DefaultTableColumnModel();
				TableColumn column = new TableColumn(0, 400);
				column.setHeaderValue("Key");
				columns.addColumn(column);
				column = new TableColumn(2, 50, new UIDefaultsRenderer(), null);
				columns.addColumn(column);
				column.setHeaderValue(Type.Painter.name());
				ArrayList<String> painters = new ArrayList<String>(1000);
				Creator.getKeys(null, painters);
				String[] painterKeys = painters.toArray(new String[painters.size()]);
				Arrays.sort(painterKeys);
				table = new UITable(new UITypeTableModel(
						painterKeys, Type.Painter, false), columns);
				table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.setRowHeight(25);
				table.setPreferredScrollableViewportSize(new Dimension(500, table.getRowHeight()*10));
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				table.getSelectionModel().addListSelectionListener(this);
				tablePane.getViewport().setView(table);
				tablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand() == "Update") {
				
			} else if (e.getActionCommand() == "Switch to Dialog") {
				if (dialog == null) {
					dialog = new JDialog((JFrame)null, true);
				}
				dialog.add(pane, BorderLayout.CENTER);
				dialog.pack();
				dialog.setVisible(true);
			}
		}

	}

	
}
