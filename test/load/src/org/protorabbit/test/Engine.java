package org.protorabbit.test;

import java.util.ArrayList;
import java.util.List;

public class Engine {

    public static void main(String[] args) {
        System.out.println("Engine start!");
        List<WebClient> clients = new ArrayList<WebClient>();
        int count = 200;
        for ( int i=0; i < count; i++ ) {
            WebClient wc = new WebClient( "Runner " + i, 20, 1000 );
        }

    }
}
