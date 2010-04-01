package aephyr.swing.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import javax.swing.*;
import javax.swing.plaf.*;

import aephyr.swing.ScrollPad;
import static aephyr.swing.ScrollPad.*;

public class ScrollPadUI extends ComponentUI {
	
	private static ScrollPadUI ui;
	
	private static Ellipse2D border;
	
	private static Ellipse2D center;
	
	private static GeneralPath arrow;
	
	private static final int DIAMETER = 40;

	
	public static ScrollPadUI createUI(JComponent c) {
		if (ui == null) {
			border = new Ellipse2D.Float(1, 1, DIAMETER-2, DIAMETER-2);
			center = new Ellipse2D.Float(-3, -3, 6, 6);
			arrow = new GeneralPath();
			arrow.moveTo(-3, -DIAMETER/2+8);
			arrow.lineTo(3, -DIAMETER/2+8);
			arrow.lineTo(0, -DIAMETER/2+5);
			arrow.closePath();
			ui = new ScrollPadUI();
		}
		return ui;
	}
	
	public void paint(Graphics g, JComponent c) {
		Color bg = c.isBackgroundSet() ? c.getBackground() : UIManager.getColor("controlLtHighlight");
		if (bg == null)
			bg = Color.WHITE;
		Color fg = c.isForegroundSet() ? c.getForeground() : UIManager.getColor("controlDkShadow");
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
		ScrollPad sp = (ScrollPad)c;
		int visibleArrows = sp.getVisibleArrows();
		if ((visibleArrows & TOP_ARROW) != 0) {
			drawArrow(g2, fg, bg);
		}
		double theta = 0.0;
		if ((visibleArrows & BOTTOM_ARROW) != 0) {
			theta = transformArrow(theta, Math.PI);
			drawArrow(g2, fg, bg);
		}
		if ((visibleArrows & LEFT_ARROW) != 0) {
			theta = transformArrow(theta, -Math.PI/2);
			drawArrow(g2, fg, bg);
		}
		if ((visibleArrows & RIGHT_ARROW) != 0) {
			theta = transformArrow(theta, Math.PI/2);
			drawArrow(g2, fg, bg);
		}
		g2.setColor(fg);
		g2.draw(center);
		boolean fillCenter = true;
		int scrollArrow = sp.getPressedButton();
		if (scrollArrow < 0) {
			fillCenter = false;
			scrollArrow = sp.getScrollArrow();
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
	
	public Dimension getPreferredSize(JComponent c) {
		return new Dimension(DIAMETER, DIAMETER);
	}

	public int getRegion(ScrollPad scrollPad, int x, int y) {
		int visibleArrows = scrollPad.getVisibleArrows();
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
	
	private static double transformArrow(double a, double b) {
		if (a != b)
			arrow.transform(AffineTransform.getRotateInstance(b-a));
		return b;
	}

	private static void drawArrow(Graphics2D g, Color fg, Color bg) {
		g.setColor(fg);
		g.draw(arrow);
		g.setColor(bg);
		g.fill(arrow);
	}
	
}
