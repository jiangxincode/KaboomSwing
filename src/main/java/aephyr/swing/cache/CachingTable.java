/*
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package aephyr.swing.cache;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import aephyr.swing.event.CacheEvent;
import aephyr.swing.event.CacheListener;

public class CachingTable extends JTable implements Cachable {

	public interface RowData {
		Object getValueAt(int column);
	}
	
	public static class DefaultRowData implements RowData {
		public DefaultRowData(Object[] data) {
			this.data = data;
		}
		
		private Object[] data;
		
		public Object getValueAt(int column) {
			return data[column];
		}
	}
	
	public interface Model extends TableModel {
		
		Callable<RowData> getRowDataAt(int row);
		
		Object getErrorValueAt(int row, int column);
		
	}

	
	public CachingTable(Model model) {
		this(model, true);
	}
	
	public CachingTable(Model model, boolean cachingEnabled) {
		super(model);
		this.cachingEnabled = cachingEnabled;
		if (cachingEnabled)
			cachingModel = createCachingModel(model);
	}
	
	private ModelAdapter cachingModel;
	
	private float cacheThreshold = 1.0f;
	
	private int loadingDelay = 300;
	
	private DeferLoading deferLoading;
	
	private boolean cachingEnabled = true;
	
	protected ModelAdapter createCachingModel(Model model) {
		return new ModelAdapter(model);
	}
	
	public void setCachingEnabled(boolean enabled) {
		if (cachingEnabled != enabled) {
			cachingEnabled = enabled;
			if (enabled) {
				cachingModel = new ModelAdapter((Model)getModel());
				validateDeferLoading();
			} else {
				if (deferLoading != null) {
					deferLoading.dispose();
					deferLoading = null;
				}
				cachingModel = null;
			}
		}
	}
	
	public boolean isCachingEnabled() {
		return cachingEnabled;
	}
	
	/**
	 * Determines the cache behavior.
	 * <p>
	 * Float.POSITIVE_INFINITY: All elements are cached and so will only be loaded once.
	 * <p>
	 * 0.0f: Only the visible indices are cached.
	 * <p>
	 * 1.0f: The visible indices and one block increment in either
	 * direction are cached.
	 * <p>
	 * 2.0f: The visible indices and two block increments in either
	 * direction are cached.
	 * <p>
	 * etc.
	 * @param cacheThreshold
	 * @throws IllegalArguemntException if <code>cacheThreshold</code> is less than zero or Float.NaN
	 */
	public void setCacheThreshold(float cacheThreshold) {
		if (cacheThreshold < 0.0f || Float.isNaN(cacheThreshold))
			throw new IllegalArgumentException();
		this.cacheThreshold = cacheThreshold;
	}

	/**
	 * @return cache threshold
	 * @see #setCacheThreshold(float)
	 */
	public float getCacheThreshold() {
		return cacheThreshold;
	}
	
	/**
	 * Sets the delay for the deferred loading. Deferred loading is used when continuous scrolling is in progress.
	 * <p>
	 * A value of 0 disables deferred loading.
	 * 
	 * @param loadingDelay interval in milliseconds to wait for deferred loading
	 * @throws IllegalArgumentException if loadingDelay is less than zero
	 */
	public void setLoadingDelay(int loadingDelay) {
		if (loadingDelay < 0)
			throw new IllegalArgumentException();
		this.loadingDelay = loadingDelay;
		validateDeferLoading();
	}
	
	private void validateDeferLoading() {
		if (cachingEnabled) {
			if (loadingDelay == 0) {
				if (deferLoading != null) {
					deferLoading.dispose();
					deferLoading = null;
				}
			} else {
				if (deferLoading != null) {
					deferLoading.setLoadingDelay(loadingDelay);
				} else if (isDisplayable()) {
					initializeDeferLoading();
				}
			}
		}
	}
	
	/**
	 * @return interval in milliseconds to wait for deferred loading
	 * @see #setLoadingDelay(float)
	 */
	public int getLoadingDelay() {
		return loadingDelay;
	}
	
	/**
	 * Clears the cache so that the elements may be garbage collected.
	 * <p>
	 * By default, the cache is cleared when this component becomes undisplayable,
	 * but not when it is hidden.
	 */
	public void clearCache() {
		if (cachingEnabled)
			getCachingModel().clearCache();
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		if <code>model</code> is not an instance of <code>CachingTable.Model</code>
	 * @see {@link #setLoadingModel(Model)}
	 */
	@Override
	public void setModel(TableModel model) {
		if (!(model instanceof Model))
			throw new IllegalArgumentException();
		setModel((Model)model);
	}
	
	public void setModel(Model model) {
		super.setModel(model);
		if (cachingModel != null)
			cachingModel.setModel(model);
	}
	
	public ModelAdapter getCachingModel() {
		return cachingModel;
	}
	
	private int getFirstVisibleRow(Rectangle visibleRect) {
		return rowAtPoint(new Point(
				visibleRect.x, visibleRect.y));
	}
	
	private int getLastVisibleRow(Rectangle visibleRect) {
		return rowAtPoint(new Point(
				visibleRect.x, visibleRect.y+visibleRect.height-1));
	}

	@Override
	public int getFirstVisibleIndex() {
		return getFirstVisibleRow(getVisibleRect());
	}

	@Override
	public int getLastVisibleIndex() {
		return getLastVisibleRow(getVisibleRect());
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (cachingEnabled && loadingDelay > 0)
			initializeDeferLoading();
	}
	
	private void initializeDeferLoading() {
		for (Container p=getParent(); p!=null; p=p.getParent()) {
			if (p instanceof JScrollPane) {
				deferLoading = new DeferLoading(this, (JScrollPane)p);
				break;
			}
		}
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		if (deferLoading != null) {
			deferLoading.dispose();
			deferLoading = null;
		}
		clearCache();
	}
	

	/**
	 * Overridden to reset the range of caching before painting.
	 * No actual custom painting is performed.
	 */
	@Override
	protected void paintComponent(Graphics g) {
		if (cachingEnabled) {
			int offset;
			int length = getModel().getRowCount();
			if (Float.isInfinite(cacheThreshold)) {
				offset = 0;
			} else {
				Rectangle visible = getVisibleRect();
				int firstIndex = getFirstVisibleRow(visible);
				int lastIndex = getLastVisibleRow(visible);
				int range = lastIndex - firstIndex + 1;
				range = Math.round(range * cacheThreshold);
				firstIndex -= range;
				lastIndex += range;
				range = lastIndex - firstIndex + 1;
				if (range < length) {
					offset = firstIndex;
					length = range;
				} else {
					offset = 0;
				}
			}
			getCachingModel().setCacheRange(offset, length);
		}
		super.paintComponent(g);
	}
	
	private boolean scrollingKeyPressed = false;

	@Override
	protected void processKeyEvent(KeyEvent e) {
		super.processKeyEvent(e);
		if (deferLoading != null) {
			switch (e.getID()) {
			case KeyEvent.KEY_PRESSED:
				if (isScrollingKey(e)) {
					// only want to start defer loading if two consecutive
					// key presses come without a key release
					if (scrollingKeyPressed) {
						deferLoading.start();
					} else {
						scrollingKeyPressed = true;
					}
				}
				break;
			case KeyEvent.KEY_RELEASED:
				if (isScrollingKey(e)) {
					scrollingKeyPressed = false;
					deferLoading.stop();
				}
				break;
			}
		}
	}

	private boolean isScrollingKey(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_PAGE_UP: case KeyEvent.VK_PAGE_DOWN:
		case KeyEvent.VK_UP: case KeyEvent.VK_DOWN:
			return true;
		}
		return false;
	}
    
	@Override
	public void sorterChanged(RowSorterEvent e) {
		if (e.getType() == RowSorterEvent.Type.SORTED) {
			ModelAdapter adapter = getCachingModel();
			if (adapter != null)
				adapter.updateCache(e);
		}
		super.sorterChanged(e);
	}
	
	@Override
	public void tableChanged(TableModelEvent e) {
		if (cachingEnabled) {
			if (e.getLastRow() == Integer.MAX_VALUE) {
				getCachingModel().clearCache();
			} else if (e.getFirstRow() != TableModelEvent.HEADER_ROW) {
				getCachingModel().updateCache(e);
			}
		}
		super.tableChanged(e);
	}
	
	@Override
	public Object getValueAt(int row, int column) {
		return cachingEnabled ?
			getCachingModel().getValueAt(
				row, convertColumnIndexToModel(column)) :
			super.getValueAt(row, column);
	}
	
	public void addCacheListener(CacheListener lis) {
		listenerList.add(CacheListener.class, lis);
	}
	
	public void removeCacheListener(CacheListener lis) {
		listenerList.remove(CacheListener.class, lis);
	}
	
	protected void fireCacheUpdate(int row) {
		Object[] listeners = listenerList.getListenerList();
		CacheEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==CacheListener.class) {
				if (e == null)
					e = new CacheEvent(this, row);
				cachingEnabled = false;
				try {
					((CacheListener)listeners[i+1]).cacheUpdated(e);
				} finally {
					cachingEnabled = true;
				}
			}
		}
	}
	
	protected class ModelAdapter extends CachingModel {
		
		public ModelAdapter(Model model) {
			this.model = model;
		}
		
		private Model model;
		
		public Model getModel() {
			return model;
		}
		
		public void setModel(Model mdl) {
			if (mdl == null)
				throw new NullPointerException();
			clearCache();
			model = mdl;
		}

		@Override
		protected Object getErrorAt(int row, Exception e) {
			row = convertRowIndexToModel(row);
			Object[] data = new Object[model.getColumnCount()];
			for (int col = data.length; --col>=0;)
				data[col] = model.getErrorValueAt(row, col);
			return new DefaultRowData(data);
		}

		@Override
		protected Callable<?> getLoaderAt(int row) {
			row = convertRowIndexToModel(row);
			return model.getRowDataAt(row);
		}

		@Override
		protected void fireUpdate(int row) {
			int modelRow = convertRowIndexToModel(row);
			fireCacheUpdate(row);
			row = convertRowIndexToView(modelRow);
			if (row < 0) {
				resizeAndRepaint();
			} else {
				Rectangle r = getCellRect(row, 0, true);
				r.x = 0;
				r.width = getWidth();
				repaint(r);
			}
		}
		
		public Object getValueAt(int viewRow, int modelColumn) {
			Object value = getCachedAt(viewRow);
			if (value instanceof RowData)
				return ((RowData)value).getValueAt(modelColumn);
			return model.getValueAt(
					convertRowIndexToModel(viewRow), modelColumn);
		}

		private boolean isUnsorted() {
			RowSorter<?> sorter = getRowSorter();
			if (sorter == null)
				return true;
			if (sorter.getClass() == TableRowSorter.class && sorter.getSortKeys().isEmpty())
				return ((TableRowSorter<?>)sorter).getRowFilter() == null;
			return false;
		}
		
		void updateCache(TableModelEvent e) {
			int firstRow = e.getFirstRow();
			int lastRow = e.getLastRow();
			if (firstRow == lastRow) {
				firstRow = convertRowIndexToView(firstRow);
				updateCache(e.getType(), firstRow, firstRow);
			} else  if (isUnsorted()) {
				updateCache(e.getType(), firstRow, lastRow);
			} else {
				int[] rows = new int[lastRow - firstRow + 1];
				for (int i=rows.length; --i>=0;)
					rows[i] = convertRowIndexToView(firstRow + i);
				Arrays.sort(rows);
				int type = e.getType();
				int i = rows.length - 1;
				lastRow = rows[i];
				firstRow = lastRow;
				while (--i>=0) {
					int row = rows[i];
					if (row != firstRow-1) {
						updateCache(type, firstRow, lastRow);
						lastRow = row;
					}
					firstRow = row;
				}
				updateCache(type, firstRow, lastRow);
			}
		}
		
		void updateCache(RowSorterEvent e) {
			Object[] cached = getCachedValues(new Object[getRowCount()]);
			Object[] vals = new Object[cached.length];
			RowSorter<?> sorter = e.getSource();
			if (e.getPreviousRowCount() == 0) {
				for (int row=vals.length; --row>=0;) {
					int view = sorter.convertRowIndexToView(row);
					if (view >= 0)
						vals[view] = cached[row];
				}
			} else {
				for (int row=vals.length; --row>=0;) {
					int mdl = e.convertPreviousRowIndexToModel(row);
					if (mdl < 0)
						continue;
					int view = sorter.convertRowIndexToView(mdl);
					if (view >= 0)
						vals[view] = cached[row];
				}
			}
			setCachedValues(vals, 0);
		}
		
	}
	
	// unrelated to caching
	
	private boolean paintsFocus = true;
	
	public void setFocusPainted(boolean paintsFocus) {
		if (paintsFocus != this.paintsFocus) {
			this.paintsFocus = paintsFocus;
			if (isFocusOwner())
				repaint();
		}
	}
	
	public boolean isFocusPainted() {
		return paintsFocus;
	}
	
	/**
	 * Overridden to supply hasFocus as false to the renderers
	 * but still allow the table to be focusable.
	 */
	@Override
	public Component prepareRenderer(TableCellRenderer renderer,
			int row, int column) {
		if (paintsFocus)
			return super.prepareRenderer(renderer, row, column);
		Object value = getValueAt(row, column);
		boolean isSelected = false;
		// Only indicate the selection and focused cell if not printing
		if (!isPaintingForPrint()) {
			isSelected = isCellSelected(row, column);
		}
		return renderer.getTableCellRendererComponent(
				this, value, isSelected, false, row, column);
	}
	
}
