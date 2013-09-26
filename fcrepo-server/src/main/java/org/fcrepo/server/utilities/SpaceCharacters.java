package org.fcrepo.server.utilities;

import java.io.PrintWriter;
import java.util.Arrays;


abstract class SpaceCharacters {
/**
 * A convenience class for getting and printing indent
 * characters without creating lots of Strings or char arrays.
 */
    private static final int SIXTY_FOUR = 64;
    
    private static final char[] SIXTY_FOUR_SPACES =
            indentChars(SIXTY_FOUR);
    
    public static char[] indentChars(int num) {
        char[] indent = new char[num];
        Arrays.fill(indent, ' ');
        return indent;
    }
    
    /**
     * Write a number of whitespaces to a writer
     * @param num
     * @param out
     */
    public static void indent(int num, PrintWriter out) {
        if (num <= SIXTY_FOUR) {
            out.write(SIXTY_FOUR_SPACES, 0, num);
            return;
        } else if (num <= 128){
            out.write(SIXTY_FOUR_SPACES, 0, SIXTY_FOUR);
            out.write(SIXTY_FOUR_SPACES, 0, num - SIXTY_FOUR);
        } else {
            int times = num / SIXTY_FOUR;
            int rem = num % SIXTY_FOUR;
            for (int i = 0; i< times; i++) {
                out.write(SIXTY_FOUR_SPACES, 0, SIXTY_FOUR);
            }
            out.write(SIXTY_FOUR_SPACES, 0, rem);
            return;
        }
    }
    
    /**
     * Write a number of whitespaces to a StringBuffer
     * @param num
     * @param out
     */
    public static void indent(int num, StringBuilder out) {
        if (num <= SIXTY_FOUR) {
            out.append(SIXTY_FOUR_SPACES, 0, num);
            return;
        } else if (num <= 128){
            // avoid initializing loop counters if only one iteration
            out.append(SIXTY_FOUR_SPACES, 0, SIXTY_FOUR);
            out.append(SIXTY_FOUR_SPACES, 0, num - SIXTY_FOUR);
        } else {
            int times = num / SIXTY_FOUR;
            int rem = num % SIXTY_FOUR;
            for (int i = 0; i< times; i++) {
                out.append(SIXTY_FOUR_SPACES, 0, SIXTY_FOUR);
            }
            out.append(SIXTY_FOUR_SPACES, 0, rem);
            return;
        }
    }

}