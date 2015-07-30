package me.kenzierocks;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        Pattern scalePat = Pattern.compile("-(?:-scale|s)(?:-(w|h))?=(\\d+)");
        @SuppressWarnings("unchecked")
        Predicate<String> isVerbose = (Predicate<String>) (Object) Predicate
                .isEqual("-v").or(Predicate.isEqual("--verbose"));
        boolean verbose = Stream.of(args).anyMatch(isVerbose);
        OptionalInt[] packed = Stream.of(args).map(scalePat::matcher)
                .filter(Matcher::matches).map(matcher -> {
                    OptionalInt[] ints = new OptionalInt[] {
                            OptionalInt.empty(), OptionalInt.empty() };
                    OptionalInt scale =
                            OptionalInt.of(Integer.parseInt(matcher.group(2)));
                    if (matcher.group(1) == null) {
                        ints[0] = ints[1] = scale;
                    } else {
                        if (matcher.group(1).equals("w")) {
                            ints[0] = scale;
                        } else {
                            ints[1] = scale;
                        }
                    }
                    return ints;
                }).reduce(new OptionalInt[] { OptionalInt.empty(),
                        OptionalInt.empty() }, (id, next) -> {
                            OptionalInt[] ints = new OptionalInt[2];
                            OptionalInt aID = id[0];
                            OptionalInt bID = id[1];
                            OptionalInt aNx = next[0];
                            OptionalInt bNx = next[1];
                            ints[0] = aNx.isPresent() ? aNx : aID;
                            ints[1] = bNx.isPresent() ? bNx : bID;
                            return ints;
                        });
        int scaleWidth = packed[0].orElse(1);
        int scaleHeight = packed[1].orElse(1);
        Pattern lightPat = Pattern.compile("-(?:-light|l)=([dDbB]+)");
        List<Function<Color, Color>> lightChanges =
                Stream.of(args).map(lightPat::matcher).filter(Matcher::matches)
                        .flatMap(m -> m.group(1).chars()
                                .mapToObj(c -> c == 'd' || c == 'D'
                                        ? (Function<Color, Color>) Color::darker
                                        : (Function<Color, Color>) Color::brighter))
                        .collect(Collectors.toList());
        args = Stream.of(args)
                .filter(isVerbose.or(scalePat.asPredicate())
                        .or(lightPat.asPredicate()).negate())
                .toArray(String[]::new);
        Random r = new Random();
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        String file = args.length > 2 ? args[2] : ":-";
        if (verbose) {
            System.err.println("Generating an image...");
        }
        byte[] pixels = new byte[width * height * 4];
        r.nextBytes(pixels);
        BufferedImage created =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0, index = 0; i < pixels.length; i += 4, index++) {
            int x = index % width;
            int y = index / width;
            Color color = new Color(((/* 0x0F */0xFF) << 24)
                    | ((pixels[i + 1] & 0xFF) << 16)
                    | ((pixels[i + 2] & 0xFF) << 8) | ((pixels[i + 3] & 0xFF)));
            for (Function<Color, Color> change : lightChanges) {
                color = change.apply(color);
            }
            created.setRGB(x, y, color.getRGB());
        }
        int scaledWidth = width * scaleWidth;
        int scaledHeight = height * scaleHeight;
        BufferedImage tmp = new BufferedImage(scaledWidth, scaledHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        AffineTransform at =
                AffineTransform.getScaleInstance(scaleWidth, scaleHeight);
        g.drawRenderedImage(created, at);
        created = tmp;
        if (verbose) {
            System.err.println("Writing to file...");
        }
        ImageIO.write(created, "PNG", file.equals(":-") ? System.out
                : Files.newOutputStream(Paths.get(file)));
        if (verbose) {
            System.err.println("Complete...");
        }
    }
}
