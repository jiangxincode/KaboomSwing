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
package aephyr.swing;

import java.awt.AWTEvent;

import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.UIManager;

import aephyr.swing.ui.ScrollPadUI;

// TODO, create ScrollPadUI and defer painting to it
public class ScrollPad extends JComponent {

	public static final int CENTER = 0;
	public static final int TOP_ARROW = 1;
	public static final int BOTTOM_ARROW = 2;
	public static final int LEFT_ARROW = 4;
	public static final int RIGHT_ARROW = 8;
	
	public ScrollPad() {
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		updateUI();
	}
	
	private int visibleArrows;
	
	private int scrollArrow;
	
	private int pressedRegion = -1;
	
	protected void processMouseEvent(MouseEvent e) {
		switch (e.getID()) {
		case MouseEvent.MOUSE_CLICKED:
		case MouseEvent.MOUSE_PRESSED:
		case MouseEvent.MOUSE_RELEASED:
			if (ui != null)
				onEvent(e);
			break;
		case MouseEvent.MOUSE_ENTERED:
			setScrollArrow(0);
			break;
		}
		super.processMouseEvent(e);
	}
	
//	private void setPressedRegion(int region, MouseEvent e) {
//		if (region != pressedRegion) {
//			pressedRegion = region;
//			repaint();
//		}
//		if (region >= 0) {
//			Object[] listeners = listenerList.getListenerList();
//			Event ae = null;
//			for (int i = listeners.length-2; i>=0; i-=2) {
//				if (listeners[i]==Listener.class) {
//					if (ae == null)
//						ae = new Event(this, e, region);
//					((Listener)listeners[i+1]).buttonPressed(ae);
//				}
//			}
//		}
//	}
	
	private void onEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			pressedRegion = getRegion(e);
			repaint();
		}
		Object[] listeners = listenerList.getListenerList();
		Event ae = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==Listener.class) {
				if (ae == null)
					ae = new Event(this, e, getRegion(e));
				switch (e.getID()) {
				case MouseEvent.MOUSE_CLICKED: ((Listener)listeners[i+1]).buttonClicked(ae); break;
				case MouseEvent.MOUSE_PRESSED: ((Listener)listeners[i+1]).buttonPressed(ae); break;
				case MouseEvent.MOUSE_RELEASED: ((Listener)listeners[i+1]).buttonReleased(ae); break;
				}
			}
		}
		if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			pressedRegion = -1;
			repaint();
		}
	}
	
	private int getRegion(MouseEvent e) {
		return getUI().getRegion(this, e.getX(), e.getY());
	}
	
	public void addScrollPadListener(Listener l) {
		listenerList.add(Listener.class, l);
	}
	
	public void removeScrollPadListener(Listener l) {
		listenerList.remove(Listener.class, l);
	}
	
	public void setVisibleArrows(int arrows) {
		if (arrows != visibleArrows) {
			visibleArrows = arrows;
			repaint();
		}
	}
	
	public int getVisibleArrows() {
		return visibleArrows;
	}

	public void setScrollArrow(int arrow) {
		arrow = visibleArrows & arrow;
		if (arrow != scrollArrow) {
			scrollArrow = arrow;
			repaint();
		}
	}
	
	public int getScrollArrow() {
		return scrollArrow;
	}
	
	public int getPressedButton() {
		return pressedRegion;
	}

	public class Event extends EventObject {
		public Event(ScrollPad source, MouseEvent event, int button) {
			super(source);
			this.button = button;
			this.event = event;
		}
		
		MouseEvent event;
		
		int button;
		
		public int getButton() {
			return button;
		}
		
		public MouseEvent getMouseEvent() {
			return event;
		}
		
	}
	
	public interface Listener extends EventListener {
		void buttonPressed(Event e);
		void buttonReleased(Event e);
		void buttonClicked(Event e);
	}

	// UI interface

	private static final String uiClassID = "ScrollPadUI";

    public void setUI(ScrollPadUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        if (UIManager.get(getUIClassID()) != null) {
            setUI((ScrollPadUI)UIManager.getUI(this));
        } else {
            setUI(ScrollPadUI.createUI(this));
        }
    }

    public ScrollPadUI getUI() {
        return (ScrollPadUI)ui;
    }

    public String getUIClassID() {
        return uiClassID;
    }
    
}
