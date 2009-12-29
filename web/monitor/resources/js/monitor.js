/*
 *  monitor.js
 *  
 *  This page contains all the necessary functions to display protorabbit statistics
 * 
 */
// used by this monitor pages
window.polling = false;
window.pageRequest = null;
var resolution = "SECOND";

window.tablebuilder = function(tableClass) {

    var _header;
    var _rows = [];
    if (!tableClass) tableClass = "";

    this.setHeader = function(_headers) {
        _header = "<tr><th>" + _headers.join("</th><th>") + "</tr>";
    };

    this.addRow = function(_cells) {
        _rows.push("<tr><td>" + _cells.join("</td><td>") + "</td></tr>");
    };
    
    this.addRawRow = function(raw) {
        _rows.push(raw);
    };

    this.toString = function() {
        return  "<table class='" + tableClass + "'>" + _header + 
          "<tbody>" + _rows.join("") + "</tbody>" +
          "</table>";
    };
};

var blockWidth = 500;

var content = {
                 "text/css" : 0,
                 "text/javascript" : 0,
                 "text/html" : 0,
                 "html fragments" : 0
              };

var accessCounts = [];

var propTypes = {
 1 : 'insert',
 2 : 'include'

};

var globalContentLength = 0;

function createCachedTable(t) {
    var text = "";
    var s3table = new tablebuilder("blockTable");
    s3table.setHeader(["id", "Content Type", "expires", "created", "Max Age", "Last Accessed", "timeout", "Status", "Content Length", "Gzip Content Length", "Access Count", "Gzip Access Count"]);
    var rowCount = 0;
    for (var i in t) {

        var s = t[i];
        var cc = s.cacheContext;
        var cl =  s.contentLength;
        var gcl = s.gzipContentLength;
        if (typeof cl == "number") {
            globalContentLength += cl;
        }
        if (s.contentType) {
            var ctotal = s.gzipContentLength || 0;
            ctotal += s.contentLength || 0;
            content[s.contentType] += ctotal;
        }
        if (typeof gcl == "number") {
            globalContentLength += gcl;
        }
        if (s.accessCount || s.gzipAccessCount) {
            var total = s.accessCount || 0;
            total += s.gzipAccessCount;
            accessCounts.push({ label : i, value : total});
        }
        var rowSpan = 1;
        if (s.userAgentResources) {
            rowSpan = 2;
        }

        var _cells = [   i || '',
                          s.contentType || '',
                          cc.expires|| '',
                          (new Date(cc.created)),
                          cc.maxAge || '',
                          (new Date(s.lastAccessed)),
                          s.timeout || '',
                          s.status || '',
                          cl || '',
                          gcl || '',
                          s.accessCount,
                          s.gzipAccessCount
                          ];
        
        var row = "<tr><td rowspan='" + rowSpan + "'>" + _cells.join("</td><td>") + "</td></tr>";
        s3table.addRawRow(row);

        if (s.userAgentResources) {
            var text2 = createCachedTable(s.userAgentResources);
            s3table.addRawRow("<tr><td colspan='12'>" + text2 +"</td></tr>");
        }
        rowCount += 1;
    }
    if (rowCount > 0) {
        text = s3table.toString();
    } else {
        text = "N/A";
    }

    return text
}

function createResourcesBlock(t) {

    var te = document.createElement("div");
    te.className = "cacheBlockBox";
    document.body.appendChild(te);
    
    var title = document.createElement("div");
    title.innerHTML = "Resources";
    title.className = "blockTitle";
    te.appendChild(title);
  
    var bb = document.createElement("div");
    bb.className = "cacheBodyBlock";
    te.appendChild(bb);

    globalContentLength = 0;

    var cachedResources = document.createElement("div");
    cachedResources.innerHTML = "Cached Resources";
    cachedResources.className = "propertiesTitle";
    bb.appendChild(cachedResources);

    var s3tableElement = document.createElement("div");
    var text = "N/A";
    if (t.cachedResources) {
        text = createCachedTable(t.cachedResources);
    }
    s3tableElement.innerHTML = text;

    bb.appendChild(s3tableElement);

    var s6TitleElement = document.createElement("div");
    s6TitleElement.className = "propertiesTitle";
    s6TitleElement.innerHTML = "Cached Templates";
    bb.appendChild(s6TitleElement);
    
    var templateResources = [];
    for (var k in t.templates) {
        if (t.templates[k].templateResource) {
            templateResources.push({ id: k, templateResource : t.templates[k].templateResource, accessCount : t.templates[k].accessCount });
        }
    }
   var s6tableElement = document.createElement("div");

    if (templateResources.length > 0) {
        var s6table = new tablebuilder("blockTable");
        s6table.setHeader(["id", "Content Type", "expires", "created", "Max Age", "Last Accessed", "timeout", "Status", "Content Length", "Gzip Content Length", "Access Count", "Gzip Access Count", "Total Count"]);
        for (var l=0; l < templateResources.length; l+=1 ) {
            var s = templateResources[l].templateResource;
            var cl =  s.contentLength;
            var gcl = s.gzipContentLength;

            if (s.accessCount || s.gzipAccessCount) {
                var total = s.accessCount || 0;
                total += s.gzipAccessCount;
                accessCounts.push({ label : templateResources[l].id, value : total});
            }

            if (s.contentType) {
                var ctotal = cl || 0;
                ctotal += gcl || 0;
                content[s.contentType] += ctotal;
                globalContentLength += ctotal;
            }
            s6table.addRow([   templateResources[l].id || '',
                               s.contentType,
                               s.cacheContext.expires,
                               (new Date(s.cacheContext.created)),
                               s.cacheContext.maxAge,
                               (new Date(s.lastAccessed)),
                               s.timeout || '',
                               s.status,
                               cl || '',
                               gcl || '',
                               s.accessCount,
                               s.gzipAccessCount,
                               templateResources[l].accessCount
                              ]);
        }
        s6tableElement.innerHTML = s6table.toString();
    } else {
      s6tableElement.innerHTML = "N/A";
    }
    bb.appendChild(s6tableElement);

    // list the fragments
    var cachedFragments = document.createElement("div");
    cachedFragments.innerHTML = "Cached Fragments";
    cachedFragments.className = "propertiesTitle";
    bb.appendChild(cachedFragments);

    var s4tableElement = document.createElement("div");
    if (t.includeFiles) {
        var s4table = new tablebuilder("blockTable");
        s4table.setHeader(["id", "timeout", "Created", "Last Refresh", "Content Length"]);
        var rowCount = 0;
        for (var i in t.includeFiles) {
            var s =t.includeFiles[i];
            var cl =  s.contentLength;

            if (typeof cl == "number") {
                globalContentLength += cl;
                content["html fragments"] += cl;
            }
            s4table.addRow([   i || '',
                               s.timeout || '',
                              (new Date(s.created)),
                              (new Date(s.lastRefresh)),
                              cl || ''
                              ]);
            rowCount +=1;
        }
        if (rowCount > 0) {
            s4tableElement.innerHTML = s4table.toString();
        } else {
            s4tableElement.innerHTML = "N/A";
        }
    } else {
      s4tableElement.innerHTML = "N/A";
    }
    bb.appendChild(s4tableElement);

    var cachedTemplates = document.createElement("div");
    cachedTemplates.innerHTML = "Cached Template Skeletons (excluding fragments)";
    cachedTemplates.className = "propertiesTitle";
    bb.appendChild(cachedTemplates);
    
    var s5tableElement = document.createElement("div");
    if (t.templates) {
        var s5table = new tablebuilder("blockTable");
        s5table.setHeader(["id", "Created", "Last Refresh", "Content Length"]);
        var rowCount = 0;
        for (var i in t.templates) {
            var s = t.templates[i].documentContext;
            if (s == null) {
                continue;
            }
            var cl =  s.contentLength;

            if (s.contentType) {
                var ctotal = cl || 0;
                content[s.contentType] += ctotal;
                globalContentLength += ctotal;
            }
            s5table.addRow([   i || '',
                              (new Date(s.created)),
                              (new Date(s.lastRefresh)),
                              cl || ''
                              ]);
            rowCount +=1;
        }
        if (rowCount > 0) {
            s5tableElement.innerHTML = s5table.toString();
        } else {
            s5tableElement.innerHTML = "N/A";
        }
    } else {
      s5tableElement.innerHTML = "N/A";
    }
    bb.appendChild(s5tableElement);

    var total = document.getElementById("totalResources");
    var gcl = globalContentLength + " bytes";
    if (globalContentLength > 1024) {
        gcl = (globalContentLength / 1024).toFixed(2) + " kb";
    }
    total.innerHTML = "Total : " + gcl;

}

function getPosition(_e){
         var pX = 0;
         var pY = 0;
         if(_e.offsetParent) {
             while(true){
                 pY += _e.offsetTop;
                 pX += _e.offsetLeft;
                 if(_e.offsetParent === null){
                     break;
                 }
                 _e = _e.offsetParent;
             }
         } else if(_e.y) {
                 pY += _e.y;
                 pX += _e.x;
         }
         return  {x: pX, y: pY};
}

function getXHR () {
    if (window.XMLHttpRequest) {
        return new window.XMLHttpRequest();
    } else if (window.ActiveXObject) {
        return new window.ActiveXObject("Microsoft.XMLHTTP");
    } else {
        return null;
    }
}

function ajax(args) {
    var _req = getXHR();
    var _async = true;
    if ( args.asynchronous === false) {
        _async = false;
    }
    _req.onreadystatechange = function() { 
        if (_req.readyState == 4) {
            if (_req.status == 200 || _req.status === 0) {
                if ( args.callback) {
                    args.callback(_req);
                } else if ( typeof args.onsuccess === "function") {
                    var model = eval("(" + _req.responseText + ")");
                    args.onsuccess.apply( {}, [ model, _req ] );
                }
            } else if (_req.status != 200) {
                if ( typeof args.onerror === "function" ) {
                    args.onerror.apply( {}, [ _req ]);
                } else {
                    alert("Error making request. Please try again later");
                }
            }
        }
    };
    _req.open( "GET", args.url, _async );
    _req.send( null );
    if ( _async === false ) {
        if (_req.status == 200 || _req.status === 0) {
            if ( args.callback) {
                args.callback(_req);
            } else if ( typeof args.onsuccess === "function") {
                var model = eval("(" + _req.responseText + ")");
                args.onsuccess.apply( {}, [ model, _req ] );
            }
        } else if (_req.status != 200) {
            if ( typeof args.onerror === "function" ) {
                args.onerror.apply( {}, [ _req ]);
            } else {
                alert("Error making request. Please try again later");
            }
        }
    }
}

function zeroFill( _num ) {
    if ( _num < 10 ) {
        return "0" + _num;
    } else {
        return _num;
    }
}

function formatTimestamp( timestamp ) {
    if (timestamp === null ) {
        return "N/A";
    }
    var _d = new Date( timestamp );
    return zeroFill( _d.getMonth() + 1 ) + "/" +
           zeroFill( _d.getDate() ) + "/" +
           _d.getFullYear() + " " +
           zeroFill( _d.getHours() ) + ":" +
           zeroFill( _d.getMinutes() ) + ":" +
           zeroFill( _d.getSeconds() );
}

function loadData() {
    var req = new ajax({ 
            url : "../prt?command=stats",
            callback : function(req) {
                var model = eval("(" + req.responseText + ")");
                createResourcesBlock(model);
                createChart(model);
            }
        }); 
}

function createChart( model ) {
   var chart = jmaki.getWidget("dispo");
   var data = [];
   for (var i in content) {
      if (content[i] > 0) {
          data.push({ label : i, values : [ { value : content[i] }] });
      }
   }
   chart.setValue(data);
   var rchart = jmaki.getWidget("topResources");
   var hdata = [];

   function sortByValue(a, b) {
        var y = a.value;
        var x = b.value;
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
   }
   accessCounts.sort(sortByValue);
   // only list the top 10 resources
   for (var i=0; i < accessCounts.length && i < 10; i+= 1) {
      hdata.push({ label : accessCounts[i].label , values : [ { value : accessCounts[i].value }] });
   }
   rchart.setValue(hdata);
}

function renderPageView( model ) {
    formatPageViews( model );
    formatPageStatCharts( model.pageStats );
    var _count = formatActiveClients( model.clients );
    var cTitle = document.getElementById("clientsTitle");
    cTitle.innerHTML = "Clients&nbsp;(" + _count + ")";
    formatErrors( model.errors );
    var _count = 0;
    if ( model !== null && model.errors.length > 0 ) {
        _count = model.errors.length;
    }
    var eTitle = document.getElementById("errorsTitle");
    eTitle.innerHTML = "Errors&nbsp;(" + _count + ")";

}

function loadPageViews(_runonce) {
    if (window.pageRequest === null &&
         (window.polling === true || _runonce === true) ) {
        var timespan = document.getElementById("timespan").value;

        resolution =  document.getElementById("resolution").value;
        window.pageRequest = new ajax({ 
            url : "../stats/all?duration=" + timespan + "&resolution=" + resolution,
            callback : function(req) {
                var model = eval("(" + req.responseText + ")");
                renderPageView( model );
                if (_runonce !== true) {
                    setTimeout(loadPageViews, 10000);
                }
                window.pageRequest = null;
            }
        });
    }
}

function updateResolution() {
    resolution =  document.getElementById("resolution").value;
    loadPageViews(true);
}

function formatPageViews(items) {

    document.getElementById( "status" ).innerHTML = "Total request(s) : " + items.total;
    /*, items.averageJSONPayload*/
    jmaki.getWidget("realtimeStats").setValue(
            {"data":[
                     items.json , items.view
                     ]
            }
     );
    items.averageJSONProcessingTime["lines"] = { "fill" : true };
    items.averageViewProcessingTime["lines"] = { "fill" : true };
    jmaki.getWidget("responseTimeChart").setValue(
            {"data":[
                     items.averageJSONProcessingTime, items.averageViewProcessingTime
                     ]
            }
     );
    

}

function formatErrors(items) {
    if (items === null || (items !== null && items.length === 0)) {
        document.getElementById("errorsPanel").innerHTML = "N/A";
        return;
    }
     var s3table = new tablebuilder("blockTable");
     s3table.setHeader(["URI",  "Timestamp", "Client Id", "Error(s)"]);

     // put everything in buckets
     for ( var i=0; i < items.length; i+=1 ) {
         var _row = [];
         var _item = items[i];
         _row.push( _item.path );
         _row.push( formatTimestamp( _item.timestamp ) );
         _row.push( _item.remoteClient );
         _row.push( _item.errors.join(",") );
         s3table.addRow( _row );
     }
    document.getElementById("errorsPanel").innerHTML = s3table;

 }

function formatActiveClients(items) {
    var tDiv = document.getElementById("clientsPanel");
    var _count = 0;
    var s3table = new tablebuilder("blockTable");
    s3table.setHeader(["Client Id", "JSON Count", "View Count", "Error Count", "Last Access"]);
    // put everything in buckets
    for ( var i in items ) {
         if ( items.hasOwnProperty(i) ) {
             _count++;
             var _row = [];
             var _item = items[i];
             _row.push( _item.clientId );
             _row.push( _item.jSONRequestCount );
             _row.push( _item.viewRequestCount );
             _row.push( _item.errorCount );
             _row.push( formatTimestamp( _item.lastAccess ) );
             s3table.addRow( _row );
         }
     }
     if (_count > 0) {
         tDiv.innerHTML = s3table;
     } else {
         tDiv.innerHTML = "N/A";
     }
    return _count;
}

function formatPageStatCharts( stats ) {
    var textHTML = stats["text/html"];
    var applicationJson = stats["application/json"];
    var pollers = stats['pollers'];
    var chart = jmaki.getWidget("views");
    var data = [];
    for (var i in textHTML) {
        data.push({ label : i, values : [ { value : textHTML[i].accessCount }] });
    }
    chart.setValue(data);
    var chart2 = jmaki.getWidget("actions");
    var data = [];
    for (var i in applicationJson) {
        data.push({ label : i, values : [ { value : applicationJson[i].accessCount }] });
    }
    chart2.setValue(data);
    // pollers
    var chart3 = jmaki.getWidget("pollers");
    var data = [];
    for (var i in pollers) {
        data.push({ label : i, values : [ { value : pollers[i].accessCount }] });
    }
    chart3.setValue(data);
}

function toggleRunning() {
    var control = document.getElementById("runControl");
    var controlStatus = document.getElementById("runControlStatus");
    if (window.polling === false) {
        window.polling = true;
        controlStatus.innerHTML = "Running";
        control.innerHTML = "Stop";
        loadPageViews();
    } else {
        window.polling = false;
        control.innerHTML = "Start";
        controlStatus.innerHTML = "Stopped";
        if (window.pageRequest) {
            // stop the ajax request
            window.pageRequest.abort();
            window.pageRequest = null;
        }
    }

}

jmaki.subscribe("/jmaki/charting/line/zoom", function() {
    if (window.polling === true) {
        toggleRunning();
    }
});

window.ONE_WEEK = 1000 * 60 * 60 * 24;

function getWeekSummary() {

    var timespan = (new Date()).getTime() - ONE_WEEK;

    window.pageRequest = new ajax({ 
        url : "../stats/summariesSinceDate/" + timespan,
        callback : function(req) {
            var model = eval("(" + req.responseText + ")");
            jmaki.getWidget("weekSummary").setValue(
                    {"data":[
                             model.json , model.view
                             ]
                    }
             );
            window.pageRequest = null;
        }
    });
}

function getDataForTimestamp() {
    renderPageView (
            {"view":{"yaxis":1,"label":"text/html","values":[{"time":1262026320000,"y":1},{"time":1262026260000,"y":1},{"time":1262025840000,"y":5},{"time":1262025000000,"y":156},{"time":1262024940000,"y":388},{"time":1262024880000,"y":395},{"time":1262024820000,"y":390},{"time":1262024760000,"y":377},{"time":1262024700000,"y":347},{"time":1262024640000,"y":245},{"time":1262024580000,"y":21}]},"total":22524,"averageViewProcessingTime":{"yaxis":1,"label":"text/html","values":[{"time":1262026320000,"y":6},{"time":1262026260000,"y":23},{"time":1262025840000,"y":13.4},{"time":1262025000000,"y":2699.3333333333335},{"time":1262024940000,"y":2.654639175257732},{"time":1262024880000,"y":2.4253164556962026},{"time":1262024820000,"y":2.628205128205128},{"time":1262024760000,"y":3.3421750663129974},{"time":1262024700000,"y":2.962536023054755},{"time":1262024640000,"y":618.861224489796},{"time":1262024580000,"y":76.19047619047619}]},"errors":[],"averageJSONProcessingTime":{"yaxis":1,"label":"application/json","values":[{"time":1262025060000,"y":5.714285714285714},{"time":1262025000000,"y":30.440999138673558},{"time":1262024940000,"y":5.404107762069885},{"time":1262024880000,"y":4.82602423542989},{"time":1262024820000,"y":4.682897862232779},{"time":1262024760000,"y":5.511448417802933},{"time":1262024700000,"y":4.7527220630372495},{"time":1262024640000,"y":10.412128712871286},{"time":1262024580000,"y":17.066176470588236}]},"averageJSONPayload":{"yaxis":1,"label":"application/json","values":[{"time":1262025060000,"y":89.26315789473684},{"time":1262025000000,"y":88.38587424633937},{"time":1262024940000,"y":87.95838890370766},{"time":1262024880000,"y":87.71177149451817},{"time":1262024820000,"y":87.78444180522565},{"time":1262024760000,"y":87.92693594031387},{"time":1262024700000,"y":87.82005730659026},{"time":1262024640000,"y":88.38737623762377},{"time":1262024580000,"y":89.17647058823529}]},"clients":{"127.0.0.1":{"pollInterval":5000,"errorCount":0,"jSONRequestCount":18835,"totalRequestCount":21153,"clientId":"127.0.0.1","lastAccess":1262027195409,"viewRequestCount":2318},"10.2.237.6":{"pollInterval":5000,"errorCount":0,"jSONRequestCount":1363,"totalRequestCount":1371,"clientId":"10.2.237.6","lastAccess":1262027195401,"viewRequestCount":8}},"json":{"yaxis":1,"label":"application/json","values":[{"time":1262025060000,"y":133},{"time":1262025000000,"y":1161},{"time":1262024940000,"y":3749},{"time":1262024880000,"y":3466},{"time":1262024820000,"y":3368},{"time":1262024760000,"y":3887},{"time":1262024700000,"y":3490},{"time":1262024640000,"y":808},{"time":1262024580000,"y":136}]},"pageStats":{"text/html":{"/about.prt":{"averageContentLength":3464,"averageProcessingTime":3464.1621287128714,"accessCount":808,"totalProcessingTime":2799043,"totalContentLength":2798912},"//welcome.prt":{"averageContentLength":4591,"averageProcessingTime":4591.4,"accessCount":5,"totalProcessingTime":22957,"totalContentLength":22955},"/welcome.prt":{"averageContentLength":4591,"averageProcessingTime":4591.156983930779,"accessCount":809,"totalProcessingTime":3714246,"totalContentLength":3714119},"/private.prt":{"averageContentLength":7758,"averageProcessingTime":7758.163584637269,"accessCount":703,"totalProcessingTime":5453989,"totalContentLength":5453874},"/blueprints-css-liquid.prt":{"averageContentLength":8179,"averageProcessingTime":12,"accessCount":1,"totalProcessingTime":12,"totalContentLength":8179}},"application/json":{"/secure/testPoller!doFoo.hop":{"averageContentLength":91.09464475079534,"averageProcessingTime":91.135737009544,"accessCount":3772,"totalProcessingTime":343764,"totalContentLength":343609},"/secure/testLongPoller!doFoo.hop":{"averageContentLength":91.09230340211936,"averageProcessingTime":91.12744004461796,"accessCount":3586,"totalProcessingTime":326783,"totalContentLength":326657},"/secure2/test!doFoo.hop":{"averageContentLength":83.0948587078302,"averageProcessingTime":83.0979708701606,"accessCount":8033,"totalProcessingTime":667526,"totalContentLength":667501},"/secure/testNamespace!doFoo.hop":{"averageContentLength":91.10734345745787,"averageProcessingTime":91.12960266278344,"accessCount":4807,"totalProcessingTime":438060,"totalContentLength":437953}}},"averageViewPayload":{"yaxis":1,"label":"text/html","values":[{"time":1262026320000,"y":4591},{"time":1262026260000,"y":4591},{"time":1262025840000,"y":5942},{"time":1262025000000,"y":5202.871794871795},{"time":1262024940000,"y":5037.943298969072},{"time":1262024880000,"y":5218.367088607595},{"time":1262024820000,"y":5093.592307692307},{"time":1262024760000,"y":5223.535809018567},{"time":1262024700000,"y":5089.190201729107},{"time":1262024640000,"y":5272.902040816327},{"time":1262024580000,"y":5616.0952380952385}]}}                   )
}
