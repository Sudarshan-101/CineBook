package util;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Draws a generated movie poster (no image files needed): gradient + title, colour chosen from the title. */
public final class Poster {
    private Poster() {}

    private static final Color[][] PALETTES = {
        {new Color(0x7C3AED), new Color(0x2563EB)}, {new Color(0xE11D48), new Color(0xF97316)},
        {new Color(0x0D9488), new Color(0x1D4ED8)}, {new Color(0xDB2777), new Color(0x7C3AED)},
        {new Color(0x16A34A), new Color(0x0E7490)}, {new Color(0xEA580C), new Color(0xBE123C)},
        {new Color(0x4F46E5), new Color(0x0F172A)}, {new Color(0xCA8A04), new Color(0xB91C1C)},
    };

    public static void paint(Graphics g0, String title, String genre, int x, int y, int w, int h, int arc, float titleSize) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color[] p = PALETTES[Math.floorMod(title.hashCode(), PALETTES.length)];
        Shape clip = new java.awt.geom.RoundRectangle2D.Float(x, y, w, h, arc, arc);
        g.setClip(clip);
        g.setPaint(new GradientPaint(x, y, p[0], x + w, y + h, p[1]));
        g.fillRect(x, y, w, h);
        // soft decorative circles
        g.setColor(new Color(255, 255, 255, 28));
        g.fillOval(x + w / 2, y - h / 6, (int) (w * 0.9), (int) (w * 0.9));
        g.setColor(new Color(0, 0, 0, 38));
        g.fillOval(x - w / 3, y + h / 2, (int) (w * 1.1), (int) (w * 1.1));
        // bottom shade for readable text
        g.setPaint(new GradientPaint(0, y + h * 0.45f, new Color(0, 0, 0, 0), 0, y + h, new Color(0, 0, 0, 170)));
        g.fillRect(x, y, w, h);
        // big initial
        String ini = title.isEmpty() ? "?" : title.substring(0, 1).toUpperCase();
        g.setFont(Theme.BASE.deriveFont(Font.BOLD, h * 0.42f));
        g.setColor(new Color(255, 255, 255, 60));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(ini, x + (w - fm.stringWidth(ini)) / 2, y + h * 0.5f);
        // title (wrapped, bottom-aligned)
        g.setFont(Theme.BASE.deriveFont(Font.BOLD, titleSize));
        fm = g.getFontMetrics();
        List<String> lines = wrap(title, fm, w - 20);
        int ly = y + h - 14 - (genre == null || genre.isEmpty() ? 0 : fm.getHeight() - 2) - (lines.size() - 1) * fm.getHeight();
        g.setColor(Color.WHITE);
        for (String l : lines) { g.drawString(l, x + 10, ly); ly += fm.getHeight(); }
        if (genre != null && !genre.isEmpty()) {
            g.setFont(Theme.BASE.deriveFont(Font.PLAIN, Math.max(10f, titleSize - 4)));
            g.setColor(new Color(255, 255, 255, 200));
            g.drawString(genre.toUpperCase(), x + 10, y + h - 12);
        }
        g.dispose();
    }

    private static List<String> wrap(String s, FontMetrics fm, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(t) > max && cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(word); }
            else cur = new StringBuilder(t);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.size() > 3 ? out.subList(0, 3) : out;
    }
}
