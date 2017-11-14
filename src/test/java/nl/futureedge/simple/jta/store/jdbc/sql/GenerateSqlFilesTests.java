package nl.futureedge.simple.jta.store.jdbc.sql;

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
