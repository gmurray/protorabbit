jmaki.namespace("jmaki.extensions.jmaki.communicator");

jmaki.extensions.jmaki.communicator.Extension = function(mixins) {
	
	var _ext = this;
	var _polltime = mixins.pollTimeout || 5000;
	
	var _home = mixins.args.server;
	
	var _subscribe = mixins.args.subscribe || [];
	
	var _publish = function(_command, args) {
		_ext.publish('publish', args);
	};

	this.publish = function(topic, args) {
		// initiate a conversation with the server
		jmaki.doAjax( {
			url : _home,
			method : 'POST',
	        content : { topic : topic, message : jmaki.json.serialize(args)},		
			callback : function(req) {
	        	jmaki.log("Server Responed with " + req.responseText)
	        }
		});	
	};

	function processMessages(messages) {
		for (var i=0; i < messages.length; i++) {
			jmaki.publish(messages[i].topic, messages[i].data);
		}
	}
	
	this.getMessages = function() {
		jmaki.doAjax( {
			url : _home,
			method : 'GET',
			callback : function(req) {
	        	var messages = jmaki.json.deserialize(req.responseText);
	        	processMessages(messages);
	        }
		});		
	}
	
	
	this.startPolling = function() {
       _ext.poller = setInterval(function() {
    	   _ext.getMessages();     	
        }, _polltime);  		
	};
	
	this.stopPolling = function() {
		if (_ext.poller) {
			clearInterval(_ext.poller); 
		}
		_ext.poller = null;
	};
	
	this.postLoad = function () {
		jmaki.log("Initialized jMaki Communicator 1.0 with server " + _home);
		
        _ext.publish('/initialize', { 
        	callback : function(req) {
				jmaki.log('Server acknowledged with : ' + req.responseText);
			}
        });
		
		// register the topics to look for
		for (var i=0; i < _subscribe.length; i++) {
			jmaki.log("Communicator : Subscribing to " + _subscribe[i]);
		    jmaki.subscribe(_subscribe[i], function(args) {
		    	// get the topic off the callee : TODO: handle the regexp
		    	var _topic = arguments.callee.arguments[0].topic;
		    	_ext.publish.apply({}, [_topic,args])});
		}

		_ext.startPolling();
	};	
	
};