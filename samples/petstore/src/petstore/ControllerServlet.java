package petstore;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.protorabbit.json.JSONSerializer;
import org.protorabbit.json.JSONUtil;
import org.protorabbit.json.SerializationFactory;

public class ControllerServlet extends HttpServlet {

    private static final long serialVersionUID = 5372835715115785622L;
    public static final String CART = "petstore.CART";
	private static Logger logger;

    public JSONObject catalog = null;

    public void init(ServletConfig cfg) throws ServletException{
        super.init(cfg);
        URL url = null;
        // easy solution to catalog where the catalog is maintained as a JSON Object
        try {
            url = cfg.getServletContext().getResource("/WEB-INF/catalog.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        InputStream is = null;
        if (url != null) {
            try {
                is = url.openStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            catalog = JSONUtil.loadFromInputStream(is);
        }
    }

    /**
     *  Find the catalog item for the given id in the JSON based catalog
     * @param id
     * @return
     */
    JSONObject findCatalogItem(String id) {

        if (catalog !=null) {
            Iterator<String> keys = catalog.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    JSONObject category = catalog.getJSONObject(key);
                    if (category.has("items")) {
                        JSONArray items = category.getJSONArray("items");
                        for (int i=0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            if (item.has("id") && item.getString("id").equals(id)) {
                                return item;
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        
      return null;
        
    }
    @SuppressWarnings("unchecked")
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException, javax.servlet.ServletException {

        String command = req.getParameter("command");
        String forwardPage = "/error.prt";

        HttpSession session = req.getSession(true);
        String id = req.getParameter("id");
        List<CartItem>  cartList = (List<CartItem>)session.getAttribute(CART);

        if (cartList == null) {
            cartList = new ArrayList<CartItem>();
        }

        if (command != null && id != null) {
            if ("addItem".equals(command)) {
                CartItem ci = null;
                for (CartItem lci : cartList) {
                    if (lci.getId().equals(id)) {
                        ci = lci;
                    }
                }
                if (ci != null) {
                    int quantity = ci.getQuantity();
                    ci.setQuantity(++quantity);
                } else {
                    JSONObject item = findCatalogItem(id);
                    ci = new CartItem();
                    try {
                        ci.setDescription(item.getString("description"));
                        ci.setLabel(item.getString("label"));
                        ci.setId(item.getString("id"));
                        ci.setPrice(item.getDouble("price"));
                        ci.setImage(item.getString("image"));
                        ci.setQuantity(1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    cartList.add(ci);
                }
                forwardPage = "/cart.prt";
            } else if ("removeItem".equals(command)) {
                int index = -1;
                for (int i=0; i < cartList.size(); i++) {
                    CartItem item = cartList.get(i);
                    if (item.getId().equals(id)) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    cartList.remove(index);
                }
                forwardPage = "/cart.prt";
            } else if ("checkout".equals(command)) {
                forwardPage = "/checkout.prt";
            }
            
        }
        session.setAttribute(CART, cartList);
        try {
            req.getRequestDispatcher(forwardPage).forward(req, resp);
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        if ("cart".equals(id)) {
            List<CartItem>  cartList = (List<CartItem>)req.getSession().getAttribute(CART);
            if (cartList == null) {
                cartList = new ArrayList<CartItem>();
                req.getSession().setAttribute(CART, cartList);
            }
            SerializationFactory factory = new SerializationFactory();
            JSONSerializer js = factory.getInstance();
            JSONArray jo = (JSONArray)js.serialize(cartList);
            resp.getWriter().println(jo);
        } else {
            JSONObject category = null;
            if (catalog != null && catalog.has(id)) {
                try {
                    category = catalog.getJSONObject(id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            resp.getWriter().println(category);
        }
    }

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("org.protrabbit");
        }
        return logger;
    }
}

