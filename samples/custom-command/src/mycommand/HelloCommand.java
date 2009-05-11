package mycommand;

import java.io.IOException;
import java.io.OutputStream;

import org.protorabbit.model.impl.BaseCommand;

public class HelloCommand extends BaseCommand {

    @Override
    public void doProcess(OutputStream out) throws IOException {
        
        String message = "hello ";
        
        if (params.length > 0) {
            message += params[0];
        }
        out.write(message.getBytes());
        
    }

}
