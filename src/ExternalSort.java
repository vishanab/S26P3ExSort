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

            int numRecords = bytesToRead / RECORD_SIZE;
            // heapsort
            Heap heap = new Heap(bb, numRecords);
            heap.heapSort();
            // write back to file
            theFile.seek(0);
            theFile.write(workingMem, 0, bytesToRead);
            theFile.close();
        }
    }


}
