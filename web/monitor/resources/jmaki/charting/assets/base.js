jmaki.namespace("jmaki.widgets.jmaki.charting");
/**
 *  This is the base class for all Flot jMaki widgets.
 *
 */
jmaki.widgets.jmaki.charting.base = function() {

    this.model = {};
    var autoSizeH = true;
    var autoSizeW = true;
    var _widget = this;
    var wargs;
    var markers = {};

    var colorSchemes = [
                        [ "#0039E6", "#1A53FF", "#3366FF", "#4D79FF", "#809FFF"],
                        ["#BE2C2B", "#D13333", "#D54848", "#DA5D5D", "#DF7272"],
                        [ "#338D25", "#42B530", "#49C936", "#5BCF4A", "#80DA72"],
                        ["#744B9B", "#8153AC", "#8E64B4", "#9B75BD", "#A887C5"],
                        ["#26A0C5", "#2DAFD7", "#43B7DB", "#58C0DF", "#6DC8E3"],
                        ["#D95F3A", "#DE714F", "#E28265", "#E6937A", "#EAA590"],
                        ["#000000", "#262626", "#404040", "#595959", "#737373"]
                       ];

    this.ctx = {
            height :300,
            width :300,
            rightMargin :0,
            leftMargin :0,
            topMargin :0,
            bottomMargin :0,
            backgroundColor : "#FFF",
            showTooltip :true,
            axisLabelFontSize :9,
            axisLineWidth :1,
            zoom : true,
            colorScheme :undefined,
            colors : [ "#edc240", "#afd8f8", "#cb4b4b", "#4da74d", "#9440ed" ],
            legend : {
                noColumns :2,
                position : "ne",
                labelFormatter : function(contents,value, x,y) {
                return contents;
            }
    },
    zoomHistory : [],
    selection : {
        mode :"x",
        color :"#e8cfac"
    },
    grid : {
        hoverable :true,
        clickable :true
    },
    labelFormatter : function(contents, value, x, y) {
        if ( typeof value !== 'undefined' ) {
            return value.x + "," + value.y + " " + contents;
        } else if ( typeof y !== 'undefined'){
            return x + "," + y;
        } else if (contents.percent){
            return contents.percent.toFixed(1) + "%";
        } else {
            return contents;
        }
    },
    hoverFormatter :  function(v) {
        return "[" + v.value.x + "," + v.value.y + "] " + v.label;
    }
    };

    function getMarkers(_ds, point) {
        if (!_ds || !_ds.markerPoints) {
            return null;
        }
        var obucket = _ds.markerPoints[point.x];
        if (obucket) {
            if (obucket[point.y]) {
                return obucket[point.y];
            }
        }
        return null;
    }

    function setMarkers(_ds, markers, point) {
        if (!_ds.markerPoints) {
            _ds.markerPoints = {};
        }
        var obucket = _ds.markerPoints[point.x];
        // create a buckets
        if (!obucket) {
            obucket = {};
            _ds.markerPoints[point.x] = obucket;
        }
        obucket[point.y] = markers;
    }

    this.processArgs = function(wargs) {
        _widget.wargs = wargs;
        if (wargs.publish) {
            _widget.publish = wargs.publish;
        }
        if (wargs.subscribe){
            if (typeof wargs.subscribe == "string") {
                _widget.subscribe = [];
                _widget.subscribe.push(wargs.subscribe);
            } else {
                _widget.subscribe = wargs.subscribe;
            }
        }     
        if (wargs.args) {
            jmaki.mixin(wargs.args, this.ctx, true);
            if (this.ctx.legend && this.ctx.legend.targetId) {
                this.ctx.legend.container = $("#" + this.ctx.legend.targetId);
            }
        }
    };

    this.getDataset = function(targetId) {
        for (var i=0; i < _widget.model.data.length; i+=1) {
            if (_widget.model.data[i].id == targetId) {
                return _widget.model.data[i];
            }
        }
        return null;
    };

    this.getChartContainer = function(wargs) {
        if (!_widget.chartContainer) {
            _widget.chartContainer = document.getElementById(wargs.uuid + "_content");
        }
        return _widget.chartContainer;
    };

    this.getChart = function(wargs, callback) {
        if (!_widget.chart) {
            _widget.chart = document.getElementById(wargs.uuid + "_chart");
        }
        return _widget.chart;
    };

    this.clear = function() {
        // clear axes
        if (_widget.x2AxislabelDiv) {
            _widget.x2AxislabelDiv.parentNode.removeChild(_widget.x2AxislabelDiv);
            _widget.x2AxislabelDiv = null;
        }
        if (_widget.xAxislabelDiv && _widget.xAxislabelDiv.parentNode) {
            _widget.xAxislabelDiv.parentNode.removeChild(_widget.xAxislabelDiv);
            _widget.xAxislabelDiv = null;
        }
        if (_widget.y2AxislabelDiv) {
            _widget.y2AxislabelDiv.parentNode.removeChild(_widget.y2AxislabelDiv);
            _widget.y2AxislabelDiv = null;
        }
        if (_widget.yAxislabelDiv && _widget.yAxislabelDiv.parentNode) {
            _widget.yAxislabelDiv.parentNode.removeChild(_widget.yAxislabelDiv);
            _widget.yAxislabelDiv = null;
        }
        if (_widget.wargs && _widget.ctx.legend.targetId && _widget.ctx.legend.targetId) {	
            $("#" + _widget.ctx.legend.targetId).html("");
        }
        this.clearMarkers();
        _widget.model.data = [];
        _widget.model.markers = {};
        $("#" + _widget.wargs.uuid + "_tooltip").remove();
        $("#" + _widget.wargs.uuid + "_zoomer").remove();
        _widget.ctx.zoomHistory = [];
        _widget.render();
    };

    this.removeDataset = function(obj) {
        var targetId;
        if (obj.message) {
            obj = obj.message;
        }
        if (obj.targetId) {
            targetId = obj.targetId;
        } else {
            targetId = obj;
        }

        if (targetId) {
            for (var i=0; i < _widget.model.data.length; i+=1) {
                if (_widget.model.data[i].id == targetId) {
                    _widget.model.data.splice(i, 1);
                }
            }
            if (_widget.model.data[targetId]) {
                delete _widget.model.data[targetId];
            }       
            // remove markers
            for (var j in _widget.model.markers) {
                if (typeof _widget.model.markers[j] == "object") {
                    var marker = _widget.model.markers[j];
                    if (marker.ds.id == targetId) {
                        _widget.removeMarker(marker);
                    }
                }
            }
            _widget.render();
        }
    };

    this.doSubscribe = function(topic, handler) {
        if (!_widget.subs) {
            _widget.subs = [];
        }
        var i = jmaki.subscribe(topic, handler);
        _widget.subs.push(i);
    };

    this.destroy = function() {
        for (var i=0; _widget.subs && i < _widget.subs.length; i+=1) {
            jmaki.unsubscribe(_widget.subs[i]);
        }
        if (typeof markers == 'undefined') {
            return;
        }
        for (var ii=0; markers && ii< markers.length; ii+=1) {
            _widget.model.markers[ii].divs[0].parentNode.removeChild(_widget.model.markers[ii].divs[0]);
            _widget.model.markers[ii].divs[1].parentNode.removeChild(_widget.model.markers[ii].divs[1]);
        }
    };

    this.resize = function() {
    };

    this.updateAxes = function(obj) {
    };

    this.addDatasetToModel = function(_data) {

        if (_data.value) {
            _data = _data.value;
        }
        if (!_widget.model.datasetMap)  {
            _widget.model.datasetMap = {};
        }
        var _ds = {};
        _ds.lines = { show: true };
        _ds.label = _data.label;
        if (_data.yaxis) {
            _ds.yaxis = _data.yaxis;
            hasY2Axes = true;
        }

        if (_data.id) {
            _ds.id = _data.id;
        }
        if (_data.color) {
            _ds.color = _data.color;
        }

        if (_widget.model.datasetMap[_ds.id]){
            var counter = _widget.model.data.length -1;
            var _id = "ds_" + counter;
            while (_widget.model.datasetMap[_id]) {
                _id = "dataset_" + (counter+=1);
            }
            _ds.id = _id;
        }
        if (typeof _data.selectable == 'boolean') {
            _ds.selectable = _data.selectable;
        }
        if (_data.bars) {
            _ds.bars = _data.bars;
        }
        if (_data.action) {
            _ds.action = _data.action;
        }
        if (_data.lines) {
            _ds.lines = _data.lines;
        }

        if (_data.explodeRadius) {
            _ds.explodeRadius = _data.explodeRadius;
        }

        if (_data.points) {
            _ds.points = _data.points;
        }
        if (_data.fill) {
            _ds.lines.fill = _data.fill;
        }
        if (_data.values && typeof _data.values[0] == 'number') {
            _ds.data = [];
            // inner
            for (var j=0; j < _data.values.length; j+=1) {
                _ds.data.push([ j,_data.values[j]]);
            }
        } else if (_data.values && _data.values.length > 0 && _data.values[0].value) {
            _ds.data =_data.values[0].value;
            // convert times
        } else if (_data.values && _data.values.length > 0 && _data.values[0].time) {
            _ds.data = [];
            for (var jj=0; jj < _data.values.length; jj+=1) {
                _ds.data.push([_data.values[jj].time , _data.values[jj].y]);
            }
        } else {
            _ds.data =_data.values;
        }
        _widget.model.data.push(_ds);
    };

    this.addDataset = function(_data) {
        _widget.addDatasetToModel(_data);
        _widget.render();
    };

    this.setValue = function(data) {
        _widget.clear();
        var values;
        if (data.data) {
            values = data.data;
            if (data.xAxis) {
                _widget.model.xAxis = data.xAxis; 
            }
            if (data.x2Axis) {
                _widget.model.x2Axis = data.x2Axis;
            }
            if (data.yAxis) {
                _widget.model.yAxis = data.yAxis; 
            }
            if (data.y2Axis) {
                _widget.model.y2Axis = data.y2Axis;
            }
            if (data.hoverFormatter) {
                _widget.model.hoverFormatter = data.hoverFormatter;
            }
        } else {
            values = data;
        }
        for (var i=0; i < values.length; i+=1) {
            _widget.addDatasetToModel(values[i]);
        } 
        _widget.positionAxes();
        _widget.render();
    };

    this.init = function(wargs) {

        _widget.container = document.getElementById(wargs.uuid);
        
        // get the data and initialize
        if (wargs.value) {
            _widget.value = wargs.value;
            _widget.processArgs(wargs);
            _widget.initPlotter(wargs);
        } else if (wargs.service) {
            jmaki.doAjax({url: wargs.service,
                synchronous : true,
                callback: function(req) {
                if (req.responseText === '') {
                    jmaki.log("Widget Error: Service " + wargs.service + " returned no data");
                    return;
                }
                _widget.value = jmaki.json.deserialize(req.responseText);
                _widget.processArgs(wargs);
                _widget.initPlotter(wargs);
            }});
        } else {
            _widget.value = { data : []};
            _widget.processArgs(wargs);
            _widget.initPlotter(wargs);
        }
    };

    this.waitForChart = function(wargs, _callback) {
        _widget.chart = document.getElementById(wargs.uuid + "_chart");
        if (_widget.chart === null) {
            var te = document.getElementById(wargs.uuid + "_content");
            te.innerHTML = "";
            var chartContainer = document.createElement("div");
            var _chartDiv = document.createElement("canvas");
            chartContainer.appendChild(_chartDiv);
            te.appendChild(_chartDiv);
        }
        _widget.chart = G_vmlCanvasManager.initElement(_chartDiv);
        _callback(wargs);
    };

    this.initPlotter = function(wargs) {
        if (jmaki.MSIE) {
            _widget.waitForChart(wargs, this.plotterReady);
        } else {
            this.plotterReady(wargs);
        }
    };

    function createRotatedTextNode(target, message, rotate, rx, ry) {
        var svgTag = document.createElementNS("http://www.w3.org/2000/svg", "svg:svg");
        svgTag.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink","http://www.w3.org/1999/xlink");
        var textTag = document.createElementNS("http://www.w3.org/2000/svg", "text");
        var txt = message;
        textTag.setAttribute("transform", " translate(" + rx + "," + ry + ") rotate(" + rotate + ")");
        textTag.setAttribute("x", 0);
        textTag.setAttribute("y", 0);
        svgTag.appendChild(textTag);
        textTag.appendChild(document.createTextNode(txt));
        target.appendChild(svgTag);
    }

    function getTextDimensions(text, styles) {
        var e = document.createElement("div");
        e.innerHTML = text;
        if (styles) {
            for (var i in styles) {
                if (i.hasOwnProperty(styles)) {
                    e.style[i] = styles[i]; 
                }
            }
        }
        e.style.position = "absolute";
        e.style.top = "-500px";
        document.body.appendChild(e);
        var tdim = { width: e.clientWidth, height : e.clientHeight };
        document.body.removeChild(e);
        return tdim;
    }

    function mixAxes(_cA, _mA) {
        if (typeof _mA == 'undefined') {
            _mA = {};
        }
        jmaki.mixin(_cA, _mA );
    }

    this.positionAxes = function() {

        var _colors = _widget.ctx.colors;
        if (typeof _widget.ctx.colorScheme == "number") {
            _colors = colorSchemes[_widget.ctx.colorScheme];
        }

        _widget.model.options = {
                backgroundColor :  _widget.ctx.backgroundColor,
                rightMargin : _widget.ctx.rightMargin,
                leftMargin : _widget.ctx.leftMargin,
                topMargin : _widget.ctx.topMargin,
                bottomMargin : _widget.ctx.bottomMargin,
                colors : _colors,
                points: { show: false },
                legend: _widget.ctx.legend,
                grid : _widget.ctx.grid,
                xaxis: _widget.model.xAxis || _widget.ctx.xAxis,
                x2axis: _widget.model.x2Axis,
                yaxis: _widget.model.yAxis || _widget.ctx.yAxis,
                y2axis: _widget.model.y2Axis,
                selection: _widget.ctx.selection,
                labelFormatter : _widget.model.labelFormatter || _widget.ctx.labelFormatter
        };

        if (_widget.chartType == "pie") {
            _widget.model.options.pie = {
                    show : true,
                    showLabel : true,
                    labelFormatter : _widget.ctx.labelFormatter,
                    labelStyles : _widget.ctx.labelStyles,
                    showLabel : _widget.ctx.showLabels,
                    explodeRadius : _widget.ctx.explodeRadius
            };
            _widget.model.options.clickable = true;
        }
        

        _widget.model.options.legend.legendBase = _widget.wargs.uuid;

        var hasY2Axes = false;
        var hasX2Axes = false;

        // reformat data from jmaki style to flot style
        if (_widget.value && _widget.value.data) {

            // add dataset info
            for (var i=0; i < _widget.value.data.length; i+=1) {
                var _ds = _widget.value.data[i];
                _widget.addDatasetToModel(_ds);
                if (_ds.xaxis === 2) {
                    hasX2Axes = true;
                }
                if (_ds.yaxis === 2) {
                    hasY2Axes = true;
                }
            }
        }
        if (hasY2Axes){
            _widget.ctx.rightMargin = 0;
        }

        if (_widget.ctx.yAxis || _widget.value.yAxis) {
            _widget.model.yAxis = {};
            mixAxes( _widget.value.yAxis || _widget.ctx.yAxis, _widget.model.yAxis);
        }
        if (_widget.ctx.y2Axis || _widget.value.y2Axis) {
            _widget.model.y2Axis = {};

            mixAxes(_widget.ctx.y2Axis || _widget.value.y2Axis , _widget.model.y2Axis);
        }

        if (_widget.ctx.xAxis || _widget.value.xAxis) {
            _widget.model.xAxis = {};
            mixAxes((_widget.value.xAxis || _widget.ctx.xAxis) , _widget.model.xAxis);
        }

        if (_widget.ctx.x2Axis || _widget.value.x2Axis) {
            mixAxes(_widget.ctx.x2Axis || _widget.value.x2Axis, _widget.model.x2Axis);
        }

        if (_widget.model.yAxis && _widget.model.yAxis.title) {
            if (!_widget.yAxislabelDiv) {
                _widget.yAxislabelDiv = document.createElement("div");
            }
            _widget.yAxislabelDiv.className = "jmaki-charting-y1-axis-label";
            _widget.yAxislabelDiv.style.visibility = "hidden";

            _widget.container.appendChild( _widget.yAxislabelDiv);

            var rotated = false;
            var tdim = getTextDimensions(_widget.model.yAxis.title);
            if (_widget.model.yAxis.rotate === true ) {
                rotated = true;
                if (!jmaki.MSIE) {
                    createRotatedTextNode(_widget.yAxislabelDiv, _widget.model.yAxis.title, -90, tdim.height - 5, tdim.width + 2);
                } else {
                    _widget.yAxislabelDiv.innerHTML = "<div style='position:relative;writing-mode:bt-rl'>" + _widget.model.yAxis.title + '</div>';
                }
            } else {
                _widget.yAxislabelDiv.innerHTML = _widget.model.yAxis.title;
            }
            _widget.positionYAxisLabel(_widget.yAxislabelDiv, rotated, tdim, 1);
        }
        if (_widget.model.y2Axis && _widget.model.y2Axis.title) {
            if (!_widget.y2AxislabelDiv) {
                _widget.y2AxislabelDiv = document.createElement("div");
            }
            _widget.y2AxislabelDiv.className = "jmaki-charting-y2-axis-label";
             _widget.y2AxislabelDiv.style.visibility = "hidden";

            _widget.container.appendChild( _widget.y2AxislabelDiv);

            var rotatedY2 = false;
            var tdimY2 = getTextDimensions(_widget.model.y2Axis.title);
            if (_widget.model.y2Axis.rotate === true ) {
                rotatedY2 = true;
                if (!jmaki.MSIE) {
                    createRotatedTextNode(_widget.y2AxislabelDiv, _widget.model.y2Axis.title, 90, 5, 0);
                } else {
                    _widget.y2AxislabelDiv.innerHTML = "<div style='position:relative;writing-mode:tb-rl'>" + _widget.model.y2Axis.title + '</div>';
                }
            } else {
                _widget.y2AxislabelDiv.innerHTML = _widget.model.y2Axis.title;
            }

            _widget.positionYAxisLabel(_widget.y2AxislabelDiv, rotatedY2, tdimY2, 2);

        }

        if (_widget.model.xAxis && _widget.model.xAxis.title) {
            if (!_widget.xAxislabelDiv) {
                _widget.xAxislabelDiv = document.createElement("div");
            }

            _widget.xAxislabelDiv.className = "jmaki-charting-x1-axis-label";
            _widget.xAxislabelDiv.style.visibility = "hidden";
            _widget.container.appendChild(_widget.xAxislabelDiv);
            _widget.xAxislabelDiv.innerHTML = _widget.model.xAxis.title;
            _widget.positionXAxisLabel();
        } 
        if (_widget.model.x2Axis && _widget.model.x2Axis.title) {
            if (!_widget.x2AxislabelDiv) {
                _widget.x2AxislabelDiv = document.createElement("div");
            }
            _widget.x2AxislabelDiv.className = "jmaki-charting-x2-axis-label";
            _widget.x2AxislabelDiv.style.visibility = "hidden";
            _widget.container.appendChild(_widget.x2AxislabelDiv);
            _widget.x2AxislabelDiv.innerHTML = _widget.model.x2Axis.title;
            _widget.positionX2AxisLabel();
        }

        if (_widget.model.yAxis && (_widget.model.yAxis.tickFormatter != 'time' &&
                typeof _widget.model.yAxis.tickFormatter != 'function' )) {
            _widget.model.yAxis.tickFormatter =  function (v, axis) {
                return getYLabel(v.toFixed(axis.tickDecimals));
            };
        }

        if (_widget.model.y2Axis && (_widget.model.y2Axis.tickFormatter != 'time'  &&
                typeof _widget.model.y2Axis.tickFormatter != 'function' ) ) {

            _widget.model.y2Axis.tickFormatter =  function (v, axis) {
                return getY2Label(v.toFixed(axis.tickDecimals));
            };
        }
        if (_widget.model.x2Axis && (_widget.model.x2Axis.tickFormatter != 'time'  &&
                typeof _widget.model.x2Axis.tickFormatter != 'function' )) {
            _widget.model.x2Axis.tickFormatter = function (v, axis) {
                return getX2Label(v.toFixed(axis.tickDecimals));
            };
        }
        if (_widget.model.xAxis &&  (_widget.model.xAxis.mode != 'time'  &&
                typeof _widget.model.xAxis.tickFormatter != 'function' )) {
            _widget.model.xAxis.tickFormatter = function (v, axis) {
                return getXLabel(v.toFixed(axis.tickDecimals));
            };
        }    
    };

    this.plotterReady = function(wargs) { 

        if (autoSizeH || autoSizeW) {
            var _dim = jmaki.getDimensions(_widget.container,50);

            if(autoSizeW) {
                _widget.ctx.width = _dim.w;
            }
            if (autoSizeH) {
                _widget.ctx.height = _dim.h;
            }
        }

        _widget.chartContainer = _widget.getChartContainer(wargs);

        _widget.getChartContainer(wargs).style.width = _widget.ctx.width  + "px";
        _widget.getChartContainer(wargs).style.height = _widget.ctx.height + "px";
        _widget.container.style.width = _widget.ctx.width  + "px";
        _widget.container.style.height = _widget.ctx.height + "px";
       

        _widget.getChart(wargs).height = _widget.ctx.height -2;
        _widget.getChart(wargs).width = _widget.ctx.width -2;
        var dScheme;

        _widget.model.data = [];
        // need clean markers array

        if (_widget.model.markers) {
            _widget.markers = _widget.model.markers;
            _widget.model.markers = {};
        }

        _widget.pos = jmaki.getPosition(_widget.chartContainer);
        _widget.positionAxes();
        var placeholder = $("#" + wargs.uuid);

        function showTooltip(x, y, contents, _value) {
            var props = { label : contents, x: x, y : y};
            if (typeof _value != "undefined") {
                jmaki.mixin(_value, props);
            }
            var hf = (_widget.model.hoverFormatter ||
                    _widget.ctx.hoverFormatter);
            var _w = 200;
            var wdim = getWindowDimensions();

            // show on other side if going off the right side of the screen
            if (x + _w > wdim.w - 30) {
                x = x - _w;
            } 

            $("<div id='" + _widget.wargs.uuid + "_tooltip'>" + hf.apply({}, [props]) + "</div>").css( {
                position: "absolute",
                top: y + 5,
                left: x + 5,
                width : _w  + "px",
                border: "1px solid #000",
                padding: "2px",
                "background-color": "#fff",
                opacity: 0.85
            }).appendTo("body");

        }

        var previousPoint = null;

        placeholder.bind("legendselect", function(e, item, elementId) {

            var ds = _widget.getDataset(item.id);

            if (_widget.ctx.explodeRadius) {

                for (var i=0; i < _widget.model.data.length; i+=1) {
                    if (_widget.model.data[i].id == item.id) {
                        _widget.model.data[i].explodeRadius = _widget.ctx.explodeRadius;
                    } else {
                        _widget.model.data[i].explodeRadius = 0;
                    }
                }

                ds.explodeRadius = _widget.ctx.explodeRadius;

            }
            _widget.render();

            if (!ds) return;
            ds.legendElementId = elementId;
            jmaki.processActions({
                topic : _widget.publish,
                widgetId : _widget.wargs.uuid,
                type : "onLegendSelect",
                action : ds.action,
                targetId : item.id,
                value : ds
            });

        });

        placeholder.bind("plothover", function (event, pos, item) {

            var margins = _widget.plot.getMargins();
            var x = pos.x + margins.left;
            var y = pos.y + margins.top;

            jmaki.publish(_widget.publish + "/onHover", { type : 'onHover', position : pos, item : item} );

            if (_widget.chartType != 'pie' && _widget.ctx.showHoverLabels !== false) {
                var _pos = { x : _widget.plot.getAxes().xaxis.p2c(x),
                        y : _widget.plot.getAxes().yaxis.p2c(y) };

                if (_widget.ctx.showTooltip === true) {
                    if (item && item != null) {
                        if (previousPoint != item.datapoint) {
                            previousPoint = item.datapoint;

                            $("#" + _widget.wargs.uuid + "_tooltip").remove();
                            if (typeof item.datapoint[0] === "number") {
                                var x = item.datapoint[0].toFixed(2),
                                    y = item.datapoint[1].toFixed(2);

                                showTooltip(item.pageX,
                                        item.pageY,
                                        item.series.label,
                                        { value : { x : item.datapoint[0], y : item.datapoint[1]},
                                    series : item.series
                                        });
                            }
                        }
                    } else {
                        $("#" + _widget.wargs.uuid + "_tooltip")
                        .remove();
                        previousPoint = null;
                    }
                }
            } else {
                if (_widget.ctx.showHoverLabels !== false) {
                    $("#" + _widget.wargs.uuid + "_tooltip").remove();
                    showTooltip(item.pageX + _widget.pos.x, 
                            item.pageY + _widget.pos.y,
                            item.label,
                            {percent : item.percent, series : item.series});
                }

            }
        });

        placeholder.bind("plotclick", function (event, pos, item) {
            if (_widget.chartType != 'pie') {

                if (item) {
                    var margins = _widget.plot.getMargins();
                    var x = pos.x + margins.left;
                    var y = pos.y + margins.top;
                    var _pos = { x : _widget.plot.getAxes().xaxis.p2c(x),
                            y : _widget.plot.getAxes().yaxis.p2c(y) };
                    jmaki.publish(_widget.publish + "/onClick", { position : _pos,  item : item} );

                    _widget.plot.highlight(item.series, item.datapoint);

                }
            } else {
                if (!item) return;
                var ds = _widget.getDataset(item.id);
                if (_widget.ctx.explodeRadius) {

                    for (var i=0; i < _widget.model.data.length; i+=1) {
                        if (_widget.model.data[i].id == item.id) {
                            _widget.model.data[i].explodeRadius = _widget.ctx.explodeRadius;
                        } else {
                            _widget.model.data[i].explodeRadius = 0;
                        }
                    }
                    if (ds.selectable !== false) {
                        ds.explodeRadius = _widget.ctx.explodeRadius;
                        _widget.render();

                    }
                }
                jmaki.processActions( { topic : _widget.publish,
                    action : ds.action, 
                    widgetId : _widget.wargs.uuid,
                    type : "onClick",
                    targetId : item.id,
                    value : item 
                });
            }
        });

        placeholder.bind("plotselected", function (event, ranges) {

            // TODO : Publish this
           // $("#selection").text(ranges.xaxis.from.toFixed(1) + " to " + ranges.xaxis.to.toFixed(1));
            if (_widget.ctx.zoom === true) {
                if (_widget.ctx.zoomHistory.length !== 0) {
                    _widget.ctx.zoomHistory.push( {xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to } } );
               } else {
                   var xaxis = _widget.plot.getAxes().xaxis;
                   _widget.ctx.zoomHistory.push( {xaxis:{from: xaxis.datamin, to: xaxis.datamax } } );
                   _widget.ctx.zoomHistory.push( {xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to } } );  
               }
                var _target = $("#" + _widget.wargs.uuid + "_content");
                var _lranges = {
                    xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
                };
                _widget.ctx.currentZoom = _lranges;

                _widget.plot = $.plot(_target, _widget.model.data,
                        $.extend(true, {}, _widget.model.options, _lranges));
                _widget.zoomMarkers( ranges );
                addZoomer();
                jmaki.publish(_widget.publish + "/zoomIn", { widgetId : _widget.wargs.uuid, ranges : _lranges , zoomHistory : _widget.ctx.zoomHistory} );
            }
        });
    };

    function addZoomer() {
        _widget.zoomer = document.getElementById(_widget.wargs.uuid + "_zoomer" );
        if (_widget.zoomer === null) {
            _widget.zoomer = document.createElement("div");
            _widget.zoomer.style.background = "#fff";
            _widget.zoomer.style.right = 2 + "px";
            _widget.zoomer.id = _widget.wargs.uuid + "_zoomer";
            _widget.zoomer.style.bottom = 0 + "px";
            _widget.zoomer.innerHTML ="[-]";
            _widget.zoomer.style.width = "16px";
            _widget.zoomer.style.position = "absolute";
            _widget.container.appendChild(_widget.zoomer);
            _widget.zoomer.onclick = _widget.zoomOut;
            _widget.zoomer.style.cursor = "pointer";
        }
    }

    function removeZoomer() {
        _widget.zoomer = document.getElementById(_widget.wargs.uuid + "_zoomer" );
        if (_widget.zoomer !== null) {
            _widget.zoomer.parentNode.removeChild( _widget.zoomer );
        }
    }

    this.zoom = function( ranges, zhistory ) {
        if (_widget.ctx.zoom === true && ranges) {
           if (zhistory ) {
               _widget.ctx.zoomHistory = zhistory;
           } else if (_widget.ctx.zoomHistory.length !== 0) {
                _widget.ctx.zoomHistory.push( {xaxis: { min: ranges.xaxis.min, max: ranges.xaxis.max } } );
           } else {
               var xaxis = _widget.plot.getAxes().xaxis;
               _widget.ctx.zoomHistory.push( {xaxis:{from: xaxis.datamin, to: xaxis.datamax } } );
               _widget.ctx.zoomHistory.push( {xaxis: { min: ranges.xaxis.min, max: ranges.xaxis.max } } );
           }

            var _target = $("#" + _widget.wargs.uuid + "_content");
            var _lranges = {
                xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
            };
            _widget.ctx.currentZoom = _lranges;

            _widget.plot = $.plot(_target, _widget.model.data,
                    $.extend(true, {}, _widget.model.options, ranges));

            addZoomer();
        }
    };

    this.zoomOut = function() {
        var _atTop = false;
        if (_widget.ctx.zoomHistory.length > 0 && _widget.ctx.zoom === true) {

            var ranges = _widget.ctx.zoomHistory[_widget.ctx.zoomHistory.length - 1];

            if ( ranges ) {
            _widget.ctx.zoomHistory.splice((_widget.ctx.zoomHistory.length - 1), 1);
            var _target = $("#" + _widget.wargs.uuid + "_content");
            if (_widget.ctx.currentZoom.xaxis.max === ranges.xaxis.max && _widget.ctx.currentZoom.xaxis.min === ranges.xaxis.min) {
                ranges = _widget.ctx.zoomHistory[_widget.ctx.zoomHistory.length - 1];
                _widget.ctx.zoomHistory.splice((_widget.ctx.zoomHistory.length - 1), 1);
                _atTop = true;
            }
            _widget.ctx.currentZoom = ranges;
            _widget.plot = $.plot(_target, _widget.model.data,
                            $.extend(true, {}, _widget.model.options, {
                                xaxis: ranges.xaxis
                            }));
            _widget.zoomMarkers(ranges);
            jmaki.publish(_widget.publish + "/zoomOut", { widgetId : _widget.wargs.uuid, ranges : ranges, zoomHistory : _widget.ctx.zoomHistory } );
            }

        }
        if (_widget.ctx.zoomHistory.length > 0 && _atTop === false ) {
            addZoomer(); 
        } else {
            removeZoomer();
        }

    };

    this.selectSlice = function(_targetId) {
        if (_widget.ctx.explodeRadius) {

            for (var i=0; i < _widget.model.data.length; i+=1) {
                if (_widget.model.data[i].id == _targetId) {
                    _widget.model.data[i].explodeRadius = _widget.ctx.explodeRadius;
                } else {
                    _widget.model.data[i].explodeRadius = 0;
                }
            }
        }
        _widget.render();
    };

    this.render = function() {
        var _target = $("#" + _widget.wargs.uuid + "_content");
        _widget.plot = $.plot(_target, _widget.model.data, _widget.model.options);
        if ( _widget.markers) {
            for (var i=0; i <  _widget.markers.length; i+=1) {
                _widget.addMarker( _widget.markers[i]);
            }
        }
    };

    function getYLabel(_val) {
        if (!_widget.model.yAxis) {
            return;
        }
        if (!_widget.model.yAxis.labels) {
            return _val;
        }
        // find a better search for this
        for (var i=0; _widget.model.yAxis.labels && i < _widget.model.yAxis.labels.length; i+=1) {
            if (_widget.model.yAxis.labels[i].value == _val) return _widget.model.yAxis.labels[i].label;
        }
        if (_widget.model.yAxis.labels[_val]) return _widget.model.yAxis.labels[_val];
        else return '';
    }   

    function getY2Label(_val) {
        if (!_widget.model.y2Axis) return;
        if (!_widget.model.y2Axis.labels) {
            return _val;
        }
        // find a better search for this
        for (var i=0; _widget.model.y2Axis.labels && i < _widget.model.y2Axis.labels.length; i+=1) {
            if (_widget.model.y2Axis.labels[i].value == _val) {
                return _widget.model.y2Axis.labels[i].label;
            }
        }
        if (_widget.model.y2Axis.labels[_val]) return _widget.model.y2Axis.labels[_val];
        else return '';
    }

    function getXLabel(_val) {
        if (!_widget.model.xAxis) return;
        if (!_widget.model.xAxis.labels) {
            return _val;
        }
        // find a better search for this
        for (var i=0; _widget.model.xAxis.labels && i < _widget.model.xAxis.labels.length;i+=1) {
            if (_widget.model.xAxis.labels[i].value == _val) return _widget.model.xAxis.labels[i].label;
        }
        if (_widget.model.xAxis.labels[_val]) return _widget.model.xAxis.labels[_val].label;
        else return _val;
    }

    function getX2Label(_val) {
        if (!_widget.model.x2Axis) return;
        if (!_widget.model.x2Axis.labels) {
            return _val;
        }
        // find a better search for this
        for (var i=0; _widget.model.x2Axis.labels && i < _widget.model.x2Axis.labels.length;i+=1) {
            if (_widget.model.x2Axis.labels[i].value == _val) return _widget.model.x2Axis.labels[i].label;
        }
        if (_widget.model.x2Axis.labels[_val]) return _widget.model.x2Axis.labels[_val].label;
        else return _val;
    }

    function processActions(_t, _pid, _type, _value) {
        jmaki.processActions({
            topic : _widget.publish,
            action : _t.action,
            type : _type,
            value : _value,
            targetId : _pid,
            widgetId : _widget.wargs.uuid
        });
    }

    function addMarkerDiv(marker) {

        if (!marker.ds) {
            jmaki.log("could not find data source for marker " + marker.id);
            return;
        }

        var mDiv = document.createElement("div");

        // give the marker a uuid so we can remove it
        if (marker.value) {
            mDiv.action = target.value.action;
        } else if (marker.action) {
            mDiv.action = marker.action;
        }

        mDiv.style.border = "1px solid #000";
        mDiv.style.fontSize = "10px";
        mDiv.style.padding = "1px";
        mDiv.style.background = "#FFF";
        mDiv.style.position = "absolute";
        mDiv.style.cursor = "pointer";
        // later for customization
        mDiv.className = "jmaki-charting-marker-label";
        if (marker.style) {
            for (var i in marker.style) {
                mDiv.style[i] = marker.style[i];
            }
        }

        var label;
        if (marker.value) label = marker.value.label;
        else if (marker.label) label = marker.label;

        label = label.replace('@{value}', marker.point.x + "," + marker.point.y, 'g');
        label = label.replace('{value}', marker.point.x + "," + marker.point.y, 'g');
        mDiv.appendChild(document.createTextNode(label));

        var mADiv = document.createElement("div");

        mADiv.style.width = "1px";
        mADiv.style.height = "15px";
        mADiv.style.background ="#000";
        mADiv.style.position = "absolute";
        // css override:
        mADiv.className = "jmaki-charting-marker-pointer";
        if (marker.pointerStyle) {    	
            for (var i in marker.pointerStyle) {
                mADiv.style[i] = marker.pointerStyle[i];
            }
        }        

        document.body.appendChild(mDiv);
        document.body.appendChild(mADiv);

        var markerCount = 0;
        var markerPoints = marker.markerPoints;
        markerCount = markerPoints.length;

        // re-calculate the offset
        var yoffset = 0;

        if (markerCount > 0) {
            for ( var i = 0; i < markerCount; i+=1) {
                if (markerPoints[i].div) {
                    yoffset += markerPoints[i].div.clientHeight;
                }
                if (i >0 ) {
                    //yoffset += 5;
                }
            }
        }

        marker.div = mDiv;
        marker.pointerDiv = mADiv;

        // calculate the coordinates
        var mx;

        if (marker.ds.xaxis == 2) {
            mx = _widget.plot.getPlotOffset().left + _widget.plot.getAxes().x2axis.p2c(marker.point.x);
            scaleX = _widget.plot.getAxes().x2axis.scale;
        } else {
            mx = _widget.plot.getPlotOffset().left + _widget.plot.getAxes().xaxis.p2c(marker.point.x);
        }
        var my;
        if (marker.ds.yaxis == 2) {
            my = _widget.plot.getPlotOffset().top + _widget.plot.getAxes().y2axis.p2c(marker.point.y) - 2;
        } else {
            my = _widget.plot.getPlotOffset().top + _widget.plot.getAxes().yaxis.p2c(marker.point.y) - 2;
        }
        mx +=   _widget.ctx.leftMargin;
        my +=  _widget.ctx.topMargin;

        // position the divs
        mDiv.style.left = _widget.pos.x  + mx + "px";
        mDiv.style.top = (_widget.pos.y - mDiv.clientHeight - mADiv.clientHeight + my) - yoffset + "px";
        mADiv.style.left = _widget.pos.x + mx + "px";
        mADiv.style.top = _widget.pos.y  - mADiv.clientHeight + my - yoffset + "px";
        mDiv.onclick = function() {
            processActions(this, this.markerId, 'onMarkerClick',  marker);
        };
        mDiv.style.visibility = "visible";
        mADiv.style.visibility = "visible";
    }

    /**
     *  Add a marker to a given chart taking into account the different chart types.
     * Marker format is expected to be in format like:
     *
     * { targetId : 'gray', label : 'I am {value}', index : 8}
     * 
     *  targetId refers to the data set id.
     *  index is the index in the dataset where to place the marker
     *  labels is the label to be used. This may include markup. If a @{value} or {value} is encountered it will be replaced with the value at the given index.
     */
    this.addMarker = function (target) {
        if (typeof target == 'undefined') return;
        if (target.message) target = target.message;
        var targetValue;
        var targetPoint;
        var targetIndex = target.index;
        var point = target.point;
        var _ds;

        var targetId = target.targetId;
        if (targetId) {
            for (var i=0; i < _widget.model.data.length; i+=1) {
                if (_widget.model.data[i].id && _widget.model.data[i].id == targetId) {
                    _ds = _widget.model.data[i];
                    if (!point && targetIndex) {
                        if (_widget.model.data[i].data[targetIndex]) {
                            point = { x :  _widget.model.data[i].data[targetIndex][0],
                                    y : _widget.model.data[i].data[targetIndex][1]
                            };
                        }
                    }
                }
            }
        };
        jmaki.log("adding marker " + target.id + " point=" + point + " index=" + targetIndex);
        if (!_ds) {
            jmaki.log("Charting Non-Fatal Error: Dataset not found for marker " + targetId);
            return;
        }
        if (!point && targetIndex) {
            jmaki.log("Charting Non-Fatal Error: Marker point not found for index " + targetIndex);
            return;
        }
        if (target.id) {
            // prevent
            if ( _widget.model.markers[target.id]) {
                jmaki.log("jMaki Charting: addMaker. Can't add marker with duplicate id");
                return;
            }
        } else {
            target.id = jmaki.genId();
        }

        target.ds = _ds;
        target.point = point;

        _widget.model.markers[target.id] = target;
        var markerPoints = getMarkers(target.ds, target.point); 
        if (!markerPoints) {
            markerPoints = [];
        }
        target.markerPoints = markerPoints;
        addMarkerDiv(target);
        markerPoints.push(target);
        setMarkers(_ds, markerPoints, point);
    };

    this.positionX2AxisLabel = function() {

        if (_widget.x2AxislabelDiv.style.visibility == "hidden") {
            _widget.x2AxislabelDiv.style.visibility = "visible";
        }
        var mx = ( _widget.ctx.width / 2) - (_widget.x2AxislabelDiv.clientWidth / 2);
        
        _widget.x2AxislabelDiv.style.top = 2 + "px";
        _widget.x2AxislabelDiv.style.left = mx + "px";
        if (!_widget.ctx.topMargin ||
            !_widget.ctx.topMargin ===0 ) {
            _widget.model.options.topMargin = _widget.ctx.axisLabelFontSize + 10;
        }
    };

    this.positionXAxisLabel = function() {

        if (_widget.xAxislabelDiv.style.visibility == "hidden")
            _widget.xAxislabelDiv.style.visibility = "visible";
        
        var mx = ( _widget.ctx.width / 2) - (_widget.xAxislabelDiv.clientWidth / 2);
        var my =  _widget.ctx.height - _widget.xAxislabelDiv.clientHeight;
        
        _widget.xAxislabelDiv.style.top = my + "px";
        _widget.xAxislabelDiv.style.left = mx + "px";
        if (!_widget.ctx.bottomMargin ||
            !_widget.ctx.bottomMargin === 0 ) {
            _widget.model.options.bottomMargin = _widget.xAxislabelDiv.clientHeight;
        }
    };

    this.positionYAxisLabel = function(target, rotated, tdim, index) {

        var mx;
        var my ;
        if (rotated) {
            mx = _widget.ctx.axisLabelFontSize + 20;

            target.style.width = tdim.height + "px";
            my =  (_widget.ctx.height / 2) - (tdim.width / 2);
        } else {
            mx = target.clientWidth;
            my = ( _widget.ctx.height + _widget.ctx.axisLabelFontSize) / 2;
        }

        if (index === 1) {
            
        } else if (index === 2) {
            target.style.left =  (_widget.ctx.width - _widget.ctx.axisLabelFontSize) + "px";
        }

        target.style.top = my + "px";

        if (target.style.visibility == "hidden") {
            target.style.visibility = "visible";
        }
        if (index === 1 &&
            !_widget.ctx.leftMargin ||
            !_widget.ctx.leftMargin === 0 ) {

            _widget.model.options.leftMargin = mx;

        } else  if (index === 2 &&
                    !_widget.ctx.rightMargin ||
                    !_widget.ctx.rightMargin === 0 ) {
            _widget.model.options.rightMargin = mx;
        }
    };

    this.clearMarkers = function() {
        for (var i in _widget.model.markers) {
            if (_widget.model.markers[i].div && _widget.model.markers[i].div.parentNode) {
                _widget.model.markers[i].div.parentNode.removeChild(_widget.model.markers[i].div);
                _widget.model.markers[i].pointerDiv.parentNode.removeChild(_widget.model.markers[i].pointerDiv);
            } else {
                _widget.model.markers[i].div = null;
                _widget.model.markers[i].pointerDiv = null;
            }
        }
    };

    this.getMarker = function(markerId) {
        if (_widget.model.markers[markerId]) {
            return _widget.model.markers[markerId];
        } else {
            return null;
        }
    };

    this.zoomMarkers = function(ranges) {

        _widget.clearMarkers();
        for (var i in _widget.model.markers) {
            if (typeof _widget.model.markers[i] == 'object') {
                var marker = _widget.model.markers[i];
                // find out if this maker x value should be in this view 
                jmaki.log("checking for " + marker.point.x);
                if (marker.point.x >= ranges.xaxis.from &&
                        marker.point.x <= ranges.xaxis.to ) {
                    jmaki.log("adding " + jmaki.inspect(marker));
                    // TODO : get the markerPoints array for this point to preserve the ordering of the points
                    //var markerPoints = getMarkers(target.ds, target.point); 
                    addMarkerDiv(marker);
                }
            }
        }
    }; 

    this.refreshMarkers = function() {
        for (var i in markerPoints) {
            if (typeof markerPoints[i] == "number") {
                _widget.respositionMarkersAtIndex(i);
            }
        }
    }; 

    this.removeMarker = function(target) {
        if (!_widget.model.markers) jmaki.log("jMaki Charting::remove no markers");
        var marker;
        if (typeof target == "object") {
            marker = target;
        }  else {
            var markerId;
            if (target.message) target = message;
            if (target.targetId) markerId = target.targetId;
            else markerId = target;
            marker = _widget.model.markers[markerId];
        }
        if (marker) {

            var markerPoints = marker.markerPoints;

            // remove the divs
            marker.div.parentNode.removeChild(marker.div);
            marker.pointerDiv.parentNode.removeChild(marker.pointerDiv);

            // we need to reposition
            if (markerPoints.length > 1) {

                var yoffset = 0;

                // calculate the coordinates
                var mx;
                if (marker.ds.xaxis == 2) mx = _widget.plot.getPlotOffset().left + _widget.plot.getAxes().x2axis.p2c(marker.point.x);
                else mx = _widget.plot.getPlotOffset().left + _widget.plot.getAxes().xaxis.p2c(marker.point.x);
                var my;
                if (marker.ds.yaxis == 2) my = _widget.plot.getPlotOffset().top + _widget.plot.getAxes().y2axis.p2c(marker.point.y) - 2;
                else my = _widget.plot.getPlotOffset().top + _widget.plot.getAxes().yaxis.p2c(marker.point.y) - 2;

                if (markerPoints) {
                    markerCount = markerPoints.length;
                    var _index = -1;
                    for ( var i = 0; i < markerCount; i+=1) {
                        if (markerPoints[i].id != markerId) {

                            markerPoints[i].div.style.left = _widget.pos.x + mx + "px";
                            markerPoints[i].div.style.top = (_widget.pos.y - markerPoints[i].div.clientHeight -
                                    markerPoints[i].pointerDiv.clientHeight + my) - yoffset + "px";
                            markerPoints[i].pointerDiv.style.left = _widget.pos.x + mx + "px";
                            markerPoints[i].pointerDiv.style.top = _widget.pos.y -
                            markerPoints[i].pointerDiv.clientHeight +
                            my - yoffset + "px";
                            yoffset += markerPoints[i].div.clientHeight;
                            //yoffset += 5;
                        } else {
                            _index = i;
                        }
                    }
                    if (_index != -1) {
                        markerPoints.splice(_index,1);
                    }
                } else {
                    markerPoints = [];
                }
                delete _widget.model.markers[markerId];
            }
        }
    };

    function getWindowDimensions() {

        var _w = 0;
        var _h = 0;
        var _sx = 0;
        var _sy = 0;
        var _sh = 0;
        var _docHeight;

        var hscrollbars = false;
        var vscrollbars = false;

        if (document.body && document.body.clientHeight){
            _docHeight = document.body.clientHeight;
        }
        if (window.innerWidth) {
            _w = window.innerWidth;
            _h = window.innerHeight;
        } else if (document.documentElement &&
                document.documentElement.clientHeight) {
            _w = document.documentElement.clientWidth;
            _h = document.documentElement.clientHeight;
            _sh = document.documentElement.scrollHeight - _docHeight;
        } else if (document.body) {
            _w = document.body.clientWidth;
            _h = document.body.clientHeight;
        }

        if (window.pageYOffset) {
            _sx = window.pageXOffset;
            _sy = window.pageYOffset;
        } else if (document.documentElement &&
                document.documentElement.scrollTop) {
            _sx = document.documentElement.scrollLeft;
            _sy = document.documentElement.scrollTop;
        } else if (document.body) {
            _sx = document.body.scrollLeft;
            _sy = document.body.scrollTop;
        }
        if ((window.scrollMaxY && window.scrollMaxY >0) ||
                (document.body.scrollHeight > _h)
        ) {
            _sh = 38;
            vscrollbars = true;
        }
        if ((window.scrollMaxX && window.scrollMaxX >0) ||
                (document.body.scrollWidth > _w)
        ) {
            hscrollbars = true;
        }      

        return {w : _w, h: _h, docHeight :_docHeight,
            scrollX : _sx, scrollY : _sy, scrollbarsX : hscrollbars,  scrollbarsY : vscrollbars, scrollHeight : _sh };
    }   
};