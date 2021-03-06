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
                    alert( "Error making request. Please try again later" );
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
            } else if ( typeof args.onsuccess === "function" ) {
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
    if (cTitle) {
        cTitle.innerHTML = "Clients&nbsp;(" + _count + ")";
    }
    var clientStatus = document.getElementById( "clientStatus" );
    if ( clientStatus ) {
        clientStatus.innerHTML = _count;
    }
    if ( document.getElementById("errorsPanel") === null ) {
        window.errors = model.errors;
    } else {
        var errorPanel = formatErrors( model.errors);
        document.getElementById("errorsPanel").innerHTML = errorPanel;
    }

    var _count = 0;
    if ( model !== null && model.errors.length > 0 ) {
        _count = model.errors.length;
    }
    var eTitle = document.getElementById("errorsTitle");
    if ( eTitle ){
        eTitle.innerHTML = "Errors&nbsp;(" + _count + ")";
    }
    var errorStatus = document.getElementById( "errorStatus" );
    if ( errorStatus ) {
        errorStatus.innerHTML = _count;
    }
}

function loadPageViews(_runonce) {
    if (window.pageRequest === null &&
         (window.polling === true || _runonce === true) ) {
        var timespan = document.getElementById("timespan").value;
        var resolution =  document.getElementById("resolution").value;
        window.pageRequest = new ajax({ 
            url : "../stats/current?duration=" + timespan + "&resolution=" + resolution,
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
    loadPageViews( true );
}

function formatPageViews(items) {
    var requestStatus = document.getElementById( "requestStatus" );
    if ( requestStatus ) {
        requestStatus.innerHTML = items.total;
    } else {
        document.getElementById( "status" ).innerHTML = "Total request(s) : " + items.total;
    }
    var chart = jmaki.getWidget("realtimeStats");
    if ( chart ) {
        /*, items.averageJSONPayload*/
        chart.setValue(
                {"data":[
                         items.json , items.view
                         ]
                }
         );
        items.averageJSONProcessingTime["lines"] = { "fill" : true };
        items.averageViewProcessingTime["lines"] = { "fill" : true };
       // items.averageJSONProcessingTime["bars"] = { "show" : true, barWidth : 'second' };
       // items.averageViewProcessingTime["bars"] = { "show" : true, barWidth : 'second' };
        jmaki.getWidget("responseTimeChart").setValue(
                {"data":[
                         items.averageJSONProcessingTime,
                         items.averageViewProcessingTime
                         ]
                }
         );
    }

}

function formatErrors( items ) {

    if ( (typeof items === "undefined") || items === null || (items !== null && items.length === 0)) {
        return "N/A";
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
     return s3table;
 }

function getActiveClientsPanel( items ) {
    var s3table = new tablebuilder("blockTable");
    var _count = 0;
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

     return { count : _count, table : s3table };
}

function formatActiveClients(items) {
    var _count = 0;
    var tDiv = document.getElementById("clientsPanel");
    if ( tDiv === null ) {
        for ( var i in items ) {
            if ( items.hasOwnProperty(i) ) {
                _count++;
            }
        }
        window.activeClients = items;
        return _count;
    }
    var panel = getActiveClientsPanel( items );
    if ( panel.count > 0) {
        tDiv.innerHTML = panel.table;
    } else {
        tDiv.innerHTML = "N/A";
    }

    return panel.count;
}

function formatPageStatCharts( stats ) {
    var chart = jmaki.getWidget("views");
    if ( !chart ) { 
        return;
    }
    var textHTML = stats["text/html"];
    var applicationJson = stats["application/json"];
    var pollers = stats['pollers'];

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
    var control = document.getElementById( "runControl" );
    var controlStatus = document.getElementById("runControlStatus");
    if (window.polling === false) {
        window.polling = true;
        controlStatus.innerHTML = "Running";
        control.innerHTML = "Stop";
        if ( document.getElementById("displayStatus") ) {
            document.getElementById("displayStatus").innerHTML = "Current";
        }
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

jmaki.subscribe("/jmaki/charting/line/zoomOut", function(args) {

    if ( args.widgetId === "realtimeStats" ) {
        jmaki.getWidget( "responseTimeChart" ).zoomOut();
    } else if ( args.widgetId === "responseTimeChart" ) {
        jmaki.getWidget( "realtimeStats" ).zoomOut();
    }
    var _start = args.ranges.xaxis.min.toFixed(0);
    var _end = args.ranges.xaxis.max.toFixed(0);
    window.minorRange = { start : _start, end : _end };
    getLogDetails();
});

jmaki.subscribe("/jmaki/charting/line/zoomIn", function(args) {

    if (args.widgetId === "realtimeStats") {
        jmaki.getWidget("responseTimeChart").zoom( args.ranges, args.zoomHistory );
    } else if ( args.widgetId === "responseTimeChart" ) {
        jmaki.getWidget("realtimeStats").zoom( args.ranges, args.zoomHistory );
    }
    if (window.polling === true) { 
        toggleRunning();
    }

    var _start = args.ranges.xaxis.min.toFixed(0);
    var _end = args.ranges.xaxis.max.toFixed(0);
    window.minorRange = { start : _start, end : _end };
    getLogDetails ();
});

function log( message ) {
    if ( console.log ) {
        console.log( message );
    }
}

jmaki.subscribe("/jmaki/charting/line/selectRange", function(args) {
log("selected " + jmaki.inspect( args, -1 ));
    if (args.widgetId === "summaryChart") {
        window.selectedRange = args;
        getSummaryDetails();
    }
});

function getSummaryDetails() {
    if ( ! window.selectedRange ) {
        log( "No range selected." );
        return;
    }
    resolution =  document.getElementById("resolution").value;
    var start = window.selectedRange.ranges.xaxis.min.toFixed(0);
    var end = window.selectedRange.ranges.xaxis.max.toFixed(0);
    window.lbm.addLightbox( {
        id : "loadStatus",
        label : "Loading",
        content : "<div style='padding:15px'><img style='float:left;padding:5px' src='resources/images/wait-spinner.gif'/>" +
                   "<div style='padding:10px'>Loading stats for " + formatTimestamp( new Number(start) ) +
                  " - " + formatTimestamp( new Number(end) ) + "</div></div>",
        startWidth : 500,
        startHeight : 145,
        resizable : false
    });
    window.pageRequest = new ajax({ 
        url : "../stats/archivedStats?start=" + start + "&end=" + end + "&resolution=" + resolution,
        callback : function(req) {
            var model = eval("(" + req.responseText + ")");
            if ( model != null) {
                renderPageView( model );
                if ( document.getElementById("displayStatus") ) {
                    document.getElementById( "displayStatus" ).innerHTML = "Displaying " + formatTimestamp( new Number(start) ) + " - " + formatTimestamp( new Number(end) );
                }
            } else {
                if ( document.getElementById("displayStatus") ) {
                    document.getElementById( "displayStatus" ).innerHTML = "No details for range " + formatTimestamp( new Number(start) ) + " - " + formatTimestamp( new Number(end) );
                }
            }
            window.lbm.removeLightbox( { targetId : "loadStatus" });
            window.pageRequest = null;
            if (window.polling === true) { 
                    toggleRunning();
            }
        }
    });
}

var summaryResolutions = {
        'ONE_DAY' : 1000 * 60 * 60 * 25, /* give one extra hour */
        'ONE_WEEK' : 1000 * 60 * 60 * 24 * 7
}

function updateSummary() {

    var sr =  document.getElementById("summaryResolution").value;

    var timespan = (new Date()).getTime() - summaryResolutions[ sr ];

    window.pageRequest = new ajax({ 
        url : "../stats/summariesSinceDate/" + timespan,
        callback : function(req) {
            var model = eval("(" + req.responseText + ")");
            if ( model === null ) return;
            jmaki.getWidget("summaryChart").setValue(
                    {"data":[
                             model.json , model.view
                             ]
                    }
             );
            window.pageRequest = null;
        }
    });
}

function getLogDetails() {
    if ( !window.minorRange ) {
        return;
    }
    var checked = document.getElementById("showLogs");
    if ( checked !== null ) {
        if ( checked.checked === true ) {
            log(" getting details for range " + window.minorRange.start + " to " + window.minorRange.end );
            var start = window.minorRange.start;
            var end = window.minorRange.end;
            window.lbm.addLightbox( {
                id : "loadStatus",
                label : "Loading",
                content : "<div style='padding:15px'><img style='float:left;padding:5px' src='resources/images/wait-spinner.gif'/>" +
                           "<div style='padding:10px'>Loading stat items for " + formatTimestamp( new Number(start) ) +
                          " - " + formatTimestamp( new Number(end) ) + "</div></div>",
                startWidth : 500,
                startHeight : 145,
                resizable : false
            });
            window.pageRequest = new ajax({ 
                url : "../stats/archivedStatItems?start=" + start + "&end=" + end,
                callback : function(req) {
                    var model = eval("(" + req.responseText + ")");
                    if ( model != null) {
                        window.statsItems = model;
                        renderStatItems();
                        if ( document.getElementById("displayItemsStatus") ) {
                            document.getElementById( "displayItemsStatus" ).innerHTML = "Displaying " + formatTimestamp( new Number(start) ) + " - " + formatTimestamp( new Number(end) );
                        }
                    } else {
                        if ( document.getElementById("displayItemsStatus") ) {
                            document.getElementById( "displayItemsStatus" ).innerHTML = "No details for range " + formatTimestamp( new Number(start) ) + " - " + formatTimestamp( new Number(end) );
                        }
                    }
                    window.lbm.removeLightbox( { targetId : "loadStatus" });
                    window.pageRequest = null;
                }
            });
            
        }
    }
    
}

function checkItem( item, filter ) {
    var regex = new RegExp(filter);
    for ( var i in item ) {
        if ( item.hasOwnProperty( i ) ) {
            if (regex.test( item[i] ) === true ) {
                log("match on " + i + " " + item[i])
                return true;
            }
        }
    }
    return false;
}

function renderStatItems( ) {
    var items = window.statsItems;
    var statItemPane = document.getElementById("statItemPane");
    var filter = null;
    if ( document.getElementById("filter").value !== "") {
        filter = document.getElementById("filter").value;
    }
    if ( statItemPane ) {
        statItemPane.style.display = "block";
    } else {
        return;
    }
    if (items === null || (items !== null && items.length === 0)) {
        document.getElementById("statItems").innerHTML = "N/A";
        return;
    }
     var s3table = new tablebuilder("blockTable");
     s3table.setHeader([  "Timestamp", "URI", "Client Id", "Content-Type", "Content-Length", "Process Time (ms)", "Error(s)"]);
     var listCount = 0;
     // put everything in buckets
     for ( var i=0; i < items.length; i+=1 ) {

         var _row = [];
         var _item = items[i];
         // add the time now so we can do a filter match on it
         _item.time = formatTimestamp( _item.timestamp );
         if ( filter !== null ) {
             if ( checkItem( _item, filter ) === false ) {
                 continue;
             }
         }
         listCount+=1;
         _row.push( _item.time );
         _row.push( _item.path );
         _row.push( _item.remoteClient );
         _row.push( _item.contentType );
         _row.push( _item.contentLength );
         _row.push( _item.processTime );
         if ( _item.errors !== null) {
             _row.push( _item.errors.join(",") );
         } else {
             _row.push( "" );
         }
         s3table.addRow( _row );
     }
    document.getElementById("statItems").innerHTML = s3table;
    document.getElementById("displayItemsCount").innerHTML = " (" + listCount + " of " + items.length + " items)";
}

function showErrors() {

    var errorsPanel = formatErrors( window.errors);
    window.lbm.addLightbox( {
        id : "errors",
        label : "Errors",
        content : "<div id='errorContent' style='padding:15px;height:540px;overflow-y:auto'>" +
                    errorsPanel +
               "</div>",
        startWidth : 900,
        startHeight : 645,
        showCloseButton : true,
        onresize : function( size ) {

          var errorContent = document.getElementById("errorContent");
          if (errorContent) {
              errorContent.style.height = size.h - 15 + "px";
          }
        }
    });

}

function showActiveClients() {

    var panel = getActiveClientsPanel( window.activeClients);
    var clientsPanel = "";
    if ( panel.count > 0) {
        clientsPanel = panel.table;
    } else {
        clientsPanel = "N/A";
    }
    window.lbm.addLightbox( {
        id : "clients",
        label : "Clients",
        content : "<div id='clientContent' style='padding:15px;height:540px;overflow-y:auto'>" +
                clientsPanel +
               "</div>",
        startWidth : 900,
        startHeight : 645,
        showCloseButton : true,
        onresize : function( size ) {

          var errorContent = document.getElementById("clientContent");
          if (errorContent) {
              errorContent.style.height = size.h - 15 + "px";
          }
        }
    });

}


