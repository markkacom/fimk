package fimk;

import nxt.Block;
import nxt.Generator;
import nxt.Nxt;

import java.util.*;

public class Starter {

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Nxt::shutdown));
            Nxt.init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t);
            t.printStackTrace();
        }

        readInputCommands();

        System.exit(0);

    }

    private static void readInputCommands() {
        Scanner scanner = new Scanner(System.in);
        String input = "";
        while (!("quit".equals(input) || "q".equals(input))) {
            input = scanner.nextLine().trim();
            processCommand(input);
        }
    }

    private static void processCommand(String input) {
        try {
            String[] ss = input.split("\\s+");
            Iterator<String> tokens = Arrays.asList(ss).iterator();
            while (tokens.hasNext()) {
                String command = tokens.next().toLowerCase();
                /*
                   popblocks 4
                 */
                if ("popblocks".equals(command)) {
                    if (tokens.hasNext()) {
                        int blocksNumber = Integer.parseInt(tokens.next());
                        try {
                            Nxt.getBlockchainProcessor().setGetMoreBlocks(false);
                            List<? extends Block> poppedBlocks = Nxt.getBlockchainProcessor().popOffTo(
                                    Nxt.getBlockchain().getHeight() - blocksNumber);
                            Optional<String> s = poppedBlocks.stream()
                                    .map(block -> block.getHeight() + " " + Long.toUnsignedString(block.getId()) + "\n")
                                    .reduce((x, y) -> x + y);
                            System.out.printf("Popped blocks:\n%s", s.orElse(""));
                        } finally {
                            Nxt.getBlockchainProcessor().setGetMoreBlocks(true);
                        }
                    }
                }
                if ("startforging".equals(command)) {
                    String secret = input.substring("startforging".length()).trim();
                    Object result = Generator.startForging(secret);
                    if (result instanceof String) {
                        System.out.println(result);
                    }
                }
                if ("stopforging".equals(command)) {
                    Generator.stopForging();
                }
                // scan height validate
                // for example: scan 5799989 true
                if ("scan".equals(command)) {
                    int height = 0;
                    if (tokens.hasNext()) {
                        height = Integer.parseInt(tokens.next());
                    }
                    boolean validate = false;
                    if (tokens.hasNext()) {
                        validate = Boolean.parseBoolean(tokens.next());
                    }
                    try {
                        Nxt.getBlockchainProcessor().setGetMoreBlocks(false);
                        Nxt.getBlockchainProcessor().scan(height, validate);
                    } finally {
                        Nxt.getBlockchainProcessor().setGetMoreBlocks(true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
