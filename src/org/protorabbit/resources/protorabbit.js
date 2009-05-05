/*
 * Protorabbit
 *
 * Copyright (c) 2009 Greg Murray (protorabbit.org)
 * 
 * Licensed under the MIT License:
 * 
 *  http://www.opensource.org/licenses/mit-license.php
 *
 *  protorabbit.js
 *
 *  This file contains the client client utilities to load deferred files
 *  and notify user.
 * 
 */

if (!window.protorabbit) {

window.protorabbit = function() {

 var ctx = {
          MSIE : /MSIE/i.test(navigator.userAgent),
          counter : 0,
          ajaxRequestQueue : [],
          deferredFragments : [],
          deferredScripts : [],
          deferredStyles : [],
          deferredProperties : [],
          debug : true
 };

 function addDeferredScript(s){
      ctx.deferredScripts.push(s);
 }

 function isDefined(_target) {
      return (typeof _target != "undefined");
 }

 function namespace(_path, target) {
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
 }

  function matchWildcard(pattern,topic) {

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
 }

 function log(message) {
     if (window.console) {
         window.console.log(message);
     }
 }

 function genId() {
     return "protrabbit_" + (ctx.counter +=1);
 }

 function trim(t) {
     return  t.replace(/^\s+|\s+$/g, "");
 }

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
  *  Publish an event to a topic
  *  @param name Name of the topic to be published to
  *  @param args payload of the publish
  */
 function publish(name, args) {
     if (typeof name == "undefined" || typeof args == "undefined"){
         return false;
     }
     if (ctx.debug) {
         log("Publish " + name);
     }

     // check the glue for listeners
     if (ctx.subs){
         for (var _l=0; _l < ctx.subs.length;_l++ ) {
             var _listener = ctx.subs[_l];
                  if ((_listener.topic instanceof RegExp &&
                       _listener.topic.test(name))  ||
                        _listener.topic == name ||
                       (typeof _listener.topic.charAt == 'function' &&
                    matchWildcard(_listener.topic,name))
                ) {

                 // set the topic on payload
                 args.topic = name;
                 if (_listener.action == 'call' && _listener.target) {
                     // get the top level object
                     var Obj;
                     var myo = 'undefined';
                     if (_listener.target.functionHandler) {
                         _listener.target.functionHandler.apply(window,[args]);
                     }
                 }
             }
         }
     }
     return true;
 }

/*
 *
 */
 function subscribe(l, t) {
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
       log("Subscribe handler required : " + l);
     }
     if (isDefined(lis)){
         if (!isDefined(ctx.subs)) {
             ctx.subs = [];
         }
         if (lis.topic){
             ctx.subs.push(lis);
         } else {
             log("Subscribe topic required" + l);
             return null;
         }
         return lis;
     }
     return null;
 }

 /**
  *  Get the XMLHttpRequest object
  *
  *  Allow for config override to allow for older ActiveX XHR for local file
  *  System with IE7
  *
  */
 function getXHR () {
     if (window.XMLHttpRequest &&
          !( ctx.MSIE &&
             ctx.forceActiveXXHR === true )) {
         return new window.XMLHttpRequest();
     } else if (window.ActiveXObject) {
         return new window.ActiveXObject("Microsoft.XMLHTTP");
     } else {
         return null;
     }
 }

 function handleAjaxError(_m, _r, args){
    if (args.onerror) {
          args.onerror(_m,_r);
        } else {
      log("Protorabbit ajax error " + _m);
    }
 }

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
 function addLibraries(_o, _cb, _inp, _cu) {

     var _libs;
     var _inprocess;
     var _cleanup = true;

     // check to see if anything is still processing and if not
     // call the callback
     var checkQueue = function() {
         var count = 0;
         for (var j in _inprocess) {
             if (_inprocess.hasOwnProperty(j)) {
                 count += 1;
             }
         }
         if (count === 0) {
             if (isDefined(_cb)){
                 setTimeout(function(){_cb();}, 0);
             }
             _inprocess = {};
             ctx.processingScripts = false;
             updateAjaxQueue();
         }
         return false;
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
         _inprocess = {};
         if (!isDefined(ctx._scriptQueue)) {
             ctx._scriptQueue =[];
         }
         ctx._scriptQueue.push({libs : _libs, callback : _cb, cleanup : _cleanup});
         return false;
     } else if (!isDefined(_inprocess)) {
         ctx.processingScripts = true;
         _inprocess = {};
     }

     if (_libs.length <= 0) {
         checkQueue();
     }
     var _uuid = new Date().getMilliseconds();
     var _lib = _libs[_libs.length-1];
     var _s_uuid = "c_script_" + genId();

     var e = document.createElement("script");
     e.start = _uuid;
     e.id =  _s_uuid;
     ctx.head.appendChild(e);

     var se =  document.getElementById(_s_uuid);
     _inprocess[_s_uuid] = _lib;
     var loadHandler = function (_id) {
         var _s =  document.getElementById(_id);
         // remove the script node
         if (_s !== null &&
                 _cleanup === true){
             _s.parentNode.removeChild(_s);
         }
         delete _inprocess[_id];
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
         document.getElementById(_s_uuid).src = _lib;
     } else {
         // the onload handler works on opera, ff, safari
         // and the addEventListener will not work on opera
         se.onload = function(){
                 var _id = _s_uuid;
               loadHandler(_id);
         };
         setTimeout(function(){
             document.getElementById(_s_uuid).src = _lib;
             }, 0);
     }
 };

 /**
  * Generalized XMLHttpRequest which can be used from evaluated code. Evaluated code is not allowed to make calls.
  * @param args is an object literal containing configuration parameters including method[get| post, get is default], body[bodycontent for a post], asynchronous[true is default]
  */
 function doAjax(args) {
     if (typeof args == 'undefined' || !args.url) {
         log("Ajax url required");
         return false;
     }
     // sync up the processing queues for script and ajax loading
     // synchronous requests should not be stopped
     if ((ctx.processingScripts && args.asynchronous) ||
         (ctx.processingAjax &&
         args.asynchronous) ) {
             ctx.ajaxRequestQueue.push(args);
             return false;
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
             handleAjaxError("Request timeout url: " + args.url, _req, args);
             updateAjaxQueue();
             return true;
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
                     handleAjaxError("Ajax server error. url : " +
                                     args.url + " status : " + _req.status, _req, args);
                 }
                 updateAjaxQueue();
                 return true;
             }
         };
     }
     try {
        if (!_c) {
            _req.open(method, args.url, async);
        }
     } catch(e) {
       _c = true;
       handleAjaxError("Ajax request open error " +  args.url,_req, args);
       updateAjaxQueue();
       return false;
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
       handleAjaxError("Ajax send body error " + args.url, _req, args);
       updateAjaxQueue();
       return false;
     }
     if (_c === false && async === false) {
         _c = true;
         if (_req.status === 200 || _req.status === 0) {
              if (callback) {
                  callback(_req);
              }
         } else {
             _c = true;
             handleAjaxError("Ajax server error. url: " + args.url + " status : " + _req.status, _req, args);
         }
         updateAjaxQueue();
     }
 }

 /**
  * Loads the style sheet by adding a link element to the DOM
  * @param target name of style sheet to load
  */
 function addStyleLink(target, media) {
      var styleElement = document.createElement("link");
      styleElement.type = "text/css";
      styleElement.rel="stylesheet";
      styleElement.href = target;
      if (media) {
          styleElement.media = media;
      }
      if (ctx.head === null) {
          var headN = document.createElement("head");
          document.documentElement.insertBefore(headN, document.documentElement.firstChild);
      }
      ctx.head.appendChild(styleElement);
 }

 function addDeferredFragement(item) {
      ctx.deferredFragments.push(item);
 }

 function addDeferredStyle(item, media) {
     ctx.deferredStyles.push({ url : item, media : media});
 }

 function addDeferredProperties(item, prefix) {
     ctx.deferredProperties.push({ url : item, prefix : prefix});
 }
 
 var oldOnload = window.onload;

  window.onload = function() {
      ctx.head = document.getElementsByTagName("head")[0];
      if (ctx.deferredStyles.length > 0) {
          for (var i=0; i < ctx.deferredStyles.length; i+=1) {
              addStyleLink(ctx.deferredStyles[i].url, ctx.deferredStyles[i].media);
          }
      }
      if (ctx.deferredScripts.length > 0) {
          addLibraries({
             libs : ctx.deferredScripts,
             callback : function(args) {
                  publish("/protorabbit/scriptLoad", { "value" : "deferredScripts"} );
              }
          });
      }
      if (ctx.deferredFragments.length > 0) {
          for (var j=0; j < ctx.deferredFragments.length; j++) {
              var item = ctx.deferredFragments[j];

              var content = doAjax( {
                 url : item.include,
                 callback : function(req) {
                     publish("/protorabbit/ajaxLoad", { value : item.url});
                     var target = document.getElementById(item.elementId);
                     target.innerHTML = req.responseText;
                     
                 }
              });
          }
      }
      if (ctx.deferredProperties.length > 0) {
          for (var k=0; k < ctx.deferredProperties.length; k++) {
              var propSet = ctx.deferredProperties[k];
              doAjax( {
                 url : propSet.url,
                 callback : function(req) {
                     var props = eval("(" + req.responseText + ")");
                     for (var p in props) {
                         var target = document.getElementById(propSet.prefix + "_" + p);
                         if (target) {
                             target.innerHTML = props[p];
                         }
                     }
                     publish("/protorabbit/propertySetLoad", { value :propSet});
                 },
                 onerror : function() {
                     log("Error loading properties from " + propSet.url); 
                 }
              });
          }
      }
      if (typeof window.oldOnload == 'function') {
          window.oldOnload.apply({},[]);
      }
  };
 
  return {
      addDeferredFragement : addDeferredFragement,
      addDeferredScript : addDeferredScript,
      addDeferredStyle : addDeferredStyle,
      addDeferredProperties : addDeferredProperties,
      publish : publish,
      subscribe : subscribe,
      log : log
  };

}();

}
