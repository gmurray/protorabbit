if (typeof jmaki == "undefined") {
    var _globalScope = window;

    var jmaki = function() {

    /**
     * Map is a general map object for storing key value pairs
     *
     * @param mixin -
     *            default set of properties
     *
     */
    var Map = function(mixin) {

       var map = this;

       if (typeof mixin == 'undefined') {
           map = {};
       } else {
           map = mixin;
       }

       /**
        * Get a list of the keys to check
        */
       this.keys = function() {
           var _keys = [];

           for (var _i in map){
               // make sure we don't return prototype properties.
               if (map.hasOwnProperty(_i)) {
                   _keys.push(_i);
               }
           }
           return _keys;
       };
       /**
        * Put stores the value in the table
        * @param key the index in the table where the value will be stored
        * @param value the value to be stored
        */
       this.put = function(key,value) {
           map[key] = value;
       };

       /**
        * Return the value stored in the table
        * @param key the index of the value to retrieve
        */
       this.get = function(key) {
           return map[key];
       };

       /**
        * Remove the value from the table
        * @param key the index of the value to be removed
        */
       this.remove =  function(key) {
           delete map[key];
       };
       /**
        *  Clear the table
        */
       this.clear = function() {
           map = {};
       };
   };

    var ctx = {
            version :"1.9",
            debugGlue :false,
            verboseDebug :false,
            debug :false,
            loaded : false,
            initialized :false,
            webRoot :"",
            resourcesRoot :"resources",
            preextensions : [],
            inspectDepth :2,
            publishToParent :false,
            displayErrorsInline :true,
            MSIE : /MSIE/i.test(navigator.userAgent),
            defaultLocale :"jmaki-en-us",
            // default locale for messages
            locale :"jmaki-en-us",
            defaultMessages : {
                "request_timeout" :"Request for {0} timed out.",
                "invalid_json" :"Invalid JSON.",
                "publish_function_not_found" :"Publish Error : function {0} not found on object {1}",
                "publish_object_not_found" :"Publish Error :  Object not found: {0}",
                "publish_match" :"<span style='color:green'>Subscribe Match :</span> : Topic: {0} listener {1}",
                "publish" :"<span style='color:red'>Publish </span> : Topic: {0} message {1}",
                "subscribe_handler_required" :"Subscribe Error : Handler required for subscriber {0}.",
                "subscribe_topic_required" :"Subscribe Error : topic or topicRegExp required for {0}.",
                "ajax_url_required" :"doAjax error: url required.",
                "ajax_error" :"jMaki.doAjax Error: {0}",
                "ajax_request_open_error" :"doAjax error making request: {0}",
                "ajax_send_body_error" :"doAjax error sending body of request to {0}.",
                "ajax_server_error" :"doAjax error communicating with {0}. Server returned status code {1}.",
                "write_dynamic_script_error" :"Attempt to write a script that can not be dynamically load widget with  id {0}. Consider using the widget in an iframe.",
                "widget_constructor_not_found" :"Unable to find widget constructor for {0}.",
                "extension_constructor_not_found" :"Unable to find extension constructor for {0}.",
                "widget_instantiation_error" :"Unable to create an instance of {0}. Enable logging for more details.",
                "unknown" :"unknown",
                "widget_error" :"<span>Error loading {0} : id={1}<br>Script: {2} (line: {3}).<br>Message: {4}</span>",
                "jmaki_logger" :"jMaki Logger",
                "clear" :"Clear",
                "x_close" :"[X]",
                "clear_logger" :"Clear Logger",
                "hide_logger" :"Hide Logger",
                "more" :"[more]",
                "jmaki_version" :"jMaki Version : {0}",
                "unable_to_load_url" :"Unable to load URL {0}."
            },
            ajaxRequestQueue : [],
            timers : [],
            subs : [],
            widgets : [],
            counter : 0,
            dcontainers :  new Map(),
            extensions : new Map(),
            attributes : new Map()
        };

    var head;
    var body;

     var namespace = function(_path, target) {
         // get the top level object
         var paths = _path.split('.');
         var _obj = window[paths[0]];
         if (typeof _obj == "undefined") {
             window[paths[0]] = _obj = {};
         }
         for ( var ii = 1; ii < paths.length; ii++) {
             if (typeof _obj[paths[ii]] != "undefined") {
                 _obj = _obj[paths[ii]];
             } else {
                 _obj[paths[ii]] = {};
                 _obj = _obj[paths[ii]];
             }
         }
         // if object provided it becomes the last in the chain
         if (typeof target == 'object') {
             _obj = target;
         }
         return _obj;
     };


    var messageFormat = function(_message, _args) {
        if (typeof _message != "undefined" &&  typeof _args != "undefined") {
            for (var i=0; i < _args.length; i++) {
                var rex = new RegExp("\\{" + i + "\\}", "g");
                _message =  _message.replace(rex, _args[i]);
            }
        }
        return _message;
    };

    var getMessage =  function(id, args) {
        var message = null;
        var lmessages;
        if (ctx.messages.get(ctx.locale)) {
            lmessages = ctx.messages.get(ctx.locale);
            // fallback
        } else {
            lmessages = ctx.messages.get(ctx.defaultLocale);
        }
        if (lmessages) {
            message = lmessages.get(id);
        }
        if (typeof args != "undefined" && message) {
            return messageFormat(message, args);
        }
        return message;
    };

    /*
     * Combination function that will do mixing in and extending depending on if
     * _par is an object or a function (constructor)
     *
     * @param _src is the source object
     * @param _par is the class or object to extend
     *
     */
     var  mixin = function(_src, _target, _override) {
        var o = false;
        if (typeof _override != "undefined") {
            o = _override;
        }
        for (var i in _src) {
            if (!_target[i] || o) {
                _target[i] = _src[i];
            }
        }
    };

     var genId = function() {
         return "jmk" + ctx.counter++;
     };

    var createElement = function(type) {
        return document.createElement(type);
    };

     function appendChild(_parent, _child) {
        if (_parent){
            _parent.appendChild(_child);
        }
    }

     var getElement = function(id) {
         return document.getElementById(id);
     };

     var log = function(text, level) {
        // cached messages until after the page has been created
        if (!ctx.initialized) {
            if (!ctx._messages) {ctx._messages = [];}
            ctx._messages.push({ text : text, level : level});
        }
        if (!ctx.debug ) { return;}
        var ld = getElement("jmakiLogger");
        var b = getElement("jmakiLoggerContent");
        if (!ld){
            ld = document.createElement("div");

            ld.id = 'jmakiLogger';
            var lds = ld.style;
            lds.border = "1px solid #000000";
            lds.fontSize = "12px";
            lds.position  = "absolute";
            lds.zIndex  = "999";
            lds.bottom = "0px";
            lds.background = "#65B2DB";
            lds.right ="0px";
            lds.width = "600px";
            lds.height = "300px";

            var tb = "<div  style='height: 14px; background : #000; color : white; font-size : 10px'>" +
                     "<div style='float:left;width:545px;text-align:center'>" +
                     getMessage("jmaki_logger") +
                     "</div><div style='right:0px,text-align:left'><a href='javascript:jmaki.clearLogger()' title='" +
                     getMessage("clear_logger") +
                     "' style='color:white;text-decoration:none'>[" +
                     getMessage("clear") +
                     "]</a> <a href='javascript:jmaki.hideLogger()' title='" +
                     getMessage("hide_logger") +
                      "' style='color:white;text-decoration:none'>" +
                      getMessage("x_close") +
                      "</a></div></div>";

            var tbE = createElement("div");
            tbE.innerHTML = tb;
            ld.appendChild(tbE);
            b = document.createElement("div");
            b.id ='jmakiLoggerContent';
            b.style.height = "286px";
            b.style.overflowY = "auto";
            b.style.textAlign = "left";
            ld.appendChild(b);

            if (body) {
              appendChild(body, ld);
            }
        }
        if (ctx.loaded && ld !== null) {ld.style.visibility = 'visible';}
        var lm = createElement("div");
        lm.style.clear = "both";
        if (text && text.length > 125 &&
            ctx.verboseDebug === false) {
            var lid = genId();
            var tn = createElement("div");
            appendChild(lm, tn);
            tn.innerHTML = "<div style='float:left;width:535px;height:12px;overflow:hidden'>" +  text.substring(0,135) + "</div><div style='float:left'>...&nbsp;</div><a id='" +  lid + "_href' href=\"javascript:jmaki.showLogMessage(\'" +
                           lid +  "\')\" style='text-decoration: none'><span id='" +
                           lid + "_link'>" + getMessage("more") + "</span></a>";
            var mn = createElement("div");
            mn.id = lid;
            mn.innerHTML = text;
            mn.style.margin = "5px";
            mn.style.background = "#FF9900";
            mn.style.display = "none";

            appendChild(lm, mn);
        } else {
            lm.innerHTML =  text;
        }
        if (b) {
            b.appendChild(lm);
        }
    };

    function addMessages(locale, _messages) {
        ctx.messages.put(locale, new Map(_messages));
    }

    var isDefined = function(_target) {
        return (typeof _target != "undefined");
    };

    var matchWildcard = function(pattern,topic) {

        var patpos = 0;
        var patlen = pattern.length;
        var strpos = 0;
        var strlen = topic.length;

        var i=0;
        var star = false;

        while (strpos+i<strlen) {
            if (patpos+i<patlen) {
                switch (pattern.charAt(patpos + i)) {
                case "?":
                    i++;
                    continue;
                case '*':
                    star = true;
                    strpos += i;
                    patpos += i;
                    do {
                        ++patpos;
                        if (patpos == patlen) {return true;}
                    } while (pattern.charAt(patpos) == '*');
                    i=0;
                    continue;
                }
                if (topic.charAt(strpos + i) != pattern.charAt(patpos + i)) {
                    if (!star) {return false;}
                    strpos++;
                    i=0;
                    continue;
                }
                i++;
            } else {
                if (!star) {return false;}
                strpos++;
                i=0;
            }
        }
        do {
            if (patpos + i == patlen) {return true;}
        } while(pattern.charAt(patpos + i++)=='*');
        return false;
    };

    /**
     *  Unsubscribe a listener
     *  @param _lis
     */
    function unsubscribe(_lis) {
        for (var _l=0; _l < ctx.subs.length;_l++ ) {
            if (ctx.subs[_l].id  == _lis.id) {
                ctx.subs.splice(_l,1);
                break;
            }
        }
    }

    /**
     * A function to cloning an object or array so that different references do not
     * end up with shared references.
     *
     * @param - t A single object or array
     *
     */
     var clone = function(t) {
        var _obj;
        if (t instanceof Array) {
            _obj = [];
            for (var _j=0;_j< t.length;_j++) {
                if (typeof t[_j] != "function") {
                    _obj.push(clone(t[_j]));
                }
            }
        } else if (t instanceof Object) {
            _obj = {};
            for (var _jj in t) {
                if (typeof t[_jj] != "function") {
                 _obj[_jj] = clone(t[_jj]);
                }
            }
        } else {
            _obj = t;
        }
        return _obj;
     };

    var inspect = function(_o, _inspectDepth, _currentDepth) {
        var _ind = ctx.inspectDepth;
        var _cd = 0;

        if (typeof _inspectDepth == "number"){
            _ind =_inspectDepth;
        }
        if (typeof _currentDepth != "undefined"){
            _cd = _currentDepth;
        }
        if (_cd >= _ind && _ind != -1) {
           if (typeof _o == "string") {
               return "'" + _o + "'";
           } else {
               return _o;
           }
        } else  {
            _cd++;
        }

        var _rs = [];
        if (typeof _o == "undefined") {
            return 'undefined';
        }
        if (_o instanceof Array) {
            for (var i=0; i < _o.length; i++) {
                _rs.push(inspect(_o[i],_ind,_cd));
            }
            return "[" +  _rs.join(" , ") + "]";
        } else if (typeof _o == "string") {
           return "'" + _o + "'";
        } else if (typeof _o == "number" ||  typeof _o == "boolean") {
           return _o;
        } else if (typeof _o == "object") {
            for (var _oi in _o) {
                    if (typeof _o[_oi] != "function") {
                        _rs.push(_oi  + " : " + inspect(_o[_oi],_ind,_cd));
                    }
            }
            if (_rs.length > 0) {
                 return "{" + _rs.join(" , ") + "}";
            }
            else {
                return "{}";
            }
        } else {
            return _o;
        }
    };

    var findObject = function(_path) {
        var paths = _path.split('.');
        var found = false;
        var _obj = window[paths[0]];
        if (_obj && paths.length == 1) {
            found = true;
        }
        if (typeof _obj != "undefined"){
            for (var ii =1; ii < paths.length; ii++) {
                var _lp = paths[ii];
                if (_lp.indexOf('()') != -1){
                  var _ns = _lp.split('()');
                  if (typeof _obj[_ns[0]] == 'function'){
                      var _fn = _obj[_ns[0]];
                      return _fn.call(window);
                  }
                }
                if (typeof _obj[_lp] != "undefined") {
                    _obj = _obj[_lp];
                    found = true;
                } else {
                    found = false;
                    break;
                }
            }
            if (found) {
                return _obj;
            }
        }
        return null;
    };

    /**
     *  All for a filter to be applied to a dataset
     *  @param input - An object you wish to filter
     *  @param filter a string representing the path to the object or
     *    a function reference to procress the input
     */
    var filter = function(input, filter){
        if (typeof filter == 'string') {
            var h = findObject(filter);
            return h.call(window,input);
        } else if (typeof filter == 'function'){
            return filter.call(window, input);
        }
        return null;
    };

    var trim = function(t) {
        return  t.replace(/^\s+|\s+$/g, "");
    };

    /**
     *  Publish an event to a topic
     *  @param name Name of the topic to be published to
     *  @param args Palyoad of the publish
     *  @param bubbleDown Sends events down to children if set
     *  @param bubbleUp Sends event to parent contexts if they contain jmaki object and current context created by jmaki
     */
    var publish = function(name, args, bubbleDown, bubbleUp) {
        if (typeof name == "undefined" || typeof args == "undefined"){
            return false;
        }
        if (ctx.debugGlue) {
            log(getMessage("publish",  [name , inspect(args)]));
        }

        // check the glue for listeners
        if (ctx.subs){
            for (var _l=0; _l < ctx.subs.length;_l++ ) {
                var _listener = ctx.subs[_l];
                     if ((_listener.topic instanceof RegExp &&
                          _listener.topic.test(name))  ||
                           _listener.topic == name   ||
                          (typeof _listener.topic.charAt == 'function' &&
                       matchWildcard(_listener.topic,name))
                   ) {

                    // set the topic on payload
                    args.topic = name;
                    if (ctx.debugGlue) {
                        var _vname = name;
                        if (_listener.topicString) {
                            _vname = _listener.topicString;
                        }
                        log(getMessage("publish_match", [_vname, _listener]));
                    }
                    if (_listener.action == 'call' && _listener.target) {
                        // get the top level object
                        var Obj;
                        var myo = 'undefined';
                        if (_listener.target.functionName) {
                            Obj = findObject(_listener.target.object);
                            // create an instance of the object if needed.
                            if (typeof _obj == 'function') {
                                myo = new Obj();
                            } else if (Obj) {
                                myo = Obj;
                            } else {
                              log(getMessage("publish_function_not_found",
                                   [_listener.target.functionName,_listener.target.object]));
                            }
                            if (typeof myo != "undefined" &&
                                typeof myo[_listener.target.functionName] == 'function'){
                                myo[_listener.target.functionName].call(window,args);
                            } else {
                                   log(getMessage("publish_object_not_found",
                                        [_listener.target.functionName,_listener.target.object]));
                            }
                        } else if (_listener.target.functionHandler) {
                            _listener.target.functionHandler.call(window,args);
                        }
                    }
                } else if (ctx.subs[_l].action == 'forward') {
                    var _topics = ctx.subs[_l].topics;
                    // now multiplex the event
                    for (var ti = 0; ti < _topics.length; ti++){
                        // don't cause a recursive loop if the topic is this one
                        if (_topics[ti] != name) {
                            publish(_topics[ti], args);
                        }
                    }
                }
            }
        }
        // publish to subframes with a global context appended
        var bd = true;
        if (typeof bubbleDown != "undefined"){
            bd = bubbleDown;
        }
        if (bd === true &&
            window.frames !== null &&
            window.frames.length > 0) {
            var _frames = ctx.dcontainers.keys();
            for (var i=0; i < _frames.length; i++){
              var _dc = ctx.dcontainers.get(_frames[i]);
              if (_dc.iframe && !_dc.externalDomain && window.frames[_dc.uuid + "_iframe"] && window.frames[_dc.uuid + "_iframe"].jmaki){
                  window.frames[_dc.uuid + "_iframe"].jmaki.publish("/global" + name, args, true, false);
              }
            }
        }
        //  publish to parent frame if we are a sub-frame. This will prevent duplicate events
        if (ctx.publishToParent){
            var bu = true;
            if (typeof bubbleUp != "undefined") {
                bu = bubbleUp;
            }
            if (bu && window.parent.jmaki){
                  window.parent.jmaki.publish("/global" + name, args, false, true);
            }
        }
        return true;
    };

    /*
     * Add a glue listener programatcially. following is an example.
     *
     *{topic : "/dojo/fisheye",action: "call", target: { object: "jmaki.dynamicfaces",functionName: "fishEyeValueUpdate"}}
     *   or
     * @param l as topic and
     * @param t as the target object path ending with a function
     */
    var subscribe = function(l, t) {
        if (!isDefined(l)) {
            return null;
        }
        // handle key word arguments
        var lis;
        if (typeof l == 'object' && !(l instanceof RegExp)) {
            if (l.topic){
                l.topic = trim(l.topic);
            }
            if (l.topicRegExp){
                l.topic = new RegExp(l.topicRegExp);
            }
            lis = l;
        // function binding
        } else if (typeof t == 'string'){
          lis = {};
          if (l.topicRegExp) {
              lis.topic = new RegExp(l.topicRegExp);
          } else {
              lis.topic = l;
          }
          lis.target = {};
          var _is = t.split('.');
          lis.action = "call";
          lis.target.functionName = _is.pop();
          lis.target.object = _is.join('.');
        // inline function
        } else if (typeof t == 'function') {
          lis = {};
          if (l.topicRegExp) {
              lis.topic =  new RegExp(l.topicRegExp);
          } else {
              lis.topic = l;
          }
          lis.target = {};
          lis.action = "call";
          lis.target.functionHandler = t;
        } else {
          log(getMessage("subscribe_handler_required", [l]));
        }
        if (isDefined(lis)){
            if (!isDefined(ctx.subs)) {
                ctx.subs = [];
            }
            if (!lis.id) {
                lis.id = genId();
            }
            if (lis.topic){
                lis.prototype = {};
                lis.prototype.toString = function() {
                    return inspect(this);
                };
                ctx.subs.push(lis);
            } else {
                log(getMessage("subscribe_topic_required", [l]));
                return null;
            }
            return lis;
        }
        return null;
    };

    var Timer = function(args, isCall) {
        var _src = this;
        this.args = args;
        var _target;

        this.processTopic = function() {
            for (var ti = 0; ti < args.topics.length; ti++){
                publish(args.topics[ti], {topic: args.topics[ti],
                type:'timer',
                src:_src,
                timeout: args.to});
            }
        };

        this.processCall = function() {
            if (!_target) {
             var  Obj = findObject(args.on);
                if (typeof Obj == 'function'){
                    _target = new Obj();
                } else if (typeof _obj == 'object'){
                    _target = Obj;
                }
            }
            if ((_target && typeof _target == 'object')) {
              if(typeof _target[args.fn] == 'function') {
                _target[args.fn]({type:'timer', src:_src, timeout: args.to});
              }
            }
        };

        this.run = function() {
            if (isCall) {
                _src.processCall();
            } else {
                _src.processTopic();
            }
            window.setTimeout(_src.run,args.to);
        };
    };


    /**
     *  Get the XMLHttpRequest object
     *
     *  Allow for config override to allow for older ActiveX XHR for local file
     *  System with IE7
     *
     */
    var getXHR = function () {
        if (window.XMLHttpRequest &&
             !( ctx.MSIE &&
                isDefined(ctx.config) &&
                typeof(ctx.config.forceActiveXXHR) == "boolean" &&
                ctx.config.forceActiveXXHR === true)) {
            return new window.XMLHttpRequest();
        } else if (window.ActiveXObject) {
            return new window.ActiveXObject("Microsoft.XMLHTTP");
        } else {
            return null;
        }
    };

    function handleAjaxError(_m, _r, args){
       if (args.onerror) {
             args.onerror(_m,_r);
           } else {
         log(getMessage("ajax_error", [_m]));
       }
    }

    var doAjax;
    var addLibraries;

    function updateAjaxQueue() {
        if (ctx.ajaxRequestQueue.length > 0) {
            doAjax(ctx.ajaxRequestQueue.pop());
        } else {
            ctx.processingAjax = false;
            if (isDefined(ctx._scriptQueue) &&
                ctx._scriptQueue.length > 0) {
                var _n = ctx._scriptQueue[0];
                ctx._scriptQueue.shift();
                addLibraries(_n);
            }
        }
    }

    /**
     * Load a set of libraries in order and call the callback function
     */
    addLibraries = function(_o, _cb, _inp, _cu) {

        var _libs;
        var _inprocess;
        var _cleanup = true;

        // check to see if anything is still processing and if not
        // call the callback
        var checkQueue = function() {
            if (_inprocess.keys().length === 0) {
                if (isDefined(_cb)){
                    setTimeout(function(){_cb();}, 0);
                }
                 _inprocess.clear();
                ctx.processingScripts = false;
                updateAjaxQueue();
            }
           return;
       };

        // overload the function to allow for object literals
        if (_o instanceof Array) {
             _libs = _o;
             _inprocess = _inp;
             _cleanup = _cu;
        } else {
            _libs = _o.libs;
            _cb = _o.callback;
            _inprocess = _o.inprocess;
            _cleanup = _o.cleanup;
        }
        // queue the request if there are scripts being loaded.
        // this prevents the 2 connections from being sucked up.
        if (!isDefined(_inprocess) && (ctx.processingScripts ||
            ctx.processingAjax)) {
            _inprocess = new Map();
            if (!isDefined(ctx._scriptQueue)) {
                ctx._scriptQueue =[];
            }
            ctx._scriptQueue.push({libs : _libs, callback : _cb, cleanup : _cleanup});
            return;
        } else if (!isDefined(_inprocess)) {
            ctx.processingScripts = true;
            _inprocess = new Map();
        }

        if (_libs.length <= 0) {
            checkQueue();
        }
        var _uuid = new Date().getMilliseconds();
        var _lib = _libs[_libs.length-1];
        var _s_uuid = "c_script_" + genId();

        var e = createElement("script");
        e.start = _uuid;
        e.id =  _s_uuid;
        head.appendChild(e);

        var se = getElement(_s_uuid);
        _inprocess.put(_s_uuid,_lib);
        var loadHandler = function (_id) {
            var _s = getElement(_id);
            // remove the script node
            if (_s  && !(isDefined(_cleanup) && _cleanup === false)){
                _s.parentNode.removeChild(_s);
            }
            _inprocess.remove(_id);
            if (_libs.length-1 > 0) {
                _libs.pop();
                addLibraries({ libs : _libs, callback : _cb, inprocess : _inprocess, cleanup : _cleanup});
            }
            checkQueue();
        };

        // wait for the script to be loaded
        if (ctx.MSIE) {
            se.onreadystatechange = function () {
                if (this.readyState == "loaded") {
                    var _id = _s_uuid;
                    loadHandler(_id);
                }
            };
            getElement(_s_uuid).src = _lib;
        } else {
            // the onload handler works on opera, ff, safari
            // and the addEventListener will not work on opera
            se.onload = function(){
                    var _id = _s_uuid;
                  loadHandler(_id);
            };
            setTimeout(function(){
                getElement(_s_uuid).src = _lib;
                }, 0);
        }
    };


    /**
     * Generalized XMLHttpRequest which can be used from evaluated code. Evaluated code is not allowed to make calls.
     * @param args is an object literal containing configuration parameters including method[get| post, get is default], body[bodycontent for a post], asynchronous[true is default]
     */
    doAjax= function(args) {
        if (typeof args == 'undefined' || !args.url) {
            log(getMessage("ajax_url_required"));
            return;
        }
        // sync up the processing queues for script and ajax loading
        // synchronous requests should not be stopped
        if ((ctx.processingScripts && args.asynchronous) ||
            (ctx.processingAjax &&
            args.asynchronous) ) {
                ctx.ajaxRequestQueue.push(args);
                return;
        }
        ctx.processingAjax = true;
        var _req =  getXHR();

        var method = "GET";
        var async = true;
        var callback;
        var _c = false;
        if (args.timeout) {
            setTimeout(function(){
              if (_c === false) {
                _c = true;
                if (_req.abort) {
                    _req.abort();
                }
                handleAjaxError(getMessage("request_timeout", [args.url]), _req, args);
                updateAjaxQueue();
                return;
               }
            }, args.timeout);
        }

        if  (typeof args.asynchronous != "undefined") {
             async=args.asynchronous;
        }
        if (args.method) {
             method=args.method;
        }
        if (typeof args.callback == 'function') {
            callback = args.callback;
        }
        var _body = null;
        if (args.body) {
            _body = args.body;
        } else if (args.content) {
            _body = "";
            for (var l in args.content) {
                if (typeof args.content[l] != "function") {
                    _body = _body +  l + "=" + encodeURIComponent(args.content[l]) + "&";
                }
            }
        }
        if (async === true && _c === false) {
            _req.onreadystatechange = function() {
                if (_req.readyState == 4 && _c === false) {
                    _c = true;
                    if ((_req.status == 200 || _req.status === 0) &&
                            callback) {
                        callback(_req);
                    } else if (_req.status != 200) {
                        _c = true;
                        handleAjaxError(getMessage("ajax_server_error",
                                        [args.url, _req.status ]), _req, args);
                    }
                    updateAjaxQueue();
                    return;
                }
            };
        }
        try {
           if (!_c) {
               _req.open(method, args.url, async);
           }
        } catch(e) {
          _c = true;
          handleAjaxError(getMessage("ajax_request_open_error", [args.url]),_req, args);
          updateAjaxQueue();
          return;
        }
        // add headers
        if (args.headers && args.headers.length > 0) {
            for (var _h=0;_h < args.headers.length; _h++) {
                _req.setRequestHeader(args.headers[_h].name, args.headers[_h].value);
            }
        }
        // customize the method
        if (args.method) {
             method=args.method;
             if (method.toLowerCase() == 'post') {
                if (!args.contentType) {
                    _req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
                }
             }
        }
        if (args.contentType) {
            _req.setRequestHeader("Content-Type", args.contentType);
        }
        try {
          if (_c === false) {
              _req.send(_body);
          }
        } catch(er) {
          _c = true;
          handleAjaxError(getMessage("ajax_send_body_error", [args.url]), _req, args);
          updateAjaxQueue();
          return;
        }
        if (_c === false && async === false) {
            _c = true;
            if (_req.status === 200 || _req.status === 0) {
                 if (callback) {
                     callback(_req);
                 }
            } else {
                _c = true;
                handleAjaxError(getMessage("ajax_server_error", [args.url,_req.status]), _req, args);
            }
            updateAjaxQueue();
        }
     };

     /**
      * Loads the style sheet by adding a link element to the DOM
      * @param target name of style sheet to load
      */
     var loadStyle = function(target) {
         var styleElement = createElement("link");
         styleElement.type = "text/css";
         styleElement.rel="stylesheet";
         if (target[0] == '/' && ctx.webRoot != '/')  {
             target = ctx.webRoot + target;
         }
         styleElement.href = target;
         if (document.getElementsByTagName('head').length === 0) {
             var headN = document.createElement("head");
             document.documentElement.insertBefore(headN, document.documentElement.firstChild);
         }
         head.appendChild(styleElement);
     };

     /**
      * Utility Function to get children
      * @param target Element for which to get the children. All document children are loaded if not specified
      * @param children An array used internally to build up a list of children found
      */
     var getAllChildren = function(target, children) {
         var _nc = target.childNodes;
         for (var l=0; _nc && l <  _nc.length; l++) {
             if (_nc[l].nodeType == 1) {
                 children.push(_nc[l]);
                 if (_nc[l].childNodes.length > 0) {
                     getAllChildren(_nc[l], children);
                 }
             }
         }
         return children;
     };


     /**
      * Find a set of child nodes that contain the className specified
      * @param className is the targetClassName you are looking for
      * @param root  An optional root node to start searching from. The entire document will be searched if not specfied.
      *
      */
      var getElementsByStyle = function(className, root){
          var elements = [];
          if (isDefined(root)) {
              var rootNode = root;
              if (typeof root == "string") {
                  rootNode = getElement(root);
              }
              elements = getAllChildren(rootNode, []);
          } else {
              elements = (document.all) ? document.all : document.getElementsByTagName("*");
          }
      var found = [];
      for (var i=0; i < elements.length; i++) {
      // Handle cases where there are multiple class names
              if (elements[i].className.indexOf(' ') != -1) {
                  var cn = elements[i].className.split(' ');
                  for (var ci =0; ci < cn.length; ci++) {
                      if (cn[ci] == className) {
                          found.push(elements[i]);
                      }
                  }
              } else  if (elements[i].className == className) {
                  found.push(elements[i]);
              }
          }
          return found;
      };

     /**
      * Replace style class
      * @param root root of the oldStyle classes
      * @param oldStyle name of class or classes to replace
      * @param targetStyle name of new class or classes to use
      */
     var replaceStyleClass = function (root, oldStyle, targetStyle) {
         var elements = getElementsByStyle(oldStyle,root);
         for (var i=0; i < elements.length; i++) {
             // Handle cases where there are multiple classnames
             if (elements[i].className.indexOf(' ') != -1) {
                 var classNames = elements[i].className.split(' ');
                 for (var ci in classNames) {
                     if (classNames[ci] == oldStyle) {
                         classNames[ci] = targetStyle;
                     }
                 }
                 // now reset the styles with the replaced values
                 elements[i].className = classNames.join(' ');
             } else  if (elements[i].className == oldStyle) {
                 elements[i].className = targetStyle;
             }
         }
     };

     /**
      * Load extension
      * @param _ext Object representing widget to load
      */
     this.loadExtension = function(_ext) {
	 
         if (ctx.extensions.get(_ext)){
             return;
         }
         var targetName ="jmaki.extensions." + _ext.name + ".Extension";
         var Con = findObject(targetName);       
         if (typeof Con != "function") {
             log(getMessage("extension_constructor_not_found", [targetName]));
         } else {
           var ex = new Con(_ext);
           if (ex.postLoad) {
               ex.postLoad.call(window);
           }
           ctx.extensions.put(_ext.name, ex);
         }

     };

     function logError(message, div) {
         if (ctx.displayErrorsInline) {
             if (!isDefined(div) || !div) {
                 div = createElement("div");
             }
             div.className = "";
             div.style.color = "red";
             appendChild(body, div);
            div.innerHTML = message;
         } else {
             log(message);
         }
     }

     function logWidgetError(name,uuid, url, line, _m, div) {
         var message= getMessage("widget_error", [name, uuid, url, line, _m]);
         logError(message, div);
     }


     /**
      * Load a widget
      * @param _jmw Object representing widget to load
      */
     var loadWidget = function(_jmw) {

         // see if the widget has been defined.
         if (ctx.attributes.get(_jmw.uuid) != null) {
             return null;
         }
         var targetName ="jmaki.widgets." + _jmw.name + ".Widget";
         var Con = findObject(targetName);
         if (typeof Con != "function") {
             logError(getMessage("widget_constructor_not_found", [targetName]), getElement(_jmw.uuid));
             return null;
         }
         var wimpl;
         // bind the value using a @{foo.obj} notation
         if ((typeof _jmw.value == 'string') && _jmw.value.indexOf("@{") === 0) {
             var _vw = /[^@{].*[^}]/.exec(_jmw.value);
             _jmw.value = findObject(_vw + "");
         }
         // do not wrap IE with exception handler
         // because we can't get the right line number
         var _uuid = _jmw.uuid;
         if (ctx.MSIE) {
             var oldError = null;
             if (window.onerror) {
                 oldError = window.onerror;
             }
             var eh = function(message, url, line) {
                 var _puuid = _uuid;
                 logWidgetError(targetName, _puuid,url, line, message, getElement(_puuid));
             };
             window.onerror = eh;
             wimpl = new Con(_jmw);

             window.onerror = null;
             if (oldError) {
                 window.onerror = oldError;
             }
         } else if (typeof Con == 'function'){
           try {
                 wimpl = new Con(_jmw);
            } catch (e){
                 var line = getMessage("unknown");
                 var description = null;
                 if (e.lineNumber) {
                     line = e.lineNumber;
                 }
                 if (e.message){
                     description = e.message;
                 }
                 if (ctx.debug) {
                     logWidgetError(targetName, _jmw.uuid,_jmw.script, line, description , getElement(_jmw.uuid));
                     return null;
                 }
             }
         }
         if (typeof wimpl == 'object') {
             ctx.attributes.put(_jmw.uuid, wimpl);
             if (wimpl.postLoad) {
                 wimpl.postLoad.call(window);
             }
             // map in any subscribe handlers.
             if (_jmw.subscribe && _jmw.subscribe.push) { //string also have length property
                 for (var _wi = 0; _wi < _jmw.subscribe.length; _wi++) {
                     var _t = _jmw.subscribe[_wi].topic;
                     var _m = _jmw.subscribe[_wi].handler;
                     var _h = null;
                     if (typeof _m == 'string' && _m.indexOf("@{") === 0) {
                          var _hw = /[^@{].*[^}]/.exec(_m);
                         _h = findObject(_hw + "");
                     } else if (wimpl[_m]) {
                         _h = wimpl[_m];
                     }
                     if (_h !== null) {
                         subscribe(_jmw.subscribe[_wi].topic,_h);
                     }
                 }
             }
             publish("/jmaki/runtime/widget/loaded", { id : _jmw.uuid});
             return wimpl;
         } else {
             logError(getMessage("widget_instantiation_error",[targetName]), getElement(_jmw.uuid ));
         }
         return null;
     };





     /**
      * An easy way to get a instance of a widget.
      * returns null if their is not a widget with the id.
      */
     var getWidget = function(id) {
         return ctx.attributes.get(id);
     };

     var removeWidget = function(_wid) {
         var _w = getWidget(_wid);
         if (_w) {
             if ( typeof _w.destroy == 'function') {
                 _w.destroy();
             }
             var _p = getElement(_wid);
             if( null !== _p) {
                 _p.parentNode.removeChild(_p);
             }
         }
         ctx.attributes.remove(_wid);
     };

     /**
      * destroy all registered widgets under the target node
      * @param _root - The _root to start at. All widgets will be removed if not specified.
      */
     this.clearWidgets = function(_root) {

         if (!isDefined(_root)) {
             var _k = ctx.attributes.keys();
             for (var l=0; l < _k.length; l++) {
                 removeWidget(_k[l]);
             }
             ctx.loaded = false;
             ctx.widgets = [];
         } else {
            var _ws = getAllChildren(_root,[]);
            for (var ll=0; ll < _ws.length; ll++) {
                 if (_ws[ll].id) {
                     removeWidget(_ws[ll].id);
                 }
             }
         }
     };

     /**
      *  Library name is added as a script element which will be loaded when the page is rendered
      *  @param lib library to add
      *  @param cb Callback handler
      */
     var addLibrary = function(lib, cb) {
       var libs = [];
       libs.push(lib);
       addLibraries({libs : libs, callback : cb});
     };

     /**
      * Register widget with jMaki
      * @param widget Object representing the widget
      */
     var addWidget = function(widget) {
         ctx.widgets.push(widget);
         if (ctx.loaded){
             loadWidget(widget);
         }
     };

      /**
      * Register widget with jMaki
      * @param ext Object representing the extension params
      */
     var addExtension = function(ext) {
         ctx.preextensions.push(ext);
     };

     /**
      * Register widget with jMaki
      * @param id The id of the extension
      */
     var getExtension = function(id) {
         return ctx.extensions.get(id);
     };

     /**
      * Bootstrap or load all registered widgets
      */
      function bootstrapWidgets() {
         ctx.loaded = true;
         for (var l=0; l < ctx.widgets.length; l++) {
             loadWidget(ctx.widgets[l]);
         }
     }

     /**
      * Bootstrap or load all registered extensions
      */
     function loadExtensions() {
         for (var l=0; l < ctx.preextensions.length; l++) {
             loadExtension(ctx.preextensions[l]);
         }
     }

    /**
      * Checks whether a script has been loaded yet
      */
     var writeScript = function(_s, _id) {
         if (ctx.loaded === true) {
             if (getElement(_id)) {
                 getElement(_id).innerHTML = getMessage("write_dynamic_script_error", [_id]);
             }
         } else {
             document.write("<script src='" + _s + "'></script>");
         }
     };
     /*
      * @param _src is the source object
      * @param _par is the class to extend
      */
     var extend = function(_src, Par) {
         _src.prototype = new Par();
         _src.prototype.constructor = _src;
         _src.superclass = Par.prototype;
         for (var i in _src.prototype) {
             if (typeof _src.prototype[i] != "undefined") {
                 _src[i] = _src.prototype[i];
             }
         }
     };

     var hideLogger = function() {
       var ld = getElement("jmakiLogger");
       if (ld){
           ld.style.visibility = 'hidden';
       }
     };

     var clearLogger = function() {
       var b = getElement("jmakiLoggerContent");
       if (b) {
           b.innerHTML = "";
       }
     };


     var showLogMessage = function(id) {
         var n = getElement(id);
         if (n && n.style){
             n.style.display = "block";
             var h = getElement(id + "_href");
             h.href = "javascript:jmaki.hideLogMessage('" + id + "')";
             var l = getElement(id + "_link");
             l.innerHTML = "&nbsp;" + getMessage("x_close");
         }
     };

     var hideLogMessage = function(id) {
         var n = getElement(id);
         if (n && n.style){
             n.style.display = "none";
             var h = getElement(id + "_href");
             h.href = "javascript:jmaki.showLogMessage('" + id + "')";
             var l = getElement(id + "_link");
             l.innerHTML = getMessage("more");
         }
     };

     /*  This function takes an object literal and performs actions if present
      *  or it publishes a message to the provided topic.
      *
      *  _t = object literal { topic : 'topic to publish to',
      *                        widgetId : 'source widget id',
      *                        targetId : 'foo',
      *                        action : [
      *                            { topic : '/some topic', message : { payload}}
      *                        ],
      *                        value : 'somevalue'
      *                      }
      * The action, targetId, and value properties are optional.
      * The topic and widgetId are required
      *
      */
     var processActions = function(_t) {

         if (_t) {
             var _topic = _t.topic;
             var _m = {widgetId : _t.widgetId, type : _t.type, targetId : _t.targetId};
             if (isDefined(_t.value)) {
                 _m.value = _t.value;
             }
             var action = _t.action;
             if (!action) {
                 _topic = _topic + "/" + _t.type;
             }
             if (isDefined(action) &&
                 action instanceof Array) {
               for (var _a=0; _a < action.length; _a++) {
                   var payload = clone(_m);
                   if (action[_a].topic) {
                       payload.topic = action[_a].topic;
                   } else {
                       payload.topic = _t.topic;
                   }
                   if (action[_a].message) {
                       payload.message = action[_a].message;
                   }
                   publish(payload.topic,payload);
               }
             } else {
                 if (action) {
                   if (action.topic) {
                            _topic = _m.topic = action.topic;
                        }
                        if (action.message) {
                            _m.message = action.message;
                        }
                }
                publish(_topic, _m);
            }
         }
     };

     /**
         * Find the position of an Element
         *
         */
      var getPosition = function(_e){
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
     };

     var getDimensions = function(n, min) {
         if (typeof n == 'undefined' ||
             n === null) {
             return null;
         }
         var _min = 0;
         if (typeof min != "undefined") {
             _min = min;
         }
         var rn = n.parentNode;
         while(rn && true) {
             if (rn.clientHeight > _min) {
                 break;
             }
             if (rn.parentNode && rn.parentNode.clientHeight) {
                 rn = rn.parentNode;
             } else {
                break;
             }
         }
         if (!rn) {
             return null;
         }
         return {h : rn.clientHeight, w : rn.clientWidth};
     };



     var addTimers = function(_timers){
         if (isDefined(_timers)){
             for (var _l=0; _l <_timers.length;_l++ ) {
                 // create a wrapper and add the timer
                 var _timer = _timers[_l];
                 if (_timer.action == 'call' &&
                 isDefined(_timer.target) &&
                 isDefined(_timer.target.object) &&
                 isDefined(_timer.target.functionName) &&
                 isDefined(_timer.timeout)) {
                     // create the timer
                     var t1 = new Timer({on: _timer.target.object,
                         fn: _timer.target.functionName,
                         to: _timer.timeout
                         },true);
                     ctx.timers.push(t1);
                     t1.run();
                 } else if (_timers[_l].action == 'publish') {
                     var t2 = new Timer( {topics: _timers[_l].topics,
                     to: _timer.timeout
                     },false);
                     ctx.timers.push(t2);
                     t2.run();
                 }
             }
         }
     };

     var addTimer = function(_timer){
         addTimers([_timer]);
     };

    var postInitialize = function() {

         if (ctx.initialized) {
             return;
         } else {
             ctx.initialized = true;
         }
         if (ctx.config.logLevel) {
             switch (ctx.config.logLevel) {
                 case 'debug' :
                         ctx.debug = true;
                         log(getMessage("jmaki_version",[ctx.version]));
                         break;

                 case 'all' :
                     ctx.debug = true;
                     ctx.debugGlue = true;
                         break;
                 case  'off' :
                     ctx.debug = false;
                         break;
             }
         }
         if (ctx.config.webRoot) {
             ctx.webRoot = ctx.config.webRoot;
         }
         // write out the dependent libraries so we have access
         if (ctx.config.glue) {
             if (ctx.config.glue.timers) {
                 addTimers(ctx.config.glue.timers);
             }
             if (ctx.config.gluelisteners){
                 for (var gl=0; gl < ctx.config.glue.listeners.length;gl++) {
                     subscribe (ctx.config.glue.listeners[gl]);
                 }
             }
         }

         // log any messages that might be queued up during pre-init
         if (ctx._messages) {
             for (var i=0; i < ctx._messages.length; i++) {
                 var _m = ctx._messages[i];
                 log(_m.text, _m.level);
             }
         }
         publish("/jmaki/runtime/intialized", {});
         loadExtensions();
         publish("/jmaki/runtime/extensionsLoaded", {});
         bootstrapWidgets();

         publish("/jmaki/runtime/widgetsLoaded", {});
         // load the theme
         if ( ctx.config.theme) {
             var theme = ctx.config.theme;
             if (!/(^http)/i.test(theme)) {
                 theme = ctx.webRoot + theme;
             }
             loadStyle(theme);
         }
         publish("/jmaki/runtime/loadComplete", {});
     };

     /**
      * Initialize jMaki by loading the config.json
      *  Write in the glue by loading dependencies and
      *  Register listeners.
      */
     var initialize = function() {
         head = document.getElementsByTagName("head")[0];
         body = document.body;
         if (!ctx.config) {
             ctx.config = {};
           doAjax({ url : typeof(jmakiConfigJson)=="string" ? jmakiConfigJson:ctx.webRoot + ctx.resourcesRoot + "/config.json",
                asynchronous : false,
                timeout : 3000,
                onerror : function() { /* do nothing and continue*/},
                callback :  function(req) {
                   if (req.responseText !== "") {
                       var obj = eval('(' + req.responseText + ')');
                       if (obj.config) {
                           ctx.config = obj.config;
                       }
                   }
               }
           });
         }
         postInitialize();
     };

    var Injector = function() {

          var _uuid = new Date().getMilliseconds();
          var _injector = this;
          var _processing = false;

          var tasks = [];

          /**
           * If were returning an text document remove any script in the
           * the document and add it to the global scope using a time out.
           */
          var getContent = function(rawContent, _task) {

           _task.embeddedScripts = [];
           _task.embeddedStyles = [];
           _task.scriptReferences = [];
           _task.styleReferences = [];

            var _t = rawContent;

            // check against the base directory
            var getReativeURL = function() {
                var root = window.location.href;
                if (root[root.length -1] == "/") {
                    return root;
                }
                var _p = root.split("/");
                // remove the file portion
                _p.pop();
                root = _p.join("/") + "/";
                return root;
            };

            // recursively go through and weed out the scripts

            var gscripts = document.getElementsByTagName("script");
            var gstyles = document.getElementsByTagName("link");

            var root = getReativeURL();
            while (_t.indexOf("<script") != -1) {
                    var realStart = _t.indexOf("<script");
                    var scriptSourceStart = _t.indexOf("src=", (realStart));
                    var scriptElementEnd = _t.indexOf(">", realStart);
                    var end = _t.indexOf("</script>", (realStart)) + "</script>".length;
                    if (realStart != -1 && scriptSourceStart != -1) {
                        var scriptSourceName;
                        var scriptSourceLinkStart= scriptSourceStart + 5;
                        var quoteType =  _t.substring(scriptSourceStart + 4, (scriptSourceStart +5));
                        var scriptSourceLinkEnd= _t.indexOf("\"", (scriptSourceLinkStart + 1));
                          scriptSourceLinkEnd= _t.indexOf(quoteType, (scriptSourceLinkStart + 1));
                        if (scriptSourceStart < scriptElementEnd) {
                            scriptSourceName = _t.substring(scriptSourceLinkStart, scriptSourceLinkEnd);
                            // prevent multiple inclusions of the same script
                            var exists = false;
                            for (var i = 0; i < gscripts.length; i++) {
                                if (typeof gscripts[i].src) {
                                    if (gscripts[i].src == scriptSourceName ||
                                            gscripts[i].src == (root + scriptSourceName)) {
                                        exists = true;
                                        break;
                                    }
                                }
                            }
                            if (!exists) {
                                _task.scriptReferences.push(scriptSourceName);
                            }
                        }
                    }
                   // now remove the script body
                   var scriptBodyStart =  scriptElementEnd + 1;
                   var sBody = _t.substring(scriptBodyStart, end - "</script>".length);
                   if (sBody.length > 0) {
                          _task.embeddedScripts.push(sBody);
                   }
                   //remove script
                   _t = _t.substring(0, realStart) + _t.substring(end, _t.length);
                   scriptSourceLinkEnd = -1;
              }
              while (_t.indexOf("<style") != -1) {
                   var rs = _t.indexOf("<style");
                   var styleElementEnd = _t.indexOf(">", rs);
                   var e2 = _t.indexOf("</style>", rs) ;
                   var styleBodyStart =  styleElementEnd + 1;
                   var sBody2 = _t.substring(styleBodyStart, e2);
                   if (sBody2.length > 0) {
                      _task.embeddedStyles.push(sBody2);
                   }
                   //remove style
                   _t = _t.substring(0, rs) + _t.substring(e2 + "</style>".length, _t.length);
                }
                // get the links
                while (_t.indexOf("<link") != -1) {
                    var rs2 = _t.indexOf("<link");
                    var styleSourceStart = _t.indexOf("href=", rs2);
                    var styleElementEnd2 = _t.indexOf(">", rs2) +1;
                    if (rs2 != -1 && styleSourceStart != -1) {
                        var styleSourceName;
                        var styleSourceLinkStart= styleSourceStart + 6;
                        var qt =  _t.substring(styleSourceStart + 5, (styleSourceStart + 6));
                        var styleSourceLinkEnd= _t.indexOf(qt, (styleSourceLinkStart + 1));
                        if (styleSourceStart < styleElementEnd2) {
                            styleSourceName = _t.substring(styleSourceLinkStart, styleSourceLinkEnd);
                              var exists2 = false;
                                for (var ii = 0; ii < gstyles.length; ii++) {
                                    if (isDefined(gstyles[ii].href)) {
                                        if (gstyles[ii].href == styleSourceName) {
                                            exists2 = true;
                                        }
                                    }
                                }
                        if (!exists2) {
                              _task.styleReferences.push(styleSourceName);
                          }
                        }
                        //remove style
                        _t = _t.substring(0, rs2) + _t.substring(styleElementEnd2, _t.length);
                    }
                }

                // inject the links
                for(var loop = 0; _task.styleReferences && loop < _task.styleReferences.length; loop++) {
                    var link = createElement("link");
                    link.href = _task.styleReferences[loop];
                    link.type = "text/css";
                    link.rel = "stylesheet";
                    head.appendChild(link);
                }

                var stylesElement;
                if (_task.embeddedStyles.length > 0) {
                    stylesElement = createElement("style");
                    stylesElement.type="text/css";
                    var stylesText;
                    for(var j = 0; j < _task.embeddedStyles.length; j++) {
                        stylesText = stylesText + _task.embeddedStyles[j];
                    }
                    if (document.styleSheets && +
                        document.styleSheets[0].cssText) {
                        document.styleSheets[0].cssText = document.styleSheets[0].cssText + stylesText;
                    } else {
                        appendChild(stylesElement, document.createTextNode(stylesText));
                        head.appendChild(stylesElement);
                    }
                }
                _task.content = _t;
                return _t;
           };

          this.inject = function(task) {
           // make sure jmaki creates a list of libraries it can not load
            if (tasks.length === 0 && !_processing) {
                inject(task);
            } else {
                tasks.push(task);
            }
          };

          // pass in a reference to the task
          // start the next task
          function processNextTask() {
              if (tasks.length >0) {
                  var _t = tasks.shift();
                  inject(_t);
              }
              _processing = false;
          }

          function processTask(injectionPoint, task) {
              clearWidgets(injectionPoint);
              var _id = "injector_" + _uuid;
              var data = task.content + "<div id='" + _id + "'></div>";
              injectionPoint.innerHTML = data;
              // wait for the content to be loaded
              var _t = setInterval(function() {
                  if (getElement(_id)) {
                      clearInterval(_t);
                      try {
                          _injector.loadScripts(task,function () {
                              // if we are using Widget Loader check for widgets and add them
                              var wf = getExtension("widgetFactory");
                              if (wf !== null) {
                                  wf.findAndAdd(injectionPoint);
                              }
                              processNextTask();
                          });
                      } catch (e) {
                          injectionPoint.innerHTML = "<span style='color:red'>" + e.message + "</span>";
                      }
                  }
              }, 25);
          }

          /**
           *
           * Load template text aloing with an associated script
           *
           * Argument p properties are as follows:
           *
           * url :              Not required but used if you want to get the template from
           *                    something other than the injection serlvet. For example if
           *                    you want to load content directly from a a JSP or HTML file.
           *
           * p.injectionPoint:  Not required. This is the id of an element into. If this is
           *                    not specfied a div will be created under the root node of
           *                    the document and the template will be injected into it.
           *                    Content is injected by setting the innerHTML property
           *                    of an element to the template text.
           */
          function inject(task) {     	  
              _processing = true;

              doAjax({
                    method:"GET",
                    url: task.url,
                    asynchronous: false,
                    callback: function(req) {         	  
                       getContent(req.responseText, task);
                       //if no parent is given append to the document root
                       var injectionPoint;
                       if (typeof task.injectionPoint == 'string') {
                           injectionPoint = getElement(task.injectionPoint);
                           
                           // wait for the injection point
                           if (!getElement(task.injectionPoint)) {
                               var _t = setInterval(function() {
                                   if (getElement(task.injectionPoint)) {
                                       clearInterval(_t);
                                       injectionPoint = getElement(task.injectionPoint);
                                       setTimeout(function(){processTask(injectionPoint,task);},0);
                                   }
                               }, 25);
                           } else {
                               processTask(injectionPoint, task);
                           }
                        } else {                    	
                            processTask(task.injectionPoint, task);
                        }
                 },
                 onerror : function(){
                    var ip = task.injectionPoint;
                    if (typeof task.injectionPoint == 'string') {
                        ip = getElement(task.injectionPoint);
                    }
                    clearWidgets(ip);                  
                    ip.innerHTML = getMessage("unable_to_load_url", [task.url]);
                    processNextTask();
                 }

               });
          }


          /**
           *
           * Load template text along with an associated script
           *
           * Argument p properties are as follows:
           *
           * url :              Not required but used if you want to get the template from
           *                    something other than the injection serlvet. For example if
           *                    you want to load content directly from a a JSP, JSF call, PHP, or HTML file.
           */
          this.get = function (p) {
              var _rd = "";
               doAjax({
                    method:"GET",
                    url: p.url,
                    asynchronous: false,
                    callback: function(req){
                        _rd = getContent(req.responseText);
                        return _rd;
                    }
                   });
                   return _rd;
          };

          this.loadScripts = function(task, initFunction) {
                  var _loadEmbeded = function() {
                      // evaluate the embedded javascripts in the order they were added
                      for(var loop = 0;task.embeddedScripts && loop < task.embeddedScripts.length; loop++) {
                          var script = task.embeddedScripts[loop];
                          // append to the script a method to call the scriptLoaderCallback
                          eval(script);
                          if (loop == (task.embeddedScripts.length -1)) {
                              if (isDefined(initFunction)) {
                                  initFunction();
                              }
                              return;
                          }
                      }
                    if ((task.embeddedScripts && task.embeddedScripts.length === 0) &&
                          isDefined(initFunction)) {
                          initFunction();
                      }
                  };
                  if (task.scriptReferences &&
                      task.scriptReferences.length > 0){
                      // load the global scripts before loading the embedded scripts
                      addLibraries({ libs : task.scriptReferences.reverse(),
                          cleanup : false,
                          callback : function() {_loadEmbeded();}
                      });
                  } else {
                      _loadEmbeded();
                  }
                  return true;
             };
          };

      ctx.injector = new Injector();


     var DContainer = function(args){

         var _self = this;
         var _container;

           if (typeof args.target == 'string') {
               _self.uuid = args.target;
               _container = getElement(_self.uuid);
           } else {
               this.uuid = args.target.id;
               _container = args.target;
           }
           if (!_self.uuid) {
               _self.uuid = genId();
           }
           // add to a reference of the jmaki containers
           ctx.dcontainers.put(_self.uuid, _self);

           var oldWidth;
           this.url = null;
           this.externalDomain = false;
           var autoSizeH = false;
           var autoSizeW = false;

           var resizing = false;
           var lastSize = 0;
           // default sizes are all based on the width of the container
           var VIEWPORT_WIDTH;
           var VIEWPORT_HEIGHT;

           if (args.autosize) {
               autoSizeH = true;
               autoSizeW = true;
           }

           if (typeof args.autosizeH == 'boolean') {
               autosizeH = args.autosizeH;
           }
           if (typeof args.autosizeW == 'boolean') {
               autoSizeW = args.autosizeW;
           }

           var getHost = function(url) {
               var host = "";
               // get the second 1/2
               var _p = url.split("://");
               if (_p[1]) {
                   if (_p[1].indexOf("/") != -1) {
                       host = _p[1].substring(0, _p[1].indexOf("/"));
                   } else {
                       host = _p[1];
                   }
               }
               return host;
           };

           this.setSize = function(size) {
               if (size.w) {
                   VIEWPORT_WIDTH = size.w;
                   _container.style.width = VIEWPORT_WIDTH + "px";
                   if (_self.iframe)  {
                       _self.iframe.style.width = VIEWPORT_WIDTH -2 + "px";
                   }
               }
               if (size.h) {
                   VIEWPORT_HEIGHT = size.h;
                   _container.style.height = VIEWPORT_HEIGHT + "px";
                   if (_self.iframe) {
                       _self.iframe.style.height = VIEWPORT_HEIGHT -2 + "px";
                   }
               }
           };

           var resize = function() {
               var _dim = getDimensions(_container);
               if (autoSizeH || autoSizeW){
                   if (!_container.parentNode){
                       return;
                   }
                  var pos = getPosition(_container);
                   if (_container.parentNode.nodeName == "BODY") {
                       if (window.innerHeight){
                           if (autoSizeH) {
                               VIEWPORT_HEIGHT = window.innerHeight - pos.y ;
                           }
                           if (autoSizeW) {
                               VIEWPORT_WIDTH = window.innerWidth - 20;
                           }
                       } else {
                           if (_dim === null) {
                               if (autoSizeW) {
                                   VIEWPORT_WIDTH = 400;
                               }
                           } else {
                               if (autoSizeW) {
                                   VIEWPORT_WIDTH = _dim.w -20;
                               }
                               if (autoSizeH) {
                                   VIEWPORT_HEIGHT = _dim.h - pos.y;
                               }
                           }
                       }
                   } else {
                       if (_dim === null) {
                           if (autoSizeW) {
                               VIEWPORT_WIDTH = 400;
                           }
                       } else {
                           if (autoSizeW) {
                               VIEWPORT_WIDTH = _dim.w;
                           }
                           if (autoSizeH) {
                               VIEWPORT_HEIGHT = _dim.h;
                           }
                       }
                   }
                   if (autoSizeH) {
                       if (VIEWPORT_HEIGHT < 0) {
                           VIEWPORT_HEIGHT = 320;
                       }
                       _container.style.height = VIEWPORT_HEIGHT + "px";
                   }
                   if (autoSizeW) {
                       _container.style.width = VIEWPORT_WIDTH + "px";
                   }
               } else {
                   _container.style.width = VIEWPORT_WIDTH + "px";
                   _container.style.height = VIEWPORT_HEIGHT + "px";
               }
               if (VIEWPORT_HEIGHT < 0) {
                   VIEWPORT_HEIGHT = 320;
               }
               if (VIEWPORT_WIDTH < 0) {
                   VIEWPORT_WIDTH = 500;
               }

               if (args.useIframe) {
                   if (_self.iframe) {
                       _self.iframe.style.height = VIEWPORT_HEIGHT -2 + "px";
                       _self.iframe.style.width = VIEWPORT_WIDTH -2 + "px";
                   }
               }
               // used for tracking with IE
               oldWidth = body.clientWidth;
           };

           var loadURL = function(_url){
               // shut down all events published to iframe
               if (_self.iframe) {
                   _self.externalDomain = true;
               }
               if (_url.message) {
                   _url = _url.message;
               }
               if (typeof _url == 'string') {
                   _self.url = _url;
               } else if (_url.url) {
                   _self.url = _url.url;
               } else if (_url.value) {
                   _self.url = _url.value;
               }
               // check for jmaki and enable events to flow to parent jmaki instances
               function enableEvents() {
                   // check to see if we are in the same domain for pushing messages from the bus
                   // check if we are an external link
                   if (/http/i.test( _self.url) &&  top.window.location.host != getHost( _self.url)) {
                       _self.externalDomain = true;
                   } else {
                       _self.externalDomain = false;
                   }
                   if (!_self.externalDomain) {
                       var _w = _self.iframe.contentWindow ? _self.iframe.contentWindow  : _self.iframe.window;
                       if (_w && _w.jmaki) {
                           _w.jmaki.publishToParent = true;
                       }
                   }
               }
               if (args.useIframe === true) {
                   // wait for the iframe if it hasn't loaded
                   if (!_self.iframe) {
                       var _t = setInterval(function() {
                           if (getElement(_self.uuid + "_iframe")) {
                               clearInterval(_t);
                               _self.iframe = getElement(_self.uuid + "_iframe");
                               // wire on event listener to wait for iframe load and then
                                   if (ctx.MSIE){
                                       _self.iframe.onreadystatechange = function() {
                                           if (this.readyState == "complete") {
                                               enableEvents();
                                           }
                                        };
                                   } else {
                                     _self.iframe.onload = enableEvents;
                                   }
                               _self.iframe.src =  _self.url;
                           }
                       }, 5);
                 } else {
                     if (ctx.MSIE){
                         _self.iframe.onreadystatechange = function() {
                             if (this.readyState == "complete") {
                                 enableEvents();
                             }
                         };
                     } else {
                         _self.iframe.onload = enableEvents;
                     }
                     _self.iframe.src = _self.url;
                }
               } else {      	   
                   ctx.injector.inject({url: _self.url, injectionPoint: _container});  
               }
           };

           var layout = function() {
               if (!ctx.MSIE) {
                   resize();
                   return;
               }
               // special handling for ie resizing.
               // we wait for no change for a full second before resizing.
               if (oldWidth != body.clientWidth && !resizing) {
                   if (!resizing) {
                       resizing = true;
                       setTimeout(layout,500);
                   }
               } else if (resizing && body.clientWidth == lastSize) {
                   resizing = false;
                   resize();
               } else if (resizing) {
                   lastSize = body.clientWidth;
                   setTimeout(layout, 500);
               }
       };

      var  initD = function() {
               if (window.attachEvent) {
                   window.attachEvent('onresize', layout);
               } else if (window.addEventListener) {
                   window.addEventListener('resize', layout, true);
               }
               var _ot = _container;
               if (_self.iframe) {
                   _ot = _self.iframe;
               }
               if (args.overflow) {
                   _ot.style.overflow = args.overflow;
               }
               if (args.overflowX) {
                   _ot.style.overflowX = args.overflowX;
               }
               if (args.overflowY) {
                   _ot.style.overflowY = args.overflowY;
               }
               if (args.startWidth) {
                   VIEWPORT_WIDTH = Number(args.startWidth);
                   _container.style.width = VIEWPORT_WIDTH + "px";
               } else {
                   VIEWPORT_WIDTH = _container.clientWidth;
                   autoSizeW = true;
               }
               if (args.startHeight) {
                   VIEWPORT_HEIGHT = Number(args.startHeight);
               } else {
                   VIEWPORT_HEIGHT = _container.clientHeight;
                   autoSizeH = true;
               }
               if (VIEWPORT_HEIGHT <= 0) {
                   VIEWPORT_HEIGHT = 320;
               }
               _container.style.height = VIEWPORT_HEIGHT + "px";
               if (args.useIFrame &&  _self.iframe) {
                   _self.iframe.style.height = VIEWPORT_HEIGHT + "px";
               }
               resize();
               if (args.url && !args.useIframe) {
                   loadURL(args.url);
               } else if (args.content && !_self.iframe) {
                   _container.innerHTML = args.content;
               } else if (args.url && !args.url) {
                   loadURL(args.url);
               }
               if (_self.iframe) {
                   _self.iframe.style.display = "inline";
               }
           };


           function createIframe(content) {
               _self.iframe = getElement(_self.uuid + "_iframe");
               if (_self.iframe) {
                   _self.iframe.parentNode.removeChild(_self.iframe);
               }
               // use this technique as creating the iframe programmatically does not allow us to turn the border off
               var iframeTemplate = "<iframe style='display:none' id='" + _self.uuid + "_iframe' name='" + _self.uuid +
                   "_iframe' frameborder=0 scrolling=" +
                   ((args.overflow == 'hidden') ? 'NO' : 'YES') + "></iframe>";
               _container.innerHTML = iframeTemplate;
               // wait for the iframe
               var _t = setInterval(function() {
                   if (getElement(_self.uuid + "_iframe")) {
                       clearInterval(_t);
                       _self.iframe = getElement(_self.uuid + "_iframe");
                       setTimeout(function(){
                           if (/http/i.test(_self.url) &&  top.window.location.host != getHost(_self.url)) {
                               _self.externalDomain = true;
                           } else {
                               _self.externalDomain = false;
                           }
                           if (!_self.externalDomain && content) {
                               var _w = _self.iframe.contentWindow ? _self.iframe.contentWindow  : _self.iframe.window;
                               if (_w && _w.document.body) {
                                   _w.document.body.innerHTML = content;
                               }
                           }
                           initD();},0);
                   }
               }, 5);
           }

       var clear = function() {
               if (args.useIframe) {
                   if (_self.iframe) {
                       loadURL("");
                   } else {
                       args.url = "";
                   }
               } else {
                   clearWidgets(_container);
                   _container.innerHTML = "";
               }
           };

           var setContent = function(_c) {
               var _con;
               if (_c.message) {
                   _c = _c.message;
               }
               if (_c.value) {
                   _con = _c.value;
               } else {
                   _con = _c;
               }
               if (!_self.iframe) {
                   _container.innerHTML = _con;
               } else {
                   clear();
                   // recreate the iframe
                   _container.innerHTML = "";
                   createIframe(_c);
               }
           };

       this.destroy = function() {
           ctx.dcontainers.remove(_self.uuid);
           if (window.attachEvent) {
               window.dettachEvent('onresize', layout);
           } else if (window.addEventListener) {
               window.removeEventListener("resize", layout, true);
           }
       };

       _self.clear = clear;
       _self.resize = resize;
       _self.loadURL = loadURL;
       _self.setContent = setContent;
       _self.init = initD;

       if (args.useIframe && args.useIframe === true) {
           createIframe(args.content);
       } else{
           initD();
       }
   };

   /*

   The code was adopt with minor modifications from:
   http://www.json.org/json2.js
   */

   ctx.json = function () {

           var f = function(n) {    // Format integers to have at least two digits.
               return n < 10 ? '0' + n : n;
           };

           var m = {    // table of character substitutions
               '\b': '\\b',
               '\t': '\\t',
               '\n': '\\n',
               '\f': '\\f',
               '\r': '\\r',
               '"' : '\\"',
               '\\': '\\\\'
           };

           var stringify = function(value, whitelist) {
               var a,          // The array holding the partial texts.
                   i,          // The loop counter.
                   k,          // The member key.
                   l,          // Length.
                   r = /["\\\x00-\x1f\x7f-\x9f]/g,
                   v;          // The member value.

               switch (typeof value) {
               case 'string':

   // If the string contains no control characters, no quote characters, and no
   // backslash characters, then we can safely slap some quotes around it.
   // Otherwise we must also replace the offending characters with safe sequences.

                   return r.test(value) ?
                       '"' + value.replace(r, function (a) {
                           var c = m[a];
                           if (c) {
                               return c;
                           }
                           c = a.charCodeAt();
                           return '\\u00' + Math.floor(c / 16).toString(16) +
                                                      (c % 16).toString(16);
                       }) + '"' :
                       '"' + value + '"';

               case 'number':

   // JSON numbers must be finite. Encode non-finite numbers as null.

                   return isFinite(value) ? String(value) : 'null';

               case 'boolean':
                   return String(value);
               case 'null':
                   return String(value);
               case 'date':
                 return value.getUTCFullYear() + '-' +
                    f(value.getUTCMonth() + 1) + '-' +
                    f(value.getUTCDate())      + 'T' +
                    f(value.getUTCHours())     + ':' +
                    f(value.getUTCMinutes())   + ':' +
                    f(value.getUTCSeconds())   + 'Z';

               case 'object':

   // Due to a specification blunder in ECMAScript,
   // typeof null is 'object', so watch out for that case.

                   if (!value) {
                       return 'null';
                   }

   // If the object has a toJSON method, call it, and stringify the result.

                   if (typeof value.toJSON === 'function') {
                       return stringify(value.toJSON());
                   }
                   a = [];
                   if (typeof value.length === 'number' &&
                           !(value.propertyIsEnumerable('length'))) {

   // The object is an array. Stringify every element. Use null as a placeholder
   // for non-JSON values.

                       l = value.length;
                       for (i = 0; i < l; i += 1) {
                           a.push(stringify(value[i], whitelist) || 'null');
                       }

   // Join all of the elements together and wrap them in brackets.

                       return '[' + a.join(',') + ']';
                   }
                   if (whitelist) {

   // If a whitelist (array of keys) is provided, use it to select the components
   // of the object.

                       l = whitelist.length;
                       for (i = 0; i < l; i += 1) {
                           k = whitelist[i];
                           if (typeof k === 'string') {
                               v = stringify(value[k], whitelist);
                               if (v) {
                                   a.push(stringify(k) + ':' + v);
                               }
                           }
                       }
                   } else {

   // Otherwise, iterate through all of the keys in the object.

                       for (k in value) {
                           if (typeof k === 'string') {
                               v = stringify(value[k], whitelist);
                               if (v) {
                                   a.push(stringify(k) + ':' + v);
                               }
                           }
                       }
                   }

   // Join all of the member texts together and wrap them in braces.

                   return '{' + a.join(',') + '}';
               }
               return 'null';
           };

           return {
               serialize: stringify,
               deserialize: function (text, filter) {
                   var j;
                   text = trim(text);
                   var walk = function(k, v) {
                       var i, n;
                       if (v && typeof v === 'object') {
                           for (i in v) {
                               if (Object.prototype.hasOwnProperty.apply(v, [i])) {
                                   n = walk(i, v[i]);
                                   if (n !== undefined) {
                                       v[i] = n;
                                   }
                               }
                           }
                       }
                       return filter(k, v);
                   };

   // Parsing happens in three stages. In the first stage, we run the text against
   // regular expressions that look for non-JSON patterns. We are especially
   // concerned with '()' and 'new' because they can cause invocation, and '='
   // because it can cause mutation. But just to be safe, we want to reject all
   // unexpected forms.

   // We split the first stage into 4 regexp operations in order to work around
   // crippling inefficiencies in IE's and Safari's regexp engines. First we
   // replace all backslash pairs with '@' (a non-JSON character). Second, we
   // replace all simple value tokens with ']' characters. Third, we delete all
   // open brackets that follow a colon or comma or that begin the text. Finally,
   // we look to see that the remaining characters are only whitespace or ']' or
   // ',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.
                   if (/^[\],:{}\s]*$/.test(text.replace(/\\./g, '@').
   replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(:?[eE][+\-]?\d+)?/g, ']').
   replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {
   // In the second stage we use the eval function to compile the text into a
   // JavaScript structure. The '{' operator is subject to a syntactic ambiguity
   // in JavaScript: it can begin a block or an object literal. We wrap the text
   // in parens to eliminate the ambiguity.
                       j = eval('(' + text + ')');

   // In the optional third stage, we recursively walk the new structure, passing
   // each name/value pair to a filter function for possible transformation.

                       return typeof filter === 'function' ? walk('', j) : j;
                   }

   // If the text is not JSON parseable, then a SyntaxError is thrown.
                  throw new Error(getMessage('invalid_json'));
               }
           };

       }();

    // Localized messages
    // where the first set is a set of languages
    ctx.messages = new Map();

    // This map is intialized with the default languages and messages
    addMessages(ctx.defaultLocale, ctx.defaultMessages);

    var oldLoad  = window.onload;

    /**
     * onload calls bootstrap function to initialize and load all registered widgets
     * override initial onload.
     */
    window.onload = function() {
        if (!ctx.initialized) {
            initialize();
        } else {
            bootstrapWidgets();
            return;
        }
        
        if (typeof oldLoad  == 'function') {
            oldLoad();
        }
    };

    /*
     * public Functions Available on the jmaki namespace
     */
    mixin({
        // public functions
        log : log,
        mixin : mixin,
        namespace : namespace,
        getMessage : getMessage,
        messageFormat : messageFormat,
        subscribe : subscribe,
        unsubscribe : unsubscribe,
        publish : publish,
        Timer : Timer,
        inspect : inspect,
        findObject : findObject,
        genId : genId,
        doAjax : doAjax,
        addLibraries : addLibraries,
        addWidget : addWidget,
        loadStyle : loadStyle,
        getElementsByStyle : getElementsByStyle,
        replaceStyleClass : replaceStyleClass,
        getAllChildren : getAllChildren,
        getWidget : getWidget,
        loadWidget : loadWidget,
        loadExtension : loadExtension,
        getExtension : getExtension,
        addExtension : addExtension,
        removeWidget : removeWidget,
        trim : trim,
        writeScript : writeScript,
        extend : extend,
        hideLogger : hideLogger,
        clearLogger : clearLogger,
        showLogMessage : showLogMessage,
        hideLogMessage : hideLogMessage,
        initialize : initialize,
        clone : clone,
        filter : filter,
        processActions : processActions,
        getDimensions : getDimensions,
        getPosition : getPosition,
        addTimer : addTimer,
        addTimers : addTimers,
        Map : Map,
        DContainer : DContainer,
        Injector : Injector
        }, ctx);
    return ctx;
}();
}

/**
 * jMaki Widget Loader 1.8.1 for Plain HTML
 * 
 * For full source see : https://jmaki-ext.dev.java.net
 *
 */
/**
 * jMaki Widget Loader 1.9.1 for Plain HTML
 * 
 * For full source see : https://jmaki-ext.dev.java.net
 *
 */
jmaki.namespace("jmaki.extensions.widgetFactory");
    
jmaki.extensions.widgetFactory.Extension = function(eargs) {
    
   var factory = this;
   factory.autoLoad = true;
   factory.loading = new jmaki.Map();
    
   jmaki.log("Loaded Widget Factory 1.9.1");      

   function createElementMap(target, prop) {
        var _map = {};
        var elms = document.getElementsByTagName(target);          
        for (var i = 0; i < elms.length; i++) {
            if (elms[i][prop]) _map[elms[i][prop]] = true;                    
        }
        return _map;
    }

    // list of the currently loaded scripts
    factory.gscripts = createElementMap("script", "src");
    // list of currently loaded styles
    factory.gstyles = createElementMap("link", "href");

    // load the widget.json
    this.loadWidgetJson = function(_widgetDir) {
        var obj = null;
        jmaki.doAjax({
            url : _widgetDir + "widget.json",
            asynchronous : false,
            callback : function(req) {
                if (req.responseText != '') {
                	try {              		
                        obj = eval("(" + req.responseText + ")");
                	} catch(e){
                		jmaki.log("Error loading widget" + e);
                	}
                }
            }
        });
        return obj;        
    };
    
    /**
     * Programtically load any jMaki widget and it's resources from the browser
     *
     */    
    this.loadWidget = function(props,_forceResources) {     	
        var forceResources = false;
        if (typeof _forceResources != "undefined") forceResources = _forceResources;
        // this is a reference to the document are working on
        var tdoc = window.document;
        // include the scripts
        var _wargs = props.widget;
        var container = props.container;      
        var _wjson = props.wjson;
        var widgetDir = _wargs.widgetDir;
        // use default widgetDir if not provided
        if (!widgetDir) {
        	// do global replace of . with /
            var temps = _wargs.name.split('.');
            widgetDir = jmaki.webRoot + jmaki.resourcesRoot + "/" + temps.join("/") + "/";  
            _wargs.widgetDir = widgetDir;        
        }
        var _callback = props.callback;
        if (!_wargs.uuid)_wargs.uuid = jmaki.genId();
        if (!container.id) container.id = _wargs.uuid + "_wrapper_";         
        // try to load the widget.json   
        if (!_wjson) {
            _wjson = factory.loadWidgetJson(widgetDir);
        }
    
        function normalizeURL(url) {
            if (/../.test(url)) {
                var toks = url.split("/");
                if (toks.length >0){
                    var _count=0;
                    var _index = toks.length -1;
                    while (_index >= 0) {
                        if (toks[_index] == '..'){                            
                            _count++;
                        } else {
                            // we have the end of a set
                            if (_count > 0) {
                                var _start = _index - (_count -1);
                                var _slen = _count * 2;
                                // don't go beyond size minimal size
                                if (_index - _count<  0){
                                    _start = 0;
                                }
                                toks.splice(_start,_slen);                                
                                _count = 0;
                            }
                        }
                        _index--;                        
                    }  
                }
                return toks.join("/");
            } else {
                return url;
            }
        }

        function loadTemplate(caller) {   
           // load the template       
           jmaki.doAjax({ url : widgetDir + "component.htm",
               asynchronous : false,
               callback : function(req) {                  
                   // global regex replace not working so using arrays
                   var temps = req.responseText.split('${uuid}');
                   template = temps.join(_wargs.uuid);
                   temps = template.split('${widgetDir}');
                   template = temps.join(_wargs.widgetDir);
                   if (_wargs.value && /\${value}/i.test(template)) {
                       temps = template.split('${value}');
                       template = temps.join(_wargs.value + '');                	   
                   }                   
                   container.innerHTML = template + "<div id='_" + _wargs.uuid + "_'></div>";
               }
           });
           
          // wait for the content to be loaded
          var _t = setInterval(function() {
              if (tdoc.getElementById("_" + _wargs.uuid + "_") != null) {
                  clearInterval(_t);                                         
                  initWidget();              
              }
          }, 10);
       }
       
       function initWidget() { 	   
           // load the css and add only if the fille exists and ! wjson.hasCSS == false
           if ((!factory.gstyles[widgetDir + "component.css"]  || forceResources) &&
                !(_wjson && typeof _wjson.hasCSS == "boolean" && _wjson.hasCSS == false)  ) {
               jmaki.doAjax({ url : widgetDir + "component.css",
               callback : function(req) {          	   
                   jmaki.loadStyle(widgetDir + "component.css");
                   factory.gstyles[widgetDir + "component.css"] = true;
               }});               
           } 
           // check for the constructor and if we don't find it load and wait for the component.js to load
           if ( jmaki.findObject("jmaki.widgets." + _wargs.name + ".Widget") == null) { 
        	     jmaki.addLibraries({
                     libs :
                   [widgetDir + "component.js"],
                   callback :  function() {     
                       factory.gscripts[widgetDir + "component.js"] = true;          
                       // create an instance and feed it the wargs using jmaki                                  
                       var _wimpl = jmaki.loadWidget(_wargs);  
                       factory.loading.remove(_wargs.uuid);
                       if (_wimpl && typeof _callback == "function") {
                           _callback(_wimpl, _wargs, container);  
                       }
                       container.style.visibility = "visible"; 
                   },
                   inprocess : undefined,
                   cleanup : false});
           } else {	        
                   var _wimpl = jmaki.loadWidget(_wargs);
                   factory.loading.remove(_wargs.uuid);
                   if (_wimpl && typeof _callback == "function") {
                       _callback(_wimpl, _wargs, container);  
                   }
                   container.style.visibility = "visible";            
           }    
       }
        // load any dependencies
        if ( _wjson  && _wjson.config) {
           var _cfg = _wjson.config.type;
            
           if (_cfg.themes) {
               var theme;
               // check each theme if they are the default. Last one wins      
               for (var i=0; i < _cfg.themes.length; i++) {
                  if (_cfg.themes[i]['default'] == true) {
                      theme = _cfg.themes[i].style;
                  }
                  // exit if there is a match to global theme
                  if (_cfg.themes[i].id == jmaki.config.globalTheme) {
                      theme = _cfg.themes[i].style;
                      break;
                  }
               }
               var _url = normalizeURL(widgetDir +  theme);
               if (!factory.gstyles[_url]){
                    jmaki.loadStyle(_url);
                    factory.gstyles[_url] = true;   
               }
           } 
           // load styles
           if (_cfg.styles) {                
               for (var j=0; j < _cfg.styles.length; j++) {             
                   var _url2 = normalizeURL(((/^http/i.test(_cfg.styles[j])) ? '' : widgetDir) + _cfg.styles[j]);                
                    if (!factory.gstyles[_url2]){                      
                        jmaki.loadStyle(_url2);
                        factory.gstyles[_url2] = true; 
                    }
               }
           }
           // do preload
           if (_cfg.preload) {
               _globalScope.eval(_cfg.preload);
           }

           // load all javascripts
           if (_cfg.libs) {
               var _libs = [];
               for (var ii = _cfg.libs.length -1; ii >= 0; ii--) {
            	   var _clib = _cfg.libs[ii];
            	   // test resource expression against user agent. If true add the resource
                    if (_cfg.libs[ii].uaTest) {
                    	if (_cfg.libs[ii].uaTest) {
                    		var t = new RegExp(_cfg.libs[ii].uaTest);
                    		if (t.test(navigator.userAgent) === true) {
                    			_clib = _clib.lib;
                    		} else {
                    			continue;
                    		}
                    	}
                    }
                    var _url3 = normalizeURL(((/^http/i.test(_clib)) ? '' :  widgetDir) + _clib);                    
                    if (!factory.gscripts[_url3]) {
                       factory.gscripts[_url3] = true;
                       _libs.push(_url3);                    
                    }
               }           
               if (_libs.length >0){
                   jmaki.addLibraries( { libs : _libs, callback : loadTemplate, cleanup :  false});
               } else {
                  loadTemplate(); 
               }
           } else {
               loadTemplate();
           }
        } else {
            loadTemplate();
        }
    };
    /**
     * Find declarative markup an element using jmakiName, jmakiArgs, jmakiValue,
     * jmakiId, jmakiPublish, jmakiSubscribe all of which map to the properties a
     * normal jMaki widget would need  to load.
    */
    this.findAndAdd = function(_target, _callback) {
    	
        var target = _target || document.body;
        if (typeof _target == "string") target = document.getElementById(targetId); 
        var targets = jmaki.getAllChildren(target, []);
        
        for (var i=0; targets && i < targets.length; i++) {
         
            if (targets[i] && targets[i].getAttribute('jmakiName')) {
                var widget = {};
                widget.name = targets[i].getAttribute('jmakiName');
                if (targets[i].getAttribute('jmakiArgs')) {
                	try {
                	widget.args = eval("(" + targets[i].getAttribute('jmakiArgs') + ")");
                	} catch(e){
                		jmaki.log("Error Parsing Args:" + e);
                	}
                }
                var val;
                var valString = targets[i].getAttribute('jmakiValue');            
                if (typeof valString == "string" && !(/@\{/.test(valString)) &&  (/\[/.test(valString)|| /\{/.test(valString))) {          	
                	try {         		
                	    widget.value = eval("(" + valString + ")" );              
                	} catch (e){
                		jmaki.log("Error Parsing Value :" + e);
                	}
                } else if (typeof valString != 'undefined') {
                	widget.value = valString;
                }          
                if (targets[i].getAttribute('jmakiService'))widget.service = targets[i].getAttribute('jmakiService');
                if (targets[i].getAttribute('jmakiPublish'))widget.publish = targets[i].getAttribute('jmakiPublish');
                if (targets[i].getAttribute('jmakiSubscribe')){
                    var sub = targets[i].getAttribute('jmakiSubscribe');
                    // we have an array
                    if (/\[/.test(sub)) {
                        widget.subscribe = eval("(" + sub + ")");
                    } else {
                        widget.subscribe = sub;
                    }
                }
                if (targets[i].getAttribute('jmakiId'))widget.uuid = targets[i].getAttribute('jmakiId');
                var loc = targets[i];
                factory.loading.put(widget.uuid, true);
                factory.loadWidget({widget : widget, container : loc });
            }
        }
        // wait for the widgets to be loaded                
        var _lt = setInterval(function() {              	
            if (factory.loading.keys().length === 0) {
            	clearInterval(_lt);  
                factory.loadCallback.apply({}, []);           
            }
        }, 10);
    };
    // auto load
    if (eargs.args &&
        typeof eargs.args.parseOnLoad == "boolean") {
            factory.autoload = eargs.args.parseOnLoad;
    }
    factory.loadCallback = function() {
        jmaki.publish("/jmaki/extensions/widgetFactory/loadComplete",{});
    };
    if (factory.autoLoad) {
        if (jmaki.loaded) factory.findAndAdd();
        else {
            jmaki.subscribe("/jmaki/runtime/loadComplete", function() {
                if (factory.autoLoad) factory.findAndAdd();
            });
        }
    }
};

jmaki.loadExtension({ name : "widgetFactory", args : {parseOnLoad : true}});
