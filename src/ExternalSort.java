import java.nio.*;
import java.io.*;

// The External Sort implementation
// -------------------------------------------------------------------------
/**
 *
 * @author Vishana Baskaran and Sital Paudel
 * @version Spring 2026
 */
public class ExternalSort {

    /**
     * The working memory available to the program: 50,000 bytes
     */
    private static final int MEMBYTES = 50000;
    private static final int RECORD_SIZE = 8;

    private static final int BLOCK_SIZE = 4096;
    private static final int RECORDS_PER_BLOCK = BLOCK_SIZE / RECORD_SIZE;
    private static final int HEAP_BLOCKS = 10;
    private static final int HEAP_RECORDS = HEAP_BLOCKS * RECORDS_PER_BLOCK;
    private static final int HEAP_BYTES = HEAP_RECORDS * RECORD_SIZE;
    private static final int OUT_BUF_BLOCKS = 1;
    private static final int OUT_BUF_BYTES = OUT_BUF_BLOCKS * BLOCK_SIZE;
    private static final int MAX_WAYS = (MEMBYTES - OUT_BUF_BYTES) / BLOCK_SIZE;

    private static int runCount;

    /**
     * Create a new ExternalSort object.
     * 
     * @param fileName
     *            The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String fileName) throws IOException {
        byte[] pool = new byte[MEMBYTES];
        String temp = fileName + ".tmp";

        long[][] runs = generateRuns(fileName, temp, pool);

        mergeAllRuns(fileName, temp, runs, pool);

        new java.io.File(temp).delete();
    }


    /**
     * Reads parts of the input file that fit into memory, sorts them,
     * and writes them as sorted runs to a temporary file.
     *
     * @param input
     *            the input file name
     * @param temp
     *            the temporary file name
     * @param pool
     *            the shared memory buffer
     * @return array describing runs (start position and length)
     * @throws IOException
     *             if file I/O fails
     */
    private static long[][] generateRuns(String input, String temp, byte[] pool)
        throws IOException {

        long[][] runs = new long[10000][2];
        runCount = 0;

        try (RandomAccessFile in = new RandomAccessFile(input, "r");
            RandomAccessFile out = new RandomAccessFile(temp, "rw")) {

            out.setLength(0);
            long fileLen = in.length();
            long filePos = 0;
            long outPos = 0;

            while (filePos < fileLen) {
                long remaining = fileLen - filePos;
                int toRead = (int)Math.min(remaining, HEAP_BYTES);
                toRead = (toRead / RECORD_SIZE) * RECORD_SIZE;
                if (toRead == 0)
                    break;

                in.seek(filePos);
                in.readFully(pool, 0, toRead);
                filePos += toRead;

                int numRecords = toRead / RECORD_SIZE;

                ByteBuffer view = ByteBuffer.wrap(pool, 0, toRead);
                Heap heap = new Heap(view, numRecords);
                heap.heapSort();

                long runStart = outPos;
                int written = 0;

                while (written < toRead) {
                    int block = Math.min(OUT_BUF_BYTES, toRead - written);
                    out.seek(outPos);
                    out.write(pool, written, block);
                    outPos += block;
                    written += block;
                }

                runs[runCount][0] = runStart;
                runs[runCount][1] = toRead;
                runCount++;
            }
        }
        return runs;
    }


    /**
     * Repeatedly merges runs until only one sorted run is left.
     *
     * @param orig
     *            the original file (final output)
     * @param temp
     *            the temporary file
     * @param runs
     *            run metadata (start, length)
     * @param pool
     *            shared memory buffer
     * @throws IOException
     *             if file I/O fails
     */
    private static void mergeAllRuns(
        String orig,
        String temp,
        long[][] runs,
        byte[] pool)
        throws IOException {

        String srcName = temp;
        String dstName = orig;

        while (runCount > 1) {
            long[][] nextRuns = new long[10000][2];
            int nextCount = 0;

            try (RandomAccessFile src = new RandomAccessFile(srcName, "r");
                RandomAccessFile dst = new RandomAccessFile(dstName, "rw")) {

                dst.setLength(0);
                long dstPos = 0;

                int i = 0;
                while (i < runCount) {
                    int ways = Math.min(MAX_WAYS, runCount - i);
                    long[] desc = new long[ways * 2];

                    for (int k = 0; k < ways; k++) {
                        desc[k * 2] = runs[i + k][0];
                        desc[k * 2 + 1] = runs[i + k][1];
                    }

                    long merged = mergeRuns(src, dst, dstPos, desc, ways, pool);

                    nextRuns[nextCount][0] = dstPos;
                    nextRuns[nextCount][1] = merged;
                    nextCount++;

                    dstPos += merged;
                    i += ways;
                }
            }

            runs = nextRuns;
            runCount = nextCount;

            String tmp = srcName;
            srcName = dstName;
            dstName = tmp;
        }

        if (srcName.equals(temp)) {
            copyFile(temp, orig, pool);
        }
    }


    /**
     * Merges multiple sorted runs into a single run using k-way merge.
     *
     * @param src
     *            source file containing runs
     * @param dst
     *            destination file
     * @param dstStart
     *            starting position in destination
     * @param runDesc
     *            run metadata (start, length pairs)
     * @param ways
     *            number of runs being merged
     * @param pool
     *            shared memory buffer
     * @return number of bytes written
     * @throws IOException
     *             if file I/O fails
     */
    private static long mergeRuns(
        RandomAccessFile src,
        RandomAccessFile dst,
        long dstStart,
        long[] runDesc,
        int ways,
        byte[] pool)
        throws IOException {

        long[] runPos = new long[ways];
        long[] runEnd = new long[ways];
        int[] bufOffset = new int[ways];
        int[] bufFilled = new int[ways];
        int[] bufIndex = new int[ways];

        for (int k = 0; k < ways; k++) {
            runPos[k] = runDesc[k * 2];
            runEnd[k] = runDesc[k * 2] + runDesc[k * 2 + 1];
            bufOffset[k] = k * BLOCK_SIZE;

            loadBlock(src, pool, bufOffset[k], runPos[k], runEnd[k]);
            int loaded = (int)Math.min(BLOCK_SIZE, runEnd[k] - runPos[k]);

            bufFilled[k] = loaded;
            bufIndex[k] = 0;
            runPos[k] += loaded;
        }

        int outOffset = ways * BLOCK_SIZE;
        int outUsed = 0;
        long total = 0;

        ByteBuffer view = ByteBuffer.wrap(pool);

        while (true) {
            int minRun = -1;
            int minKey = 0;

            for (int k = 0; k < ways; k++) {
                if (bufIndex[k] * RECORD_SIZE >= bufFilled[k]) {
                    if (runPos[k] < runEnd[k]) {
                        loadBlock(src, pool, bufOffset[k], runPos[k],
                            runEnd[k]);

                        int loaded = (int)Math.min(BLOCK_SIZE, runEnd[k]
                            - runPos[k]);

                        bufFilled[k] = loaded;
                        bufIndex[k] = 0;
                        runPos[k] += loaded;
                    }
                    else
                        continue;
                }

                int keyPos = bufOffset[k] + bufIndex[k] * RECORD_SIZE;
                int key = view.getInt(keyPos);

                if (minRun == -1 || Integer.compareUnsigned(key, minKey) < 0) {
                    minKey = key;
                    minRun = k;
                }
            }

            if (minRun == -1)
                break;

            int srcPos = bufOffset[minRun] + bufIndex[minRun] * RECORD_SIZE;

            System.arraycopy(pool, srcPos, pool, outOffset + outUsed,
                RECORD_SIZE);

            bufIndex[minRun]++;
            outUsed += RECORD_SIZE;

            if (outUsed >= OUT_BUF_BYTES) {
                dst.seek(dstStart + total);
                dst.write(pool, outOffset, outUsed);
                total += outUsed;
                outUsed = 0;
            }
        }

        if (outUsed > 0) {
            dst.seek(dstStart + total);
            dst.write(pool, outOffset, outUsed);
            total += outUsed;
        }

        return total;
    }


    /**
     * Loads a block of data from a run into the buffer.
     *
     * @param src
     *            source file
     * @param pool
     *            shared memory buffer
     * @param offset
     *            buffer offset
     * @param pos
     *            current position in run
     * @param end
     *            end position of run
     * @throws IOException
     *             if file I/O fails
     */
    private static void loadBlock(
        RandomAccessFile src,
        byte[] pool,
        int offset,
        long pos,
        long end)
        throws IOException {

        int toRead = (int)Math.min(BLOCK_SIZE, end - pos);
        if (toRead <= 0)
            return;

        src.seek(pos);
        src.readFully(pool, offset, toRead);
    }


    /**
     * Copies one file to another using buffer.
     *
     * @param srcPath
     *            source file path
     * @param dstPath
     *            destination file path
     * @param pool
     *            shared memory buffer
     * @throws IOException
     *             if file I/O fails
     */
    private static void copyFile(String srcPath, String dstPath, byte[] pool)
        throws IOException {

        try (RandomAccessFile src = new RandomAccessFile(srcPath, "r");
            RandomAccessFile dst = new RandomAccessFile(dstPath, "rw")) {

            dst.setLength(0);
            long pos = 0;
            long len = src.length();

            while (pos < len) {
                int toRead = (int)Math.min(pool.length, len - pos);
                src.seek(pos);
                src.readFully(pool, 0, toRead);
                dst.seek(pos);
                dst.write(pool, 0, toRead);
                pos += toRead;
            }
        }
    }

}
