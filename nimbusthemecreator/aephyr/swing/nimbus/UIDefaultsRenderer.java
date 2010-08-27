package aephyr.swing.nimbus;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import javax.swing.Painter; // 1.7
//import com.sun.java.swing.Painter; // 1.6


class UIDefaultsRenderer extends JComponent implements TableCellRenderer {
	
	static final Font BOOLEAN_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	
	Object value;
	Type type;
	int row = -1;
	boolean selected = false;
	
	@Override
	public Component getTableCellRendererComponent(JTable tbl,
			Object val, boolean isSelected, boolean hasFocus, int row,
			int column) {
		UITableModel mdl = (UITableModel)tbl.getModel();
		value = val;
		type = mdl.getType(tbl.convertRowIndexToModel(row));
		setToolTipText(toolTipText());
		this.row = row;
		selected = isSelected;
		return this;
	}
	
	private String toolTipText() {
		switch (type) {
		case Color: {
			Color c = (Color)value;
			return c.getRed()+", "+c.getGreen()+", "+c.getBlue();
			}
		case Insets: {
			Insets i = (Insets)value;
			return i.top+", "+i.left+", "+i.bottom+", "+i.right;
			}
		case Font: {
			Font f = (Font)value;
			String style;
			switch (f.getStyle()) {
			default: style = "PLAIN"; break;
			case Font.BOLD: style = "BOLD"; break;
			case Font.ITALIC: style = "ITALIC"; break;
			case Font.BOLD | Font.ITALIC: style = "BOLDITALIC"; break;
			}
			return f.getFamily()+", "+style+", "+f.getSize();
			}
		case String: {
			return (String)value;
			}
		case Dimension: {
			Dimension d = (Dimension)value;
			return d.width+" \u00D7 "+d.height;
			}
		case Object: {
			return value.getClass().getSimpleName();
			}
		}
		return null;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		if (selected) {
			g.setColor(UIManager.getColor("Table[Enabled+Selected].textBackground"));
			g.fillRect(0, 0, getWidth(), getHeight());
		} else if (row%2==0) {
			g.setColor(UIManager.getColor("Table.alternateRowColor"));
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		switch (type) {
		case Color: {
			Color col = (Color)value;
			g.setColor(col);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.fillRoundRect(2, 2, getWidth()-4, getHeight()-4, 10, 10);
		} break;
		case Painter: {
			if (value instanceof Icon) {
				paintIcon(g, (Icon)value);
			} else {
				Painter<JComponent> painter = (Painter<JComponent>)value;
				g.translate((getWidth()-getHeight())/2, 0);
				painter.paint((Graphics2D)g, this, getHeight(), getHeight());
			}
		} break;
		case Insets: {
			Insets in = (Insets)value;
			g.setColor(Color.BLACK);
			g.drawRect(2, 2, getWidth()-4, getHeight()-4);
			g.setColor(Color.GRAY);
			g.drawRect(3+in.left, 3+in.top, getWidth()-6-in.right-in.left, getHeight()-6-in.bottom-in.top);
		} break;
		case Font: {
			Font font = (Font)value;
			drawString(g, font.getFamily(), font);
		} break;
		case Boolean:
			drawString(g, value.toString(), BOOLEAN_FONT);
			break;
		case Integer: case String:
			drawString(g, value.toString(), getFont());
			break;
		case Icon: {
			paintIcon(g, (Icon)value);
		} break;
		case Dimension: {
			Dimension d = (Dimension)value;
			if (d.width < getWidth()-2 && d.height < getHeight()-2) {
				g.setColor(Color.GRAY);
				g.drawRect((getWidth()-d.width)/2, (getHeight()-d.height)/2, d.width, d.height);
			} else {
				drawString(g, d.width+" \u00D7 "+d.height, getFont());
			}
		} break;
		case Object: {
			drawString(g, getToolTipText(), getFont());
		} break;
		}
	}
	
	private void paintIcon(Graphics g, Icon icn) {
		int x = (getWidth()-icn.getIconWidth())/2;
		int y = (getHeight()-icn.getIconHeight())/2;
		icn.paintIcon(this, g, x, y);
	}
	
	private void drawString(Graphics g, String str, Font font) {
		g.setColor(selected ?
				UIManager.getColor("Table[Enabled+Selected].textForeground") :
				UIManager.getColor("Table.textForeground"));
		g.setFont(font);
		((Graphics2D)g).setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		FontMetrics metrics = g.getFontMetrics();
		int w = metrics.stringWidth(str);
		int y =(getHeight()-metrics.getHeight())/2+metrics.getAscent();
		int x;
		int cw = getWidth();
		if (w > cw) {
			w = metrics.charWidth('.')*3;
			int i = 0;
			while (w < cw)
				w += metrics.charWidth(str.charAt(i++));
			str = str.substring(0, i-1).concat("...");
			x = 0;
		} else {
			x = (cw-w)/2;
		}
		g.drawString(str, x, y);
	}
}
