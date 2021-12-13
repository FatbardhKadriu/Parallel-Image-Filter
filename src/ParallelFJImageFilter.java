import java.util.concurrent.RecursiveAction;

public class ParallelFJImageFilter extends RecursiveAction {
    private final int[] src;
    private final int[] dst;
    private final int width;
    private final int start;
    private final int end;

    private final int threshold;

    public ParallelFJImageFilter(int[] src, int[] dst, int width, int start, int end, int threshold) {
        this.src = src;
        this.dst = dst;
        this.width = width;
        this.start = start;
        this.end = end;
        this.threshold = threshold;
    }

    @Override
    protected void compute() {
        if (start - end < threshold) {
            apply();
            return;
        }

        int middle = (start + end) / 2;

        invokeAll(new ParallelFJImageFilter(src, dst, width, start, middle, threshold),
                new ParallelFJImageFilter(src, dst, width, middle, end, threshold));
    }

    public void apply() {
        int index, pixel;
        for (int i = start; i < end; i++) {
            for (int j = 1; j < width - 1; j++) {
                float rt = 0, gt = 0, bt = 0;
                for (int k = i - 1; k <= i + 1; k++) {
                    index = k * width + j - 1;
                    pixel = src[index];
                    rt += (float) ((pixel & 0x00ff0000) >> 16);
                    gt += (float) ((pixel & 0x0000ff00) >> 8);
                    bt += (float) ((pixel & 0x000000ff));

                    index = k * width + j;
                    pixel = src[index];
                    rt += (float) ((pixel & 0x00ff0000) >> 16);
                    gt += (float) ((pixel & 0x0000ff00) >> 8);
                    bt += (float) ((pixel & 0x000000ff));

                    index = k * width + j + 1;
                    pixel = src[index];
                    rt += (float) ((pixel & 0x00ff0000) >> 16);
                    gt += (float) ((pixel & 0x0000ff00) >> 8);
                    bt += (float) ((pixel & 0x000000ff));
                }
                // Re-assemble destination pixel.
                index = i * width + j;
                int dpixel = (0xff000000) | (((int) rt / 9) << 16) | (((int) gt / 9) << 8) | (((int) bt / 9));
                dst[index] = dpixel;
            }
        }
    }
}

