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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class GenerateSqlFilesTests {

    @Test(expected = IllegalArgumentException.class)
    public void nullArguments() {
        GenerateSqlFiles.main(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noArguments() {
        GenerateSqlFiles.main(new String[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void notADirectory() throws Exception {
        final Path tempPath = Files.createTempFile("generate-sql-files-test", null);
        final File tempFile = tempPath.toFile();
        GenerateSqlFiles.main(new String[]{tempFile.getAbsolutePath()});
        Files.delete(tempPath);
    }

    @Test
    public void ok() throws Exception {
        final Path tempPath = Files.createTempDirectory("generate-sql-files-test");
        final File tempDirectory = tempPath.toFile();
        GenerateSqlFiles.main(new String[]{tempDirectory.getAbsolutePath()});

        for (final File sqlFile : tempDirectory.listFiles()) {
            Files.delete(sqlFile.toPath());
        }
        Files.delete(tempPath);
    }
}
