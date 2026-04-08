import java.nio.*;
import java.io.*;

/**
 * Heap class
 * 
 * @author Vishana Baskaran and Sital Paudel
 * @version Spring 2026
 */
public class Heap {
    /**
     * The record size is 8
     */
    private static final int RECORD_SIZE = 8;

    private ByteBuffer bb;
    private int size;

    /**
     * constructs a heap with given ByteBuffer
     * 
     * @param bb
     *            the ByteBuffer containing the records
     * @param numRecords
     *            the number of records stored in the buffer
     */
    public Heap(ByteBuffer bb, int numRecords) {
        this.bb = bb;
        this.size = numRecords;
        makeHeap();
    }


    /**
     * in-place heap sort on the records stored in the ByteBuffer
     */
    public void heapSort() {
        for (int i = size - 1; i > 0; i--) {
            swap(0, i);
            swapDown(0, i);
        }
    }


    /**
     * converts the records into a max-heap
     */
    public void makeHeap() {
        for (int i = size / 2 - 1; i >= 0; i--) {
            swapDown(i, size);
        }

    }


    /**
     * Restores the max heap property by moving the element at the root to the
     * proper position
     *
     * @param root
     *            the index of the element to move down
     * @param heapSize
     *            the size of the heap during this operation
     */
    public void swapDown(int root, int heapSize) {
        while (true) {
            int largest = root;
            int left = 2 * root + 1;
            int right = 2 * root + 2;

            if (left < heapSize && Integer.compareUnsigned(key(left), key(
                largest)) > 0) {
                largest = left;
            }
            if (right < heapSize && Integer.compareUnsigned(key(right), key(
                largest)) > 0) {
                largest = right;
            }

            if (largest != root) {
                swap(root, largest);
                root = largest;
            }
            else {
                break;
            }
        }
    }


    /*
     * swaps two records in the ByteBuffer
     */
    private void swap(int i, int j) {
        int posI = i * RECORD_SIZE;
        int posJ = j * RECORD_SIZE;

        long recordI = bb.getLong(posI);
        bb.putLong(posI, bb.getLong(posJ));
        bb.putLong(posJ, recordI);
    }


    /*
     * gets the key (first 4 bytes) of a record for comparison.
     */
    private int key(int recordIndex) {
        return bb.getInt(recordIndex * RECORD_SIZE);
    }

}
