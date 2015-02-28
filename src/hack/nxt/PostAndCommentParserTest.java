package nxt;

import java.io.UnsupportedEncodingException;

import nxt.util.Convert;


public class PostAndCommentParserTest {
  
    static class PostTest {
        String message;
        byte expected_type;
        long expected_identifier;
      
        public PostTest(String message, byte expected_type, long expected_identifier) {
            this.message = message;
            this.expected_type = expected_type;
            this.expected_identifier = expected_identifier;
        }
        
        public byte[] getMessage() throws UnsupportedEncodingException {
            return message.getBytes("UTF-8");
        }
        
        public String getStringMessage() {
            return message;
        }
        
        public byte getExpectedType() {
            return expected_type;
        }
  
        public long getExpectedIdentifier() {
            return expected_identifier;
        }
    }
    
    static class CommentTest {
        String message;
        long expected_identifier;
        
        public CommentTest(String message, long expected_identifier) {
            this.message = message;
            this.expected_identifier = expected_identifier;
        }
        
        public byte[] getMessage() throws UnsupportedEncodingException {
            return message.getBytes("UTF-8");
        }
  
        public String getStringMessage() {
            return message;
        }
          
        public long getExpectedIdentifier() {
            return expected_identifier;
        }      
    }
    
    static int success_count = 0;
    static int failed_count = 0;
  
    public static void main(String[] args) throws UnsupportedEncodingException {
      
        PostTest[] postTests = {
            new PostTest("post1:hello", (byte) '1', 0),
            new PostTest("post2:hello", (byte) '2', 0),
            new PostTest("post3:hello", (byte) '3', 0),
            new PostTest("post2:9963198050454909001:hello", (byte) '2', Convert.parseUnsignedLong("9963198050454909001")),
            new PostTest("post4:1426773769405531361014267737694055313610:hello", (byte) '4', 0),
            new PostTest("posT4:9963198050454909001:hello", (byte) 0, 0),
            new PostTest("pos4:9963198050454909001:hello", (byte) 0, 0),
        };
        
        CommentTest[] commentTests = {
            new CommentTest("comm9963198050454909001:hello", Convert.parseUnsignedLong("9963198050454909001")),
            new CommentTest("comm13346583409133620743:hello", Convert.parseUnsignedLong("13346583409133620743")),
            new CommentTest("comm12964068664814710491:hello", Convert.parseUnsignedLong("12964068664814710491")),
            new CommentTest("comm1426773769405531361014267737694055313610:hello", 0),
            new CommentTest("comm0:hello", 0),
            new CommentTest("comm19963198050454909001:hello", 0),
            new CommentTest("13346583409133620743:hello", 0),
            new CommentTest("comn12964068664814710491:hello", 0),
            new CommentTest("com3%1426773769405531361014267737694055313610:hello", 0),
            new CommentTest("com3%0:hello", 0)            
        };
        
        log("");
        log("Starting POST tests");        
        
        for (PostTest test : postTests) {
            runPostTest(test);
        }
        
        log("");
        log("Starting COMMENT tests");        

        for (CommentTest test : commentTests) {
            runCommentTest(test);
        }
        
        log("");
        log("Test COMPLETE success="+success_count+" failed="+failed_count);        

    }
    
    private static void runPostTest(PostTest test) throws UnsupportedEncodingException {
        log("");
        log("Running POST test");
        log(" > " + test.getStringMessage());
      
        byte type = MofoMessaging.parsePost(test.getMessage());
        if (type != test.getExpectedType()) {
            log(" FAILED! Expected type to be "+Byte.toString(test.getExpectedType())+" but got "+Byte.toString(type));
            failed_count++;
            return;
        }
        
        if (type != 0) {
            long identifier = MofoMessaging.parsePostIdentifier(test.getMessage());
            if (identifier != test.getExpectedIdentifier()) {
                log(" FAILED! Expected identifier to be "+Long.toString(test.getExpectedIdentifier())+" but got "+Long.toString(identifier));
                failed_count++;
                return;
            }
        }
        
        success_count++;
        log(" SUCCESS");
    }
  
    private static void runCommentTest(CommentTest test) throws UnsupportedEncodingException {
        log("");
        log("Running COMMENT test");
        log(" > " + test.getStringMessage());
        
        long identifier = MofoMessaging.parseComment(test.getMessage());
        if (identifier != test.getExpectedIdentifier()) {
            log(" FAILED! Expected identifier to be "+Long.toString(test.getExpectedIdentifier())+" but got "+Long.toString(identifier));
            failed_count++;
            return;
        }
        
        success_count++;
        log(" SUCCESS");     
    }
  
    static void log(String message) {
        System.out.println(message);
    }
}
