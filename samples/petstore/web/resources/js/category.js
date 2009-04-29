function renderCategory(cateogry) {
    var items = cateogry.items;
    var categoryDiv = document.getElementById("category");

    var table = new window.tablebuilder();
    
    table.setHeader(['', 'Item', 'Description', 'Price', '']);

    for (var i=0; i < items.length; i+=1) {
        table.addRow([ '<img src="' + items[i].image + '"/>',
                       items[i].label,
                       items[i].description,
                       formatCurency(items[i].price),
                       "<form method='post' action='controller?command=addItem'><input type='hidden' name='id' value='" + items[i].id + "'/><input type='submit' value='Add to Cart'/></form>" ]);
    }

    categoryDiv.innerHTML = table.toString();

}


// load the catalog for the given id
window.onload = function() {

    var categoryId = getURLParam("id");
    jQuery.getJSON( "controller?id=" + categoryId, function(data) {
        renderCategory(data);
    });
};