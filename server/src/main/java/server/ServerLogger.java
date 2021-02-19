package server;



import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerLogger {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public ServerLogger() throws IOException {
        FileHandler fileHandler = new FileHandler("logs/log.log", true);
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);
    }

    public void logInfo(String message){
        logger.log(Level.INFO, message);
    }
    public void logExcOrError(String message) { logger.log(Level.SEVERE, message);}
    public void logExcOrError(String message, Throwable throwable) { logger.log(Level.SEVERE, message, throwable); }
}
