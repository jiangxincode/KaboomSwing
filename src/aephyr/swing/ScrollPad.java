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
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.EventListener;
import java.util.EventObject;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class ScrollPad extends JComponent {

	public static final int CENTER = 0;
	public static final int TOP_ARROW = 1;
	public static final int BOTTOM_ARROW = 2;
	public static final int LEFT_ARROW = 4;
	public static final int RIGHT_ARROW = 8;
	
	static final int DIAMETER = 40;
	
	public ScrollPad() {
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		border = new Ellipse2D.Float(1f, 1f, DIAMETER-2, DIAMETER-2);
		center = new Ellipse2D.Float(-3, -3, 6, 6);
		arrow = new GeneralPath();
		arrow.moveTo(-3, -DIAMETER/2+8);
		arrow.lineTo(3, -DIAMETER/2+8);
		arrow.lineTo(0, -DIAMETER/2+5);
		arrow.closePath();
	}
	
	private final Ellipse2D border;
	private final Ellipse2D center;
	private final GeneralPath arrow;
	
	private int visibleArrows;
	
	private int scrollArrow;
	
	private int pressedRegion = -1;
	
	protected void processMouseEvent(MouseEvent e) {
		switch (e.getID()) {
		case MouseEvent.MOUSE_PRESSED: setPressedRegion(getRegion(e), e); break;
		case MouseEvent.MOUSE_RELEASED: onRelease(e); break;
		case MouseEvent.MOUSE_ENTERED: setScrollArrow(0); break;
		}
		super.processMouseEvent(e);
	}
	
	private void setPressedRegion(int region, MouseEvent e) {
		if (region != pressedRegion) {
			pressedRegion = region;
			repaint();
		}
		if (region >= 0) {
			Object[] listeners = listenerList.getListenerList();
			Event ae = null;
			for (int i = listeners.length-2; i>=0; i-=2) {
				if (listeners[i]==Listener.class) {
					if (ae == null)
						ae = new Event(this, e, region);
					((Listener)listeners[i+1]).buttonPressed(ae);
				}
			}
		}
	}
	
	
	public void addScrollPadListener(Listener l) {
		listenerList.add(Listener.class, l);
	}
	
	public void removeScrollPadListener(Listener l) {
		listenerList.remove(Listener.class, l);
	}
	
	private void onRelease(MouseEvent e) {
		Object[] listeners = listenerList.getListenerList();
		Event ae = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==Listener.class) {
				if (ae == null)
					ae = new Event(this, e, getRegion(e));
				((Listener)listeners[i+1]).buttonReleased(ae);
			}
		}
		setPressedRegion(-1, null);
	}
	
	private int getRegion(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (y <= DIAMETER/4) {
			if (x >= DIAMETER/4 && x <= DIAMETER*3/4) {
				return visibleArrows & TOP_ARROW;
			}
		} else if (y >= DIAMETER*3/4) {
			if (x >= DIAMETER/4 && x <= DIAMETER*3/4) {
				return visibleArrows & BOTTOM_ARROW;
			}
		} else if (x <= DIAMETER/4) {
			return visibleArrows & LEFT_ARROW;
		} else if (x >= DIAMETER*3/4) {
			return visibleArrows & RIGHT_ARROW;
		} else {
			return CENTER;
		}
		return -1;
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

	public void paint(Graphics g) {
		Color bg = isBackgroundSet() ? getBackground() : UIManager.getColor("controlLtHighlight");
		if (bg == null)
			bg = Color.WHITE;
		Color fg = isForegroundSet() ? getForeground() : UIManager.getColor("controlDkShadow");
		if (fg == null)
			fg = Color.BLACK;
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setStroke(new BasicStroke(1.25f));
		Composite comp = g2.getComposite();
		AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);				
		g2.setComposite(alphaComposite);
		g2.setColor(bg);
		g2.fill(border);

		g2.setComposite(comp);
		g2.setColor(fg);
		g2.draw(border);

		g2.translate(DIAMETER/2, DIAMETER/2);
		g2.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		if ((visibleArrows & TOP_ARROW) != 0) {
			drawArrow(g2, fg, bg);
		}
		double theta = 0.0;
		if ((visibleArrows & BOTTOM_ARROW) != 0) {
			theta = Math.PI;
			arrow.transform(AffineTransform.getRotateInstance(theta));
			drawArrow(g2, fg, bg);
		}
		if ((visibleArrows & LEFT_ARROW) != 0) {
			arrow.transform(AffineTransform.getRotateInstance(theta-Math.PI/2));
			drawArrow(g2, fg, bg);
			theta = -Math.PI/2;
		}
		if ((visibleArrows & RIGHT_ARROW) != 0) {
			arrow.transform(AffineTransform.getRotateInstance(Math.PI/2-theta));
			drawArrow(g2, fg, bg);
			theta = Math.PI/2;
		}
		g2.setColor(fg);
		g2.draw(center);
		boolean fillCenter = true;
		int scrollArrow = pressedRegion;
		if (scrollArrow < 0) {
			fillCenter = false;
			scrollArrow = this.scrollArrow;
		}
		switch (scrollArrow) {
		default:
			g2.fill(center);
			transformArrow(theta, 0.0);
			return;
		case TOP_ARROW: theta = transformArrow(theta, 0.0); break;
		case BOTTOM_ARROW: theta = transformArrow(theta, Math.PI); break;
		case LEFT_ARROW: theta = transformArrow(theta, -Math.PI/2); break;
		case LEFT_ARROW | TOP_ARROW: theta = transformArrow(theta, -Math.PI/4); break;
		case LEFT_ARROW | BOTTOM_ARROW: theta = transformArrow(theta, -Math.PI*3/4); break;
		case RIGHT_ARROW: theta = transformArrow(theta, Math.PI/2); break;
		case RIGHT_ARROW | TOP_ARROW: theta = transformArrow(theta, Math.PI/4); break;
		case RIGHT_ARROW | BOTTOM_ARROW: theta = transformArrow(theta, Math.PI*3/4); break;
		}
		g2.draw(arrow);
		g2.fill(arrow);
		if (!fillCenter)
			g2.setColor(bg);
		g2.fill(center);
		transformArrow(theta, 0.0);
		
	}
	
	private double transformArrow(double a, double b) {
		if (a != b)
			arrow.transform(AffineTransform.getRotateInstance(b-a));
		return b;
	}

	private void drawArrow(Graphics2D g, Color fg, Color bg) {
		g.setColor(fg);
		g.draw(arrow);
		g.setColor(bg);
		g.fill(arrow);
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(DIAMETER, DIAMETER);
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
	}
	
}
