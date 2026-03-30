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

    /**
     * Create a new ExternalSort object.
     * 
     * @param theFileName
     *            The name of the file to be sorted
     *
     * @throws IOException
     */
    public static void sort(String theFileName) throws IOException {
        RandomAccessFile theFile = new RandomAccessFile(theFileName, "rw");

        byte[] workingMem = new byte[MEMBYTES];

        // read in stuff
        long fileLength = theFile.length();
        int bytesToRead = (int)Math.min(fileLength, MEMBYTES);
        bytesToRead = (bytesToRead / RECORD_SIZE) * RECORD_SIZE;

        if (bytesToRead > 0) {
            theFile.seek(0);
            theFile.readFully(workingMem, 0, bytesToRead);

            ByteBuffer bb = ByteBuffer.wrap(workingMem);
            IntBuffer ib = bb.asIntBuffer();

            int numRecords = bytesToRead / RECORD_SIZE;
            // heapsort
            heapSort(bb, ib, numRecords);
            // write back to file
            theFile.seek(0);
            theFile.write(workingMem, 0, bytesToRead);
        }
    }


    public static void heapSort(ByteBuffer bb, IntBuffer ib, int n) {
        // make heap
        for (int i = n / 2 - 1; i >= 0; i--) {
            swapDown(bb, ib, i, n);
        }
        // sort it
        for (int i = n - 1; i > 0; i--) {
            swap(bb, 0, i);
            swapDown(bb, ib, 0, i);
        }
    }


    public static void swapDown(
        ByteBuffer bb,
        IntBuffer ib,
        int root,
        int heapSize) {
        while (true) {
            int largest = root;
            int left = 2 * root + 1;
            int right = 2 * root + 2;

            if (left < heapSize && ib.get(2 * left) > ib.get(2 * largest)) {
                largest = left;
            }
            if (right < heapSize && ib.get(2 * right) > ib.get(2 * largest)) {
                largest = right;
            }

            if (largest != root) {
                swap(bb, root, largest);
                root = largest;
            }
            else {
                break;
            }
        }
    }


    private static void swap(ByteBuffer bb, int i, int j) {
        int posI = i * RECORD_SIZE;
        int posJ = j * RECORD_SIZE;

        long recordI = bb.getLong(posI);
        bb.putLong(posI, bb.getLong(posJ));
        bb.putLong(posJ, recordI);
    }
}
