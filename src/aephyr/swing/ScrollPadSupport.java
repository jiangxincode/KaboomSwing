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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;


public class ScrollPadSupport extends MouseAdapter implements ActionListener, PopupMenuListener, ScrollPad.Listener {
	
	private static final int TOP = ScrollPad.TOP_ARROW;
	private static final int BOTTOM = ScrollPad.BOTTOM_ARROW;
	private static final int LEFT = ScrollPad.LEFT_ARROW;
	private static final int RIGHT = ScrollPad.RIGHT_ARROW;
	
	public ScrollPadSupport() {}
	
	/**
	 * Registers the specified component to automatically show a
	 * ScrollPad when the middle mouse button is clicked.
	 * 
	 * @param c
	 * 
	 * @see #unregister(JComponent)
	 */
	public void register(JComponent c) {
		c.addMouseListener(this);
	}
	
	/**
	 * Unregisters the specified component from automatically showing
	 * a ScrollPad.
	 * 
	 * @param c
	 * 
	 * @see #register(JComponent)
	 */
	public void unregister(JComponent c) {
		c.removeMouseListener(this);
	}
	
	private final Rectangle visible = new Rectangle();
	
	private JPopupMenu scrollPopup;
	
	private ScrollPad scrollPad;
	
	private Timer scrollTimer;
	
	private int originX;
	private int originY;
	
	private int currentX;
	private int currentY;

	private boolean buttonAutoscrolls = false;
	private int scrollButton = -1;
	private int scrollMagnitude;
	
	/**
	 * 
	 * @return the shared ScrollPad component used by this instance
	 */
	public ScrollPad getScrollPad() {
		if (scrollPad == null)
			scrollPad = new ScrollPad();
		return scrollPad;
	}

	/**
	 * Sets the behavior associated with clicking on the arrow
	 * buttons of the scroll pad. If true, a click will initial
	 * autoscrolling in the specified directions and subsequent
	 * clicks will speed up the autoscrolling. If false, the 
	 * behavior is similar to the scroll buttons of JScrollBar.
	 * 
	 * @param b - true if arrow buttons cause autoscrolling,
	 * 		default value is false
	 */
	public void setButtonAutoscrolls(boolean b) {
		buttonAutoscrolls = b;
	}
	
	/**
	 * 
	 * @return true if arrow buttons cause autoscrolling
	 * 
	 * @see #setButtonAutoscrolls(boolean)
	 */
	public boolean getButtonAutoscrolls() {
		return buttonAutoscrolls;
	}
	
	/**
	 * Sets the delay between subsequent calls to scroll for autoscrolling
	 * and when the arrow buttons are held down in a pressed state.
	 * 
	 * @param ms - delay between scroll calls in milliseconds
	 */
	public void setScrollSpeed(int ms) {
		if (ms < 0)
			throw new IllegalArgumentException();
		if (scrollTimer == null) {
			scrollTimer = new Timer(ms, this);
		} else {
			scrollTimer.setDelay(ms);
		}
	}
	
	/**
	 * 
	 * @return delay between scroll calls in milliseconds
	 * 
	 * @see #setScrollSpeed(int)
	 */
	public int getScrollSpeed() {
		return scrollTimer != null ? scrollTimer.getDelay() : 100;
	}
	
	/**
	 * Displays the scroll pad centered at the specified coordinates
	 * of Component <code>parent</code>. The scroll pad may not be
	 * displayed if the parent component is not big enough and the 
	 * coordinates may be adjusted so that the scroll pad does not
	 * extend outside of the parent component's bounds.
	 * 
	 * @param parent - the component to display the scroll pad over
	 * @param x - center x position to display the scroll pad
	 * @param y - center y position to display the scroll pad
	 */
	public void showScrollPad(JComponent parent, int x, int y) {
		Rectangle r = visible;
		parent.computeVisibleRect(r);
		if (r.width < ScrollPad.DIAMETER || r.height < ScrollPad.DIAMETER)
			return;
		int arrows = 0;
		if (r.y > 0)
			arrows |= TOP;
		if (r.y+r.height < parent.getHeight())
			arrows |= BOTTOM;
		if (r.x > 0)
			arrows |= LEFT;
		if (r.x+r.width < parent.getWidth())
			arrows |= RIGHT;
		if (arrows == 0)
			return;
		if (scrollPopup == null) {
			scrollPopup = new JPopupMenu();
			if (scrollPad == null)
				scrollPad = new ScrollPad();
			scrollPad.addScrollPadListener(this);
			if (scrollTimer == null)
				scrollTimer = new Timer(100, this);
			scrollTimer.setInitialDelay(300);
			scrollPopup.addPopupMenuListener(this);
			scrollPopup.setOpaque(false);
			scrollPopup.setBorder(BorderFactory.createEmptyBorder());
			scrollPopup.setLayout(new BorderLayout());
			scrollPopup.add(scrollPad, BorderLayout.CENTER);
			scrollPopup.setPopupSize(scrollPad.getPreferredSize());
		}
		scrollPad.setVisibleArrows(arrows);
		scrollPad.setScrollArrow(0);

//		x -= ScrollPad.DIAMETER/2;
//		y -= ScrollPad.DIAMETER/2;
//		if (x < r.x) {
//			x = r.x;
//		} else if (x+ScrollPad.DIAMETER > r.x+r.width) {
//			x = r.x + r.width - ScrollPad.DIAMETER;
//		}
//		if (y < r.y) {
//			y = r.y;
//		} else if (y+ScrollPad.DIAMETER > r.y+r.height) {
//			y = r.y + r.height - ScrollPad.DIAMETER;
//		}
//		Point p = new Point(x, y);
//		SwingUtilities.convertPointToScreen(p, parent);
//		currentX = originX = p.x + ScrollPad.DIAMETER/2;
//		currentY = originY = p.y + ScrollPad.DIAMETER/2;
		Point p = new Point(x, y);
		scrollPadLocation(parent, p);
		scrollPopup.show(parent, p.x, p.y);
		parent.addMouseMotionListener(this);
	}
	
	// the commented code above was refactored to this method so
	// DragScroller will be be able to share its functionality
	private void scrollPadLocation(Component parent, Point p) {
		Rectangle r = visible;
		p.x -= ScrollPad.DIAMETER/2;
		p.y -= ScrollPad.DIAMETER/2;
		if (p.x < r.x) {
			p.x = r.x;
		} else if (p.x+ScrollPad.DIAMETER > r.x+r.width) {
			p.x = r.x + r.width - ScrollPad.DIAMETER;
		}
		if (p.y < r.y) {
			p.y = r.y;
		} else if (p.y+ScrollPad.DIAMETER > r.y+r.height) {
			p.y = r.y + r.height - ScrollPad.DIAMETER;
		}
		Point pAbs = new Point(p);
		SwingUtilities.convertPointToScreen(pAbs, parent);
		currentX = originX = pAbs.x + ScrollPad.DIAMETER/2;
		currentY = originY = pAbs.y + ScrollPad.DIAMETER/2;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON2) {
			showScrollPad((JComponent)e.getSource(), e.getX(), e.getY());
		}
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		if (scrollPopup != null && scrollPopup.isVisible())
			scrollTimer.stop();
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
		if (scrollPopup != null && scrollPopup.isVisible())
			scrollTimer.start();
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		if (scrollButton >= 0)
			return;
		currentX = e.getXOnScreen();
		currentY = e.getYOnScreen();
		int x = currentX - originX;
		int y = currentY - originY;
		double theta = Math.atan2(y, x);
		if (theta <= Math.PI/8) {
			if (theta >= -Math.PI*5/8) {
				if (theta <= -Math.PI*3/8) {
					scrollPad.setScrollArrow(TOP);
				} else if (theta > -Math.PI/8) {
					scrollPad.setScrollArrow(RIGHT);
				} else {
					scrollPad.setScrollArrow(TOP | RIGHT);
				}
			} else {
				if (theta <= -Math.PI*7/8) {
					scrollPad.setScrollArrow(LEFT);
				} else {
					scrollPad.setScrollArrow(TOP | LEFT);
				}
			}
		} else {
			if (theta <= Math.PI*5/8) {
				if (theta >= Math.PI*3/8) {
					scrollPad.setScrollArrow(BOTTOM);
				} else {
					scrollPad.setScrollArrow(BOTTOM | RIGHT);
				}
			} else {
				if (theta >= Math.PI*7/8) {
					scrollPad.setScrollArrow(LEFT);
				} else {
					scrollPad.setScrollArrow(BOTTOM | LEFT);
				}
			}
		}
	}
	
	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		scrollTimer.stop();
		scrollPopup.getInvoker().removeMouseMotionListener(this);
		scrollButton = -1;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (scrollButton >= 0) {
			if ((scrollPad.getVisibleArrows() & scrollButton) != 0)
				scroll(scrollButton, scrollMagnitude);
		} else {
			int a = scrollPad.getScrollArrow();
			switch (scrollPad.getScrollArrow()) {
			case 0: return;
			case TOP: case BOTTOM: scroll(0, exp(currentY-originY), a, true); break;
			case LEFT: case RIGHT: scroll(exp(currentX-originX), 0, a, true); break;
			default: scroll(exp(currentX-originX), exp(currentY-originY), a, true); break;
			}
		} 
	}
	
	private void scroll(int scrollButton, int mag) {
		if (scrollPopup.getInvoker() instanceof Scrollable) {
			Scrollable s = (Scrollable)scrollPopup.getInvoker();
			int o;
			int d;
			switch (scrollButton) {
			default: return;
			case TOP: o = SwingConstants.VERTICAL; d = -1; break;
			case BOTTOM: o = SwingConstants.VERTICAL; d = 1; break;
			case LEFT: o = SwingConstants.HORIZONTAL; d = -1; break;
			case RIGHT: o = SwingConstants.HORIZONTAL; d = 1; break;
			}
			((JComponent)s).computeVisibleRect(visible);
			int u = s.getScrollableUnitIncrement(visible, o, d);
			mag *= u;
		}
		switch (scrollButton) {
		case TOP: scroll(0, -mag, scrollButton, false); break;
		case BOTTOM: scroll(0, mag, scrollButton, false); break;
		case LEFT: scroll(-mag, 0, scrollButton, false); break;
		case RIGHT: scroll(mag, 0, scrollButton, false); break;
		}
	}
	
	private void scroll(int dx, int dy, int arrow, boolean updateScrollArrow) {
		JComponent c = (JComponent)scrollPopup.getInvoker();
		c.computeVisibleRect(visible);
		visible.x += dx;
		visible.y += dy;
		int vArrows = scrollPad.getVisibleArrows();
		if ((arrow & TOP) != 0) {
			if (visible.y <= 0) {
				visible.y = 0;
				arrow &= ~TOP;
				vArrows &= ~TOP;
			}
			vArrows |= BOTTOM;
		} else if ((arrow & BOTTOM) != 0) {
			if (visible.y+visible.height >= c.getHeight()) {
				visible.y = c.getHeight()-visible.height;
				arrow &= ~BOTTOM;
				vArrows &= ~BOTTOM;
			}
			vArrows |= TOP;
		}
		if ((arrow & LEFT) != 0) {
			if (visible.x <= 0) {
				visible.x = 0;
				arrow &= ~LEFT;
				vArrows &= ~LEFT;
			}
			vArrows |= RIGHT;
		} else if ((arrow & RIGHT) != 0) {
			if (visible.x+visible.width >= c.getWidth()) {
				visible.x = c.getWidth()-visible.width;
				arrow &= ~RIGHT;
				vArrows &= ~RIGHT;
			}
			vArrows |= LEFT;
		}
		scrollPad.setVisibleArrows(vArrows);
		if (updateScrollArrow)
			scrollPad.setScrollArrow(arrow);
		c.scrollRectToVisible(visible);
	}

	@Override
	public void buttonPressed(ScrollPad.Event e) {
		int button = e.getButton();
		if (button != scrollButton) {
			if (button == ScrollPad.CENTER) {
				scrollButton = -1;
			} else {
				scrollButton = button;
				scrollMagnitude = 1;
			}
		} else {
			scrollMagnitude++;
		}
		scroll(button, 1);
		scrollTimer.restart();
	}

	@Override
	public void buttonReleased(ScrollPad.Event e) {
		if (!buttonAutoscrolls) {
			scrollButton = -1;
		}
	}

	// f(x) = ax*x + b/x, 	f(1) = 1, 	f(100) = 100
	// 1 = a + b 	-> b = 1-a
	// 100 = a100*100 + (1-a)/100
	// 100*100 = a100*100*100 + 1 - a	-> a = (100*100-1)/(100*100*100-1)
	private static final float a = (100f*100 - 1)/(100*100*100 - 1);
	private static final float b = 1 - a;
	private static int exp(int n) {
		// n will be in the range -ScrollPad.DIAMETER/2 > n > ScrollPad.DIAMETER/2
		// therefore never 0
		return n < 0 ?
				-(int)(a*n*n - b/n) :
				(int)(a*n*n + b/n);
	}
	
	private class DragScroller extends MouseAdapter {
		
		Point start = new Point();

		@Override
		public void mousePressed(MouseEvent e) {

		}

		@Override
		public void mouseDragged(MouseEvent e) {

		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}
		
		
	}
	
	
}
