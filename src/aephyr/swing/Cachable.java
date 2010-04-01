package aephyr.swing;

public interface Cachable {
	
	public int getLoadingDelay();

	public int getFirstVisibleIndex();
	
	public int getLastVisibleIndex();
	
	public CachingModel getCachingModel();
}
