package org.spv.model.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.spv.Config;
import org.spv.accelerator.CombinedResourceManager;
import org.spv.model.ITemplate;
import org.spv.model.ResourceURI;

public class IncludeReferencesCommand extends BaseCommand {

	public IncludeReferencesCommand(int commandType, int start, int end, String... params) {
		super(commandType, start, end, params);
	}

	@Override
	public void doProcess(OutputStream out) throws IOException {
		if (params == null || params.length < 1) {
			System.out.println("Warning: IncludeReferences called with no parameter.");
			return;
		}
		Config cfg = ctx.getConfig();
		String target = params[0].toLowerCase();
		
		ITemplate t = cfg.getTemplate(ctx.getTemplateId());
		CombinedResourceManager crm = cfg.getCombinedResourceManager();
		
		if ("scripts".equals(target)) {
			if (t.getCombineScripts()) {
			    List<ResourceURI> scripts = t.getAllScripts();
			    crm.processScripts(scripts, ctx, out);
			} else {
				String tFile = cfg.getResourceReferences(ctx.getTemplateId(), params[0], ctx);
				out.write(tFile.getBytes());
			}
			
		} else if ("styles".equals(target)) {
			if (t.getCombineStyles()) {
			    List<ResourceURI> styles = t.getAllStyles();
			    crm.processStyles(styles, ctx, out);
			} else {
				String tFile = cfg.getResourceReferences(ctx.getTemplateId(), params[0], ctx);
				out.write(tFile.getBytes());				
			}
	
		}
		

		
		 
		
	}

}
