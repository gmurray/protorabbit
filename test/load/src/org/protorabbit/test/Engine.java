package org.protorabbit.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Engine {

    public static void main(String[] args) {
        System.out.println( "Engine start!" );
        List<WebClient> clients = new ArrayList<WebClient>();
        int count = 125;
        List<ClientDetails> dlist = new ArrayList<ClientDetails>();

        ClientDetails d1 = new ClientDetails();
        d1.setRunCount( 50 );
        d1.setTimeout( 60000 );
        d1.setUrl( "http://localhost:9090/protorabbit/welcome.prt" );
        d1.setId( "R1" );
        dlist.add( d1 );

        ClientDetails d2 = new ClientDetails();
        d2.setRunCount( 50 );
        d2.setId( "R2" );
        d2.setTimeout( 60000 );
        d2.setUrl( "http://localhost:9090/protorabbit/about.prt" );
        dlist.add( d2 );

        ClientDetails d3 = new ClientDetails();
        d3.setRunCount( 50 );
        d3.setId( "R3" );
        d3.setTimeout( 60000 );
        d3.setUrl( "http://localhost:9090/protorabbit/private.prt" );
        dlist.add( d3 );

        ClientDetails d4 = new ClientDetails();
        d4.setRunCount( 150 );
        d4.setId( "R4" );
        d4.setUrl( "http://localhost:9090/protorabbit/secure/testNamespace!doFoo.hop?name=5.2&json={blah:1}" );
        d4.setTimeout( 5000 );
        dlist.add( d4 );

        ClientDetails d5 = new ClientDetails();
        d5.setRunCount( 150 );
        d5.setTimeout( 5000 );
        d5.setId( "R5" );
        d5.setUrl( "http://localhost:9090/protorabbit/secure2/test!doFoo.hop?name=5.2&json={blah:1}" );
        dlist.add( d5 );

        int tcount = dlist.size();
        Random rgen = new Random();

        for ( int i=0; i < count; i++ ) {
            int r = rgen.nextInt ( tcount );
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
