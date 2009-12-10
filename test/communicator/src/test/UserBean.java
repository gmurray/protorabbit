package test;

import org.jmaki.Message;
import org.jmaki.PubSub;

public class UserBean {

	protected String name = null;
	protected String phone = null;
	
	public UserBean() {}
	
	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
		System.out.println("UserBean: phone has been set to " + phone);
	}

	public void setName(String name) {
		this.name = name;
		System.out.println("UserBean: name has been set to " + name);
		PubSub.getInstance().publish(new Message("/UserBean/updated", this));		
	}
	
	public String getName() {
		return name;
	}
	
}
