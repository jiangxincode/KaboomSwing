package aephyr.swing;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.Position;

public class CachingList extends JList implements Cachable {
	
	public interface Model {
		int getSize();
		Callable<?> getLoaderAt(int index);
		Object getLoadingElementAt(int index);
		Object getErrorElementAt(int index);
		String getSearchStringAt(int index);
		void addListDataListener(ListDataListener l);
		void removeListDataListener(ListDataListener l);
		
	}
	
	public CachingList(Model model) {
		this(new ModelAdapter(model));
	}
	
	protected CachingList(ModelAdapter adapter) {
		super(adapter);
	}
	
	private float cacheThreshold = 1.0f;
	
	private int loadingDelay = 300;
	
	private DeferLoading deferLoading;
	
	protected DeferLoading getDeferLoading() {
		return deferLoading;
	}
	
	public void setCustomLoading(boolean customLoading) {
		getCachingModel().setCustomLoading(customLoading);
	}
	
	public void setCachingEnabled(boolean enabled) {
		getCachingModel().setCachingEnabled(enabled);
	}
	
	public boolean isCachingEnabled() {
		return getCachingModel().isCachingEnabled();
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
	
	/**
	 * @return interval in milliseconds to wait for deferred loading
	 * @see #setLoadingDelay(float)
	 */
	public int getLoadingDelay() {
		return loadingDelay;
	}

	/**
	 * Sets the model for this CachingList.
	 * 
	 * @param model
	 */
	public void setLoadingModel(Model model) {
		getCachingModel().setModel(model);
	}

	/**
	 * @return the model for this CachingList
	 * @see #setLoadingModel(Model)
	 */
	public Model getLoadingModel() {
		return getCachingModel().getModel();
	}
	
	/**
	 * Clears the cache so that the elements may be garbage collected.
	 * <p>
	 * By default, the cache is cleared when this component becomes undisplayable,
	 * but not when it is hidden.
	 */
	public void clearCache() {
		getCachingModel().clearCache();
	}

	/**
	 * CachingList uses its own internal model to implement the caching/loading behavior.
	 * 
	 * @throws UnsupportedOperationException
	 * @see {@link #setLoadingModel(Model)}
	 */
	public void setModel(ListModel model) {
		throw new UnsupportedOperationException();
	}
	
	public ModelAdapter getCachingModel() {
		return (ModelAdapter)super.getModel();
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (loadingDelay > 0)
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
		int offset;
		int length = getModel().getSize();
		if (Float.isInfinite(cacheThreshold)) {
			offset = 0;
		} else {
			int firstIndex = getFirstVisibleIndex();
			int lastIndex = getLastVisibleIndex();
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
		case KeyEvent.VK_LEFT: case KeyEvent.VK_RIGHT:
			return true;
		}
		return false;
	}

	
	/**
	 * Overridden to use string values supplied by {@link #Model.getSearchStringAt(int)}.
	 */
	@Override
	public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
		Model model = getCachingModel().getModel();
		int max = model.getSize();
		if (prefix == null) {
			throw new IllegalArgumentException();
		}
		if (startIndex < 0 || startIndex >= max) {
			throw new IllegalArgumentException();
		}
		prefix = prefix.toUpperCase();

		// start search from the next element after the selected element
		int increment = (bias == Position.Bias.Forward) ? 1 : -1;
		int index = startIndex;
		do {
			String string = model.getSearchStringAt(index);
			if (string != null && string.toUpperCase().startsWith(prefix)) {
				return index;
			}
			index = (index + increment + max) % max;
		} while (index != startIndex);
		return -1;
	}
	
	
	protected static class ModelAdapter extends CachingModel
			implements ListModel, ListDataListener {

		public ModelAdapter(Model model) {
			this.model = model;
			model.addListDataListener(this);
		}
		
		private Model model;
		
		private ListDataListener[] listeners;
		
		private boolean cachingEnabled = true;
		
		public void setCachingEnabled(boolean enabled) {
			cachingEnabled = enabled;
		}
		
		public boolean isCachingEnabled() {
			return cachingEnabled;
		}
		
		public Model getModel() {
			return model;
		}
		
		public void setModel(Model mdl) {
			if (mdl == null)
				throw new NullPointerException();
			model.removeListDataListener(this);
			int oldSize = model.getSize();
			clearCache();
			model = mdl;
			mdl.addListDataListener(this);
			int newSize = mdl.getSize();
			if (newSize < oldSize) {
				fireIntervalRemoved(newSize, oldSize-1);
				if (newSize > 0)
					fireContentsChanged(0, newSize-1);
			} else if (newSize > oldSize) {
				if (oldSize > 0)
					fireContentsChanged(0, oldSize-1);
				fireIntervalAdded(oldSize, newSize-1);
			} else if (newSize > 0) {
				fireContentsChanged(0, newSize-1);
			}
		}

		
		@Override
		protected Object getErrorAt(int index, Exception e) {
			return model.getErrorElementAt(index);
		}

		@Override
		protected Callable<?> getLoaderAt(int index) {
			return model.getLoaderAt(index);
		}

		@Override
		protected void fireUpdate(int index) {
			fireContentsChanged(index, index);
		}

		
		@Override
		public Object getElementAt(int index) {
			if (cachingEnabled) {
				Object value = getCachedAt(index);
				if (value != null)
					return value;
			}
			return model.getLoadingElementAt(index);
		}

		@Override
		public int getSize() {
			return model.getSize();
		}

		@Override
		public void addListDataListener(ListDataListener l) {
			if (listeners == null) {
				listeners = new ListDataListener[] { l };
			} else {
				int i = listeners.length;
				listeners = Arrays.copyOf(listeners, i+1);
				listeners[i] = l;
			}
		}
		
		@Override
		public void removeListDataListener(ListDataListener l) {
			if (listeners == null)
				return;
			if (listeners.length == 1) {
				if (listeners[0] == l)
					listeners = null;
			} else {
				ArrayList<ListDataListener> list = new ArrayList<ListDataListener>(
						Arrays.asList(listeners));
				if (list.remove(l))
					listeners = list.toArray(new ListDataListener[list.size()]);
			}
		}
		
		
		@Override
		public void contentsChanged(ListDataEvent e) {
			updateCache(UPDATE, e.getIndex0(), e.getIndex1());
			fireContentsChanged(e.getIndex0(), e.getIndex1());
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			updateCache(INSERT, e.getIndex0(), e.getIndex1());
			fireIntervalAdded(e.getIndex0(), e.getIndex1());
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			updateCache(DELETE, e.getIndex0(), e.getIndex1());
			fireIntervalRemoved(e.getIndex0(), e.getIndex1());
		}

		
		protected void fireContentsChanged(int firstIndex, int lastIndex) {
			if (listeners != null) {
				ListDataEvent e = new ListDataEvent(this, 
						ListDataEvent.CONTENTS_CHANGED, firstIndex, lastIndex);
				for (ListDataListener l : listeners)
					l.contentsChanged(e);
			}
		}
		
		protected void fireIntervalAdded(int firstIndex, int lastIndex) {
			if (listeners != null) {
				ListDataEvent e = new ListDataEvent(this, 
						ListDataEvent.INTERVAL_ADDED, firstIndex, lastIndex);
				for (ListDataListener l : listeners)
					l.intervalAdded(e);
			}
		}
		
		protected void fireIntervalRemoved(int firstIndex, int lastIndex) {
			if (listeners != null) {
				ListDataEvent e = new ListDataEvent(this, 
						ListDataEvent.INTERVAL_REMOVED, firstIndex, lastIndex);
				for (ListDataListener l : listeners)
					l.intervalRemoved(e);
			}
		}
		
	}
	
}