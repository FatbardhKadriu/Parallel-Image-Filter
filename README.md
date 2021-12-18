# Parallel-Image-Filter

## Description
The class ImageFilter implements a sequential, iterative nine-point image convolution filter working on a linearized (2D) image. In each of the NRSTEPS (=100) iteration steps, the average RGB-value of each pixel p in the source array is computed, considering p and its 8 neighbor pixels (in 2D), and written to the destination array. The ParallelFJImageFilter class has been implemented for parallel image filtering using the Java Fork/Join framework. For parallelization, the pixels in the image are recursively partitioned among all the tasks used for parallel execution.

ParallelFJImageFilter offers:
- a public constructor with the same parameter types as class ImageFilter, and
- a public method void apply(int nthreads), where the parameter nthreads corresponds to the number of threads used for parallel execution.

The test program has to verifies for all parallel image filter executions that the output image is exactly the same as the one produced by the sequential image filter (pixel-wise comparison!). The test program also verifies for all parallel executions that the parallel efficiency is at least 0.7, especially when #threads=#cores.
Parallel image filter works with images of arbitrary size.

## Solution
Parallelization of this task is done using Fork / Join Framework. The original image is represented by an array of integers, where each integer contains color values for a single pixel. The modified image is also represented by an array of integers of the same size as the source. Parallelization is performed by dividing the pixel array recursively into smaller parts according to the Divide and conquer algorithm and dividing them into bowls.

Program steps:
1. A task is installed that shows all the work to be performed.

``` ParallelFJImageFilter filter = new ParallelFJImageFilter (src, dst, width, 1, height-1, threshold); ```

2. ForkJoinPool is installed with the number of threads as a parameter.

``` ForkJoinPool pool = new ForkJoinPool (numberOfThreads); ```

3. Execution is done by giving as a parameter the instance of the ParallelFJImageFilter class.

``` pool.invoke (filter); ```

The values of threshold and the number of threads used as test cases are:

- thresholds = {3, 4, 5, 6, 7, 15};
- threadsToUse = {1, 2, 4, 8, 16, 32};

## Execution of the program
TestFilter can be used to filter a JPG image by following these steps:
1. Navigate to the Parallel-Image-Filter / src folder 
2. Execute:

```
javac TestImageFilter.java && java TestImageFilter IMAGE1.JPG
```


## Members

[Arbena Musa](https://github.com/ArbenaMusa)

[Fatbardh Kadriu](https://github.com/fatbardhKadriu)
