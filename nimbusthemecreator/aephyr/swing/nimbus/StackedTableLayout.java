package aephyr.swing.nimbus;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JScrollPane;
import javax.swing.JTable;

class StackedTableLayout implements LayoutManager {
	
	StackedTableLayout() {
		this(5, 15, true);
	}
	
	StackedTableLayout(int minRows, int prefRows, boolean fillHeight) {
		this.minRows = minRows;
		this.prefRows = prefRows;
		this.fillHeight = fillHeight;
	}
	
	private int minRows;
	private int prefRows;
	private boolean fillHeight;

	@Override
	public void addLayoutComponent(String name, Component comp) {}
	
	private JScrollPane[] scrollers(Container parent) {
		synchronized (parent.getTreeLock()) {
			int n = parent.getComponentCount();
			if (n == 0)
				return null;
			JScrollPane[] scrollers = new JScrollPane[n];
			while (--n>=0)
				scrollers[n] = (JScrollPane)parent.getComponent(n);
			return scrollers;
		}
	}

	@Override
	public void layoutContainer(Container parent) {
		JScrollPane[] scrollers = scrollers(parent);
		if (scrollers == null)
			return;
		int[] max = new int[scrollers.length];
		int[] rowHeights = new int[scrollers.length];
		int[] yInsets = new int[scrollers.length];
		int maxTot = 0;
		Insets insets = parent.getInsets();
		int y = insets.top;
		int x = insets.left;
		int height = parent.getHeight() - y - insets.bottom;
		int width = parent.getWidth() - x - insets.right;
		for (int i=scrollers.length; --i>=0;) {
			JTable table = (JTable)scrollers[i].getViewport().getView();
			Dimension size = scrollers[i].getPreferredSize();
			int h = size.height;
			size = table.getPreferredScrollableViewportSize();
			yInsets[i] = h - size.height;
			rowHeights[i] = table.getRowHeight();
			max[i] = table.getRowHeight() * table.getRowCount();
			maxTot += max[i] + yInsets[i];
		}
		if (maxTot <= height) {
			for (int i=0; i<scrollers.length; i++) {
				int h = max[i]+yInsets[i];
				scrollers[i].setBounds(x, y, width, h);
				y += h;
			}
		} else {
			int count = max.length;
			int availableHeight = height;
			while (count > 1) {
				int min = Integer.MAX_VALUE;
				int minIdx = -1;
				for (int i=max.length; --i>=0;) {
					if (max[i] >= 0 && max[i]+yInsets[i] < min) {
						min = max[i]+yInsets[i];
						minIdx = i;
					}
				}
				if (min > availableHeight/count)
					break;
				availableHeight -= min;
				max[minIdx] = -min;
				count--;
			}
			int rem = availableHeight % count;
			availableHeight /= count;
			for (int i=scrollers.length; --i>=0;) {
				int h = max[i];
				if (h < 0)
					continue;
				if (h+yInsets[i] > availableHeight) {
					h = availableHeight;
					int r = (h - yInsets[i]) % rowHeights[i];
					h -= r;
					rem += r;
					max[i] = h;
				} else {
					max[i] = -h - yInsets[i];
				}
			}
			for (int i=0; i<scrollers.length; i++) {
				int h = max[i];
				if (h < 0) {
					h = -h;
				} else {
					if (rem > rowHeights[i]) {
						h += rowHeights[i];
						rem -= rowHeights[i];
					}
				}
				scrollers[i].setBounds(x, y, width, h);
				y += h;
			}
		}
		if (fillHeight) {
			JScrollPane s = scrollers[scrollers.length-1];
			s.setSize(width, s.getHeight()+height-y);
		}
	}


	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return size(parent, true);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return size(parent, false);
	}
	
	private Dimension size(Container parent, boolean min) {
		JScrollPane[] scrollers = scrollers(parent);
		if (scrollers == null)
			return new Dimension(0, 0);
		Insets insets = parent.getInsets();
		int height = insets.top + insets.bottom;
		int xInsets = insets.left + insets.right;
		int maxWidth = 0;
		int rows = min ? minRows : prefRows;
		for (int i=scrollers.length; --i>=0;) {
			JTable table = (JTable)scrollers[i].getViewport().getView();
			Dimension size = scrollers[i].getPreferredSize();
			int w = size.width;
			int h = size.height;
			size = table.getPreferredScrollableViewportSize();
			height += h - size.height + 
				Math.min(rows, table.getRowCount()) * table.getRowHeight();
			w -= size.width;
			size = min ? table.getMinimumSize() : table.getPreferredSize();
			w += size.width;
			if (w > maxWidth)
				maxWidth = w;
		}
		return new Dimension(maxWidth+xInsets, height);
	}

	@Override
	public void removeLayoutComponent(Component comp) {}
	
}
