package fimk;

import nxt.Nxt;

public class Starter {
    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Nxt::shutdown));
            Nxt.init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t);
            t.printStackTrace();
        }
    }
}
