package ui;

import javax.swing.*;
import java.awt.*;

/** A panel that wraps its children like text and reports a correct height inside a JScrollPane. */
public class WrapPanel extends JPanel implements Scrollable {
    public WrapPanel(int hgap, int vgap) {
        super(new FlowLayout(FlowLayout.LEFT, hgap, vgap) {
            @Override public Dimension preferredLayoutSize(Container t) { return size(t); }
            @Override public Dimension minimumLayoutSize(Container t) { return size(t); }
            private Dimension size(Container t) {
                synchronized (t.getTreeLock()) {
                    int width = t.getParent() instanceof JViewport vp ? vp.getWidth() : t.getWidth();
                    if (width <= 0) width = 600;
                    Insets in = t.getInsets();
                    int max = width - in.left - in.right - getHgap() * 2;
                    int x = 0, rowH = 0, h = getVgap();
                    for (Component c : t.getComponents()) {
                        if (!c.isVisible()) continue;
                        Dimension d = c.getPreferredSize();
                        if (x > 0 && x + d.width > max) { h += rowH + getVgap(); x = 0; rowH = 0; }
                        x += d.width + getHgap();
                        rowH = Math.max(rowH, d.height);
                    }
                    h += rowH + getVgap() + in.top + in.bottom;
                    return new Dimension(width, h);
                }
            }
        });
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
