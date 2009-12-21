var testURIs = [
  "/protorabbit/secure/testPoller!doFoo.hop?name=5.2&json={blah:1}",
  "/protorabbit/secure/testLongPoller!doFoo.hop?name=5.2&json={blah:1}",
  "/protorabbit/secure/testNamespace!doFoo.hop?name=5.2&json={blah:1}",
  "/protorabbit/secure2/test!doFoo.hop?name=5.2&json={blah:1}"
];

var testContext = {};

function startTest() {

    var status = document.getElementById( "status" );
    status.innerHTML = "Starting...";

    var runCount = parseInt(document.getElementById("runCount").value);
    testContext = {
            successCount : 0,
            errorCount : 0,
            runCount : 0
    };
    testContext.runTargetCount = runCount;
    testContext.wait = parseInt( document.getElementById("timespan").value );
    runTest();
}

function runTest() {
    var status = document.getElementById( "status" );
    if ( testContext.runCount < testContext.runTargetCount ) {
        testContext.runCount++;
        var _index = Math.floor(Math.random() * testURIs.length );
        var _req = ajax( { asynchronous : false,
                           url : testURIs[ _index ],
                           onsuccess : function() {
                               testContext.successCount = testContext.successCount + 1;
                           },
                           onerror : function() {
                               testContext.errorCount = testContext.errorCount + 1;
                           }
                      });
        var _timeout = Math.floor(Math.random() * testContext.wait );
        status.innerHTML = "Timeout : " + _timeout + " Runs : " + testContext.runCount + " Success : " + testContext.successCount + " Errors : " + testContext.errorCount;
        setTimeout( runTest, _timeout );
    } else {
        status.innerHTML = "Test Complete. Runs : " + testContext.runCount + " Success : " + testContext.successCount + " Errors : " + testContext.errorCount;
    }
}