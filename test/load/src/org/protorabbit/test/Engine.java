package org.protorabbit.test;

import java.util.ArrayList;
import java.util.List;

public class Engine {

    public static void main(String[] args) {
        System.out.println("Engine start!");
        List<WebClient> clients = new ArrayList<WebClient>();
        int count = 50;
        for ( int i=0; i < count; i++ ) {
            WebClient wc = new WebClient( "Runner " + i, 50, 1500 );
            wc.setRandomRange( true );
            clients.add(wc);
            wc.start();
        }
        for (WebClient wc : clients) {
            try {
                wc.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println("All done!!!1");
    }
}
