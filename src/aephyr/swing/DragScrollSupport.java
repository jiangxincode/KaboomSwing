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

public class DragScrollSupport extends MouseAdapter {

	public DragScrollSupport() {}
	
	public void register(JComponent c) {
		c.addMouseListener(this);
		c.addMouseMotionListener(this);
	}
	
	public void unregister(JComponent c ) {
		c.removeMouseListener(this);
		c.removeMouseMotionListener(this);
	}
	
	private final Rectangle visible = new Rectangle();
	
	private int startX, startY;
	
	private int maxX, maxY;
	
	private Cursor cursor;
	
	private boolean autoscrolls;
	
	private boolean isDragging;
	
	public void mousePressed(MouseEvent e) {
		JComponent c = (JComponent)e.getSource();
		startX = e.getX();
		startY = e.getY();
		int w = c.getWidth();
		int h = c.getHeight();
		c.computeVisibleRect(visible);
		maxX = w - visible.width;
		maxY = h - visible.height;
		isDragging = false;
	}
	
	public void mouseReleased(MouseEvent e) {
		if (isDragging) {
			JComponent c = (JComponent)e.getSource();
			move(e);
			c.setCursor(cursor);
			cursor = null;
			c.setAutoscrolls(autoscrolls);
			e.consume();
		}
	}
	
	public void mouseDragged(MouseEvent e) {
		if (!isDragging) {
			JComponent c = (JComponent)e.getSource();
			isDragging = true;
			cursor = c.isCursorSet() ? c.getCursor() : null;
			autoscrolls = c.getAutoscrolls();
			c.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			c.setAutoscrolls(false);
		}
		move(e);
		e.consume();
	}
	
	private void move(MouseEvent e) {
		Rectangle v = visible;
		int newX = v.x + startX - e.getX();
		int newY = v.y + startY - e.getY();
		if (newX < 0)
			newX = 0;
		else if (newX > maxX)
			newX = maxX;
		if (newY < 0)
			newY = 0;
		else if (newY > maxY)
			newY = maxY;
		v.x = newX;
		v.y = newY;
		((JComponent)e.getSource()).scrollRectToVisible(v);
	}

}
