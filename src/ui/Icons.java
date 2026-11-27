package ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

// Small line icons drawn with SVG paths (on a 24 x 24 grid), so no image files are needed
public class Icons {
    public static final String REVENUE  = "M2 6h20v12H2z M12 9a3 3 0 1 0 0.01 0z M6 12h.01 M18 12h.01";
    public static final String SALES    = "M5 2h14v20l-3-2-2 2-2-2-2 2-2-2-3 2z M9 8h6 M9 12h6";
    public static final String WALLET   = "M3 7h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M3 7V6a2 2 0 0 1 2-2h12 M16 14h2";
    public static final String CHART    = "M3 3v18h18 M7 15l4-5 3 3 5-7";
    public static final String ALERT    = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M12 7v6 M12 16.5h.01";
    public static final String TOP      = "M4 3h16 M8 3v16 M12 3v10 M16 3v13";
    public static final String LOGOUT   = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9";
    public static final String PERSON   = "M12 4a4 4 0 1 0 0.01 0z M4 21a8 8 0 0 1 16 0z";

    public static final String FILTER   = "M3 4h18l-7 8v7l-4 2v-9z";
    public static final String SEARCH   = "M11 3a8 8 0 1 0 0.01 0z M21 21l-5-5";
    public static final String ADD      = "M3 3h18v18H3z M12 8v8 M8 12h8";
    public static final String DELETE   = "M20 5H9l-7 7 7 7h11z M12 9l6 6 M18 9l-6 6";
    public static final String BOX      = "M3 8h18v12H3z M3 8l3-4h12l3 4 M12 12v4 M10 14h4";
    public static final String CLOCK    = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M12 6v6l4 2";
    public static final String REORDER  = "M3 6h18 M3 12h12 M3 18h8 M18 14v6 M15 17h6";
    public static final String EMPTY    = "M3 3h18v18H3z M3 3l18 18";

    public static final String CLOSE    = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M9 9l6 6 M15 9l-6 6";
    public static final String IMAGE    = "M3 3h18v18H3z M3 16l5-5 4 4 3-3 6 6 M8 8h.01";
    public static final String RETURN   = "M9 14L4 9l5-5 M4 9h10a6 6 0 0 1 0 12h-3";
    public static final String CART     = "M2 3h3l3 12h11l2-8H6 M9 20h.01 M18 20h.01";
    public static final String USER_ADD = "M9 11a4 4 0 1 0 0.01 0z M2 21a7 7 0 0 1 14 0 M19 8v6 M16 11h6";
    public static final String EDIT     = "M4 20h4L19 9l-4-4L4 16z M13 7l4 4";

    // Creates an outlined (black line) icon of the given size
    public static Node outline(String path, double size) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.BLACK);
        svg.setStrokeWidth(2);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return new Group(svg);
    }

    // Creates a filled icon of the given size and color (used for the avatar)
    public static Node filled(String path, double size, Color color) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.setFill(color);
        double scale = size / 24.0;
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return new Group(svg);
    }
}
