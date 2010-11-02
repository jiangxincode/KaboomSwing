package aephyr.swing.event;

import aephyr.swing.cache.Cachable;

public class CacheEvent extends java.util.EventObject {
	
	public CacheEvent(Cachable source, int index) {
		super(source);
		this.index = index;
	}
	
	private int index;
	
	public int getIndex() {
		return index;
	}
	
}
