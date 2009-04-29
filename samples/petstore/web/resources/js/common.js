window.tablebuilder = function() {

    var _header;
    var _rows = [];

    this.setHeader = function(_headers) {
        _header = "<tr><th>" + _headers.join("</th><th>") + "</tr>";
    };

    this.addRow = function(_cells) {
        _rows.push("<tr><td>" + _cells.join("</td><td>") + "</td></tr>");
    };

    this.toString = function() {
        return  "<table>" + _header + 
          "<tbody>" + _rows.join("") + "</tbody>" +
          "</table>";
    };
};

function getURLParam(name) {
    var url = window.location.href;
   var start = url.indexOf("?");
   if ( start > 0 ) {
    var queryString = url.substr(start + 1);
    var params = queryString.split("&");
    var ex = new RegExp("^" + name + "=");
    for (var i=0; i < params.length; i+=1){
        if (ex.test(params[i])){
            var _vals =  params[i].split("=");
            if (_vals.length > 0) {
            return _vals[1];
            }
        }
    }
   }
   return null;
}

function formatCurency(price) {
    price = price.toFixed(2);
    var dollars =  Math.floor(price);
    var rprice = "$" + dollars;
    var cents = Math.floor((price - dollars) * 100);
    if (cents === 0) {
        rprice += ".00";
    } else if (cents < 10) {
        rprice += ".0" + cents;
    } else {
        rprice += "." + cents;
    }
    return rprice;
}
