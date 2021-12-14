import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {

    private static final int NRSTEPS = 100;
    private static final int[] threadsToUse = { 1, 2, 4, 8, 16, 32 };
    private static final int[] thresholds = { 3, 4, 5, 6, 7 };
    private static final float[] speedUpValues = { 0.7f, 1.4f, 2.8f, 5.6f, -1f, -1f };
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

        invokeSequentialFilter(srcSequential, dstSequential, width, height);

        // availableProcessor returns number of logic processor so divide by 2 to get number of physical processors
        System.out.println("\nAvailable processors: " + Runtime.getRuntime().availableProcessors() / 2);

        for (int threshold: thresholds) {
            System.out.println("\nThreshold: "+ threshold);
            for (int t = 0; t < threadsToUse.length; t++) {
                int[] srcParallel = image.getRGB(0, 0, width, height, null, 0, width);
                int[] dstParallel = new int[srcParallel.length];

                invokeParallelFilter(srcParallel, dstParallel, width, height, threadsToUse[t], threshold);

                if (verifySolution(srcSequential, srcParallel)) {
                    measureSpeedUp(speedUpValues[t]);
                }
            }
        }
    }

    private static void measureSpeedUp(float expectedSpeedUp) {
        float speedUp = (float) timeOfSequentialExecution / timeOfParallelExecution;

        if (expectedSpeedUp > 0f) {
            System.out.println("Speedup: " + speedUp + (speedUp >= expectedSpeedUp ? "" : " not") + " ok (>= " + expectedSpeedUp + ")");
        } else {
            System.out.println("Speedup: " + speedUp);
        }
    }

    private static boolean verifySolution(int[] src, int[] dst) {
        for (int i = 0; i < src.length; i++) {
            if (src[i] != dst[i]) {
                System.out.println("Output image verified failed!");
                return false;
            }
        }
        System.out.println("Output image verified successfully!");
        return true;
    }

    private static void invokeSequentialFilter(int[] src, int[] dst, final int w, final int h) throws IOException {
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
        File dstFile = new File("SequentialFilteredImages/" + dstName);
        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Output image: " + dstName);
    }

    private static void invokeParallelFilter(int[] src, int[] dst, final int width, final int height, final int numberOfThreads, final int threshold) throws IOException {
        System.out.println("\nStarting parallel image filter using " + numberOfThreads + " threads.");

        ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
        ParallelFJImageFilter filter;
        long startTime = System.currentTimeMillis();
        for (int steps = 0; steps < NRSTEPS; steps++) {
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

        System.out.println("Number of generated tasks " + ParallelFJImageFilter.numberOfTasks + ".");

        if (numberOfThreads == 16) {
            BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            dstImage.setRGB(0, 0, width, height, dst, 0, width);

            String dstName = "Filtered" + srcFileName;
            File dstFile = new File("ParallelFilteredImages/" + dstName);
            ImageIO.write(dstImage, "jpg", dstFile);

            // Print in the end
            System.out.println("Output image for parallel filter: " + dstName);
        }
    }
}
