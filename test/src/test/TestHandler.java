package test;

import org.jmaki.Handler;
import org.jmaki.Message;
import org.jmaki.PubSub;
import org.json.JSONException;
import org.json.JSONObject;

public class TestHandler implements Handler {
	
	public TestHandler() {}

	public String processRequest(JSONObject jo) {
		try {
			System.out.println("Test Handler was invoked with name: " + jo.getString("name"));
			PubSub.getInstance().publish(new Message("/server/hello", "Hello " + jo.getString("name")));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

}
