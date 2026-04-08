import student.TestCase;
import java.nio.*;
import java.io.*;

/**
 * This class was designed to test the External Sort class.
 * Each tests generates random ascii and binary files of the specified size,
 * then sorts both and then checking each one with the file checker.
 *
 * @author CS3114/5040 Staff
 * @version Spring 2026
 */
public class ExternalSortTest extends TestCase {
    private CheckFile fileChecker;

    /**
     * This method sets up the tests that follow.
     */
    /** Bytes per record. */
    private static final int RECORD_SIZE = 8;

    /** Bytes per disk block. */
    private static final int BLOCK_SIZE = 4096;

    /** Records per block. */
    private static final int RECORDS_PER_BLOCK = BLOCK_SIZE / RECORD_SIZE;

    /**
     * Records that fit in the heap during run generation (10 blocks).
     * Each heap-sort pass produces one run of this many records.
     */
    private static final int HEAP_RECORDS = 10 * RECORDS_PER_BLOCK; // 5120

    /**
     * Maximum simultaneous merge ways.
     * MAX_WAYS = (50000 - 4096) / 4096 = 11
     */
    private static final int MAX_WAYS = 11;
    private FileGenerator fileGenerator;

    /**
     * initializes the file checker and file generator before each test
     */
    public void setUp() {
        fileChecker = new CheckFile();
        fileGenerator = new FileGenerator();
    }


    /**
     * creates a bytebuffer containing records where each key and data value are
     * identical
     */
    private ByteBuffer makeBuffer(int[] keys) {
        ByteBuffer bb = ByteBuffer.allocate(keys.length * RECORD_SIZE);
        for (int key : keys) {
            bb.putInt(key); // key
            bb.putInt(key); // data = key so we can cross-check
        }
        bb.rewind();
        return bb;
    }


    /**
     * extracts all record keys from the given bytebuffer into an array
     */
    private int[] extractKeys(ByteBuffer bb, int numRecs) {
        int[] keys = new int[numRecs];
        for (int i = 0; i < numRecs; i++) {
            keys[i] = bb.getInt(i * RECORD_SIZE);
        }
        return keys;
    }


    /**
     * checks whether the given array of keys is sorted in ascending unsigned
     * order
     */
    private boolean isSorted(int[] arr) {
        for (int i = 1; i < arr.length; i++) {
            if (Integer.compareUnsigned(arr[i - 1], arr[i]) > 0) {
                return false;
            }
        }
        return true;
    }


    // ----------------------------------------------------------
    /**
     * Helper method for the tests: Run a test suite for a given size.
     * Creates two files (one "ascii" and one "binary") of the specified size,
     * then for each one, runs the sort and runs the checker.
     * 
     * @param fileSize
     *            Number of (4096 byte) blocks to test for
     * @throws Exception
     */
    public void sortHelper(int fileSize) throws Exception {

        FileGenerator it = new FileGenerator();
        String namea = "input" + fileSize + "asave.bin";
        String nameb = "input" + fileSize + "bsave.bin";
        it.generateFile(namea, fileSize, "a");
        it.generateFile(nameb, fileSize, "b");
        String[] args = new String[1];

        String testFilea = "testa" + fileSize + ".bin";
        args[0] = testFilea;
        SortUtils.copyFile(namea, testFilea);
        System.out.println("Sorting " + testFilea);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFileA(testFilea, fileSize));

        String testFileb = "testb" + fileSize + ".bin";
        args[0] = testFileb;
        SortUtils.copyFile(nameb, testFileb);
        System.out.println("Sorting " + testFileb);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFile(testFileb, fileSize));
    }


    // ----------------------------------------------------------
    /**
     * Test a file with 1 block
     * 
     * @throws Exception
     */
    public void test1() throws Exception {
        sortHelper(1);
    }


    /**
     * Test a file with 5 block
     * 
     * @throws Exception
     */
    public void test5() throws Exception {
        sortHelper(5);
    }


    /**
     * verifies heap sort behavior with a single record
     */
    public void testHeapSingleRecord() {
        ByteBuffer bb = makeBuffer(new int[] { 42 });
        Heap heap = new Heap(bb, 1);
        heap.heapSort();
        assertEquals(42, bb.getInt(0));
        assertEquals(42, bb.getInt(4)); // data preserved
    }


    /**
     * verifies that two already sorted records remain sorted after heap sort
     */
    public void testHeapTwoRecordsAlreadySorted() {
        ByteBuffer bb = makeBuffer(new int[] { 1, 2 });
        Heap heap = new Heap(bb, 2);
        heap.heapSort();
        int[] keys = extractKeys(bb, 2);
        assertTrue("Two sorted records must stay sorted", isSorted(keys));
        assertEquals(1, keys[0]);
        assertEquals(2, keys[1]);
    }


    /**
     * verifies that two reverse ordered records are sorted into ascending order
     */
    public void testHeapTwoRecordsReversed() {
        ByteBuffer bb = makeBuffer(new int[] { 5, 3 });
        Heap heap = new Heap(bb, 2);
        heap.heapSort();
        int[] keys = extractKeys(bb, 2);
        assertEquals(3, keys[0]);
        assertEquals(5, keys[1]);
    }


    /**
     * verifies heap sort maintains order for a small ascending input
     */
    public void testHeapSmallAscending() {
        ByteBuffer bb = makeBuffer(new int[] { 10, 20, 30, 40, 50 });
        Heap heap = new Heap(bb, 5);
        heap.heapSort();
        assertTrue(isSorted(extractKeys(bb, 5)));
    }


    /**
     * verifies heap sort correctly sorts a small descending input
     */
    public void testHeapSmallDescending() {
        ByteBuffer bb = makeBuffer(new int[] { 50, 40, 30, 20, 10 });
        Heap heap = new Heap(bb, 5);
        heap.heapSort();
        int[] keys = extractKeys(bb, 5);
        assertTrue("Reverse-sorted input must become ascending", isSorted(
            keys));
        assertEquals(10, keys[0]);
        assertEquals(50, keys[4]);
    }


    /**
     * ensures that record data values remain paired with their keys after
     * sorting
     */
    public void testHeapDataPreserved() {
        int[] input = { 9, 2, 7, 4, 5, 1, 8, 3, 6 };
        ByteBuffer bb = makeBuffer(input);
        Heap heap = new Heap(bb, input.length);
        heap.heapSort();

        for (int i = 0; i < input.length; i++) {
            int key = bb.getInt(i * RECORD_SIZE);
            int data = bb.getInt(i * RECORD_SIZE + 4);
            assertEquals("Data must match key at record " + i + " (key=" + key
                + " data=" + data + ")", key, data);
        }
    }


    /**
     * verifies heap sort correctness on exactly one full disk block of records
     */
    public void testHeapOneBlock() {
        int n = RECORDS_PER_BLOCK; // 512
        ByteBuffer bb = ByteBuffer.allocate(n * RECORD_SIZE);
        for (int i = n; i >= 1; i--) {
            bb.putInt(i); // key
            bb.putInt(i); // data
        }
        bb.rewind();

        Heap heap = new Heap(bb, n);
        heap.heapSort();

        int[] keys = extractKeys(bb, n);
        assertTrue("512-record block must be sorted ascending", isSorted(keys));
        assertEquals("Smallest key must be 1", 1, keys[0]);
        assertEquals("Largest key must be 512", n, keys[n - 1]);
    }


    /**
     * generates sorts and validates a test file with a specified format and
     * size
     */
    private void runSort(int blocks, String format, String label)
        throws Exception {

        String savedName = label + "save.bin";
        String testName = label + "test.bin";

        fileGenerator.generateFile(savedName, blocks, format);
        SortUtils.copyFile(savedName, testName);

        String[] args = { testName };
        ExternalSortProj.main(args);

        boolean sorted = format.equals("a")
            ? fileChecker.checkFileA(testName, blocks)
            : fileChecker.checkFile(testName, blocks);

        assertTrue("File " + testName + " (" + blocks + " blocks, format="
            + format + ") must be sorted", sorted);
    }


    /**
     * tests sorting of a 1 block file with random binary keys
     */
    public void test1Block() throws Exception {
        runSort(1, "b", "sort1b");
    }


    /**
     * tests sorting of a 1 block file with ascii keys
     */
    public void test1BlockAscii() throws Exception {
        runSort(1, "a", "sort1a");
    }


    /**
     * tests sorting of a 5 block file within a single heap run
     */
    public void test5Blocks() throws Exception {
        runSort(5, "b", "sort5b");
    }


    /**
     * tests sorting of a 10 block file that exactly fills the heap capacity
     */
    public void test10Blocks() throws Exception {
        runSort(10, "b", "sort10b");
    }


    /**
     * tests sorting of an 11 block file that produces two initial runs
     * requiring a merge
     */
    public void test11Blocks() throws Exception {
        runSort(11, "b", "sort11b");
    }


    /**
     * tests sorting of a 20 block file producing two full runs
     */
    public void test20Blocks() throws Exception {
        runSort(20, "b", "sort20b");
    }


    /**
     * tests sorting of a file that produces exactly the maximum number of merge
     * ways
     */
    public void testExactlyMaxWaysRuns() throws Exception {
        int blocks = MAX_WAYS * (HEAP_RECORDS / RECORDS_PER_BLOCK); // 110
        runSort(blocks, "b", "sort110b");
    }


    /**
     * tests sorting of a file that produces one more than the maximum merge
     * ways
     */
    public void testMaxWaysPlusOneRuns() throws Exception {
        int blocks = (MAX_WAYS + 1) * (HEAP_RECORDS / RECORDS_PER_BLOCK); // 120
        runSort(blocks, "b", "sort120b");
    }


    /**
     * verifies that an already sorted file remains correctly sorted after
     * processing
     */
    public void testAlreadySorted() throws Exception {
        String savedName = "sortedInputSave.bin";
        String testName = "sortedInputTest.bin";
        int blocks = 5;
        int totalRecords = blocks * RECORDS_PER_BLOCK;

        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(savedName)))) {
            for (int i = 1; i <= totalRecords; i++) {
                out.writeInt(i); // key ascending
                out.writeInt(i); // data
            }
        }
        SortUtils.copyFile(savedName, testName);
        String[] args = { testName };
        ExternalSortProj.main(args);
        assertTrue("Pre-sorted file must remain sorted", fileChecker.checkFile(
            testName, blocks));
    }


    /**
     * verifies that a reverse sorted file is correctly sorted into ascending
     * order
     */
    public void testReverseSorted() throws Exception {
        String savedName = "reverseSave.bin";
        String testName = "reverseTest.bin";
        int blocks = 5;
        int totalRecords = blocks * RECORDS_PER_BLOCK;

        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(savedName)))) {
            for (int i = totalRecords; i >= 1; i--) {
                out.writeInt(i); // key descending
                out.writeInt(i); // data
            }
        }
        SortUtils.copyFile(savedName, testName);
        String[] args = { testName };
        ExternalSortProj.main(args);
        assertTrue("Reverse-sorted file must be sorted ascending", fileChecker
            .checkFile(testName, blocks));
    }


    /**
     * verifies that a file with identical keys is still considered correctly
     * sorted
     */
    public void testAllSameKey() throws Exception {
        String savedName = "allSameSave.bin";
        String testName = "allSameTest.bin";
        int blocks = 3;
        int totalRecords = blocks * RECORDS_PER_BLOCK;

        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(savedName)))) {
            for (int i = 0; i < totalRecords; i++) {
                out.writeInt(1000000); // same key for every record
                out.writeInt(i + 1); // distinct data values
            }
        }
        SortUtils.copyFile(savedName, testName);
        String[] args = { testName };
        ExternalSortProj.main(args);
        assertTrue("All-same-key file must be considered sorted", fileChecker
            .checkFile(testName, blocks));
    }


    /**
     * tests sorting of a large file requiring multiple merge passes and both
     * formats
     */
    public void testLargeFile() throws Exception {
        // 13 runs: 130 blocks binary
        runSort(130, "b", "sort130b");
        // Also test ASCII format at a moderate size
        runSort(25, "a", "sort25a");
    }


    /**
     * asserts that two integer arrays are equal element by element
     */
    private void assertArrayEquals(String msg, int[] expected, int[] actual) {
        assertEquals(msg + " (length)", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(msg + " at index " + i, expected[i], actual[i]);
        }
    }

}
