package org.protorabbit.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Engine {

    public static void main(String[] args) {
        System.out.println( "Engine start!" );
        long FOUR_MINUTES = 240000;
        long TWO_MINUTES = 120000;
        List<WebClient> clients = new ArrayList<WebClient>();
        int count = 125;
        List<ClientDetails> dlist = new ArrayList<ClientDetails>();

        ClientDetails d1 = new ClientDetails();
        d1.setRunCount( 5550 );
        d1.setTimeout( TWO_MINUTES );
        d1.setExpectedMinContentLength( 4591 );
        d1.setExpectedMaxContentLength( 4591 );
        d1.setUrl( "http://localhost:8080/protorabbit/welcome.prt" );
        d1.setId( "R1" );
        dlist.add( d1 );

        ClientDetails d2 = new ClientDetails();
        d2.setRunCount( 5550 );
        d2.setExpectedMinContentLength( 3464 );
        d2.setExpectedMaxContentLength( 3464 );
        d2.setId( "R2" );
        d2.setTimeout( FOUR_MINUTES );
        d2.setUrl( "http://localhost:8080/protorabbit/about.prt" );
        dlist.add( d2 );

        ClientDetails d3 = new ClientDetails();
        d3.setRunCount( 5550 );
        d3.setId( "R3" );
        d3.setExpectedMinContentLength( 7758 );
        d3.setExpectedMaxContentLength( 7758 );
        d3.setTimeout( 60000 );
        d3.setUrl( "http://localhost:8080/protorabbit/private.prt" );
        dlist.add( d3 );

        ClientDetails p1 = new ClientDetails();
        p1.setRunCount( 5550 );
        p1.setId( "P1" );
        p1.setExpectedMinContentLength( 91 );
        p1.setExpectedMaxContentLength( 124 );
        p1.setUrl( "http://localhost:8080/protorabbit/secure/testPoller!doFoo.hop?name=5.2&json={blah:1}" );
        p1.setTimeout( 15000 );
        dlist.add( p1 );

        ClientDetails p2 = new ClientDetails();
        p2.setRunCount( 5550 );
        p2.setId( "P2" );
        p2.setExpectedMinContentLength( 91 );
        p2.setExpectedMaxContentLength( 124 );
        p2.setUrl( "http://localhost:8080/protorabbit/secure/testLongPoller!doFoo.hop?name=5.2&json={blah:1}" );
        p2.setTimeout( 15000 );
        dlist.add( p2 );

        ClientDetails d4 = new ClientDetails();
        d4.setRunCount( 5550 );
        d4.setId( "R4" );
        d4.setExpectedMinContentLength( 91 );
        d4.setExpectedMaxContentLength( 124 );
        d4.setUrl( "http://localhost:8080/protorabbit/secure/testNamespace!doFoo.hop?name=5.2&json={blah:1}" );
        d4.setTimeout( 15000 );
        dlist.add( d4 );

        ClientDetails d5 = new ClientDetails();
        d5.setRunCount( 5550 );
        d5.setTimeout( 15000 );
        d5.setExpectedMinContentLength( 83 );
        d5.setExpectedMaxContentLength( 124 );
        d5.setId( "R5" );
        d5.setUrl( "http://localhost:8080/protorabbit/secure2/test!doFoo.hop?name=5.2&json={blah:1}" );
        dlist.add( d5 );

        int tcount = dlist.size();
        Random rgen = new Random();

        for ( int i=0; i < count; i++ ) {
            int r = rgen.nextInt ( tcount );
            ClientDetails cd = dlist.get(r);
            WebClient wc = new WebClient( cd.getUrl(), cd.getId() + " Runner " + i, cd.getRuncount(), cd.getTimeout() );
            wc.setExpectedMinContentLength( cd.getExpectedMinContentLength() );
            wc.setExpectedMaxContentLength( cd.getExpectedMaxContentLength() );
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
