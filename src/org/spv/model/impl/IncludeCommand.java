package org.spv.model.impl;

import java.io.IOException;
import java.io.OutputStream;

import org.spv.Config;

public class IncludeCommand extends BaseCommand {

	public IncludeCommand(int commandType, int start, int end, String... params) {
		super(commandType, start, end, params);
	}

	@Override
	public void doProcess(OutputStream out) throws IOException {
		Config cfg = ctx.getConfig();
		StringBuffer buff = cfg.getIncludeFileContent(ctx.getTemplateId(), params[0],ctx);
		// TODO allow for encodings other than the current
		if (buff != null) {
			out.write(buff.toString().getBytes());
		}
		out.flush();
	}

}
