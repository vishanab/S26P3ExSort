import java.nio.*;
import java.io.*;

public class Heap {
    private static final int RECORD_SIZE = 8;

    private ByteBuffer bb;
    private int size;

    public Heap(ByteBuffer bb, int numRecords) {
        this.bb = bb;
        this.size = numRecords;
        makeHeap();
    }


    public void heapSort() {
        for (int i = size - 1; i > 0; i--) {
            swap(0, i);
            swapDown(0, i);
        }
    }
    
    public void makeHeap() {
        for (int i = size / 2 - 1; i >= 0; i--) {
            swapDown(i, size);
        }

    }

    public void swapDown(
        int root,
        int heapSize) {
        while (true) {
            int largest = root;
            int left = 2 * root + 1;
            int right = 2 * root + 2;

            if (left < heapSize && key(left) > key(largest)) {
                largest = left;
            }
            if (right < heapSize && key(right) > key(largest)) {
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


    private void swap(int i, int j) {
        int posI = i * RECORD_SIZE;
        int posJ = j * RECORD_SIZE;

        long recordI = bb.getLong(posI);
        bb.putLong(posI, bb.getLong(posJ));
        bb.putLong(posJ, recordI);
    }


    // Read the key of a record
    private int key(int recordIndex) {
        return bb.getInt(recordIndex * RECORD_SIZE);
    }

}
