package org.protorabbit.model.impl;

import org.protorabbit.Config;

import java.io.IOException;
import java.io.OutputStream;

public class InsertCommand extends BaseCommand {

    @Override
    public void doProcess(OutputStream out) throws IOException {

        Config cfg = ctx.getConfig();
        String tFile = cfg.getContent(ctx.getTemplateId(), params[0], ctx);
        if (tFile != null) {
            // TODO allow for encodings other than the current
            out.write(tFile.getBytes());
        } else {
            String message = "InsertWarning: Unable find property " +  params[0];
            Config.getLogger().warning(message);
        }
    }

}
