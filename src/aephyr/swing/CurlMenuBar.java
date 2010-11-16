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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

import javax.swing.BorderFactory;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

public class CurlMenuBar extends JMenuBar {

	public CurlMenuBar() {
		setBorder(BorderFactory.createEmptyBorder());
	}
	
	private boolean curlVisible = true;
	
	private boolean bottomBorderVisible = true;
	
	private int curlWidth = 80;
	
	private int overlap =  15;
	
	private Color borderColor;
	
	public void setCurlVisible(boolean visible) {
		curlVisible = visible;
	}
	
	public boolean isCurlVisible() {
		return curlVisible;
	}
	
	public void setBottomBorderVisible(boolean visible) {
		bottomBorderVisible = visible;
	}
	
	public boolean isBottomBorderVisible() {
		return bottomBorderVisible;
	}
	
	public void setCurlWidth(int width) {
		curlWidth = width;
	}
	
	public int getCurlWidth() {
		return curlWidth;
	}
	
	public void setOverlap(int overlap) {
		this.overlap = overlap;
	}
	
	public int getOverlap() {
		return overlap;
	}
	
	public void setBorderColor(Color color) {
		borderColor = color;
	}
	
	public Color getBorderColor() {
		return borderColor;
	}
	
	@Override
	public Insets getInsets() {
		Insets i = super.getInsets();
		if (bottomBorderVisible)
			i.bottom += 1;
		return i;
	}
	
	@Override
	public Insets getInsets(Insets i) {
		i = super.getInsets(i);
		if (bottomBorderVisible)
			i.bottom += 1;
		return i;
	}
	
	@Override
	public void updateUI() {
		super.updateUI();
		if (borderColor == null || borderColor instanceof UIResource) {
			// Nimbus lacks MenuBar.borderColor property
			borderColor = UIManager.getColor("nimbusBorder");
			if (borderColor == null) {
				borderColor = UIManager.getColor("MenuBar.borderColor");
				if (borderColor == null)
					borderColor = new ColorUIResource(Color.GRAY);
			}
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		if (isCurlVisible())
			size.width += curlWidth-overlap;
		return size;
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		if (isCurlVisible() && g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D)g;
			Shape clip = g.getClip();
			GeneralPath path = new GeneralPath();
			int h = getHeight();
			int w = getWidth();
			int curlWidth = this.curlWidth;
			boolean ltr = getComponentOrientation().isLeftToRight();
			if (ltr) {
				path.moveTo(w-2, -1);
				path.curveTo(w-1-curlWidth/2, 0, w-1-curlWidth/2,
						h-1, w-2-curlWidth, h);
				path.lineTo(0, h);
				path.lineTo(0, 0);
				path.closePath();
				g2.clip(path);
			} else {
				path.moveTo(1, -1);
				path.curveTo(curlWidth/2, 0, curlWidth/2,
						h-1, curlWidth, h);
				path.lineTo(w, h);
				path.lineTo(w, 0);
				path.closePath();
				g2.clip(path);
			}
			super.paintComponent(g);
			g.setClip(clip);
			path = new GeneralPath();
			int bot = bottomBorderVisible ? 1 : 0;
			if (ltr) {
				path.moveTo(w-1, -1);
				path.curveTo(w-1-curlWidth/2, 0, w-1-curlWidth/2,
						h-bot, w-2-curlWidth, h-bot);
				path.lineTo(0, h-bot);
			} else {
				path.moveTo(0, -1);
				path.curveTo(curlWidth/2, 0, curlWidth/2,
						h-bot, curlWidth, h-bot);
				path.lineTo(w, h-bot);
			}
			Object antialiasing = g2.getRenderingHint(
					RenderingHints.KEY_ANTIALIASING);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			Object stroke = g2.getRenderingHint(
					RenderingHints.KEY_STROKE_CONTROL);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
					RenderingHints.VALUE_STROKE_NORMALIZE);
			g2.setColor(borderColor);
			g2.draw(path);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, stroke);
		} else {
			super.paintComponent(g);
		}
	}
}
