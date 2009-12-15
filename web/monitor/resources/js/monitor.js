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
    _req.onreadystatechange = function() { 
        if (_req.readyState == 4) {
            if ((_req.status == 200 || _req.status === 0) &&
                    args.callback) {
              args.callback(_req);
            } else if (_req.status != 200) {
                alert("Error making request. Please try again later");
            }
        }
    };
    _req.open("GET", args.url, true);
    _req.send(null);
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

function loadStats() {
    loadPageStats();
    loadPageViews();
}

function loadPageViews() {
    var timespan = document.getElementById("timespan").value;
    document.getElementById( "bodyContent" ).innerHTML = "Loading...";

    var req = new ajax({ 
        url : "../prt?command=accessMetrics&duration=" + timespan,
        callback : function(req) {
            var model = eval("(" + req.responseText + ")");
            formatPageViews( model );
        }
    }); 
}

function roundToSecond(timestamp) {
    var mod = timestamp % 1000;
    return timestamp - mod;
}

function formatPageViews(items) {

    var s3table = new tablebuilder("blockTable");
    s3table.setHeader(["URI",  "Timestamp", "Client Id", "Content Type", "Content Length", "Process Time (ms)"]);

    var ds1 = { "yaxis" : 1, label : "text/html Requests", values : [] };
    var ds2 = { "yaxis" : 1, label : "application/json Requests", values : [] };

    for ( var i=0; i < items.length; i+=1 ) {
        var _row = [];
        var _item = items[i];
        if (_item.contentType === "text/html") {
     //       ds1.values.push({time : _item.timestamp, )
        } else if (item.contentType === "application/json"){
            
        }
        _row.push( _item.path );
        _row.push( new Date(_item.timestamp) );
        _row.push( _item.remoteClient );
        _row.push( _item.contentType );
        _row.push( _item.contentLength );
        _row.push( _item.processTime );
        s3table.addRow( _row );
    }
    /*
    jmaki.getWidget("realtimeStats").setValue(

            {"data":[

//{"lines":{"color":null,"barWidth":null,"show":false,"stacked":false},"yaxis":2,"label":"Satisfied","values":[{"time":1235959200000,"y":20},{"time":1235962800000,"y":50},{"time":1235966400000,"y":25},{"time":1235970000000,"y":22},{"time":1235973600000,"y":52},{"time":1235977200000,"y":26},{"time":1235980800000,"y":23},{"time":1235984400000,"y":51},{"time":1235988000000,"y":15},{"time":1235991600000,"y":26},{"time":1235995200000,"y":16},{"time":1235998800000,"y":54},{"time":1236002400000,"y":32},{"time":1236006000000,"y":21},{"time":1236009600000,"y":66},{"time":1236013200000,"y":35},{"time":1236016800000,"y":34},{"time":1236020400000,"y":53},{"time":1236024000000,"y":30},{"time":1236027600000,"y":24},{"time":1236031200000,"y":52},{"time":1236034800000,"y":35},{"time":1236038400000,"y":40}],"bars":{"color":null,"barWidth":"hour","show":true,"stacked":true},"id":"satisfied","points":null},{"lines":{"color":null,"barWidth":null,"show":false,"stacked":false},"yaxis":2,"label":"Unsatisfied with Service","values":[{"time":1235959200000,"y":10},{"time":1235962800000,"y":5},{"time":1235966400000,"y":6},{"time":1235970000000,"y":2},{"time":1235973600000,"y":4},{"time":1235977200000,"y":5},{"time":1235980800000,"y":6},{"time":1235984400000,"y":12},{"time":1235988000000,"y":4},{"time":1235991600000,"y":5},{"time":1235995200000,"y":15},{"time":1235998800000,"y":15},{"time":1236002400000,"y":21},{"time":1236006000000,"y":9},{"time":1236009600000,"y":11},{"time":1236013200000,"y":4},{"time":1236016800000,"y":10},{"time":1236020400000,"y":1},{"time":1236024000000,"y":8},{"time":1236027600000,"y":6},{"time":1236031200000,"y":6},{"time":1236034800000,"y":7},{"time":1236038400000,"y":2}],"bars":

//{"color":null,"barWidth":"hour","show":true,"stacked":true},"id":"unsatisfied-service","points":null},{"lines":{"color":null,"barWidth":null,"show":false,"stacked":false},"yaxis":2,"label":"Unsatisfied with Agent","values":[{"time":1235959200000,"y":3},{"time":1235962800000,"y":2},{"time":1235966400000,"y":1},{"time":1235970000000,"y":4},{"time":1235973600000,"y":4},{"time":1235977200000,"y":5},{"time":1235980800000,"y":3},{"time":1235984400000,"y":2},{"time":1235988000000,"y":6},{"time":1235991600000,"y":19},{"time":1235995200000,"y":15},{"time":1235998800000,"y":22},{"time":1236002400000,"y":24},{"time":1236006000000,"y":16},{"time":1236009600000,"y":10},{"time":1236013200000,"y":3},{"time":1236016800000,"y":5},{"time":1236020400000,"y":3},{"time":1236024000000,"y":4},{"time":1236027600000,"y":6},{"time":1236031200000,"y":2},{"time":1236034800000,"y":3},{"time":1236038400000,"y":5}],"bars":{"color":null,"barWidth":"hour","show":true,"stacked":true},"id":"unsatisfied-rep","points":null},

//{"lines":{"color":null,"barWidth":null,"show":false,"stacked":false},"yaxis":2,"label":"Unsatisfied with Agent and Service","values":[{"time":1235959200000,"y":4},{"time":1235962800000,"y":1},{"time":1235966400000,"y":7},{"time":1235970000000,"y":5},{"time":1235973600000,"y":1},{"time":1235977200000,"y":0},{"time":1235980800000,"y":0},{"time":1235984400000,"y":1},{"time":1235988000000,"y":0},{"time":1235991600000,"y":1},{"time":1235995200000,"y":4},{"time":1235998800000,"y":2},{"time":1236002400000,"y":4},{"time":1236006000000,"y":11},{"time":1236009600000,"y":4},{"time":1236013200000,"y":6},{"time":1236016800000,"y":6},{"time":1236020400000,"y":5},{"time":1236024000000,"y":6},{"time":1236027600000,"y":9},{"time":1236031200000,"y":7},{"time":1236034800000,"y":7},{"time":1236038400000,"y":7}],"bars":{"color":null,"barWidth":"hour","show":true,"stacked":true},"id":"unsatisfied-rep-and-service","points":null},

{"lines":null,"yaxis":1,"label":"Satisfaction Percentage","values":[{"time":1235959200000,"y":85.44},{"time":1235962800000,"y":87.22},{"time":1235966400000,"y":86.22},{"time":1235970000000,"y":83.22},{"time":1235973600000,"y":85.82},{"time":1235977200000,"y":84.22},{"time":1235980800000,"y":80.12},{"time":1235984400000,"y":79.12},{"time":1235988000000,"y":77.12},{"time":1235991600000,"y":75.11},{"time":1235995200000,"y":72.50},{"time":1235998800000,"y":71.33},{"time":1236002400000,"y":67.22},{"time":1236006000000,"y":65.22},{"time":1236009600000,"y":70.12},{"time":1236013200000,"y":75.12},{"time":1236016800000,"y":73.11},{"time":1236020400000,"y":74.12},{"time":1236024000000,"y":80.12},{"time":1236027600000,"y":81.91},{"time":1236031200000,"y":85.12},{"time":1236034800000,"y":88.11},{"time":1236038400000,"y":89.51}],"bars":null,"id":"satisfaction_percentage","points":{"color":null,"barWidth":null,"show":true,"stacked":false}}],}
]


});
   */ 
    document.getElementById( "bodyContent" ).innerHTML = s3table;
    document.getElementById( "status" ).innerHTML = "Total request(s) : " + items.length;
}

function loadPageStats() {

    var req = new ajax({ 
            url : "../prt?command=pageMetrics",
            callback : function(req) {
                var model = eval("(" + req.responseText + ")");
                formatPageStatCharts( model );
                setTimeout(loadPageStats, 10000);
            }
        }); 
}

function formatPageStatCharts( stats ) {
    var textHTML = stats["text/html"];
    var applicationJson = stats["application/json"];
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
}
