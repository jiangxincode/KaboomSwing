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
	 * buttons of the scroll pad. If true, a click will initiate
	 * autoscrolling in the specified direction and subsequent
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
		ScrollPad sp = getScrollPad();
		Dimension sz = sp.getPreferredSize();
		if (r.width < sz.width || r.height < sz.height)
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
			scrollPad.addScrollPadListener(this);
//			new DragScroller(scrollPad);
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

		x -= sz.width/2;
		y -= sz.height/2;
		if (x < r.x) {
			x = r.x;
		} else if (x+sz.width > r.x+r.width) {
			x = r.x + r.width - sz.width;
		}
		if (y < r.y) {
			y = r.y;
		} else if (y+sz.height > r.y+r.height) {
			y = r.y + r.height - sz.height;
		}
		Point p = new Point(x, y);
		SwingUtilities.convertPointToScreen(p, parent);
		currentX = originX = p.x + sz.width/2;
		currentY = originY = p.y + sz.height/2;
		scrollPopup.show(parent, x, y);
		parent.addMouseMotionListener(this);
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
				} else if (theta >= -Math.PI/8) {
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
			switch (a) {
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
	
	@Override
	public void buttonClicked(ScrollPad.Event e) {
		if (e.getButton() == ScrollPad.CENTER) {
			scrollPopup.setVisible(false);
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
	
	
	//TODO incomplete/broken/yadayada...
	private class DragScroller extends MouseAdapter {
		
		DragScroller(ScrollPad s) {
			s.addMouseListener(this);
			s.addMouseMotionListener(this);
		}
		
		Rectangle absRect = new Rectangle();
		
		int startX, startY;
		
		Point min = new Point();
		
		Point max = new Point();
		
		int dragButton = MouseEvent.BUTTON1;
		
		boolean isDragging = false;

		int originXOffset, originYOffset;
		
		
		
		@Override
		public void mousePressed(MouseEvent e) {
			System.out.println(e);
			if (e.getButton() == dragButton) {
				startX = e.getXOnScreen();
				startY = e.getYOnScreen();
//				originXOffset = e.getXOnScreen() - originX + ScrollPad.DIAMETER/2;
//				originYOffset = e.getYOnScreen() - originY + ScrollPad.DIAMETER/2;
				JComponent c = (JComponent)scrollPopup.getInvoker();
				c.computeVisibleRect(absRect);
				min.setLocation(0, 0);
				max.setLocation(c.getWidth() - absRect.width, c.getHeight() - absRect.height);
				SwingUtilities.convertPointToScreen(min, c);
				SwingUtilities.convertPointToScreen(max, c);
				Point p = absRect.getLocation();
				SwingUtilities.convertPointToScreen(p, c);
				absRect.setLocation(p);
				isDragging = false;
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (startX >= 0) {
				if (!isDragging) {
					isDragging = true;
					
				}
				move(e);
				e.consume();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (isDragging) {
				move(e);
				isDragging = false;
				
				e.consume();
			}
		}
		
		private void move(MouseEvent e) {
			Rectangle v = absRect;
			int newX = v.x + startX - e.getXOnScreen();
			int newY = v.y + startY - e.getYOnScreen();
			if (newX < min.x)
				newX = min.x;
			else if (newX > max.x)
				newX = max.x;
			if (newY < min.y)
				newY = min.y;
			else if (newY > max.y)
				newY = max.y;
			v.x = newX;
			v.y = newY;
			Point p = v.getLocation();
			JComponent c = (JComponent)scrollPopup.getInvoker();
			SwingUtilities.convertPointFromScreen(p, c);
			visible.setBounds(p.x, -p.y, v.width, v.height);
			System.out.println("\tmove: "+visible + " " + c.getY());
			c.scrollRectToVisible(visible);

			originX = e.getXOnScreen()-originXOffset;
			originY = e.getYOnScreen()-originYOffset;
			scrollPopup.setLocation(originX, originY);
		}
	}
	
	
}
