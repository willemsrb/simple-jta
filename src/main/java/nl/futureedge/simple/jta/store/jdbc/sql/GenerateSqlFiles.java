package nl.futureedge.simple.jta.store.jdbc.sql;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class GenerateSqlFiles {

    public static final Map<String, JdbcSqlTemplate> TEMPLATES = new HashMap<>();

    static {
        TEMPLATES.put("default", new DefaultSqlTemplate());
        TEMPLATES.put("hsqldb", new HsqldbSqlTemplate());
        TEMPLATES.put("mysql", new MysqlSqlTemplate());
        TEMPLATES.put("postgresql", new PostgresqlSqlTemplate());
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("One and only argument should contain a directory name");
        }

        final File directory = new File(args[0]);
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException("Argument should contain a directory name");
            }
        } else {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException("Could not create directory");
            }
        }

        TEMPLATES.forEach((code, template) -> {
            try (final PrintWriter writer = new PrintWriter(new FileWriter(new File(directory, code + ".sql")))) {
                writer.println(template.createTransactionIdSequence());
                writer.println(template.createTransactionTable());
                writer.println(template.createResourceTable());
            } catch (final IOException e) {
                throw new IllegalArgumentException("Could not write SQL to file", e);
            }
        });
    }

}
