package nl.futureedge.simple.jta.store.jdbc.sql;

/*-
 * #%L
 * Simple JTA
 * %%
 * Copyright (C) 2017 Future Edge IT
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public final class GenerateSqlFiles {

    private static final Map<String, JdbcSqlTemplate> TEMPLATES = new HashMap<>();

    static {
        TEMPLATES.put("default", new DefaultSqlTemplate());
        TEMPLATES.put("hsqldb", new HsqldbSqlTemplate());
        TEMPLATES.put("mysql", new MysqlSqlTemplate());
        TEMPLATES.put("postgresql", new PostgresqlSqlTemplate());
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 1) {
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
                writer.println(template.createTransactionIdSequence() + ";");
                writer.println();
                writer.println(template.createTransactionTable() + ";");
                writer.println();
                writer.println(template.createResourceTable() + ";");
                writer.println();
            } catch (final IOException e) {
                throw new IllegalArgumentException("Could not write SQL to file", e);
            }
        });
    }

}
