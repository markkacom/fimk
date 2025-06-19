package nxt.util;

import org.junit.Test;

import java.util.Arrays;

public class SearchTest {

    @Test
    public void testParseTags() {
        String[] parsed = Search.parseTags("one two, three four", 3, 20, 3);
        System.out.println(Arrays.toString(parsed));
        parsed = Search.parseTags("one 2, three four", 3, 20, 3);
        System.out.println(Arrays.toString(parsed));
        parsed = Search.parseTags("1, two, three four", 3, 20, 3);
        System.out.println(Arrays.toString(parsed));
        parsed = Search.parseTags("1-1, two-2, three four", 3, 20, 3);
        System.out.println(Arrays.toString(parsed));
    }

}
