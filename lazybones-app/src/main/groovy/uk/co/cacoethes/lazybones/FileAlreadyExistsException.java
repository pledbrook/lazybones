package uk.co.cacoethes.lazybones;

import java.io.File;

/**
 * Created by pledbrook on 04/03/2016.
 */
public class FileAlreadyExistsException extends RuntimeException {
    private static final String BASE_MSG = "The file '#file' already exists";

    private final File file;

    public FileAlreadyExistsException(File file) {
        super(buildMessage(file));
        this.file = file;
    }

    public FileAlreadyExistsException(File file, String message) {
        super(message);
        this.file = file;
    }

    public FileAlreadyExistsException(File file, String message, Throwable cause) {
        super(message, cause);
        this.file = file;
    }

    public FileAlreadyExistsException(File file, Throwable cause) {
        super(buildMessage(file), cause);
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    private static String buildMessage(File file) {
        return BASE_MSG.replace("#file", file.getName());
    }
}
