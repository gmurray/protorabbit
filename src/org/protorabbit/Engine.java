package org.protorabbit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import org.protorabbit.json.JSONUtil;
import org.protorabbit.model.ICommand;
import org.protorabbit.model.IContext;
import org.protorabbit.model.ITemplate;
import org.protorabbit.model.impl.FileSystemContext;


public class Engine {

	public static void renderTemplate(String tid, Config cfg, OutputStream out, IContext ctx) {
		
		long startTime = (new Date()).getTime();

		try {

			ctx.setTemplateId(tid);
			ITemplate template = cfg.getTemplate(tid);

			StringBuffer buff = template.getContent(ctx);
			List<ICommand> cmds = getCommands(cfg, buff, template.getJSON());
			int index = 0;			
            if (cmds != null) {
				Iterator<ICommand> it = cmds.iterator();

				while (it.hasNext()) {
					ICommand c = it.next();
					c.setContext(ctx);
	
					// output everything before the first command
					out.write(buff.substring(index, c.getStartIndex()).getBytes());
					
					try {
						c.doProcess(out);
					} catch (IOException e) {
						e.printStackTrace();
					}
	
					index = c.getEndIndex();
				}
            }
			if (buff != null) {
				out.write(buff.substring(index).getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		long stopTime = (new Date()).getTime();
		System.out.println(" Render time=" + (stopTime - startTime) + "ms");
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length < 1) {
			System.out.println("Usage: --template [template name] templateId");
			System.exit(0);
		}
		
		long startTime = (new Date()).getTime();

		ArrayList<String> cTemplates = new ArrayList<String>();
		
		String documentRoot = "";

		for (int i=0; i <= args.length - 1; i++) {
			System.out.println("Processing " + args[i]);
            if ("-template".equals(args[i])) {
            	cTemplates.add(args[i + 1]);
            	i+=1;
            } else if ("-documentRoot".equals(args[i])) {
            	documentRoot = args[i+1];
            	i+=1;       	
            }
		}
		Config cfg = new Config();	
		FileSystemContext ctx = new FileSystemContext(cfg, documentRoot);		
		
		if (cTemplates.size() == 0) {
			System.out.println("Error: You need to specify at least 1 template file.");
			System.exit(0);
		}
		Iterator<String> it = cTemplates.iterator();
		while (it.hasNext()) {
			String ctemplate = it.next();
			int lastSep = ctemplate.lastIndexOf(File.separator);
			String cBase = "";
			if (lastSep != -1) {
				cBase = ctemplate.substring(0, lastSep + 1);
			}
			try {
                JSONObject jo = JSONUtil.loadFromFile(ctemplate);
			    cfg.registerTemplates(jo.getJSONArray("templates"), cBase);
			    System.out.println("Registered template " + ctemplate + " with baseDir=" + cBase);
			} catch (Exception e) {
				
			}	
		}
		
		long postConfigTime = (new Date()).getTime();
				
		// last item is the target;
		String targetTemplate = args[args.length -1];
			
		System.out.println("** Config Processing Time : " + (postConfigTime -  startTime) + "\n\n");
		
		renderTemplate(targetTemplate, cfg, System.out, ctx);

		long stopTime = (new Date()).getTime();
		
		System.out.println("\n\nRender time for 3 templates=" + (stopTime - postConfigTime) + "ms");
	}
	
	/*
	 * A command looks like 
	 * 
	 * <% include('foo') %> or <% include('foo', 'baz', 'bin') %>
	 * <% insert('bar') %>
	 * 
	 */
	public static List<ICommand> getCommands(Config cfg, StringBuffer doc, JSONObject template) {
		
		List<ICommand> commands = new ArrayList<ICommand>();
		if (doc == null) return  null;
		int index = 0;
		int len = doc.length();
		
		while(index < len) {
			index = doc.indexOf("<%", index);
			int end = doc.indexOf("%>", index);			
			
			if (index == -1 || end == -1) {
			    break;
			}
			
			// find the full expression 
			String exp = doc.substring(index + 2, end);
			
			//find the command
		    int paramStart = exp.indexOf("(");
		    int paramEnd = exp.indexOf(")", paramStart);
		    
		    if (paramStart != -1 && paramEnd != -1 && paramEnd > paramStart) {
		       
		       // get commandType
		    	String commandTypeString = exp.substring(0,paramStart).trim();
		    	
                ICommand cmd = cfg.getCommand(commandTypeString);

                if (cmd != null) {
			    	// get the params
			    	String paramsString = exp.substring(paramStart +1, paramEnd);
			    	String[] params = paramsString.split(",");
			    	
			    	if (params != null) {
				    	// clean up the params
				    	for (int i=0; i < params.length; i++) {
				    		params[i] = params[i].trim();
				    		String[] sparams = params[i].split("\'");    		
				    		// take the middle value that was between the quotes
				    		if (sparams != null && sparams.length > 0) {
				    			params[i] = sparams[1];
				    		}
				    	}
				    	cmd.setParams(params);		    	
			    	}
	                
			    	cmd.setStartIndex(index);		    	
	                cmd.setEndIndex(end + 2);

			    	if ("include".equals(commandTypeString) && params.length > 0) {
			    		if ("scripts".equals(params[0]) ||
			    			"styles".equals(params[0])) {
			    			cmd.setType(ICommand.INCLUDE_REFERENCES);
			    		} else {
			    			cmd.setType(ICommand.INCLUDE);
			    		}
			    	} else if ("insert".equals(commandTypeString)) {
			    		cmd.setType(ICommand.INSERT);
			    	} else {
			    		cmd.setType(ICommand.CUSTOM);
			    	}
			    	commands.add(cmd);
		        }
		    }
			// start the process over
			index = end + 2;
		}

		return commands;
	}
	

}
