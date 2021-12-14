import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {

    private static final int NRSTEPS = 100;
    private static final int[] threadsToUse = {1, 2, 4, 8, 16, 32};
    private static final int[] thresholds = {3, 4, 5, 6, 7, 15};
    private static final float[] speedUpValues = {0.7f, 1.4f, 2.8f, 5.6f, -1f, -1f};
    private static String srcFileName = null;
    private static int[] saveDstParallel;
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

        // This function return number of logical processors so divide it by 2 to get the number of physical cores
        System.out.println("\nAvailable processors: " + Runtime.getRuntime().availableProcessors() / 2);

        for (int threshold: thresholds) {
            System.out.println("\nThreshold: "+ threshold);
            for (int t = 0; t < threadsToUse.length; t++) {
                int[] srcParallel = image.getRGB(0, 0, width, height, null, 0, width);
                int[] dstParallel = new int[srcParallel.length];

                invokeParallelFilter(srcParallel, dstParallel, width, height, threadsToUse[t], threshold);

                if (solutionCorrect(dstSequential, dstParallel)) {
                    measureSpeedUp(speedUpValues[t]);
                } else {
                    System.out.println("The parallel filter isn't working as expected.");
                    System.exit(-1);
                }
                ParallelFJImageFilter.numberOfTasks = 0;
            }
        }

        // save parallel filtered image in file
        if (saveDstParallel != null) {
            saveImageInFile(saveDstParallel, width, height, false);
        }
    }

    private static void saveImageInFile(int[] dst, int width, int height, boolean isSequential) throws IOException {
        BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, width, height, dst, 0, width);

        String dstName = "Filtered" + srcFileName;
        String pathname = (isSequential ? "SequentialFilteredImages/" : "ParallelFilteredImages/") + dstName;
        File dstFile = new File(pathname);
        ImageIO.write(dstImage, "jpg", dstFile);

        if (isSequential) {
            System.out.println("Output image: " + dstName);
        } else {
            System.out.println("\nOutput image (parallel filter): " + dstName);
        }
    }

    private static void measureSpeedUp(float expectedSpeedUp) {
        float speedUp = (float) timeOfSequentialExecution / timeOfParallelExecution;

        if (expectedSpeedUp > 0) {
            System.out.println("Speedup: " + speedUp +
                    (speedUp >= expectedSpeedUp ? "" : " not") +
                    " ok (>= " + expectedSpeedUp + ")");
        } else {
            System.out.println("Speedup: " + speedUp);
        }
    }

    private static boolean solutionCorrect(int[] src, int[] dst) {
        for (int i = 0; i < src.length; i++) {
            if (src[i] != dst[i]) {
                System.out.println("Output image verified failed!");
                return false;
            }
        }
        System.out.println("Output image verified successfully!");
        return true;
    }

    private static void invokeSequentialFilter(int[] src,
                                               int[] dst,
                                               final int width,
                                               final int height) throws IOException {
        System.out.println("Starting sequential image filter.");

        long startTime = System.currentTimeMillis();
        ImageFilter filter = new ImageFilter(src, dst, width, height);
        filter.apply();
        long endTime = System.currentTimeMillis();

        long tSequential = endTime - startTime;
        System.out.println("Sequential image filter took " + tSequential + " milliseconds.");
        timeOfSequentialExecution = tSequential;

        saveImageInFile(dst, width, height, true);
    }

    private static void invokeParallelFilter(int[] src,
                                             int[] dst,
                                             final int width,
                                             final int height,
                                             final int numberOfThreads,
                                             final int threshold) throws IOException {
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

        if (numberOfThreads == 32) {
            saveDstParallel = dst;
        }
    }
}
