import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {

    private static final int STEPS = 100;
    private static final int[] threadsToUse = { 1, 2, 4, 8, 16 };
    private static final float[] speedUpValues = { 0.7f, 1.4f, 2.8f, 5.6f, -1f };
    private static final int[] thresholds = { 200, 400, 600, 800 };
    private static String srcFileName = null;
    private static long timeOfSequentialExecution;
    private static long timeOfParallelExecution;

    public static void main(String[] args) throws Exception {
        BufferedImage image = null;
        String srcPath;
        try {
            srcFileName = args[0];
            srcPath = "src/" + srcFileName;
            File srcFile = new File(srcPath);
            image = ImageIO.read(srcFile);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: java TestAll <image-file>");
            System.exit(1);
        } catch (IIOException e) {
            System.out.println("Error reading image file " + srcFileName + " !");
            System.exit(1);
        }

        System.out.println("Source image: " + srcFileName);

        int width = image.getWidth();
        int height = image.getHeight();
        System.out.println("Image size is " + width + "x" + height + "\n");

        int[] srcSequential = image.getRGB(0, 0, width, height, null, 0, width);
        int[] dstSequential = new int[srcSequential.length];

        execSequentialFilter(srcSequential, dstSequential, width, height);

        // availableProcessor returns number of logic processor so divide by 2 to get number of physical processors
        System.out.println("\nAvailable processors: " + Runtime.getRuntime().availableProcessors() / 2);

        for (int threshold: thresholds) {
            for (int t = 0; t < threadsToUse.length; t++) {
                int[] srcParallel = image.getRGB(0, 0, width, height, null, 0, width);
                int[] dstParallel = new int[srcParallel.length];

                execParallelFilter(srcParallel, dstParallel, width, height, threadsToUse[t], threshold);

                checkSolution(srcSequential, srcParallel);
                showSpeedUp(speedUpValues[t]);
            }
        }
    }


    private static void showSpeedUp(float speedToCheck) {
        float speedUp = (float) timeOfSequentialExecution / timeOfParallelExecution;

        if (speedToCheck > 0f) {
            System.out.println("Speedup: " + speedUp + (speedUp >= speedToCheck ? "" : " not") + " ok (>= " + speedToCheck + ")");
        } else {
            System.out.println("Speedup: " + speedUp);
        }
    }

    private static void checkSolution(int[] source, int[] destination) {
        for (int i = 0; i < source.length; i++) {
            if (source[i] != destination[i]) {
                System.out.println("Output image verified failed!");
                return;
            }
        }
        System.out.println("Output image verified successfully!");
    }

    private static void execSequentialFilter(int[] src, int[] dst, final int w, final int h) throws IOException {
        System.out.println("Starting sequential image filter.");

        long startTime = System.currentTimeMillis();
        ImageFilter filter = new ImageFilter(src, dst, w, h);
        filter.apply();
        long endTime = System.currentTimeMillis();

        long tSequential = endTime - startTime;
        System.out.println("Sequential image filter took " + tSequential + " milliseconds.");
        timeOfSequentialExecution = tSequential;

        BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        String dstName = "Filtered" + srcFileName;
        File dstFile = new File("src/SequentialFilteredImages/" + dstName);
        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Output image: " + dstName);
    }

    private static void execParallelFilter(int[] src, int[] dst, final int width, final int height, final int numberOfThreads, final int threshold) throws IOException {
        System.out.println("\nStarting parallel image filter using " + numberOfThreads + " threads.");

        ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
        ParallelFJImageFilter filter;
        long startTime = System.currentTimeMillis();
        for (int steps = 0; steps < STEPS; steps++) {
            filter = new ParallelFJImageFilter(src, dst, width, 1, height - 1, threshold);
            pool.invoke(filter);

            // swap references
            int[] help;
            help = src;
            src = dst;
            dst = help;
        }

        long endTime = System.currentTimeMillis();
        long tParallel = endTime - startTime;

        System.out.println("Parallel image filter took " + tParallel + " milliseconds using " + numberOfThreads + " threads.");
        timeOfParallelExecution = tParallel;

        if (numberOfThreads == 16) {
            BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            dstImage.setRGB(0, 0, width, height, dst, 0, width);

            String dstName = "Filtered" + srcFileName;
            File dstFile = new File("src/ParallelFilteredImages/" + dstName);
            ImageIO.write(dstImage, "jpg", dstFile);

            // Print in the end
            System.out.println("Output image for parallel filter: " + dstName);
        }
    }
}
