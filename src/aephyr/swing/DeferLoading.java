package aephyr.swing;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;


public class DeferLoading implements AdjustmentListener,
		ActionListener, PropertyChangeListener {
	
	public DeferLoading(Cachable cachable, JScrollPane scroller) {
		this.cachable = cachable;
		if (scroller != null) {
			this.scroller =  scroller;
			JScrollBar vsb = scroller.getVerticalScrollBar();
			if (vsb != null)
				vsb.addAdjustmentListener(this);
			JScrollBar hsb = scroller.getHorizontalScrollBar();
			if (hsb != null)
				hsb.addAdjustmentListener(this);
			scroller.addPropertyChangeListener(this);
		}
	}
	
	private Cachable cachable;
	
	private JScrollPane scroller;
	
	private Timer timer;
	
	private Set<Integer> loadingSet;
	
	private boolean scrollingKeyPressed = false;
	
	private boolean managed = false;
	
	public void addIndex(int idx) {
		loadingSet.add(new Integer(idx));
	}
	
	public void setLoadingDelay(int delay) {
		if (timer != null) {
			timer.setDelay(delay);
			timer.setInitialDelay(delay);
		}
	}
	
	public void start() {
		managed = true;
		restart();
	}
	
	public void stop() {
		managed = false;
		if (timer != null)
			stop(true);
	}
	
	private void restart() {
		if (timer == null) {
			loadingSet = new HashSet<Integer>();
			timer = new Timer(cachable.getLoadingDelay(), this);
			timer.start();
			cachable.getCachingModel().setDeferLoading(this);
		} else {
			timer.restart();
		}
	}
	
	private void stop(boolean loadVisibleIndices) {
		cachable.getCachingModel().setDeferLoading(null);
		timer.stop();
		timer = null;
		if (loadVisibleIndices)
			loadVisibleIndices();
		loadingSet = null;
	}
	
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getValueIsAdjusting() || EventQueue.getCurrentEvent() instanceof MouseWheelEvent) {
			restart();
		} else if (timer != null && !scrollingKeyPressed && !managed) {
			stop(true);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (managed) {
			stop(true);
			managed = false;
		} else {
			loadVisibleIndices();
		}
	}
	
	private void loadVisibleIndices() {
		int firstIndex = cachable.getFirstVisibleIndex();
		int lastIndex = cachable.getLastVisibleIndex();
		CachingModel model = cachable.getCachingModel();
		Iterator<Integer> iterator = loadingSet.iterator();
		while (iterator.hasNext()) {
			int idx = iterator.next();
			if (idx >= firstIndex && idx <= lastIndex) {
				model.loadIndex(idx);
				iterator.remove();
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName() == "horizontalScrollBar" ||
				evt.getPropertyName() == "verticalScrollBar") {
			JScrollBar oldValue = (JScrollBar)evt.getOldValue();
			if (oldValue != null)
				oldValue.removeAdjustmentListener(this);
			JScrollBar newValue = (JScrollBar)evt.getNewValue();
			if (newValue != null)
				newValue.addAdjustmentListener(this);
		}
	}
	
	public void dispose() {
		JScrollPane scroller = this.scroller;
		if (scroller != null) {
			this.scroller = null;
			scroller.removePropertyChangeListener(this);
			JScrollBar vsb = scroller.getVerticalScrollBar();
			if (vsb != null)
				vsb.removeAdjustmentListener(this);
			JScrollBar hsb = scroller.getHorizontalScrollBar();
			if (hsb != null)
				hsb.removeAdjustmentListener(this);
		}
		if (timer != null)
			stop(false);
	}

}
