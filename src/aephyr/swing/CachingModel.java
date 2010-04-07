package aephyr.swing;

import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;


abstract class CachingModel {
	
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
			if (values == null)
				return;
			int idx = index-offset;
			if (idx < 0 || idx >= values.length || values[idx] != this) {
				idx = Arrays.asList(values).indexOf(this);
				if (idx < 0)
					return;
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
	
	public void setCacheRange(int newOffset, int length) {
		if (values == null) {
			values = new Object[length];
		} else {
			if (offset > newOffset) {
				int dstPos = offset-newOffset;
				if (dstPos < values.length) {
					System.arraycopy(values, 0, values, dstPos, values.length-dstPos);
					Arrays.fill(values, 0, dstPos, null);
				} else {
					Arrays.fill(values, null);
				}
			} else if (offset < newOffset) {
				int srcPos = newOffset - offset;
				if (srcPos < values.length) {
					System.arraycopy(values, srcPos, values, 0, values.length-srcPos);
					Arrays.fill(values, values.length-srcPos, values.length, null);
				} else {
					Arrays.fill(values, null);
				}
			}
			if (values.length != length) {
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
	

	public void loadIndex(int index) {
		Callable<?> callable = getLoaderAt(index);
		if (callable != null) {
			Loader loader = new Loader(index, callable);
			values[index-offset] = loader;
			loader.execute();
		}
	}
	
	public static final int DELETE = TableModelEvent.DELETE;
	
	public static final int UPDATE = TableModelEvent.UPDATE;
	
	public static final int INSERT = TableModelEvent.INSERT;
	
	protected void updateCache(int type, int firstIndex, int lastIndex) {
		if (values != null) {
			firstIndex -= offset;
			lastIndex -= offset;
			if (lastIndex >= 0 && firstIndex < values.length) {
				if (firstIndex < 0)
					firstIndex = 0;
				if (++lastIndex > values.length)
					lastIndex = values.length;
				switch (type) {
				case DELETE:
					System.arraycopy(values, lastIndex, values, firstIndex, values.length-lastIndex);
					Arrays.fill(values, values.length-lastIndex, values.length, null);
					break;
				case UPDATE:
					Arrays.fill(values, firstIndex, lastIndex, null);
					break;
				case INSERT:
					System.arraycopy(values, firstIndex, values, lastIndex, values.length-lastIndex);
					Arrays.fill(values, firstIndex, lastIndex, null);
					break;
				}
			}
		}
	}

}
