jmaki.namespace("jmaki.extensions.jmaki.lightbox");

jmaki.extensions.jmaki.lightbox.Extension = function(wargs) {

    var _widget = this;
    _widget.baseZIndex = 10000;
    _widget.containerOffset = 85;

    jmaki.loadStyle(wargs.widgetDir + "styles.css");
    _widget.lightboxes = {};
    var publish = "/jmaki/lightbox";
    var subscribe = ["/jmaki/lightbox"];

    if (wargs.publish) {
        publish = wargs.publish;
    }

    var themes = wargs.themes || {
          kame : 'green',
          ocean : 'blue'
    };

    var currentTheme;
    if (wargs.args && wargs.args.theme) {
        currentTheme = wargs.args.theme;
    } else {
        currentTheme = themes['ocean'];
    }
    if (jmaki.config && jmaki.config.globalTheme)  {
        if (themes[jmaki.config.globalTheme]) {
            currentTheme = themes[jmaki.config.globalTheme];
        }
    }
    if (wargs.args && wargs.args.theme) {
        currentTheme = wargs.args.theme;
    }
    function doSubscribe(topic, handler) {
        var i = jmaki.subscribe(topic, handler);
        _widget.subs.push(i);
    }

    this.postLoad = function() {
        if (wargs.subscribe){
            if (typeof wargs.subscribe == "string") {
                subscribe = [];
                subscribe.push(wargs.subscribe);
            } else {
                subscribe = wargs.subscribe;
            }
        }          
        _widget.subs = [];

        for (var _i=0; _i < subscribe.length; _i+=1) {
            doSubscribe(subscribe[_i]  + "/addLightbox", _widget.addLightbox);
            doSubscribe(subscribe[_i]  + "/hide", _widget.hideLightbox);
            doSubscribe(subscribe[_i]  + "/show", _widget.showLightbox);
        }
    };

    function getMousePos(e) {
        var lx = 0;
        var ly = 0;
        if (!e) {
            e = window.event;
        }
        if (e.pageX || e.pageY) {
            lx = e.pageX;
            ly = e.pageY;

        } else if (e.clientX || e.clientY) {
            lx = e.clientX;
            ly = e.clientY;
        }
        // calculate scroll offsetsets
        if (jmaki.MSIE) {
            ly += (document.documentElement.scrollTop) ?
                    document.documentElement.scrollTop :
                    document.body.scrollTop;
            lx += (document.documentElement.scrollLeft) ?
                    document.documentElement.scrollLeft :
                        document.body.scrollLeft;
        }
        return {x:lx,y:ly};
    }

    this.addLightbox= function(o, _callback) {
        _widget.dim = _widget.getWindowDimensions();
        var i;
        if (o.message) {
            o = o.message;
        } else {
            i = o; 
        }
        if (i.value) {
            i = i.value;
        }
        if (!i.id) {
            i.id = jmaki.genId();
        }
        if (typeof o.onresize === 'function') {
            i.lonresize = o.onresize;
        }
        if (typeof o.onfocus === 'function') {
            i.customOnFocus = o.onfocus;
        }
        if (typeof o.onhide === 'function'){
            i.customOnHide = o.onhide;
        }
        // handle as a dialog where user is given control
        if (i.showCloseButton == 'undefined') {
            i.showCloseButton = true;
        }
        if (i.modal && i.modal == true) {
            i.resizable = false;
        } else if (typeof i.resizable == 'undefined') {
            i.resizable = true;
        }
        i.node = document.createElement("div");
        if (i.startWidth) {
            i.node.style.width = i.startWidth + "px"; 
        }

        if (i.startHeight) {
            i.node.style.height = i.startHeight + "px";
            i.node.startHeight = i.startHeight;
        }

        i.node.className = "jmk-lightbox";
        i.node.style.visibility = "hidden";
        var titleBar = document.createElement("div");
        titleBar.className = "jmk-lightbox-titlebar jmk-lightbox-titlebar-" + currentTheme;

        i.titleNode = document.createElement("div");
        titleBar.appendChild(i.titleNode);

        i.titleNode.className = "jmk-lightbox-title";
        i.titleNode.innerHTML = i.label;
        i.node.appendChild(titleBar);
        // create the icons
        var icons = document.createElement("div");
        icons.className = "jmk-lightbox-icons";
        titleBar.appendChild(icons);

        i.setTitle = function(nTitle) {
            i.titleNode.innerHTML = nTitle;
        };

        i.setContent = function(_c) {
            i.contentNode.innerHTML = _c;
        };

        if (!i.modal === true) {
            var close = document.createElement("a");
            close.className = "jmk-lightbox-close";
            close.innerHTML = "[x]";
            close.frameId = i.id;
            close.onclick = function(e) {
                var _t;
                if (!e) {
                    _t = window.event.srcElement;
                } else {
                    _t = e.target;
                }
                if (_t.frameId){
                    _widget.hideLightbox(_t.frameId);
                }
            };
            icons.appendChild(close);
        }

        if (i.content || i.include) {
            i.contentNode = document.createElement("div");
            var _overflowX = i.overflowX;
            var _overflowY = i.overflowY;
            var _overflow = i.overflow;
            var dargs = {target:  i.contentNode,
                    useIframe : i.iframe,
                    overflow : _overflow,
                    overflowY : _overflowY,
                    overflowX : _overflowX,
                    content : i.content,
                    autosize : i.resizable};
            if (i.startHeight) {
                dargs.startHeight = i.startHeight - _widget.containerOffset;
            }
            i.dcontainer = new jmaki.DContainer(dargs);

        } else if (i.widget) {
            i.contentNode = document.createElement("div");
            var _w = document.createElement("div");
            i.contentNode.appendChild(_w);
            var wf = jmaki.getExtension("widgetFactory");
            wf.loadWidget(
                { widget : i.widget,
                  container :_w
                });
        } else {
           jmaki.log("Could not create Lightbox. Need a content, widget, or include");
           return null;
        }

        i.contentNode.style.clear = "both";

        i.node.appendChild(i.contentNode);
        if (i.showCloseButton) {
            var closeDiv = document.createElement("div");
            closeDiv.className = "jmk-lightbox-close-buttons";
            var closeButton = document.createElement("input");
            closeButton.type = "button";
            closeButton.onclick = function() {
                if (i.modal) {
                    _widget.disableBlocker();
                }
                i.hide.apply();
            };
            closeButton.value = "Close";
            closeDiv.appendChild(closeButton);
            i.node.appendChild(closeDiv);
        }

        i.shadow = document.createElement("div");
        i.shadow.className = "jmk-lightbox-shadow";

        if (typeof i.status != 'undefined') {
            i.statusDiv = document.createElement("div");
            i.statusDiv.className = "jmk-lightbox-status";
            i.statusDiv.innerHTML = i.status;
            i.node.appendChild(i.statusDiv);
        }

        if (i.include && i.dcontainer) {
            i.dcontainer.loadURL(i.include, _callback);
        }

        i.isVisible = function() {
            return (i.node.style.display === "block");
        };

        i.destroy = function() {

            i.hide();
            if (i.resizable === true) {
                if (typeof document.attachEvent != 'undefined') {
                    document.detachEvent("onmousemove", mouseMove);
                } else {
                    document.removeEventListener("mousemove", mouseMove, true);
                }
            }
            if (i.modal === false || i.resizable === true) {
                if (typeof document.detachEvent != 'undefined') {
                    document.detachEvent("onmouseup", dragStop);
                    window.detachEvent("onmouseup", dragStop);
                } else {
                    document.removeEventListener("mouseup", dragStop, true);
                }
            }
            jmaki.clearWidgets(i.node);
            i.shadow.parentNode.removeChild(i.shadow);
            i.node.parentNode.removeChild(i.node);

        };

        i.hide = function(){
            _widget.disableBlocker(i.id);
            i.node.style.display = "none";
            i.shadow.style.display = "none";
            jmaki.publish(publish + "/lightboxHidden", { targetId : i.id});
            if (typeof i.customOnHide === "function") {
                i.customOnHide.apply({},[]);
            }
        };

        i.show = function(){
            if (i.modal) {
                _widget.enableBlocker();
            }

            i.node.style.display = "block";
            if (i.resizable === true) {
                i.dcontainer.setSize( { w :  i.node.clientWidth });
            }

            i.shadow.style.height = i.node.clientHeight + "px";
            i.shadow.style.width = i.node.clientWidth + "px";

            i.shadow.style.display = "block";
            i.lfocus();
        };

        function dragStop(e) {

            if (i.hasFocus !== true) {
                return;
            }

            if (i.dragStart !== null) {
                var dim =  { w : i.node.clientWidth, h : i.node.clientHeight };
                jmaki.publish(publish + "/resizeComplete",
                              { id: i.id,
                                lightbox : dim,
                                container : { h : dim.h - _widget.containerOffset, w : dim.w}
                             });
                if (i.dcontainer && i.resizable) {
                    i.dcontainer.setSize({ h : dim.h - _widget.containerOffset, w : dim.w});
                }
            }
            i.dragStart = null;
            i.dragAnchor = null;
            i.isMoveEvent = false;

            if (!i.modal === true) {
                _widget.disableBlocker();
           }
        }

        function dragStart(e, moveEvent) {
             if ( i.dragStart !== null) {
                 dragStop(e);
             }
            _widget.dim = _widget.getWindowDimensions();
            if (!i.modal) {
                _widget.enableBlocker(0);
            }
            // reset the flag of whether this is a move / resize event
            // and set it to what was passed in (if passed in);
            i.isMoveEvent = false;
            if (typeof moveEvent == "boolean") {
                i.isMoveEvent = moveEvent;
            }

            i.dragAnchor = jmaki.getPosition(i.node);
            i.dragStart = getMousePos(e);

            // this is the offset between the mouse click and the position of the container
            // used when the window is dragged
            i.dragOffset = { x : (i.dragStart.x - i.dragAnchor.x),
                             y : (i.dragStart.y - i.dragAnchor.y) };

            if (e) {
                e.preventDefault = true;
            }

            return true;
        }

        i.resized = function(offset) {
                // if not provided we just want to make sure everyone got notified
                if (!offset) {
                    offset = {
                            w : i.node.clientWidth,
                            h : i.node.clientHeight
                    };
                }
                if (offset.w > 0) {
                    i.node.style.width = offset.w + "px";
                    i.shadow.style.width = offset.w + "px";
                }
                if (offset.h > 0) {
                    i.node.style.height = offset.h + "px";
                    i.shadow.style.height = offset.h + "px";
                }
                // resize the dcontainer content
                i.dcontainer.setSize( { h : offset.h - _widget.containerOffset});
                // call any resize handlers
                if (i.lonresize) {
                    i.lonresize( { h : offset.h - _widget.containerOffset, w : offset.w});
                }
        };

        i.dragStart = null;
        // make it resizable
        if (i.resizable === true) {
            i.dragger = document.createElement("div");
            i.dragger.className = "jmk-lightbox-dragger";
            i.node.appendChild(i.dragger);
            i.dragger.style.background = "url(" + wargs.widgetDir + "images/resize.gif" + ") left -10px";
            i.dragger.style.width = "15px";
            i.dragger.style.height = "15px";
            // drag done
             i.dragger.onmousedown = dragStart;
             i.dragger.onmouseup = dragStop;
             i.dragger.onclick = dragStop;

        }
        if (i.modal !== true || i.resizable === true) {
            // attach listeners to doc
                if (typeof document.attachEvent != 'undefined') {
                    document.attachEvent("onmousemove", mouseMove);
                } else {
                    document.addEventListener("mousemove",mouseMove, true);
                }
                if (typeof document.attachEvent != 'undefined') {
                    document.attachEvent("onmouseup", dragStop);
                } else {
                    document.addEventListener("mouseup", dragStop, true);
                }
        }
        if (i.modal !== true) {
            titleBar.onmousedown = function(e) {
                _widget.defocusAll();
                i.node.style.zIndex = _widget.baseZIndex + 5;
                i.shadow.style.zIndex = _widget.baseZIndex + 4;
                return dragStart(e, true);
            };
        }

        function getOffset(e) {
            if (i.dragAnchor && 
                i.dragStart !== null) {

              var pos = getMousePos(e);

              // TODO handle negative
              var  w = pos.x - i.dragAnchor.x;
              var h =  pos.y - i.dragAnchor.y;

              return  { w : w, h : h, mouse : pos };

            } else {
                return null;
            }
        }

        // resize handler for the lightbox
        function mouseMove(e) {

            var offset = getOffset(e);

            if (offset !== null) {
                if (i.isMoveEvent === true) {

                    var _x = offset.mouse.x - i.dragOffset.x;
                    var _y = offset.mouse.y - i.dragOffset.y;
                    if (_y < 15 || _x <=0) {
                        return;
                    }
                    // move the frame
                    i.node.style.left =  _x + "px";
                    i.node.style.top = _y + "px";

                    // move the shadow
                    i.shadow.style.left = (_x + 10) + "px";
                    i.shadow.style.top = (_y + 10) + "px";

                } else {
                    if (i.resizable === true) {
                        jmaki.publish(publish + "/resize",
                                { id: i.id,
                                  lightbox : offset,
                                  container : { h : offset.h - _widget.containerOffset, w : offset.w}
                         });
                         i.resized(offset);
                    }
                }
            }
            if (e) {
                  e.preventDefault = true;
            }
            return true;
        }
        // bring the current frame into focus
        i.lfocus = function(e) {

            // prevent focusing if we already have it or if dragging
            if (i.hasFocus === true ) {
                return;
            }
            _widget.defocusAll();
            i.hasFocus = true;
            i.node.style.zIndex = _widget.baseZIndex + 5;
            i.shadow.style.zIndex = _widget.baseZIndex + 4;
            if (typeof i.customOnFocus == 'function' && i.dragStart === null) {
                i.customOnFocus.apply({}, [e]);
            }
            if (e) {
                e.preventDefault = true;
            }
            return false;
        };

        i.node.onmousedown = i.lfocus;
        document.body.appendChild(i.shadow);
        document.body.appendChild(i.node);
        _widget.lightboxes[i.id] = i;

        setOpacity(i.shadow, 15);

        // position the box
        var _t =  o.top || (_widget.dim .h/2) - (i.node.clientHeight/2) + _widget.dim .scrollY;
        if (_t < 1) {
            _t = 1;
        }

        var _left = (o.left || ((_widget.dim .w/2) - (i.node.clientWidth /2) + _widget.dim .scrollX));

        i.node.style.top = _t + "px";
        i.node.style.left = _left + "px";
        i.node.style.visibility = "visible";
        i.shadow.style.top = (_t + 10) + "px";
        i.shadow.style.left = (_left + 10) + "px";

        i.shadow.style.height = i.node.clientHeight + "px";
        i.shadow.style.width = i.node.clientWidth + "px";
        i.show.apply({},[]);
        if (i.content && i.dcontainer && typeof _callback === "function") {
           _callback.apply({}, [i]);
        }
        i.hasFocus = true;
        return i;
    };

    this.removeLightbox = function(o) {
        var targetId;
        if (o.message) {
            o = o.message;
        }
        if (o.targetId) {
            targetId = o.targetId;
        } else { 
            targetId = o;
        }
        var _f = _widget.lightboxes[targetId];
        if (_f) {
            _f.destroy();
            delete _widget.lightboxes[targetId];
        }
    };

    this.clearAll = function(_exception) {
        for (var i in _widget.lightboxes) {
            if ( _widget.lightboxes.hasOwnProperty(i) && i !== _exception) {
                _widget.removeLightbox(i);
            }
        }
    };

    this.hideLightbox = function(o) {
        var targetId;
        if (o.message) {
            o = o.message;
        }
        if (o.targetId) {
            targetId = o.targetId;
        } else {
            targetId = o;
        }
        var _f = _widget.lightboxes[targetId];
        if (_f) {
            _f.hide.apply({});
        }
    };

    function setOpacity(target, opacity) {
        target.style.opacity = opacity / 100;
        target.style.filter = "alpha(opacity='" + opacity + "')";
    }

    this.enableBlocker = function(opacity) {
       _widget.dim = _widget.getWindowDimensions();
       if (typeof opacity == 'undefined') {
           opacity = 50;
       }
       if (!_widget.blocker) {
           _widget.blocker = document.createElement("div");
           _widget.blocker.id = wargs.uuid + "_blocker";
           _widget.blocker.style.background = "#000";
           _widget.blocker.style.position = "absolute";
           _widget.blocker.style.left ="0px";
           _widget.blocker.style.top  = "0px";
           document.body.appendChild(_widget.blocker);
       }
       setOpacity(_widget.blocker, opacity);

       var _h = _widget.dim.h;
       var _w = _widget.dim.w;
       // account for scrollbars
       if (_widget.dim.scrollbarsY) {
           _w -= 15;
           // windows sidebars are 17px
           if (/Windows/i.test(navigator.userAgent)) {
               _w -= 2;
           }
       }
       if (_widget.dim.h > _h) {
           _h = _widget.dim.h;
       }
       _widget.blocker.style.width = _w + "px";
       _widget.blocker.style.height = _h + _widget.dim.scrollY + "px";
       _widget.blocker.style.zIndex = _widget.baseZIndex + 3;
       _widget.blocker.style.display = "block";
    };

    this.disableBlocker = function(sourceId) {
        // only disable blocker if there aren't other visible modals
        var _currentModals = [];
        for (var i in _widget.lightboxes) {
            if (_widget.lightboxes[i].hasOwnProperty('modal')) {
                _widget.lightboxes[i].modal === true;
                if (i !== sourceId && _widget.lightboxes[i].isVisible() === true) {
                    _currentModals.push(i);
                }
            }
        }
        if (_widget.blocker && _currentModals.length === 0) {
            _widget.blocker.style.display = "none";
        }
    };

    /*
     *   Hide all of the lightboxes
     */
    this.hideAll = function() {
        for (var i in _widget.lightboxes) {
            if (_widget.lightboxes[i].hasOwnProperty('hide')) {
                _widget.lightboxes[i].hide.apply({});
            }
        }
    };

    this.defocusAll = function() {
        for (var j in _widget.lightboxes) {
            var lb = _widget.lightboxes[j];
            if (lb && lb.hasOwnProperty('node') && lb.modal !== true) {
                lb.node.style.zIndex =  _widget.baseZIndex + 1;
                lb.shadow.style.zIndex =  _widget.baseZIndex;
                lb.hasFocus = false;
            }
        }
    };

    this.showLightbox = function(o) {
        var targetId;
        if (o.message) {
            o = o.message;
        }
        if (o.targetId) {
            targetId = o.targetId;
        }
            else {
                targetId = o;
        }
        var _f = _widget.lightboxes[targetId];
        if (_f) {
            if (_widget.modal) {
                _widget.enableBlocker();
            }
            _f.show.apply({});
        }
    };

    /**
     * Return the dimensions and the region of the page scrolled to.
    */  
    this.getWindowDimensions = function() {
        var _w = 0;
        var _h = 0;
        var _sx = 0;
        var _sy = 0;
        var _sh = 0;
        var _docHeight;
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
        if ( (window.scrollMaxY && window.scrollMaxY > 0 ||
              (document.body.scrollHeight !== document.body.offsetHeight &&
               document.body.scrollHeight <= document.body.offsetHeight + 16))) {

            if (!document.body.offsetHeight) {
                _sh = 13;
            }
            vscrollbars = true;
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
        return {w : _w, h: _h, docHeight :_docHeight,
                scrollX : _sx, scrollY : _sy, scrollbarsY : vscrollbars, scrollHeight : _sh };
    };
};
