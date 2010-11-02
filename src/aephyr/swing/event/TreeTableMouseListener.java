package aephyr.swing.event;

import java.util.EventListener;

public interface TreeTableMouseListener extends EventListener {

    public void mouseClicked(TreeTableMouseEvent e);

    public void mousePressed(TreeTableMouseEvent e);

    public void mouseReleased(TreeTableMouseEvent e);

}
