//////////////////////////////////////////////////////////////////////////////////
//                                                                               //
// This is the episode management code. It gathers all the episodic timing       //
// information, returns that as a data structure, and can send it as a beacon    //
// to a specified URL.                                                           //
//                                                                               //
// This code could come from one of three places:                                //
//   1. implemented by the owner of the web page                                 //
//   2. from the implementation of "episodes.js" shared across the industry      //
//   3. in the future, this functionality would be built into the browser itself //
//                                                                               //
///////////////////////////////////////////////////////////////////////////////////


// Don't overwrite pre-existing instances of the object (esp. for older browsers).
var EPISODES = EPISODES || {};

EPISODES.init = function() {
    EPISODES.done = false;
    EPISODES.marks = {};
    EPISODES.measures = {};
    EPISODES.starts = {};
    EPISODES.addEventListener("message", EPISODES.handleEpisodeMessage, false);
    EPISODES.findStartTime();
    EPISODES.addEventListener("beforeunload", EPISODES.beforeUnload, false);
    EPISODES.addEventListener("load", function() { window.postMessage("EPISODES:measure:onload:starttime", "http://stevesouders.com"); window.postMessage("EPISODES:mark:onload", "http://stevesouders.com"); }, false);
}

EPISODES.isCompatible = function() {
    return ( "undefined" != typeof(window.postMessage) );
}

// Parse an EPISODES message and perform the desired function.
EPISODES.handleEpisodeMessage = function(event) {
    var message = event.data;
    var aParts = message.split(':');
    if ( "EPISODES" === aParts[0] ) {
        var action = aParts[1];
        if ( "init" === action ) {
            // "EPISODES:init"
            EPISODES.init();
        }
        else if ( "mark" === action ) {
            // "EPISODES:mark:markName[:markTime]"
            var markName = aParts[2];
            EPISODES.marks[markName] = aParts[3] || Number(new Date());
        }
        else if ( "measure" === action ) {
            // "EPISODES:measure:episodeName[:startMarkName|startEpochTime[:endMarkName|endEpochTime]]"
            var episodeName = aParts[2];

            // If no startMakeName is specified, assume it's the same as the episode name.
            var startMakeName = ( "undefined" != typeof(aParts[3]) ? aParts[3] : episodeName );
            // If the startMakeName doesn't exist, assume it's an actual time measurement.
            var startEpochTime = ( "undefined" != typeof(EPISODES.marks[startMakeName]) ? EPISODES.marks[startMakeName] : 
                                   ( ("" + startMakeName) === parseInt(startMakeName) ? startMakeName : undefined ) );

            var endEpochTime = ( "undefined" === typeof(aParts[4]) ? Number(new Date()) : 
                                 ( "undefined" != typeof(EPISODES.marks[aParts[4]]) ? EPISODES.marks[aParts[4]] : aParts[4] ) );

            if ( startEpochTime ) {
                EPISODES.measures[episodeName] = parseInt(endEpochTime - startEpochTime);
                EPISODES.starts[episodeName] = parseInt(startEpochTime);
            }
        }
        else if ( "done" === action ) {
            // "EPISODES:done"
            EPISODES.done = true;
        }
    }
}

// Return an object of episode names and their corresponding durations.
EPISODES.getMeasures = function() {
    return EPISODES.measures;
}

// Return an object of episode names and their corresponding durations.
EPISODES.getStarts = function() {
    return EPISODES.starts;
}

// Construct a querystring of episodic time measurements and send it to the specified URL.
EPISODES.sendBeacon = function(url) {
    var measures = EPISODES.getMeasures();
    var sTimes = "";
    for ( var key in measures ) {
        sTimes += "," + key + ":" + measures[key];
    }

    if ( sTimes ) {
        // strip the leading ","
        sTimes = sTimes.substring(1);

        img = new Image();
        img.src = url + "?ets=" + sTimes;
        return img.src;
    }

    return "";
}

// Use various techniques to determine the time at which this page started.
EPISODES.findStartTime = function() {
    var aCookies = document.cookie.split(' ');
    for ( var i = 0; i < aCookies.length; i++ ) {
        if ( 0 === aCookies[i].indexOf("EPISODES=") ) {
            var aSubCookies = aCookies[i].substring("EPISODES=".length).split('&');
            var startTime, bReferrerMatch;
            for ( var j = 0; j < aSubCookies.length; j++ ) {
                if ( 0 === aSubCookies[j].indexOf("s=") ) {
                    startTime = aSubCookies[j].substring(2);
                }
                else if ( 0 === aSubCookies[j].indexOf("r=") ) {
                    var startPage = aSubCookies[j].substring(2);
                    bReferrerMatch = ( escape(document.referrer) == startPage );
                }
            }
            if ( bReferrerMatch && startTime ) {
                window.postMessage("EPISODES:mark:starttime:" + startTime, "http://stevesouders.com");
            }
        }
    }
}

// Set a cookie when the page unloads. Consume this cookie on the next page to get a "start time".
EPISODES.beforeUnload = function(e) {
    document.cookie = "EPISODES=s=" + Number(new Date()) + "&r=" + escape(document.location) + "; path=/";
}

// Wrapper for FF's window.addEventListener and IE's window.attachEvent.
EPISODES.addEventListener = function(sType, callback, bCapture) {
    if ( "undefined" != typeof(window.attachEvent) ) {
        return window.attachEvent("on" + sType, callback);
    }
    else if ( window.addEventListener ){
        return window.addEventListener(sType, callback, bCapture);
    }
}

if ( EPISODES.isCompatible() ) {
    // If this browser is NOT compatible, we call EPISODES.init at the bottom of episodes-compat.js.
    EPISODES.init();
}

///////////////////////////////////////////////////////////////////////////////////
//                                                                               //
// This JavaScript code is for browsers that don't support window.postMessage.   //
//                                                                               //
///////////////////////////////////////////////////////////////////////////////////
if (!window.postMessage) {
/*
Copyright (c) 2008, Yahoo! Inc. All rights reserved.
Code licensed under the BSD License:
http://developer.yahoo.net/yui/license.txt
version: 2.5.2
*/
if(typeof YAHOO=="undefined"||!YAHOO){var YAHOO={};}YAHOO.namespace=function(){var A=arguments,E=null,C,B,D;for(C=0;C<A.length;C=C+1){D=A[C].split(".");E=YAHOO;for(B=(D[0]=="YAHOO")?1:0;B<D.length;B=B+1){E[D[B]]=E[D[B]]||{};E=E[D[B]];}}return E;};YAHOO.log=function(D,A,C){var B=YAHOO.widget.Logger;if(B&&B.log){return B.log(D,A,C);}else{return false;}};YAHOO.register=function(A,E,D){var I=YAHOO.env.modules;if(!I[A]){I[A]={versions:[],builds:[]};}var B=I[A],H=D.version,G=D.build,F=YAHOO.env.listeners;B.name=A;B.version=H;B.build=G;B.versions.push(H);B.builds.push(G);B.mainClass=E;for(var C=0;C<F.length;C=C+1){F[C](B);}if(E){E.VERSION=H;E.BUILD=G;}else{YAHOO.log("mainClass is undefined for module "+A,"warn");}};YAHOO.env=YAHOO.env||{modules:[],listeners:[]};YAHOO.env.getVersion=function(A){return YAHOO.env.modules[A]||null;};YAHOO.env.ua=function(){var C={ie:0,opera:0,gecko:0,webkit:0,mobile:null,air:0};var B=navigator.userAgent,A;if((/KHTML/).test(B)){C.webkit=1;}A=B.match(/AppleWebKit\/([^\s]*)/);if(A&&A[1]){C.webkit=parseFloat(A[1]);if(/ Mobile\//.test(B)){C.mobile="Apple";}else{A=B.match(/NokiaN[^\/]*/);if(A){C.mobile=A[0];}}A=B.match(/AdobeAIR\/([^\s]*)/);if(A){C.air=A[0];}}if(!C.webkit){A=B.match(/Opera[\s\/]([^\s]*)/);if(A&&A[1]){C.opera=parseFloat(A[1]);A=B.match(/Opera Mini[^;]*/);if(A){C.mobile=A[0];}}else{A=B.match(/MSIE\s([^;]*)/);if(A&&A[1]){C.ie=parseFloat(A[1]);}else{A=B.match(/Gecko\/([^\s]*)/);if(A){C.gecko=1;A=B.match(/rv:([^\s\)]*)/);if(A&&A[1]){C.gecko=parseFloat(A[1]);}}}}}return C;}();(function(){YAHOO.namespace("util","widget","example");if("undefined"!==typeof YAHOO_config){var B=YAHOO_config.listener,A=YAHOO.env.listeners,D=true,C;if(B){for(C=0;C<A.length;C=C+1){if(A[C]==B){D=false;break;}}if(D){A.push(B);}}}})();YAHOO.lang=YAHOO.lang||{};(function(){var A=YAHOO.lang,C=["toString","valueOf"],B={isArray:function(D){if(D){return A.isNumber(D.length)&&A.isFunction(D.splice);}return false;},isBoolean:function(D){return typeof D==="boolean";},isFunction:function(D){return typeof D==="function";},isNull:function(D){return D===null;},isNumber:function(D){return typeof D==="number"&&isFinite(D);},isObject:function(D){return(D&&(typeof D==="object"||A.isFunction(D)))||false;},isString:function(D){return typeof D==="string";},isUndefined:function(D){return typeof D==="undefined";},_IEEnumFix:(YAHOO.env.ua.ie)?function(F,E){for(var D=0;D<C.length;D=D+1){var H=C[D],G=E[H];if(A.isFunction(G)&&G!=Object.prototype[H]){F[H]=G;}}}:function(){},extend:function(H,I,G){if(!I||!H){throw new Error("extend failed, please check that "+"all dependencies are included.");}var E=function(){};E.prototype=I.prototype;H.prototype=new E();H.prototype.constructor=H;H.superclass=I.prototype;if(I.prototype.constructor==Object.prototype.constructor){I.prototype.constructor=I;}if(G){for(var D in G){if(A.hasOwnProperty(G,D)){H.prototype[D]=G[D];}}A._IEEnumFix(H.prototype,G);}},augmentObject:function(H,G){if(!G||!H){throw new Error("Absorb failed, verify dependencies.");}var D=arguments,F,I,E=D[2];if(E&&E!==true){for(F=2;F<D.length;F=F+1){H[D[F]]=G[D[F]];}}else{for(I in G){if(E||!(I in H)){H[I]=G[I];}}A._IEEnumFix(H,G);}},augmentProto:function(G,F){if(!F||!G){throw new Error("Augment failed, verify dependencies.");}var D=[G.prototype,F.prototype];for(var E=2;E<arguments.length;E=E+1){D.push(arguments[E]);}A.augmentObject.apply(this,D);},dump:function(D,I){var F,H,K=[],L="{...}",E="f(){...}",J=", ",G=" => ";if(!A.isObject(D)){return D+"";}else{if(D instanceof Date||("nodeType" in D&&"tagName" in D)){return D;}else{if(A.isFunction(D)){return E;}}}I=(A.isNumber(I))?I:3;if(A.isArray(D)){K.push("[");for(F=0,H=D.length;F<H;F=F+1){if(A.isObject(D[F])){K.push((I>0)?A.dump(D[F],I-1):L);}else{K.push(D[F]);}K.push(J);}if(K.length>1){K.pop();}K.push("]");}else{K.push("{");for(F in D){if(A.hasOwnProperty(D,F)){K.push(F+G);if(A.isObject(D[F])){K.push((I>0)?A.dump(D[F],I-1):L);}else{K.push(D[F]);}K.push(J);}}if(K.length>1){K.pop();}K.push("}");}return K.join("");},substitute:function(S,E,L){var I,H,G,O,P,R,N=[],F,J="dump",M=" ",D="{",Q="}";for(;;){I=S.lastIndexOf(D);if(I<0){break;}H=S.indexOf(Q,I);if(I+1>=H){break;}F=S.substring(I+1,H);O=F;R=null;G=O.indexOf(M);if(G>-1){R=O.substring(G+1);O=O.substring(0,G);}P=E[O];if(L){P=L(O,P,R);}if(A.isObject(P)){if(A.isArray(P)){P=A.dump(P,parseInt(R,10));}else{R=R||"";var K=R.indexOf(J);if(K>-1){R=R.substring(4);}if(P.toString===Object.prototype.toString||K>-1){P=A.dump(P,parseInt(R,10));}else{P=P.toString();}}}else{if(!A.isString(P)&&!A.isNumber(P)){P="~-"+N.length+"-~";N[N.length]=F;}}S=S.substring(0,I)+P+S.substring(H+1);}for(I=N.length-1;I>=0;I=I-1){S=S.replace(new RegExp("~-"+I+"-~"),"{"+N[I]+"}","g");}return S;},trim:function(D){try{return D.replace(/^\s+|\s+$/g,"");}catch(E){return D;}},merge:function(){var G={},E=arguments;for(var F=0,D=E.length;F<D;F=F+1){A.augmentObject(G,E[F],true);}return G;},later:function(K,E,L,G,H){K=K||0;E=E||{};var F=L,J=G,I,D;if(A.isString(L)){F=E[L];}if(!F){throw new TypeError("method undefined");}if(!A.isArray(J)){J=[G];}I=function(){F.apply(E,J);};D=(H)?setInterval(I,K):setTimeout(I,K);return{interval:H,cancel:function(){if(this.interval){clearInterval(D);}else{clearTimeout(D);}}};},isValue:function(D){return(A.isObject(D)||A.isString(D)||A.isNumber(D)||A.isBoolean(D));}};A.hasOwnProperty=(Object.prototype.hasOwnProperty)?function(D,E){return D&&D.hasOwnProperty(E);}:function(D,E){return !A.isUndefined(D[E])&&D.constructor.prototype[E]!==D[E];};B.augmentObject(A,B,true);YAHOO.util.Lang=A;A.augment=A.augmentProto;YAHOO.augment=A.augmentProto;YAHOO.extend=A.extend;})();YAHOO.register("yahoo",YAHOO,{version:"2.5.2",build:"1076"});(function(){var B=YAHOO.util,K,I,J={},F={},M=window.document;YAHOO.env._id_counter=YAHOO.env._id_counter||0;var C=YAHOO.env.ua.opera,L=YAHOO.env.ua.webkit,A=YAHOO.env.ua.gecko,G=YAHOO.env.ua.ie;var E={HYPHEN:/(-[a-z])/i,ROOT_TAG:/^body|html$/i,OP_SCROLL:/^(?:inline|table-row)$/i};var N=function(P){if(!E.HYPHEN.test(P)){return P;}if(J[P]){return J[P];}var Q=P;while(E.HYPHEN.exec(Q)){Q=Q.replace(RegExp.$1,RegExp.$1.substr(1).toUpperCase());}J[P]=Q;return Q;};var O=function(Q){var P=F[Q];if(!P){P=new RegExp("(?:^|\\s+)"+Q+"(?:\\s+|$)");F[Q]=P;}return P;};if(M.defaultView&&M.defaultView.getComputedStyle){K=function(P,S){var R=null;if(S=="float"){S="cssFloat";}var Q=P.ownerDocument.defaultView.getComputedStyle(P,"");if(Q){R=Q[N(S)];}return P.style[S]||R;};}else{if(M.documentElement.currentStyle&&G){K=function(P,R){switch(N(R)){case"opacity":var T=100;try{T=P.filters["DXImageTransform.Microsoft.Alpha"].opacity;}catch(S){try{T=P.filters("alpha").opacity;}catch(S){}}return T/100;case"float":R="styleFloat";default:var Q=P.currentStyle?P.currentStyle[R]:null;return(P.style[R]||Q);}};}else{K=function(P,Q){return P.style[Q];};}}if(G){I=function(P,Q,R){switch(Q){case"opacity":if(YAHOO.lang.isString(P.style.filter)){P.style.filter="alpha(opacity="+R*100+")";if(!P.currentStyle||!P.currentStyle.hasLayout){P.style.zoom=1;}}break;case"float":Q="styleFloat";default:P.style[Q]=R;}};}else{I=function(P,Q,R){if(Q=="float"){Q="cssFloat";}P.style[Q]=R;};}var D=function(P,Q){return P&&P.nodeType==1&&(!Q||Q(P));};YAHOO.util.Dom={get:function(R){if(R&&(R.nodeType||R.item)){return R;}if(YAHOO.lang.isString(R)||!R){return M.getElementById(R);}if(R.length!==undefined){var S=[];for(var Q=0,P=R.length;Q<P;++Q){S[S.length]=B.Dom.get(R[Q]);}return S;}return R;},getStyle:function(P,R){R=N(R);var Q=function(S){return K(S,R);};return B.Dom.batch(P,Q,B.Dom,true);},setStyle:function(P,R,S){R=N(R);var Q=function(T){I(T,R,S);};B.Dom.batch(P,Q,B.Dom,true);},getXY:function(P){var Q=function(R){if((R.parentNode===null||R.offsetParent===null||this.getStyle(R,"display")=="none")&&R!=R.ownerDocument.body){return false;}return H(R);};return B.Dom.batch(P,Q,B.Dom,true);},getX:function(P){var Q=function(R){return B.Dom.getXY(R)[0];};return B.Dom.batch(P,Q,B.Dom,true);},getY:function(P){var Q=function(R){return B.Dom.getXY(R)[1];};return B.Dom.batch(P,Q,B.Dom,true);},setXY:function(P,S,R){var Q=function(V){var U=this.getStyle(V,"position");if(U=="static"){this.setStyle(V,"position","relative");U="relative";}var X=this.getXY(V);if(X===false){return false;}var W=[parseInt(this.getStyle(V,"left"),10),parseInt(this.getStyle(V,"top"),10)];if(isNaN(W[0])){W[0]=(U=="relative")?0:V.offsetLeft;}if(isNaN(W[1])){W[1]=(U=="relative")?0:V.offsetTop;}if(S[0]!==null){V.style.left=S[0]-X[0]+W[0]+"px";}if(S[1]!==null){V.style.top=S[1]-X[1]+W[1]+"px";}if(!R){var T=this.getXY(V);if((S[0]!==null&&T[0]!=S[0])||(S[1]!==null&&T[1]!=S[1])){this.setXY(V,S,true);}}};B.Dom.batch(P,Q,B.Dom,true);},setX:function(Q,P){B.Dom.setXY(Q,[P,null]);},setY:function(P,Q){B.Dom.setXY(P,[null,Q]);},getRegion:function(P){var Q=function(R){if((R.parentNode===null||R.offsetParent===null||this.getStyle(R,"display")=="none")&&R!=R.ownerDocument.body){return false;}var S=B.Region.getRegion(R);return S;};return B.Dom.batch(P,Q,B.Dom,true);},getClientWidth:function(){return B.Dom.getViewportWidth();},getClientHeight:function(){return B.Dom.getViewportHeight();},getElementsByClassName:function(T,X,U,V){X=X||"*";U=(U)?B.Dom.get(U):null||M;if(!U){return[];}var Q=[],P=U.getElementsByTagName(X),W=O(T);for(var R=0,S=P.length;R<S;++R){if(W.test(P[R].className)){Q[Q.length]=P[R];if(V){V.call(P[R],P[R]);}}}return Q;},hasClass:function(R,Q){var P=O(Q);var S=function(T){return P.test(T.className);};return B.Dom.batch(R,S,B.Dom,true);},addClass:function(Q,P){var R=function(S){if(this.hasClass(S,P)){return false;}S.className=YAHOO.lang.trim([S.className,P].join(" "));return true;};return B.Dom.batch(Q,R,B.Dom,true);},removeClass:function(R,Q){var P=O(Q);var S=function(T){if(!Q||!this.hasClass(T,Q)){return false;}var U=T.className;T.className=U.replace(P," ");if(this.hasClass(T,Q)){this.removeClass(T,Q);}T.className=YAHOO.lang.trim(T.className);return true;};return B.Dom.batch(R,S,B.Dom,true);},replaceClass:function(S,Q,P){if(!P||Q===P){return false;}var R=O(Q);var T=function(U){if(!this.hasClass(U,Q)){this.addClass(U,P);return true;}U.className=U.className.replace(R," "+P+" ");if(this.hasClass(U,Q)){this.replaceClass(U,Q,P);}U.className=YAHOO.lang.trim(U.className);return true;};return B.Dom.batch(S,T,B.Dom,true);},generateId:function(P,R){R=R||"yui-gen";var Q=function(S){if(S&&S.id){return S.id;}var T=R+YAHOO.env._id_counter++;if(S){S.id=T;}return T;};return B.Dom.batch(P,Q,B.Dom,true)||Q.apply(B.Dom,arguments);},isAncestor:function(P,Q){P=B.Dom.get(P);Q=B.Dom.get(Q);if(!P||!Q){return false;}if(P.contains&&Q.nodeType&&!L){return P.contains(Q);}else{if(P.compareDocumentPosition&&Q.nodeType){return !!(P.compareDocumentPosition(Q)&16);}else{if(Q.nodeType){return !!this.getAncestorBy(Q,function(R){return R==P;});}}}return false;},inDocument:function(P){return this.isAncestor(M.documentElement,P);},getElementsBy:function(W,Q,R,T){Q=Q||"*";R=(R)?B.Dom.get(R):null||M;if(!R){return[];}var S=[],V=R.getElementsByTagName(Q);for(var U=0,P=V.length;U<P;++U){if(W(V[U])){S[S.length]=V[U];if(T){T(V[U]);}}}return S;},batch:function(T,W,V,R){T=(T&&(T.tagName||T.item))?T:B.Dom.get(T);if(!T||!W){return false;}var S=(R)?V:window;if(T.tagName||T.length===undefined){return W.call(S,T,V);}var U=[];for(var Q=0,P=T.length;Q<P;++Q){U[U.length]=W.call(S,T[Q],V);}return U;},getDocumentHeight:function(){var Q=(M.compatMode!="CSS1Compat")?M.body.scrollHeight:M.documentElement.scrollHeight;var P=Math.max(Q,B.Dom.getViewportHeight());return P;},getDocumentWidth:function(){var Q=(M.compatMode!="CSS1Compat")?M.body.scrollWidth:M.documentElement.scrollWidth;var P=Math.max(Q,B.Dom.getViewportWidth());return P;},getViewportHeight:function(){var P=self.innerHeight;
var Q=M.compatMode;if((Q||G)&&!C){P=(Q=="CSS1Compat")?M.documentElement.clientHeight:M.body.clientHeight;}return P;},getViewportWidth:function(){var P=self.innerWidth;var Q=M.compatMode;if(Q||G){P=(Q=="CSS1Compat")?M.documentElement.clientWidth:M.body.clientWidth;}return P;},getAncestorBy:function(P,Q){while(P=P.parentNode){if(D(P,Q)){return P;}}return null;},getAncestorByClassName:function(Q,P){Q=B.Dom.get(Q);if(!Q){return null;}var R=function(S){return B.Dom.hasClass(S,P);};return B.Dom.getAncestorBy(Q,R);},getAncestorByTagName:function(Q,P){Q=B.Dom.get(Q);if(!Q){return null;}var R=function(S){return S.tagName&&S.tagName.toUpperCase()==P.toUpperCase();};return B.Dom.getAncestorBy(Q,R);},getPreviousSiblingBy:function(P,Q){while(P){P=P.previousSibling;if(D(P,Q)){return P;}}return null;},getPreviousSibling:function(P){P=B.Dom.get(P);if(!P){return null;}return B.Dom.getPreviousSiblingBy(P);},getNextSiblingBy:function(P,Q){while(P){P=P.nextSibling;if(D(P,Q)){return P;}}return null;},getNextSibling:function(P){P=B.Dom.get(P);if(!P){return null;}return B.Dom.getNextSiblingBy(P);},getFirstChildBy:function(P,R){var Q=(D(P.firstChild,R))?P.firstChild:null;return Q||B.Dom.getNextSiblingBy(P.firstChild,R);},getFirstChild:function(P,Q){P=B.Dom.get(P);if(!P){return null;}return B.Dom.getFirstChildBy(P);},getLastChildBy:function(P,R){if(!P){return null;}var Q=(D(P.lastChild,R))?P.lastChild:null;return Q||B.Dom.getPreviousSiblingBy(P.lastChild,R);},getLastChild:function(P){P=B.Dom.get(P);return B.Dom.getLastChildBy(P);},getChildrenBy:function(Q,S){var R=B.Dom.getFirstChildBy(Q,S);var P=R?[R]:[];B.Dom.getNextSiblingBy(R,function(T){if(!S||S(T)){P[P.length]=T;}return false;});return P;},getChildren:function(P){P=B.Dom.get(P);if(!P){}return B.Dom.getChildrenBy(P);},getDocumentScrollLeft:function(P){P=P||M;return Math.max(P.documentElement.scrollLeft,P.body.scrollLeft);},getDocumentScrollTop:function(P){P=P||M;return Math.max(P.documentElement.scrollTop,P.body.scrollTop);},insertBefore:function(Q,P){Q=B.Dom.get(Q);P=B.Dom.get(P);if(!Q||!P||!P.parentNode){return null;}return P.parentNode.insertBefore(Q,P);},insertAfter:function(Q,P){Q=B.Dom.get(Q);P=B.Dom.get(P);if(!Q||!P||!P.parentNode){return null;}if(P.nextSibling){return P.parentNode.insertBefore(Q,P.nextSibling);}else{return P.parentNode.appendChild(Q);}},getClientRegion:function(){var R=B.Dom.getDocumentScrollTop(),Q=B.Dom.getDocumentScrollLeft(),S=B.Dom.getViewportWidth()+Q,P=B.Dom.getViewportHeight()+R;return new B.Region(R,S,P,Q);}};var H=function(){if(M.documentElement.getBoundingClientRect){return function(Q){var R=Q.getBoundingClientRect();var P=Q.ownerDocument;return[R.left+B.Dom.getDocumentScrollLeft(P),R.top+B.Dom.getDocumentScrollTop(P)];};}else{return function(R){var S=[R.offsetLeft,R.offsetTop];var Q=R.offsetParent;var P=(L&&B.Dom.getStyle(R,"position")=="absolute"&&R.offsetParent==R.ownerDocument.body);if(Q!=R){while(Q){S[0]+=Q.offsetLeft;S[1]+=Q.offsetTop;if(!P&&L&&B.Dom.getStyle(Q,"position")=="absolute"){P=true;}Q=Q.offsetParent;}}if(P){S[0]-=R.ownerDocument.body.offsetLeft;S[1]-=R.ownerDocument.body.offsetTop;}Q=R.parentNode;while(Q.tagName&&!E.ROOT_TAG.test(Q.tagName)){if(Q.scrollTop||Q.scrollLeft){if(!E.OP_SCROLL.test(B.Dom.getStyle(Q,"display"))){if(!C||B.Dom.getStyle(Q,"overflow")!=="visible"){S[0]-=Q.scrollLeft;S[1]-=Q.scrollTop;}}}Q=Q.parentNode;}return S;};}}();})();YAHOO.util.Region=function(C,D,A,B){this.top=C;this[1]=C;this.right=D;this.bottom=A;this.left=B;this[0]=B;};YAHOO.util.Region.prototype.contains=function(A){return(A.left>=this.left&&A.right<=this.right&&A.top>=this.top&&A.bottom<=this.bottom);};YAHOO.util.Region.prototype.getArea=function(){return((this.bottom-this.top)*(this.right-this.left));};YAHOO.util.Region.prototype.intersect=function(E){var C=Math.max(this.top,E.top);var D=Math.min(this.right,E.right);var A=Math.min(this.bottom,E.bottom);var B=Math.max(this.left,E.left);if(A>=C&&D>=B){return new YAHOO.util.Region(C,D,A,B);}else{return null;}};YAHOO.util.Region.prototype.union=function(E){var C=Math.min(this.top,E.top);var D=Math.max(this.right,E.right);var A=Math.max(this.bottom,E.bottom);var B=Math.min(this.left,E.left);return new YAHOO.util.Region(C,D,A,B);};YAHOO.util.Region.prototype.toString=function(){return("Region {"+"top: "+this.top+", right: "+this.right+", bottom: "+this.bottom+", left: "+this.left+"}");};YAHOO.util.Region.getRegion=function(D){var F=YAHOO.util.Dom.getXY(D);var C=F[1];var E=F[0]+D.offsetWidth;var A=F[1]+D.offsetHeight;var B=F[0];return new YAHOO.util.Region(C,E,A,B);};YAHOO.util.Point=function(A,B){if(YAHOO.lang.isArray(A)){B=A[1];A=A[0];}this.x=this.right=this.left=this[0]=A;this.y=this.top=this.bottom=this[1]=B;};YAHOO.util.Point.prototype=new YAHOO.util.Region();YAHOO.register("dom",YAHOO.util.Dom,{version:"2.5.2",build:"1076"});YAHOO.util.CustomEvent=function(D,B,C,A){this.type=D;this.scope=B||window;this.silent=C;this.signature=A||YAHOO.util.CustomEvent.LIST;this.subscribers=[];if(!this.silent){}var E="_YUICEOnSubscribe";if(D!==E){this.subscribeEvent=new YAHOO.util.CustomEvent(E,this,true);}this.lastError=null;};YAHOO.util.CustomEvent.LIST=0;YAHOO.util.CustomEvent.FLAT=1;YAHOO.util.CustomEvent.prototype={subscribe:function(B,C,A){if(!B){throw new Error("Invalid callback for subscriber to '"+this.type+"'");}if(this.subscribeEvent){this.subscribeEvent.fire(B,C,A);}this.subscribers.push(new YAHOO.util.Subscriber(B,C,A));},unsubscribe:function(D,F){if(!D){return this.unsubscribeAll();}var E=false;for(var B=0,A=this.subscribers.length;B<A;++B){var C=this.subscribers[B];if(C&&C.contains(D,F)){this._delete(B);E=true;}}return E;},fire:function(){this.lastError=null;var K=[],E=this.subscribers.length;if(!E&&this.silent){return true;}var I=[].slice.call(arguments,0),G=true,D,J=false;if(!this.silent){}var C=this.subscribers.slice(),A=YAHOO.util.Event.throwErrors;for(D=0;D<E;++D){var M=C[D];if(!M){J=true;}else{if(!this.silent){}var L=M.getScope(this.scope);if(this.signature==YAHOO.util.CustomEvent.FLAT){var B=null;if(I.length>0){B=I[0];}try{G=M.fn.call(L,B,M.obj);}catch(F){this.lastError=F;if(A){throw F;}}}else{try{G=M.fn.call(L,this.type,I,M.obj);}catch(H){this.lastError=H;if(A){throw H;}}}if(false===G){if(!this.silent){}break;}}}return(G!==false);},unsubscribeAll:function(){for(var A=this.subscribers.length-1;A>-1;A--){this._delete(A);}this.subscribers=[];return A;},_delete:function(A){var B=this.subscribers[A];if(B){delete B.fn;delete B.obj;}this.subscribers.splice(A,1);},toString:function(){return"CustomEvent: "+"'"+this.type+"', "+"scope: "+this.scope;}};YAHOO.util.Subscriber=function(B,C,A){this.fn=B;this.obj=YAHOO.lang.isUndefined(C)?null:C;this.override=A;};YAHOO.util.Subscriber.prototype.getScope=function(A){if(this.override){if(this.override===true){return this.obj;}else{return this.override;}}return A;};YAHOO.util.Subscriber.prototype.contains=function(A,B){if(B){return(this.fn==A&&this.obj==B);}else{return(this.fn==A);}};YAHOO.util.Subscriber.prototype.toString=function(){return"Subscriber { obj: "+this.obj+", override: "+(this.override||"no")+" }";};if(!YAHOO.util.Event){YAHOO.util.Event=function(){var H=false;var I=[];var J=[];var G=[];var E=[];var C=0;var F=[];var B=[];var A=0;var D={63232:38,63233:40,63234:37,63235:39,63276:33,63277:34,25:9};return{POLL_RETRYS:2000,POLL_INTERVAL:20,EL:0,TYPE:1,FN:2,WFN:3,UNLOAD_OBJ:3,ADJ_SCOPE:4,OBJ:5,OVERRIDE:6,lastError:null,isSafari:YAHOO.env.ua.webkit,webkit:YAHOO.env.ua.webkit,isIE:YAHOO.env.ua.ie,_interval:null,_dri:null,DOMReady:false,throwErrors:false,startInterval:function(){if(!this._interval){var K=this;var L=function(){K._tryPreloadAttach();};this._interval=setInterval(L,this.POLL_INTERVAL);}},onAvailable:function(P,M,Q,O,N){var K=(YAHOO.lang.isString(P))?[P]:P;for(var L=0;L<K.length;L=L+1){F.push({id:K[L],fn:M,obj:Q,override:O,checkReady:N});}C=this.POLL_RETRYS;this.startInterval();},onContentReady:function(M,K,N,L){this.onAvailable(M,K,N,L,true);},onDOMReady:function(K,M,L){if(this.DOMReady){setTimeout(function(){var N=window;if(L){if(L===true){N=M;}else{N=L;}}K.call(N,"DOMReady",[],M);},0);}else{this.DOMReadyEvent.subscribe(K,M,L);}},addListener:function(M,K,V,Q,L){if(!V||!V.call){return false;}if(this._isValidCollection(M)){var W=true;for(var R=0,T=M.length;R<T;++R){W=this.on(M[R],K,V,Q,L)&&W;}return W;}else{if(YAHOO.lang.isString(M)){var P=this.getEl(M);if(P){M=P;}else{this.onAvailable(M,function(){YAHOO.util.Event.on(M,K,V,Q,L);});return true;}}}if(!M){return false;}if("unload"==K&&Q!==this){J[J.length]=[M,K,V,Q,L];return true;}var Y=M;if(L){if(L===true){Y=Q;}else{Y=L;}}var N=function(Z){return V.call(Y,YAHOO.util.Event.getEvent(Z,M),Q);};var X=[M,K,V,N,Y,Q,L];var S=I.length;I[S]=X;if(this.useLegacyEvent(M,K)){var O=this.getLegacyIndex(M,K);if(O==-1||M!=G[O][0]){O=G.length;B[M.id+K]=O;G[O]=[M,K,M["on"+K]];E[O]=[];M["on"+K]=function(Z){YAHOO.util.Event.fireLegacyEvent(YAHOO.util.Event.getEvent(Z),O);};}E[O].push(X);}else{try{this._simpleAdd(M,K,N,false);}catch(U){this.lastError=U;this.removeListener(M,K,V);return false;}}return true;},fireLegacyEvent:function(O,M){var Q=true,K,S,R,T,P;S=E[M].slice();for(var L=0,N=S.length;L<N;++L){R=S[L];if(R&&R[this.WFN]){T=R[this.ADJ_SCOPE];P=R[this.WFN].call(T,O);Q=(Q&&P);}}K=G[M];if(K&&K[2]){K[2](O);}return Q;},getLegacyIndex:function(L,M){var K=this.generateId(L)+M;if(typeof B[K]=="undefined"){return -1;}else{return B[K];}},useLegacyEvent:function(L,M){if(this.webkit&&("click"==M||"dblclick"==M)){var K=parseInt(this.webkit,10);if(!isNaN(K)&&K<418){return true;}}return false;},removeListener:function(L,K,T){var O,R,V;if(typeof L=="string"){L=this.getEl(L);}else{if(this._isValidCollection(L)){var U=true;for(O=L.length-1;O>-1;O--){U=(this.removeListener(L[O],K,T)&&U);}return U;}}if(!T||!T.call){return this.purgeElement(L,false,K);}if("unload"==K){for(O=J.length-1;O>-1;O--){V=J[O];if(V&&V[0]==L&&V[1]==K&&V[2]==T){J.splice(O,1);return true;}}return false;}var P=null;var Q=arguments[3];if("undefined"===typeof Q){Q=this._getCacheIndex(L,K,T);}if(Q>=0){P=I[Q];}if(!L||!P){return false;}if(this.useLegacyEvent(L,K)){var N=this.getLegacyIndex(L,K);var M=E[N];if(M){for(O=0,R=M.length;O<R;++O){V=M[O];if(V&&V[this.EL]==L&&V[this.TYPE]==K&&V[this.FN]==T){M.splice(O,1);break;}}}}else{try{this._simpleRemove(L,K,P[this.WFN],false);}catch(S){this.lastError=S;return false;}}delete I[Q][this.WFN];delete I[Q][this.FN];I.splice(Q,1);return true;},getTarget:function(M,L){var K=M.target||M.srcElement;return this.resolveTextNode(K);},resolveTextNode:function(L){try{if(L&&3==L.nodeType){return L.parentNode;}}catch(K){}return L;},getPageX:function(L){var K=L.pageX;if(!K&&0!==K){K=L.clientX||0;if(this.isIE){K+=this._getScrollLeft();}}return K;},getPageY:function(K){var L=K.pageY;if(!L&&0!==L){L=K.clientY||0;if(this.isIE){L+=this._getScrollTop();}}return L;
},getXY:function(K){return[this.getPageX(K),this.getPageY(K)];},getRelatedTarget:function(L){var K=L.relatedTarget;if(!K){if(L.type=="mouseout"){K=L.toElement;}else{if(L.type=="mouseover"){K=L.fromElement;}}}return this.resolveTextNode(K);},getTime:function(M){if(!M.time){var L=new Date().getTime();try{M.time=L;}catch(K){this.lastError=K;return L;}}return M.time;},stopEvent:function(K){this.stopPropagation(K);this.preventDefault(K);},stopPropagation:function(K){if(K.stopPropagation){K.stopPropagation();}else{K.cancelBubble=true;}},preventDefault:function(K){if(K.preventDefault){K.preventDefault();}else{K.returnValue=false;}},getEvent:function(M,K){var L=M||window.event;if(!L){var N=this.getEvent.caller;while(N){L=N.arguments[0];if(L&&Event==L.constructor){break;}N=N.caller;}}return L;},getCharCode:function(L){var K=L.keyCode||L.charCode||0;if(YAHOO.env.ua.webkit&&(K in D)){K=D[K];}return K;},_getCacheIndex:function(O,P,N){for(var M=0,L=I.length;M<L;M=M+1){var K=I[M];if(K&&K[this.FN]==N&&K[this.EL]==O&&K[this.TYPE]==P){return M;}}return -1;},generateId:function(K){var L=K.id;if(!L){L="yuievtautoid-"+A;++A;K.id=L;}return L;},_isValidCollection:function(L){try{return(L&&typeof L!=="string"&&L.length&&!L.tagName&&!L.alert&&typeof L[0]!=="undefined");}catch(K){return false;}},elCache:{},getEl:function(K){return(typeof K==="string")?document.getElementById(K):K;},clearCache:function(){},DOMReadyEvent:new YAHOO.util.CustomEvent("DOMReady",this),_load:function(L){if(!H){H=true;var K=YAHOO.util.Event;K._ready();K._tryPreloadAttach();}},_ready:function(L){var K=YAHOO.util.Event;if(!K.DOMReady){K.DOMReady=true;K.DOMReadyEvent.fire();K._simpleRemove(document,"DOMContentLoaded",K._ready);}},_tryPreloadAttach:function(){if(F.length===0){C=0;clearInterval(this._interval);this._interval=null;return ;}if(this.locked){return ;}if(this.isIE){if(!this.DOMReady){this.startInterval();return ;}}this.locked=true;var Q=!H;if(!Q){Q=(C>0&&F.length>0);}var P=[];var R=function(T,U){var S=T;if(U.override){if(U.override===true){S=U.obj;}else{S=U.override;}}U.fn.call(S,U.obj);};var L,K,O,N,M=[];for(L=0,K=F.length;L<K;L=L+1){O=F[L];if(O){N=this.getEl(O.id);if(N){if(O.checkReady){if(H||N.nextSibling||!Q){M.push(O);F[L]=null;}}else{R(N,O);F[L]=null;}}else{P.push(O);}}}for(L=0,K=M.length;L<K;L=L+1){O=M[L];R(this.getEl(O.id),O);}C--;if(Q){for(L=F.length-1;L>-1;L--){O=F[L];if(!O||!O.id){F.splice(L,1);}}this.startInterval();}else{clearInterval(this._interval);this._interval=null;}this.locked=false;},purgeElement:function(O,P,R){var M=(YAHOO.lang.isString(O))?this.getEl(O):O;var Q=this.getListeners(M,R),N,K;if(Q){for(N=Q.length-1;N>-1;N--){var L=Q[N];this.removeListener(M,L.type,L.fn);}}if(P&&M&&M.childNodes){for(N=0,K=M.childNodes.length;N<K;++N){this.purgeElement(M.childNodes[N],P,R);}}},getListeners:function(M,K){var P=[],L;if(!K){L=[I,J];}else{if(K==="unload"){L=[J];}else{L=[I];}}var R=(YAHOO.lang.isString(M))?this.getEl(M):M;for(var O=0;O<L.length;O=O+1){var T=L[O];if(T){for(var Q=0,S=T.length;Q<S;++Q){var N=T[Q];if(N&&N[this.EL]===R&&(!K||K===N[this.TYPE])){P.push({type:N[this.TYPE],fn:N[this.FN],obj:N[this.OBJ],adjust:N[this.OVERRIDE],scope:N[this.ADJ_SCOPE],index:Q});}}}}return(P.length)?P:null;},_unload:function(Q){var K=YAHOO.util.Event,N,M,L,P,O,R=J.slice();for(N=0,P=J.length;N<P;++N){L=R[N];if(L){var S=window;if(L[K.ADJ_SCOPE]){if(L[K.ADJ_SCOPE]===true){S=L[K.UNLOAD_OBJ];}else{S=L[K.ADJ_SCOPE];}}L[K.FN].call(S,K.getEvent(Q,L[K.EL]),L[K.UNLOAD_OBJ]);R[N]=null;L=null;S=null;}}J=null;if(I){for(M=I.length-1;M>-1;M--){L=I[M];if(L){K.removeListener(L[K.EL],L[K.TYPE],L[K.FN],M);}}L=null;}G=null;K._simpleRemove(window,"unload",K._unload);},_getScrollLeft:function(){return this._getScroll()[1];},_getScrollTop:function(){return this._getScroll()[0];},_getScroll:function(){var K=document.documentElement,L=document.body;if(K&&(K.scrollTop||K.scrollLeft)){return[K.scrollTop,K.scrollLeft];}else{if(L){return[L.scrollTop,L.scrollLeft];}else{return[0,0];}}},regCE:function(){},_simpleAdd:function(){if(window.addEventListener){return function(M,N,L,K){M.addEventListener(N,L,(K));};}else{if(window.attachEvent){return function(M,N,L,K){M.attachEvent("on"+N,L);};}else{return function(){};}}}(),_simpleRemove:function(){if(window.removeEventListener){return function(M,N,L,K){M.removeEventListener(N,L,(K));};}else{if(window.detachEvent){return function(L,M,K){L.detachEvent("on"+M,K);};}else{return function(){};}}}()};}();(function(){var EU=YAHOO.util.Event;EU.on=EU.addListener;
/* DOMReady: based on work by: Dean Edwards/John Resig/Matthias Miller */
if(EU.isIE){YAHOO.util.Event.onDOMReady(YAHOO.util.Event._tryPreloadAttach,YAHOO.util.Event,true);var n=document.createElement("p");EU._dri=setInterval(function(){try{n.doScroll("left");clearInterval(EU._dri);EU._dri=null;EU._ready();n=null;}catch(ex){}},EU.POLL_INTERVAL);}else{if(EU.webkit&&EU.webkit<525){EU._dri=setInterval(function(){var rs=document.readyState;if("loaded"==rs||"complete"==rs){clearInterval(EU._dri);EU._dri=null;EU._ready();}},EU.POLL_INTERVAL);}else{EU._simpleAdd(document,"DOMContentLoaded",EU._ready);}}EU._simpleAdd(window,"load",EU._load);EU._simpleAdd(window,"unload",EU._unload);EU._tryPreloadAttach();})();}YAHOO.util.EventProvider=function(){};YAHOO.util.EventProvider.prototype={__yui_events:null,__yui_subscribers:null,subscribe:function(A,C,F,E){this.__yui_events=this.__yui_events||{};var D=this.__yui_events[A];if(D){D.subscribe(C,F,E);}else{this.__yui_subscribers=this.__yui_subscribers||{};var B=this.__yui_subscribers;if(!B[A]){B[A]=[];}B[A].push({fn:C,obj:F,override:E});}},unsubscribe:function(C,E,G){this.__yui_events=this.__yui_events||{};var A=this.__yui_events;if(C){var F=A[C];if(F){return F.unsubscribe(E,G);}}else{var B=true;for(var D in A){if(YAHOO.lang.hasOwnProperty(A,D)){B=B&&A[D].unsubscribe(E,G);}}return B;}return false;},unsubscribeAll:function(A){return this.unsubscribe(A);},createEvent:function(G,D){this.__yui_events=this.__yui_events||{};var A=D||{};var I=this.__yui_events;
if(I[G]){}else{var H=A.scope||this;var E=(A.silent);var B=new YAHOO.util.CustomEvent(G,H,E,YAHOO.util.CustomEvent.FLAT);I[G]=B;if(A.onSubscribeCallback){B.subscribeEvent.subscribe(A.onSubscribeCallback);}this.__yui_subscribers=this.__yui_subscribers||{};var F=this.__yui_subscribers[G];if(F){for(var C=0;C<F.length;++C){B.subscribe(F[C].fn,F[C].obj,F[C].override);}}}return I[G];},fireEvent:function(E,D,A,C){this.__yui_events=this.__yui_events||{};var G=this.__yui_events[E];if(!G){return null;}var B=[];for(var F=1;F<arguments.length;++F){B.push(arguments[F]);}return G.fire.apply(G,B);},hasEvent:function(A){if(this.__yui_events){if(this.__yui_events[A]){return true;}}return false;}};YAHOO.util.KeyListener=function(A,F,B,C){if(!A){}else{if(!F){}else{if(!B){}}}if(!C){C=YAHOO.util.KeyListener.KEYDOWN;}var D=new YAHOO.util.CustomEvent("keyPressed");this.enabledEvent=new YAHOO.util.CustomEvent("enabled");this.disabledEvent=new YAHOO.util.CustomEvent("disabled");if(typeof A=="string"){A=document.getElementById(A);}if(typeof B=="function"){D.subscribe(B);}else{D.subscribe(B.fn,B.scope,B.correctScope);}function E(J,I){if(!F.shift){F.shift=false;}if(!F.alt){F.alt=false;}if(!F.ctrl){F.ctrl=false;}if(J.shiftKey==F.shift&&J.altKey==F.alt&&J.ctrlKey==F.ctrl){var G;if(F.keys instanceof Array){for(var H=0;H<F.keys.length;H++){G=F.keys[H];if(G==J.charCode){D.fire(J.charCode,J);break;}else{if(G==J.keyCode){D.fire(J.keyCode,J);break;}}}}else{G=F.keys;if(G==J.charCode){D.fire(J.charCode,J);}else{if(G==J.keyCode){D.fire(J.keyCode,J);}}}}}this.enable=function(){if(!this.enabled){YAHOO.util.Event.addListener(A,C,E);this.enabledEvent.fire(F);}this.enabled=true;};this.disable=function(){if(this.enabled){YAHOO.util.Event.removeListener(A,C,E);this.disabledEvent.fire(F);}this.enabled=false;};this.toString=function(){return"KeyListener ["+F.keys+"] "+A.tagName+(A.id?"["+A.id+"]":"");};};YAHOO.util.KeyListener.KEYDOWN="keydown";YAHOO.util.KeyListener.KEYUP="keyup";YAHOO.util.KeyListener.KEY={ALT:18,BACK_SPACE:8,CAPS_LOCK:20,CONTROL:17,DELETE:46,DOWN:40,END:35,ENTER:13,ESCAPE:27,HOME:36,LEFT:37,META:224,NUM_LOCK:144,PAGE_DOWN:34,PAGE_UP:33,PAUSE:19,PRINTSCREEN:44,RIGHT:39,SCROLL_LOCK:145,SHIFT:16,SPACE:32,TAB:9,UP:38};YAHOO.register("event",YAHOO.util.Event,{version:"2.5.2",build:"1076"});YAHOO.register("yahoo-dom-event", YAHOO, {version: "2.5.2", build: "1076"});

var EPISODES = EPISODES || {};

// Create a custom event for browsers that don't support window.postMessage.
EPISODES.postMessageCustomEvent = new YAHOO.util.CustomEvent("postMessageCustomEvent");

// Define window.postMessage using the custom event.
window.postMessage = function(message, domain) { EPISODES.postMessageCustomEvent.fire({data: message, domain: domain}); };

// Redefine window.addEventListenter to support the "message" type.
if ( window.addEventListener ) {
    // Make window.addEventListener recognize the "message" event type.
    window.addEventListenerOrig = window.addEventListener; // save the default definition of addEventListener
    window.addEventListener = function(type, listener, useCapture) {
        if ( "message" != type ) { 
            window.addEventListenerOrig(type, listener, useCapture); 
        }
        else {
            // need to send this to our custom event
            EPISODES.postMessageCustomEvent.subscribe( function(type, args) { listener.call(undefined, args[0]); } );
        }
    }
}
else if ( window.attachEvent ) {
    // Make window.attachEvent recognize the "message" event type.
    window.attachEventOrig = window.attachEvent; // save the default definition of attachEvent
    window.attachEvent = function(type, listener) {
        if ( "onmessage" != type ) { 
            window.attachEventOrig(type, listener); 
        }
        else {
            // need to send this to our custom event
            EPISODES.postMessageCustomEvent.subscribe( function(type, args) { listener.call(undefined, args[0]); } );
        }
    }
}

EPISODES.init();
}

window.episodesDefaultLoad = true;
var _oldLoad = null;

if (window.onload) {
    _oldLoad = window.onload;
}
window.onload = function() {

    window.postMessage("EPISODES:measure:frontend:firstbyte", "*");
    if (_oldLoad) {_oldLoad();}
    if (window.episodesDefaultLoad === true) {

        window.postMessage("EPISODES:done", "*");
    }
};
//If the owner of the web page chooses to do something with the final results,
//they do that by listening for the "EPISODES:done" message.
function handleEpisodeResults(event) {
    if ( "EPISODES:done" === event.data ) {

        window.postMessage("EPISODES:measure:totaltime:starttime", "*");

        var data = JSON.stringify({
                                   starts : EPISODES.getStarts(),
                                   uri : window.location.href,
                                   measures : EPISODES.getMeasures() });
        protorabbit.doAjax({
            method : "POST",
            url : "prt?command=recordProfile",
            content : { 
               timestamp : serverTimestamp,
               timeshift : timeshift,
               data : data
            }
        });
    }
}

EPISODES.addEventListener("message", handleEpisodeResults, false);

/*
http://www.JSON.org/json2.js
2009-04-16
*/

if (!this.JSON) {
JSON = {};
}
(function () {

function f(n) {
    // Format integers to have at least two digits.
    return n < 10 ? '0' + n : n;
}

if (typeof Date.prototype.toJSON !== 'function') {

    Date.prototype.toJSON = function (key) {

        return this.getUTCFullYear()   + '-' +
             f(this.getUTCMonth() + 1) + '-' +
             f(this.getUTCDate())      + 'T' +
             f(this.getUTCHours())     + ':' +
             f(this.getUTCMinutes())   + ':' +
             f(this.getUTCSeconds())   + 'Z';
    };

    String.prototype.toJSON =
    Number.prototype.toJSON =
    Boolean.prototype.toJSON = function (key) {
        return this.valueOf();
    };
}

var cx = /[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
    escapable = /[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,
    gap,
    indent,
    meta = {    // table of character substitutions
        '\b': '\\b',
        '\t': '\\t',
        '\n': '\\n',
        '\f': '\\f',
        '\r': '\\r',
        '"' : '\\"',
        '\\': '\\\\'
    },
    rep;


function quote(string) {

//If the string contains no control characters, no quote characters, and no
//backslash characters, then we can safely slap some quotes around it.
//Otherwise we must also replace the offending characters with safe escape
//sequences.

    escapable.lastIndex = 0;
    return escapable.test(string) ?
        '"' + string.replace(escapable, function (a) {
            var c = meta[a];
            return typeof c === 'string' ? c :
                '\\u' + ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
        }) + '"' :
        '"' + string + '"';
}


function str(key, holder) {

//Produce a string from holder[key].

    var i,          // The loop counter.
        k,          // The member key.
        v,          // The member value.
        length,
        mind = gap,
        partial,
        value = holder[key];

//If the value has a toJSON method, call it to obtain a replacement value.

    if (value && typeof value === 'object' &&
            typeof value.toJSON === 'function') {
        value = value.toJSON(key);
    }

//If we were called with a replacer function, then call the replacer to
//obtain a replacement value.

    if (typeof rep === 'function') {
        value = rep.call(holder, key, value);
    }

//What happens next depends on the value's type.

    switch (typeof value) {
    case 'string':
        return quote(value);

    case 'number':

//JSON numbers must be finite. Encode non-finite numbers as null.

        return isFinite(value) ? String(value) : 'null';

    case 'boolean':
    case 'null':

//If the value is a boolean or null, convert it to a string. Note:
//typeof null does not produce 'null'. The case is included here in
//the remote chance that this gets fixed someday.

        return String(value);

//If the type is 'object', we might be dealing with an object or an array or
//null.

    case 'object':

//Due to a specification blunder in ECMAScript, typeof null is 'object',
//so watch out for that case.

        if (!value) {
            return 'null';
        }

//Make an array to hold the partial results of stringifying this object value.

        gap += indent;
        partial = [];

//Is the value an array?

        if (Object.prototype.toString.apply(value) === '[object Array]') {

//The value is an array. Stringify every element. Use null as a placeholder
//for non-JSON values.

            length = value.length;
            for (i = 0; i < length; i += 1) {
                partial[i] = str(i, value) || 'null';
            }

//Join all of the elements together, separated with commas, and wrap them in
//brackets.

            v = partial.length === 0 ? '[]' :
                gap ? '[\n' + gap +
                        partial.join(',\n' + gap) + '\n' +
                            mind + ']' :
                      '[' + partial.join(',') + ']';
            gap = mind;
            return v;
        }

//If the replacer is an array, use it to select the members to be stringified.

        if (rep && typeof rep === 'object') {
            length = rep.length;
            for (i = 0; i < length; i += 1) {
                k = rep[i];
                if (typeof k === 'string') {
                    v = str(k, value);
                    if (v) {
                        partial.push(quote(k) + (gap ? ': ' : ':') + v);
                    }
                }
            }
        } else {

//Otherwise, iterate through all of the keys in the object.

            for (k in value) {
                if (Object.hasOwnProperty.call(value, k)) {
                    v = str(k, value);
                    if (v) {
                        partial.push(quote(k) + (gap ? ': ' : ':') + v);
                    }
                }
            }
        }

//Join all of the member texts together, separated with commas,
//and wrap them in braces.

        v = partial.length === 0 ? '{}' :
            gap ? '{\n' + gap + partial.join(',\n' + gap) + '\n' +
                    mind + '}' : '{' + partial.join(',') + '}';
        gap = mind;
        return v;
    }
}

//If the JSON object does not yet have a stringify method, give it one.

if (typeof JSON.stringify !== 'function') {
    JSON.stringify = function (value, replacer, space) {

//The stringify method takes a value and an optional replacer, and an optional
//space parameter, and returns a JSON text. The replacer can be a function
//that can replace values, or an array of strings that will select the keys.
//A default replacer method can be provided. Use of the space parameter can
//produce text that is more easily readable.

        var i;
        gap = '';
        indent = '';

//If the space parameter is a number, make an indent string containing that
//many spaces.

        if (typeof space === 'number') {
            for (i = 0; i < space; i += 1) {
                indent += ' ';
            }

//If the space parameter is a string, it will be used as the indent string.

        } else if (typeof space === 'string') {
            indent = space;
        }

//If there is a replacer, it must be a function or an array.
//Otherwise, throw an error.

        rep = replacer;
        if (replacer && typeof replacer !== 'function' &&
                (typeof replacer !== 'object' ||
                 typeof replacer.length !== 'number')) {
            throw new Error('JSON.stringify');
        }

//Make a fake root object containing our value under the key of ''.
//Return the result of stringifying the value.

        return str('', {'': value});
    };
}


//If the JSON object does not yet have a parse method, give it one.

if (typeof JSON.parse !== 'function') {
    JSON.parse = function (text, reviver) {

//The parse method takes a text and an optional reviver function, and returns
//a JavaScript value if the text is a valid JSON text.

        var j;

        function walk(holder, key) {

//The walk method is used to recursively walk the resulting structure so
//that modifications can be made.

            var k, v, value = holder[key];
            if (value && typeof value === 'object') {
                for (k in value) {
                    if (Object.hasOwnProperty.call(value, k)) {
                        v = walk(value, k);
                        if (v !== undefined) {
                            value[k] = v;
                        } else {
                            delete value[k];
                        }
                    }
                }
            }
            return reviver.call(holder, key, value);
        }


//Parsing happens in four stages. In the first stage, we replace certain
//Unicode characters with escape sequences. JavaScript handles many characters
//incorrectly, either silently deleting them, or treating them as line endings.

        cx.lastIndex = 0;
        if (cx.test(text)) {
            text = text.replace(cx, function (a) {
                return '\\u' +
                    ('0000' + a.charCodeAt(0).toString(16)).slice(-4);
            });
        }

//In the second stage, we run the text against regular expressions that look
//for non-JSON patterns. We are especially concerned with '()' and 'new'
//because they can cause invocation, and '=' because it can cause mutation.
//But just to be safe, we want to reject all unexpected forms.

//We split the second stage into 4 regexp operations in order to work around
//crippling inefficiencies in IE's and Safari's regexp engines. First we
//replace the JSON backslash pairs with '@' (a non-JSON character). Second, we
//replace all simple value tokens with ']' characters. Third, we delete all
//open brackets that follow a colon or comma or that begin the text. Finally,
//we look to see that the remaining characters are only whitespace or ']' or
//',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.

        if (/^[\],:{}\s]*$/.
test(text.replace(/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g, '@').
replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {

//In the third stage we use the eval function to compile the text into a
//JavaScript structure. The '{' operator is subject to a syntactic ambiguity
//in JavaScript: it can begin a block or an object literal. We wrap the text
//in parens to eliminate the ambiguity.

            j = eval('(' + text + ')');

//In the optional fourth stage, we recursively walk the new structure, passing
//each name/value pair to a reviver function for possible transformation.

            return typeof reviver === 'function' ?
                walk({'': j}, '') : j;
        }

//If the text is not JSON parseable, then a SyntaxError is thrown.

        throw new SyntaxError('JSON.parse');
    };
}
}());

