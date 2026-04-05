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
public class ExternalSortTest extends TestCase
{
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
    public void setUp()
    {
        fileChecker = new CheckFile();
        fileGenerator = new FileGenerator();
    }

    private ByteBuffer makeBuffer(int[] keys) {
        ByteBuffer bb = ByteBuffer.allocate(keys.length * RECORD_SIZE);
        for (int key : keys) {
            bb.putInt(key);   // key
            bb.putInt(key);   // data = key so we can cross-check
        }
        bb.rewind();
        return bb;
    }
    private int[] extractKeys(ByteBuffer bb, int numRecs) {
        int[] keys = new int[numRecs];
        for (int i = 0; i < numRecs; i++) {
            keys[i] = bb.getInt(i * RECORD_SIZE);
        }
        return keys;
    }
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
     * @param fileSize Number of (4096 byte) blocks to test for
     * @throws Exception
     */
    public void sortHelper(int fileSize)
        throws Exception
    {

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
     * @throws Exception
     */
    public void test1()
        throws Exception
    {
        sortHelper(1);
    }
    /**
     * Test a file with 5 block
     * @throws Exception
     */
    public void test5()
        throws Exception
    {
        sortHelper(5);
    }
    public void testHeapSingleRecord() {
        ByteBuffer bb = makeBuffer(new int[]{42});
        Heap heap = new Heap(bb, 1);
        heap.heapSort();
        assertEquals(42, bb.getInt(0));
        assertEquals(42, bb.getInt(4)); // data preserved
    }
    public void testHeapTwoRecordsAlreadySorted() {
        ByteBuffer bb = makeBuffer(new int[]{1, 2});
        Heap heap = new Heap(bb, 2);
        heap.heapSort();
        int[] keys = extractKeys(bb, 2);
        assertTrue("Two sorted records must stay sorted", isSorted(keys));
        assertEquals(1, keys[0]);
        assertEquals(2, keys[1]);
    }
    /**
     * Two records in descending order must be reversed to ascending.
     */
    public void testHeapTwoRecordsReversed() {
        ByteBuffer bb = makeBuffer(new int[]{5, 3});
        Heap heap = new Heap(bb, 2);
        heap.heapSort();
        int[] keys = extractKeys(bb, 2);
        assertEquals(3, keys[0]);
        assertEquals(5, keys[1]);
    }
 
    /**
     * Five records already in ascending order must remain sorted.
     */
    public void testHeapSmallAscending() {
        ByteBuffer bb = makeBuffer(new int[]{10, 20, 30, 40, 50});
        Heap heap = new Heap(bb, 5);
        heap.heapSort();
        assertTrue(isSorted(extractKeys(bb, 5)));
    }
 
    /**
     * Five records in strict descending order must be sorted ascending.
     */
    public void testHeapSmallDescending() {
        ByteBuffer bb = makeBuffer(new int[]{50, 40, 30, 20, 10});
        Heap heap = new Heap(bb, 5);
        heap.heapSort();
        int[] keys = extractKeys(bb, 5);
        assertTrue("Reverse-sorted input must become ascending", isSorted(keys));
        assertEquals(10, keys[0]);
        assertEquals(50, keys[4]);
    }
 

 
    /**
     * Data values must travel with their keys: after sorting, each
     * record's data value must equal its key (given our makeBuffer
     * convention where data == key initially).
     */
    public void testHeapDataPreserved() {
        int[] input = {9, 2, 7, 4, 5, 1, 8, 3, 6};
        ByteBuffer bb = makeBuffer(input);
        Heap heap = new Heap(bb, input.length);
        heap.heapSort();
 
        for (int i = 0; i < input.length; i++) {
            int key  = bb.getInt(i * RECORD_SIZE);
            int data = bb.getInt(i * RECORD_SIZE + 4);
            assertEquals(
                "Data must match key at record " + i
                + " (key=" + key + " data=" + data + ")",
                key, data);
        }
    }
 
    /**
     * Sort exactly one full block (512 records) — the smallest unit of
     * block-aligned I/O used by ExternalSort.
     */
    public void testHeapOneBlock() {
        int n = RECORDS_PER_BLOCK; // 512
        ByteBuffer bb = ByteBuffer.allocate(n * RECORD_SIZE);
        // Fill with descending keys so sort has real work to do
        for (int i = n; i >= 1; i--) {
            bb.putInt(i);   // key
            bb.putInt(i);   // data
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
     * Helper: generate a file, copy it to a test file, sort it, check it.
     *
     * @param blocks number of 4096-byte blocks
     * @param format "a" for ASCII keys, "b" for random binary keys
     * @param label  prefix used to name the saved and test files
     * @throws Exception if generation, sorting, or checking fails
     */
    private void runSort(int blocks, String format, String label)
        throws Exception {
 
        String savedName = label + "save.bin";
        String testName  = label + "test.bin";
 
        fileGenerator.generateFile(savedName, blocks, format);
        SortUtils.copyFile(savedName, testName);
 
        String[] args = {testName};
        ExternalSortProj.main(args);
 
        boolean sorted = format.equals("a")
            ? fileChecker.checkFileA(testName, blocks)
            : fileChecker.checkFile(testName, blocks);
 
        assertTrue("File " + testName + " (" + blocks
                   + " blocks, format=" + format + ") must be sorted",
                   sorted);
    }
 

 
    /**
     * 1-block file (512 records) — smallest valid file; one run, no merge.
     * Tests format b (random binary keys).
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test1Block() throws Exception {
        runSort(1, "b", "sort1b");
    }
 
    /**
     * 1-block file with format-a ASCII keys — exercises checkFileA path.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test1BlockAscii() throws Exception {
        runSort(1, "a", "sort1a");
    }
 
    /**
     * 5-block file (2560 records) — comfortably inside one run (5120 cap).
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test5Blocks() throws Exception {
        runSort(5, "b", "sort5b");
    }
 
    /**
     * 10-block file (5120 records) — exactly fills one heap load.
     * Edge case: run length == HEAP_RECORDS.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test10Blocks() throws Exception {
        runSort(10, "b", "sort10b");
    }

 
    /**
     * 11-block file (5632 records) — spills into exactly 2 runs.
     * First run: 10 blocks. Second run: 1 block.
     * Tests the first real merge path.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test11Blocks() throws Exception {
        runSort(11, "b", "sort11b");
    }
 
    /**
     * 20-block file (10240 records) — exactly 2 full runs of 10 blocks each.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void test20Blocks() throws Exception {
        runSort(20, "b", "sort20b");
    }
 
    /**
     * File producing exactly MAX_WAYS (11) runs — all merged in a single
     * MAX_WAYS-way merge pass with no leftover group.
     * Size: 11 * 10 = 110 blocks.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testExactlyMaxWaysRuns() throws Exception {
        int blocks = MAX_WAYS * (HEAP_RECORDS / RECORDS_PER_BLOCK); // 110
        runSort(blocks, "b", "sort110b");
    }
 
    /**
     * File producing MAX_WAYS+1 (12) runs — first group merges 11 runs,
     * second group merges the remaining 1 run (a no-op merge), then a
     * second pass merges those 2 super-runs.
     * Size: 12 * 10 = 120 blocks.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testMaxWaysPlusOneRuns() throws Exception {
        int blocks = (MAX_WAYS + 1) * (HEAP_RECORDS / RECORDS_PER_BLOCK); // 120
        runSort(blocks, "b", "sort120b");
    }
 
 
    /**
     * Write a file that is already sorted in ascending key order, then sort
     * it. The result must still be correctly sorted.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testAlreadySorted() throws Exception {
        String savedName = "sortedInputSave.bin";
        String testName  = "sortedInputTest.bin";
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
        String[] args = {testName};
        ExternalSortProj.main(args);
        assertTrue("Pre-sorted file must remain sorted",
                   fileChecker.checkFile(testName, blocks));
    }
 
    /**
     * Write a file sorted in strictly descending key order, then sort it.
     * This is the adversarial input for Heapsort's make-heap phase.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testReverseSorted() throws Exception {
        String savedName = "reverseSave.bin";
        String testName  = "reverseTest.bin";
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
        String[] args = {testName};
        ExternalSortProj.main(args);
        assertTrue("Reverse-sorted file must be sorted ascending",
                   fileChecker.checkFile(testName, blocks));
    }
 
    /**
     * Write a file where every record has the same key. The output must
     * satisfy the non-decreasing requirement (all equal keys trivially are).
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testAllSameKey() throws Exception {
        String savedName = "allSameSave.bin";
        String testName  = "allSameTest.bin";
        int blocks = 3;
        int totalRecords = blocks * RECORDS_PER_BLOCK;
 
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(savedName)))) {
            for (int i = 0; i < totalRecords; i++) {
                out.writeInt(1000000); // same key for every record
                out.writeInt(i + 1);  // distinct data values
            }
        }
        SortUtils.copyFile(savedName, testName);
        String[] args = {testName};
        ExternalSortProj.main(args);
        assertTrue("All-same-key file must be considered sorted",
                   fileChecker.checkFile(testName, blocks));
    }
 
 
    /**
     * Large file requiring at least two full merge passes.
     * 130 blocks → 13 initial runs → pass 1 yields 2 super-runs
     * (11-way merge + 2-way merge) → pass 2 merges those 2 super-runs.
     * Also tests format-a ASCII output for a larger file.
     *
     * @throws Exception if I/O or sorting fails
     */
    public void testLargeFile() throws Exception {
        // 13 runs: 130 blocks binary
        runSort(130, "b", "sort130b");
        // Also test ASCII format at a moderate size
        runSort(25, "a", "sort25a");
    }
 
 
    private void assertArrayEquals(String msg, int[] expected, int[] actual) {
        assertEquals(msg + " (length)", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(msg + " at index " + i, expected[i], actual[i]);
        }
    }
 
}
