/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://fedora-commons.org/license/).
 */
package fedora.utilities;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Comparator for sorting files and directories in a deterministic manner. The
 * sort order can be case-sensitive (default) or case-insensitive, ascending
 * (default) or descending, with directories occurring before (if ascending) or
 * after files within the same directory. The default behavior has the
 * convenient property that it sorts in the same order as would be done when
 * performing a pre-order traversal of the filesystem.
 * 
 * <pre>
 * Example input:
 *   afile
 *   file1
 *   file2
 *   dir1/
 *   FILE3
 *   dir1/file4
 *   dir2/file5
 *   dir2/file10
 *   dir2/
 *   file6
 *
 * Example output (with default behavior: case-sensitive, ascending):
 *   dir1
 *   dir1\file4
 *   dir2
 *   dir2\file10
 *   dir2\file5
 *   FILE3
 *   afile
 *   file1
 *   file2
 *   file6
 * </pre>
 * 
 * @author Chris Wilper
 */
public class FileComparator
        implements Comparator<Object> {

    private int m_multiplier = 1;

    private boolean m_ignoreCase = false;

    /**
     * Construct a case-sensitive comparator that sorts in ascending order.
     */
    public FileComparator() {
    }

    /**
     * Construct a case-sensitive comparator that sorts in the specified order.
     */
    public FileComparator(boolean useDescendingOrder) {
        if (useDescendingOrder) {
            m_multiplier = -1;
        }
    }

    /**
     * Construct a comparator that sorts in the specified order using the
     * specified case sensitivity.
     */
    public FileComparator(boolean useDescendingOrder, boolean ignoreCase) {
        this(useDescendingOrder);
        m_ignoreCase = ignoreCase;
    }

    public int compare(Object o1, Object o2) {

        String s1 = getComparable(o1);
        String s2 = getComparable(o2);

        if (m_ignoreCase) {
            return m_multiplier * s1.compareToIgnoreCase(s2);
        } else {
            return m_multiplier * s1.compareTo(s2);
        }

    }

    /**
     * Does this comparator work for descending sorts?
     */
    public boolean descends() {
        return m_multiplier == -1;
    }

    /**
     * Does this comparator ignore case?
     */
    public boolean ignoresCase() {
        return m_ignoreCase;
    }

    private static String getComparable(Object o) {

        File f = (File) o;

        StringBuffer out = new StringBuffer();

        // prepend a space to the name of all directories in the path
        StringTokenizer names =
                new StringTokenizer(f.getPath(), File.separator);
        int last = names.countTokens();
        int count = 0;
        while (names.hasMoreTokens()) {
            String name = names.nextToken();
            count++;
            if (count != 1) {
                out.append(File.separator);
            }
            if (count == last && !f.isDirectory()) {
                out.append(name);
            } else {
                out.append(" " + name);
            }
        }

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FileComparator) {
            FileComparator c = (FileComparator) o;
            return descends() == c.descends()
                    && ignoresCase() == c.ignoresCase();
        } else {
            return false;
        }
    }

    /**
     * Command-line entry point for simple testing of this class. Given a
     * directory, get all files and dirs (recursively) in no particular order.
     * Print all filenames (using spaces to denote directory names) in unsorted,
     * default sorted, and FileComparator sorted order.
     */
    public static void main(String[] args) {

        List<File> list = new ArrayList<File>();
        getFilesAndDirs(new File(args[0]), list);

        File[] files = new File[list.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = (File) list.get(i);
        }

        print("Before sorting", files);

        Arrays.sort(files);
        print("Default File sorting", files);

        Arrays.sort(files, new FileComparator());
        print("Sorted with FileComparator", files);

        Arrays.sort(files, new FileComparator(true));
        print("Sorted with FileComparator in reverse", files);
    }

    // for testing via main
    private static void print(String kind, File[] f) {
        System.out.println(kind);
        for (File element : f) {
            System.out.println(getComparable(element));
        }
        System.out.println();
    }

    // for testing via main
    private static void getFilesAndDirs(File dir, List<File> list) {
        File[] files = dir.listFiles();
        for (File element : files) {
            list.add(element);
            if (element.isDirectory()) {
                getFilesAndDirs(element, list);
            }
        }
    }

}