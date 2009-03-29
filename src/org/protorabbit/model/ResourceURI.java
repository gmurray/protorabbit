package org.protorabbit.model;

public class ResourceURI {

	public static final int SCRIPT = 1;
	public static final int LINK = 2;
	public static final int TEMPLATE = 3;
	
	private String uri = null;
    private String baseURI = null;
    
    private boolean isExternal = false;    
    private boolean written = false;
    private boolean combine = false;
    private boolean gzip = false;
    private int type = -1;
    private String id = null;
    private String mediaType = "";

    public ResourceURI(String uri, int type, boolean isExternal) {
    	this.uri = uri;
    	this.type = type;
    	this.isExternal = isExternal;
    }
    
    public ResourceURI(String uri, String baseURI, int type) {
    	this.uri = uri;
    	this.baseURI = baseURI;
    	this.type = type;
    }
    
    public boolean isWritten() {
    	return written;
    }
    
    public void setWritten(boolean written) {
    	this.written = written;
    }
    
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getBaseURI() {
		return baseURI;
	}
	public void setbaseURI(String baseURI) {
		this.baseURI = baseURI;
	}
	public boolean isCombine() {
		return combine;
	}
	public void setCombine(boolean combine) {
		this.combine = combine;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}

	public void setExternal(boolean isExternal) {
		this.isExternal = isExternal;
	}

	public boolean isExternal() {
		return isExternal;
	}

	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}

	public boolean isGzip() {
		return gzip;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		if (id == null) {
			return uri;
		} else {
		    return id;
		}
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public String getMediaType() {
		return mediaType;
	}

	public String getFullURI() {
		return baseURI + uri;
	}

    
}