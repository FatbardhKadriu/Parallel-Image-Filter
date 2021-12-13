import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {
	private static final int NRSTEPS = 100;
	private static String srcFileName = null;
	private static long timeOfSequentialExecution;
	private static long timeOfParallelExecution;

	public static void main(String[] args) throws Exception {
		BufferedImage image = null;
		String srcPath = null;
		try {
			srcFileName = args[0];
			srcPath = "src/" + srcFileName;
			File srcFile = new File(srcPath);
			image = ImageIO.read(srcFile);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Usage: java TestAll <image-file>");
			System.exit(1);
		}
		catch (IIOException e) {
			System.out.println("Error reading image file " + srcFileName + " !");
			System.exit(1);
		}

		System.out.println("Source image: " + srcFileName);

		int w = image.getWidth();
		int h = image.getHeight();
		System.out.println("Image size is " + w + "x" + h + "\n");

		int[] src = image.getRGB(0, 0, w, h, null, 0, w);

		int[] dst = new int[src.length];

//		execSequentialFilter(src, dst, w, h);

		System.out.println("\nAvailable processors: " + Runtime.getRuntime().availableProcessors() / 2);

		int[] parallelSource = image.getRGB(0, 0, w, h, null, 0 , w);
		int[] parallelDst = new int[parallelSource.length];

//		execParallelFilter(parallelSource, parallelDst, w, h, 2);
		ComparingImages.diff("src/SequentialFilteredImages/FilteredIMAGE1.JPG",
				"src/ParallelFilteredImages/FilteredIMAGE1.jpg");
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

	private static void execParallelFilter(int[] src, int[] dst, final int width, final int height, final int numberOfThreads) throws  IOException {
		System.out.println("\nStarting parallel image filter using " + numberOfThreads + " threads.");

		ForkJoinPool pool = new ForkJoinPool(numberOfThreads);
		ParallelFJImageFilter filter;
		long startTime = System.currentTimeMillis();
		for (int steps = 0; steps < NRSTEPS; steps++) {
			filter = new ParallelFJImageFilter(src, dst, width, 1, height - 1);
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

		BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		dstImage.setRGB(0, 0, width, height, dst, 0, width);

		String dstName = "Filtered" + srcFileName;
		File dstFile = new File("src/ParallelFilteredImages/" + dstName);
		ImageIO.write(dstImage, "jpg", dstFile);

		System.out.println("Output image for parallel filter: " + dstName);
	}
}
