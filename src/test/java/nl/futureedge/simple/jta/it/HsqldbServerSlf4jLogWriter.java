package nl.futureedge.simple.jta.it;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.function.Consumer;
import org.hsqldb.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

public class HsqldbServerSlf4jLogWriter implements FactoryBean<PrintWriter>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private String level;
    private PrintWriter printWriter;

    @Required
    public void setLevel(final String level) {
        this.level = level;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        final LoggerWriter loggerWriter;
        if ("info".equalsIgnoreCase(level)) {
            loggerWriter = new LoggerWriter(LOGGER::info);
        } else if ("warn".equalsIgnoreCase(level)) {
            loggerWriter = new LoggerWriter(LOGGER::warn);
        } else {
            throw new IllegalArgumentException("Level should be 'info' or 'warn'.");
        }
        printWriter = new PrintWriter(loggerWriter);
    }

    @Override
    public PrintWriter getObject() throws Exception {
        return printWriter;
    }

    @Override
    public Class<?> getObjectType() {
        return PrintWriter.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private static final class LoggerWriter extends Writer {

        private final StringBuilder messageBuffer = new StringBuilder();
        private final Consumer<String> logger;

        public LoggerWriter(final Consumer<String> logger) {
            this.logger = logger;
        }

        @Override
        public void close() throws IOException {
            flush();
        }

        @Override
        public void flush() throws IOException {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);

            if (message.endsWith("\n")) {
                message = message.substring(0, message.length() - 2);
            }

            logger.accept(message);
        }

        @Override
        public void write(final char[] buffer, final int offset, final int length) throws IOException {
            messageBuffer.append(buffer, offset, length);
        }

    }
}
