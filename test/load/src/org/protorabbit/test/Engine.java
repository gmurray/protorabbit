package org.protorabbit.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Engine {

    public static void main(String[] args) {
        System.out.println( "Engine start!" );
        List<WebClient> clients = new ArrayList<WebClient>();
        int count = 150;
        List<ClientDetails> dlist = new ArrayList<ClientDetails>();

        ClientDetails d1 = new ClientDetails();
        d1.setRunCount( 50 );
        d1.setUrl( "http://localhost:8080/protorabbit/welcome.prt" );
        d1.setId( "R1" );
        dlist.add( d1 );

        ClientDetails d2 = new ClientDetails();
        d2.setRunCount( 50 );
        d2.setId( "R2" );
        d2.setUrl( "http://localhost:8080/protorabbit/about.prt" );
        dlist.add( d2 );

        ClientDetails d3 = new ClientDetails();
        d3.setRunCount( 50 );
        d3.setId( "R3" );
        d3.setUrl( "http://localhost:8080/protorabbit/private.prt" );
        dlist.add( d3 );

        Random rgen = new Random();

        for ( int i=0; i < count; i++ ) {
            int r = rgen.nextInt (3 );
            ClientDetails cd = dlist.get(r);
            WebClient wc = new WebClient( cd.getUrl(), cd.getId() + " Runner " + i, cd.getRuncount(), cd.getTimeout() );
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
        System.out.println( "All done!!!1" );
    }
}
