package me.kenzierocks;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by kenzie on 7/14/15.
 */
public class WritingRandomImages {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Ahem, I need at least 2 arguments please.");
            System.exit(1);
            return;
        }
        Pattern scalePat = Pattern.compile("-(?:-scale|s)=(\\d+)");
        @SuppressWarnings("unchecked")
        Predicate<String> isVerbose = (Predicate<String>) (Object) Predicate.isEqual("-v").or(Predicate.isEqual("--verbose"));
        boolean verbose = Stream.of(args).anyMatch(isVerbose);
        int scale = Stream.of(args).map(scalePat::matcher).filter(Matcher::matches).map(matcher -> matcher.group(1))
                .mapToInt(Integer::parseInt).findFirst().orElse(1);
        args = Stream.of(args).filter(isVerbose.or(scalePat.asPredicate()).negate()).toArray(String[]::new);
        Random r = new Random();
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        String file = args.length > 2 ? args[2] : ":-";
        if (verbose) {
            System.err.println("Generating an image...");
        }
        byte[] pixels = new byte[width * height * 4];
        r.nextBytes(pixels);
        BufferedImage created = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0, index = 0; i < pixels.length; i += 4, index++) {
            int x = index % width;
            int y = index / width;
            created.setRGB(x, y, new Color(((/*0x0F*/0xFF) << 24) |
                    ((pixels[i + 1] & 0xFF) << 16) |
                    ((pixels[i + 2] & 0xFF) << 8) |
                    ((pixels[i + 3] & 0xFF))).brighter().brighter().getRGB());
        }
        int scaledWidth = width * scale;
        int scaledHeight = height * scale;
        BufferedImage tmp = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
        g.drawRenderedImage(created, at);
        created = tmp;
        if (verbose) {
            System.err.println("Writing to file...");
        }
        ImageIO.write(created, "PNG", file.equals(":-") ? System.out : Files.newOutputStream(Paths.get(file)));
        if (verbose) {
            System.err.println("Complete...");
        }
    }
}
