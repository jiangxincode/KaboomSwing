package aephyr.swing.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;



public abstract class CachingModel implements Runnable {
	
	private class Loader extends SwingWorker<Object,Object> {
		
		Loader(int idx, Callable<?> ldr) {
			index = idx;
			loader = ldr;
		}
		
		private int index;
		
		private Callable<?> loader;

		@Override
		protected Object doInBackground() throws Exception {
			return loader.call();
		}
		
		@Override
		protected void done() {
			if (values == null || isCancelled())
				return;
			int idx = index-offset;
			if (idx < 0 || idx >= values.length || values[idx] != this) {
				idx = indexOf(this);
				if (idx < 0) {
					System.out.println("loader not found: " + index);
					return;
				}
			}
			Object value;
			try {
				value = get();
			} catch (Exception e) {
				value = getErrorAt(idx+offset, e);
			}
			values[idx] = value;
			fireUpdate(idx+offset);
		}
		
		public String toString() {
			return "Loader: " + index;
		}
	}
	
	protected CachingModel() {}
	
	private Object[] values;
	private int offset;
	
	private DeferLoading deferLoading;
	
	protected abstract Object getErrorAt(int index, Exception e);

	protected abstract void fireUpdate(int index);
	
	protected abstract Callable<?> getLoaderAt(int index);
	
	public void setDeferLoading(DeferLoading deferLoading) {
		this.deferLoading = deferLoading;
	}
	
	public void clearCache() {
		values = null;
	}
	
	private void clear(Object[] values, int fromIndex, int toIndex) {
		while (--toIndex >= fromIndex) {
			Object obj = values[toIndex];
			if (obj != null) {
				if (obj instanceof Loader)
					cancel((Loader)obj);
				values[toIndex] = null;
			}
		}
	}
	
	public void setCacheRange(int newOffset, int length) {
		if (values == null) {
			values = new Object[length];
		} else {
			if (offset > newOffset) {
				int dstPos = offset-newOffset;
				if (dstPos < values.length) {
					insertRange(0, dstPos, false);
				} else {
					clear(values, 0, values.length);
				}
			} else if (offset < newOffset) {
				int srcPos = newOffset - offset;
				if (srcPos < values.length) {
					removeRange(0, srcPos, false);
				} else {
					clear(values, 0, values.length);
				}
			}
			if (values.length != length) {
				if (values.length > length)
					clear(values, length, values.length);
				values = Arrays.copyOf(values, length);
			}
		}
		offset = newOffset;
	}

	/**
	 * @param index index of cached object in the loading model coordinates
	 * @return cached value or null if the value is loading or not in the cache range
	 */
	protected Object getCachedAt(int index) {
		int idx = index-offset;
		if (values == null || idx < 0 || idx >= values.length)
			return null;
		Object value = values[idx];
		if (value == null) {
			if (deferLoading != null) {
				deferLoading.addIndex(index);
			} else {
				loadIndex(index);
			}
		} else if (value instanceof Loader) {
			value = null;
		}
		return value;
	}
	
	protected Object[] getCachedValues(Object[] dst) {
		int off = Math.max(0, offset);
		int len = Math.min(values.length, dst.length-off);
		if (len > 0)
			System.arraycopy(values, 0, dst, off, len);
		return dst;
	}
	
	protected void setCachedValues(Object[] values, int offset) {
		this.offset = offset;
		this.values = values;
		if (loaders != null && !loaders.isEmpty()) {
			synchronized (this) {
				remap(-1);
			}
		}
	}
	

	public void loadIndex(int index) {
		Callable<?> callable = getLoaderAt(index);
		if (callable != null) {
			Loader loader = new Loader(index, callable);
			values[index-offset] = loader;
			execute(loader);
		}
	}
	
	public static final int DELETE = TableModelEvent.DELETE;
	
	public static final int UPDATE = TableModelEvent.UPDATE;
	
	public static final int INSERT = TableModelEvent.INSERT;
	
	protected void updateCache(int type, int firstIndex, int lastIndex) {
		if (values != null) {
			firstIndex -= offset;
			lastIndex -= offset;
			if (firstIndex < 0) {
				if (type == DELETE)
					offset += firstIndex-lastIndex-1;
				firstIndex = 0;
			}
			if (lastIndex >= 0 && firstIndex < values.length) {
				if (++lastIndex > values.length)
					lastIndex = values.length;
				switch (type) {
				case DELETE:
					removeRange(firstIndex, lastIndex, true);
					break;
				case UPDATE:
					clear(values, firstIndex, lastIndex);
					break;
				case INSERT:
					insertRange(firstIndex, lastIndex, true);
					break;
				}
			}
		}
	}
	
	private void remap(Integer key) {
		key = loaders.ceilingKey(key);
		if (key != null) {
			Loader loader = loaders.remove(key);
			remap(key);
			int idx = indexOf(loader);
			if (idx >= 0) {
				loader.index = offset + idx;
				loaders.put(loader.index, loader);
			}
		}
	}
	
	private int indexOf(Object obj) {
		Object[] v = values;
		for (int i=v.length; --i>=0;)
			if (v[i] == obj)
				return i;
		return -1;
	}
	
	
	private void removeRange(int firstIndex, int lastIndex, boolean shiftLoaderIndices) {
		clear(values, firstIndex, lastIndex);
		System.arraycopy(values, lastIndex, values, firstIndex, values.length-lastIndex);
		Arrays.fill(values, firstIndex+values.length-lastIndex, values.length, null);
		if (shiftLoaderIndices && loaders != null && !loaders.isEmpty()) {
			synchronized (this) {
				remap(firstIndex-1);
			}
		}
	}
	
	private void insertRange(int firstIndex, int lastIndex, boolean shiftLoaderIndices) {
		int len = values.length-lastIndex;
		clear(values, firstIndex+len, values.length);
		System.arraycopy(values, firstIndex, values, lastIndex, len);
		Arrays.fill(values, firstIndex, lastIndex, null);
		if (shiftLoaderIndices && loaders != null && !loaders.isEmpty()) {
			synchronized (this) {
				remap(firstIndex-1);
			}
		}
	}
	
	public void setCustomLoading(boolean custom) {
		TreeMap<Integer,Loader> map = loaders;
		if (custom) {
			if (map == null) {
				synchronized (this) {
					loaders = new TreeMap<Integer,Loader>();
				}
			}
		} else {
			if (map != null) {
				synchronized (this) {
					loaders = null;
					notifyAll();
				}
				for (Loader loader : map.values())
					loader.execute();
			}
		}
	}
	
	private TreeMap<Integer,Loader> loaders;
	
	private Thread thread;
	
	private void cancel(Loader loader) {
		loader.cancel(true);
		if (loaders != null) {
			synchronized (this) {
				loaders.remove(loader.index);
			}
		}
	}
	
	private void execute(Loader loader) {
		if (loaders == null) {
			loader.execute();
		} else {
			synchronized (this) {
				loaders.put(loader.index, loader);
				if (thread == null) {
					thread = new Thread(this);
					thread.setDaemon(true);
					thread.start();
				} else {
					notifyAll();
				}
			}
		}
	}
	
	@Override
	public void run() {
		for (;;) {
			Map.Entry<?,Loader> entry = null;
			synchronized (this) {
				if (loaders != null)
					entry = loaders.pollFirstEntry();
				if (entry == null) {
					if (loaders != null) {
						try {
							wait(30000);
							if (loaders != null && !loaders.isEmpty())
								continue;
						} catch (InterruptedException e) {}
					}
					thread = null;
					return;
				}
			}
			entry.getValue().run();
		}
	}

}
