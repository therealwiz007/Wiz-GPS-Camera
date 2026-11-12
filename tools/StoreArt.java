import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Launcher icon and Play Store art for Wiz GPS Camera, all drawn in code (Wiz black + yellow).
 *
 *   java tools/StoreArt.java launcher <app/src/main/res>          (adaptive foreground + legacy mipmaps)
 *   java tools/StoreArt.java icon     store-assets/icon-512.png 512
 *   java tools/StoreArt.java feature  store-assets/feature-graphic-1024x500.png
 *   java tools/StoreArt.java frame    <in.png> <out.png> "<title>" "<subtitle>"
 */
public class StoreArt {
    static Color c(long argb) { return new Color((int) argb, true); }
    static final Color BG = c(0xFF0B0D10L), YELLOW = c(0xFFFFC400L), YELLOW_DEEP = c(0xFFE0A800L),
        MUTED = c(0xFF8B93A1L), TEXT = c(0xFFF4F5F7L), INK = c(0xFF0B0D10L), GREEN = c(0xFF2BD97CL);
    static final String SANS = pick("Helvetica Neue", "Helvetica", "Arial", "SansSerif");

    static String pick(String... names) {
        java.util.Set<String> have = new java.util.HashSet<>(java.util.Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String n : names) if (have.contains(n)) return n;
        return "SansSerif";
    }

    public static void main(String[] a) throws Exception {
        switch (a[0]) {
            case "launcher" -> launcher(new File(a[1]));
            case "icon" -> write(icon(Integer.parseInt(a[2]), true), a[1], true);
            case "feature" -> write(feature(), a[1], true);
            case "frame" -> write(frame(ImageIO.read(new File(a[1])), a[3], a[4]), a[2], true);
            default -> throw new IllegalArgumentException(a[0]);
        }
    }

    static void write(BufferedImage img, String path, boolean opaque) throws Exception {
        File f = new File(path);
        f.getAbsoluteFile().getParentFile().mkdirs();
        BufferedImage out = img;
        if (opaque) { // Play wants opaque PNGs.
            out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setColor(BG); g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.drawImage(img, 0, 0, null); g.dispose();
        }
        ImageIO.write(out, "png", f);
        System.out.println("wrote " + path + " " + img.getWidth() + "x" + img.getHeight());
    }

    static Graphics2D smooth(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    // ---------- Icon ----------

    static void iconBackground(Graphics2D g, int s) {
        g.setPaint(new LinearGradientPaint(0, 0, 0, s, new float[]{0f, 1f}, new Color[]{c(0xFF22262EL), c(0xFF0B0D10L)}));
        g.fillRect(0, 0, s, s);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(s * .5, s * .56), s * .55f, new float[]{0f, 1f},
            new Color[]{c(0x33FFC400L), c(0x00FFC400L)}));
        g.fillRect(0, 0, s, s);
    }

    /** Camera body with a map-pin lens, drawn in a unit box scaled to [s] around (ox, oy). */
    static void drawGlyph(Graphics2D g, double ox, double oy, double s) {
        AffineTransform old = g.getTransform();
        g.translate(ox, oy);
        g.scale(s, s);
        // Soft shadow
        g.setColor(c(0x55000000L));
        g.fill(new RoundRectangle2D.Double(.14, .33, .72, .50, .16, .16));
        // Body + viewfinder hump
        Area body = new Area(new RoundRectangle2D.Double(.13, .30, .74, .50, .16, .16));
        body.add(new Area(new RoundRectangle2D.Double(.33, .22, .30, .14, .07, .07)));
        g.setPaint(new LinearGradientPaint(0f, .22f, 0f, .80f, new float[]{0f, 1f}, new Color[]{c(0xFFFFD43BL), YELLOW_DEEP}));
        g.fill(body);
        // Flash window
        g.setColor(INK);
        g.fill(new RoundRectangle2D.Double(.70, .37, .08, .05, .03, .03));
        // Lens: dark ring, yellow rim, dark glass
        double cx = .5, cy = .555;
        g.setColor(INK); g.fill(new Ellipse2D.Double(cx - .215, cy - .215, .43, .43));
        g.setColor(c(0xFFFFE27AL)); g.setStroke(new BasicStroke(.022f)); g.draw(new Ellipse2D.Double(cx - .165, cy - .165, .33, .33));
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx - .05, cy - .06), .17f, new float[]{0f, 1f},
            new Color[]{c(0xFF2A2F38L), c(0xFF0B0D10L)}));
        g.fill(new Ellipse2D.Double(cx - .15, cy - .15, .30, .30));
        // GPS crosshair ticks inside the glass
        g.setColor(c(0x66FFC400L)); g.setStroke(new BasicStroke(.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy - .14, cx, cy - .11));
        g.draw(new Line2D.Double(cx, cy + .11, cx, cy + .14));
        g.draw(new Line2D.Double(cx - .14, cy, cx - .11, cy));
        g.draw(new Line2D.Double(cx + .11, cy, cx + .14, cy));
        // Map pin in the lens
        double pr = .062, tipY = cy + .085;
        Path2D pin = new Path2D.Double();
        pin.moveTo(cx, tipY);
        pin.curveTo(cx - pr * 1.25, tipY - pr * 1.1, cx - pr * 1.2, tipY - pr * 2.6, cx, tipY - pr * 2.6);
        pin.curveTo(cx + pr * 1.2, tipY - pr * 2.6, cx + pr * 1.25, tipY - pr * 1.1, cx, tipY);
        pin.closePath();
        g.setColor(YELLOW); g.fill(pin);
        g.setColor(INK); g.fill(new Ellipse2D.Double(cx - pr * .45, tipY - pr * 1.65 - pr * .45, pr * .9, pr * .9));
        // Glint
        g.setColor(c(0x55FFFFFFL)); g.fill(new Ellipse2D.Double(cx - .11, cy - .12, .05, .035));
        g.setTransform(old);
    }

    static BufferedImage icon(int s, boolean withBg) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        if (withBg) iconBackground(g, s);
        drawGlyph(g, 0, 0, s);
        g.dispose();
        return img;
    }

    /** Adaptive foreground (108dp canvas, glyph inside the 66dp safe zone) + legacy square/round mipmaps. */
    static void launcher(File res) throws Exception {
        int fg = 432;
        BufferedImage f = new BufferedImage(fg, fg, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(f);
        double gs = fg * 0.64;
        drawGlyph(g, (fg - gs) / 2, (fg - gs) / 2 + fg * .01, gs);
        g.dispose();
        write(f, new File(res, "drawable-nodpi/ic_launcher_foreground.png").getPath(), false);
        String[] dirs = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        int[] sizes = {48, 72, 96, 144, 192};
        for (int i = 0; i < dirs.length; i++) {
            int s = sizes[i];
            BufferedImage full = icon(s * 4, true);
            for (boolean round : new boolean[]{false, true}) {
                BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
                Graphics2D o = smooth(out);
                Shape clip = round ? new Ellipse2D.Double(0, 0, s, s) : new RoundRectangle2D.Double(0, 0, s, s, s * .36, s * .36);
                o.setClip(clip);
                o.drawImage(full.getScaledInstance(s, s, Image.SCALE_SMOOTH), 0, 0, null);
                o.dispose();
                write(out, new File(res, "mipmap-" + dirs[i] + "/" + (round ? "ic_launcher_round.png" : "ic_launcher.png")).getPath(), false);
            }
        }
    }

    static void drawIconTile(Graphics2D g, int x, int y, int size) {
        BufferedImage ic = icon(size, true);
        Shape clip = new RoundRectangle2D.Double(x, y, size, size, size * 0.42, size * 0.42);
        Shape old = g.getClip();
        g.setClip(clip); g.drawImage(ic, x, y, null); g.setClip(old);
        g.setColor(c(0x22FFFFFFL)); g.setStroke(new BasicStroke(2f)); g.draw(clip);
    }

    // ---------- Feature graphic ----------

    static void darkBackdrop(Graphics2D g, int w, int h, double gx, double gy, float radius) {
        g.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 1f}, new Color[]{c(0xFF191D24L), c(0xFF07080AL)}));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(gx, gy), radius, new float[]{0f, 1f},
            new Color[]{c(0x40FFC400L), c(0x00FFC400L)}));
        g.fillRect(0, 0, w, h);
    }

    static BufferedImage feature() {
        int W = 1024, H = 500;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        darkBackdrop(g, W, H, 200, 250, 320f);
        // Orbit tracks and satellites around the icon (the app's "locking on" hero)
        double cx = 200, cy = 250;
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 12f}, 0f));
        g.setColor(c(0x44FFC400L));
        for (double r : new double[]{160, 200}) g.draw(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(YELLOW);
        for (double[] p : new double[][]{{160, -0.9}, {200, 2.4}, {200, 0.6}}) {
            double x = cx + p[0] * Math.cos(p[1]), y = cy + p[0] * Math.sin(p[1]);
            g.setStroke(new BasicStroke(5f)); g.draw(new Line2D.Double(x - 14, y, x + 14, y));
            g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
        }
        drawIconTile(g, 80, 130, 240);

        int tx = 410;
        g.setColor(TEXT);
        g.setFont(new Font(SANS, Font.BOLD, 62));
        g.drawString("Wiz GPS Camera", tx, 190);
        g.setColor(YELLOW);
        g.setFont(new Font(SANS, Font.BOLD, 29));
        g.drawString("Photos that prove where you were", tx, 242);
        g.setColor(MUTED);
        g.setFont(new Font(SANS, Font.PLAIN, 22));
        g.drawString("Place, address, map & time stamped on every shot", tx, 284);
        int x = tx;
        for (String chip : new String[]{"GPS stamp", "Map & address", "Proof ID"}) x = chip(g, x, 320, chip) + 14;
        g.dispose();
        return img;
    }

    static int chip(Graphics2D g, int x, int y, String text) {
        g.setFont(new Font(SANS, Font.BOLD, 21));
        int w = g.getFontMetrics().stringWidth(text) + 36;
        g.setColor(c(0xFF1F242CL)); g.fill(new RoundRectangle2D.Double(x, y, w, 44, 44, 44));
        g.setColor(c(0x66FFC400L)); g.setStroke(new BasicStroke(1.5f)); g.draw(new RoundRectangle2D.Double(x, y, w, 44, 44, 44));
        g.setColor(TEXT); g.drawString(text, x + 18, y + 29);
        return x + w;
    }

    // ---------- Framed screenshots (1080x1920, 9:16) ----------

    static BufferedImage frame(BufferedImage shot, String title, String subtitle) {
        int W = 1080, H = 1920;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = smooth(img);
        darkBackdrop(g, W, H, W / 2.0, 260, 700f);
        g.setColor(TEXT);
        g.setFont(new Font(SANS, Font.BOLD, 70));
        int y = 170;
        for (String line : wrap(g, title, W - 120)) { center(g, line, W, y); y += 82; }
        g.setColor(YELLOW);
        g.setFont(new Font(SANS, Font.PLAIN, 38));
        y += 4;
        for (String line : wrap(g, subtitle, W - 160)) { center(g, line, W, y); y += 50; }

        int top = Math.max(y + 30, 400);
        int sh = H - top + 60; // phone runs off the bottom edge
        int sw = (int) Math.round(shot.getWidth() * (sh / (double) shot.getHeight()));
        if (sw > 820) { sw = 820; sh = (int) Math.round(shot.getHeight() * (sw / (double) shot.getWidth())); }
        int sx = (W - sw) / 2, bezel = 18;
        RoundRectangle2D body = new RoundRectangle2D.Double(sx - bezel, top - bezel, sw + 2 * bezel, sh + 2 * bezel, 90, 90);
        g.setColor(c(0x66000000L)); g.fill(new RoundRectangle2D.Double(sx - bezel + 8, top - bezel + 16, sw + 2 * bezel, sh + 2 * bezel, 90, 90));
        g.setColor(c(0xFF1B1F26L)); g.fill(body);
        g.setColor(c(0xFF343A44L)); g.setStroke(new BasicStroke(3f)); g.draw(body);
        Shape screen = new RoundRectangle2D.Double(sx, top, sw, sh, 64, 64);
        Shape old = g.getClip();
        g.setClip(screen);
        g.drawImage(shot.getScaledInstance(sw, sh, Image.SCALE_SMOOTH), sx, top, null);
        g.setClip(old);
        g.dispose();
        return img;
    }

    static void center(Graphics2D g, String s, int w, int y) {
        g.drawString(s, (w - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    static List<String> wrap(Graphics2D g, String text, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            String t = line.isEmpty() ? w : line + " " + w;
            if (g.getFontMetrics().stringWidth(t) > max && !line.isEmpty()) { out.add(line.toString()); line = new StringBuilder(w); }
            else line = new StringBuilder(t);
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }
}
