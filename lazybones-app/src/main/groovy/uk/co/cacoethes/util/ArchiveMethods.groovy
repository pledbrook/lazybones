package uk.co.cacoethes.util

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * This class contains some static utility methods defined in such a way that
 * they can be used as Groovy extension methods. The zip-related methods have
 * been shamelessly borrowed from Tim Yate's Groovy Common Extensions library
 * but modified to support retention of file permissions.
 */
@groovy.transform.CompileStatic
class ArchiveMethods {
    private static final String EXCEPTION_TEXT = "File#unzip() has to be called on a *.zip file."

    /**
     * Unzips a file to a target directory, retaining the file permissions where
     * possible. You can also provide a closure that acts as a filter, returning
     * {@code true} if you want the file or directory extracted, {@code false}
     * otherwise.
     * @param self The zip file to extract.
     * @param destination The directory to extract the zip to. Of course, it must
     * be a directory, otherwise this method throws an IllegalArgumentException.
     * @param filter (optional) A closure that acts as a filter. It must accept a
     * single argument that is a File and return {@code true} if that zip entry
     * should be extracted, or {@code false} otherwise.
     */
    static Collection<File> unzip(File self, File originalDestination, Closure<Boolean> filter = null) {
        checkUnzipFileType(self)
        checkUnzipDestination(originalDestination)

        // if destination directory is not given, we'll fall back to the parent directory of 'self'
        def destination = originalDestination ?: new File(self.parent)

        def zipFile = new ZipFile(self)

        try {
            return unpackZipEntries(zipFile, destination, filter)
        }
        finally {
            zipFile.close()
        }
    }

    protected static Collection<File> unpackZipEntries(ZipFile zipFile, File destination, Closure<Boolean> filter) {
        def unzippedFiles = []

        // The type coercion here is down to http://jira.codehaus.org/browse/GROOVY-6123
        for (ZipArchiveEntry entry in (zipFile.entries as List<ZipArchiveEntry>)) {
            final file = new File(destination, entry.name)
            if (filter == null || filter(file)) {
                if (entry.isDirectory()) {
                    file.mkdirs()
                }
                else {
                    file.parentFile?.mkdirs()

                    def output = new FileOutputStream(file)
                    output.withStream {
                        output << zipFile.getInputStream(entry)
                    }
                }

                unzippedFiles << file
                updateFilePermissions(file, entry.unixMode)
            }
        }

        return unzippedFiles
    }

    /**
     * <p>Sets appropriate Unix file permissions on a file based on a 'mode'
     * number, such as 0644 or 0755. Note that those numbers are in octal
     * format!</p>
     * <p>The left-most number represents the owner permission (1 = execute,
     * 2 = write, 4 = read, 5 = read/exec, 6 = read/write, 7 = read/write/exec).
     * The middle number represents the group permissions and the last number
     * applies to everyone. In reality, because of limitations in the underlying
     * Java API this method will only honour owner and everyone settings. The
     * group permissions will be set to the same as those for everyone.</p>
     */
    static void updateFilePermissions(File self, long unixMode) {
        self.setExecutable((unixMode & 0100) as Boolean, !(unixMode & 0001))
        self.setReadable((unixMode & 0400) as Boolean, !(unixMode & 0004))
        self.setWritable((unixMode & 0200) as Boolean, !(unixMode & 0002))
    }

    /**
     * Checks that the given file is both a file (not a directory, link, etc)
     * and that its name has a .zip extension.
     */
    private static void checkUnzipFileType(File self) {
        if (!self.isFile()) throw new IllegalArgumentException(EXCEPTION_TEXT)

        def filename = self.name
        if (!filename.toLowerCase().endsWith(".zip")) throw new IllegalArgumentException(EXCEPTION_TEXT)
    }

    /**
     * Checks that the given file is a directory.
     */
    private static void checkUnzipDestination(File file) {
        if (file && !file.isDirectory()) throw new IllegalArgumentException("'destination' has to be a directory.")
    }
}
