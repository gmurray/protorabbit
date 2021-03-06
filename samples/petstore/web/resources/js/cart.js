function renderCart(items) {
    var items = items;

    var cartDiv = document.getElementById("cart");
 
    if (items.length > 0) {
        var table = new window.tablebuilder();
        
        table.setHeader(['', 'Item', 'Quantity', 'Price', '']);
    
        for (var i=0; i < items.length; i+=1) {
            table.addRow([ '<img src="' + items[i].image + '"/>',
                           items[i].label,
                           items[i].quantity,
                           formatCurency(items[i].price),
                           "<form method='post' action='controller?command=removeItem'><input type='hidden' name='id' value='" + items[i].id + "'/><input type='submit' value='Remove from Cart'/></form>" ]);
        }
    
        cartDiv.innerHTML = table.toString();
        // show the checkout link
        $("#checkout").css("display", "block");
    } else {
        cartDiv.innerHTML = "Your cart is empty";
    }
}

if (window.onload) {
    var oldOnload = window.onload;
}
// load the catalog for the given id
window.onload = function() {
    if (oldOnload) {
        oldOnload();
    }
    var categoryId = getURLParam("id");
    $.ajaxSetup({ cache: false });
    jQuery.getJSON( "controller?id=cart", function(data) {
                          renderCart(data);
                      });
};