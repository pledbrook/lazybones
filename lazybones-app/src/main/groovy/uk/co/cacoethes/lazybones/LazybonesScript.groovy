package uk.co.cacoethes.lazybones

import groovy.io.FileType
import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import groovy.util.logging.Log
import org.apache.commons.io.FilenameUtils
import uk.co.cacoethes.lazybones.commands.InstallationScriptExecuter
import uk.co.cacoethes.util.AntPathMatcher
import uk.co.cacoethes.util.Naming

import java.lang.reflect.Method
import java.util.logging.Logger

/**
 * Base script that will be applied to the lazybones.groovy root script in a lazybones template
 *
 * @author Tommy Barker
 */
@Log
class LazybonesScript extends Script {

    protected static final String DEFAULT_ENCODING = "utf-8"

    /**
     * The target project directory. For project templates, this will be the
     * same as the directory into which the template was installed, i.e.
     * {@link #templateDir}. But for subtemplates, this will be the directory
     * of the project that the <code>generate</code> command is running in.
     */
    File projectDir

    /**
     * The location of the unpacked template. This will be the same as
     * {@link #projectDir} for project templates, but will be different for
     * subtemplates. This is the base path used when searching for files
     * that need filtering.
     * @since 0.7
     */
    File templateDir

    /**
     * Stores the values for {@link #ask(java.lang.String, java.lang.String, java.lang.String)}
     * calls when a parameter name is specified.
     * @since 0.7
     */
    Map parentParams = [:]

    /**
     * @since 0.7
     */
    List<String> tmplQualifiers = []

    /**
     * The encoding/charset used by the files in the template. This is UTF-8
     * by default.
     */
    String fileEncoding = DEFAULT_ENCODING

    File scmExclusionsFile

    /**
     * Model params. Used to run sub-scripts.
     */
    Map<String, String> model

    /**
     * The reader stream from which user input will be pulled. Defaults to a
     * wrapper around stdin using the platform's default encoding/charset.
     */
    Reader reader

    private TemplateEngine templateEngine = new SimpleTemplateEngine()

    private final Map registeredEngines = [:]

    private final AntPathMatcher antPathMatcher =
            new AntPathMatcher(pathSeparator: System.getProperty("file.separator"))

    /**
     * Provides access to the script's logger.
     * @since 0.9
     */
    Logger getLog() { return this.log }

    /**
     * Declares the list of file patterns that should be excluded from SCM.
     * @since 0.5
     */
    void scmExclusions(String... exclusions) {
        if (!scmExclusionsFile) return

        log.fine "Writing SCM exclusions file with: ${exclusions}"
        scmExclusionsFile.withPrintWriter(fileEncoding) { writer ->
            for (String exclusion in exclusions) {
                writer.println exclusion
            }
        }
    }

    /**
     * Converts a name from one convention to another, e.g. from camel case to
     * natural form ("TestString" to "Test String").
     * @param args Both {@code from} and {@code to} arguments are required and
     * must be instances of {@link uk.co.cacoethes.util.NameType}.
     * @param name The string to convert.
     * @return The converted string, or {@code null} if the given name is {@code
     * null}, or an empty string if the given string is empty.
     * @since 0.5
     */
    String transformText(Map args, String name) {
        return Naming.convert(args, name)
    }

    /**
     * Registers a template engine against a file suffix. For example, you can
     * register a {@code HandlebarsTemplateEngine} instance against the suffix
     * "hbs" which would result in *.hbs files being processed by that engine.
     * @param suffix The file suffix, excluding the dot ('.')
     * @param engine An instance of Groovy's {@code TemplateEngine}, e.g.
     * {@code SimpleTemplateEngine}.
     * @since 0.6
     */
    void registerEngine(String suffix, TemplateEngine engine) {
        this.registeredEngines[suffix] = engine
    }

    /**
     * Registers a template engine as the default. This template engine will be
     * used by {@link #processTemplates(java.lang.String, java.util.Map)} for
     * files that don't have a template-specific suffix. The normal default is
     * an instance of Groovy's {@code SimpleTemplateEngine}.
     * @param engine An instance of Groovy's {@code TemplateEngine}, e.g.
     * {@code SimpleTemplateEngine}. Cannot be {@code null}. Use
     * {@link #clearDefaultEngine()} if you want to disable the default engine.
     * @since 0.6
     */
    void registerDefaultEngine(TemplateEngine engine) {
        this.templateEngine = engine
    }

    /**
     * Disables the default template engine. The result is that
     * {@link #processTemplates(java.lang.String, java.util.Map)} will simply
     * ignore any files that don't have a registered suffix (via
     * {@link #registerEngine(java.lang.String, groovy.text.TemplateEngine)}),
     * even if they match the given pattern.
     * @since 0.6
     */
    void clearDefaultEngine() {
        this.templateEngine = null
    }

    /**
     * Prints a message asking for a property value.  If the user has no response the default
     * value will be returned.  null can be returned
     *
     * @param message
     * @param defaultValue
     * @return the response
     * @since 0.4
     */
    String ask(String message, String defaultValue = null) {
        System.out.print message
        String line = reader.readLine()

        return line ?: defaultValue
    }

    /**
     * Include another script in the main script, and execute it in-line.
     * @param file subscript
     * @param bindings values to pass from parent script to this script. These are additional model parameters,
     * and are added to the existing parent model parameters.
     * @return the last expression evaluated in the subscript
     */
    def include(String file, Map<String, String> bindings) {
        def m  = [:]
        m << model
        m << bindings
        def executor = new InstallationScriptExecuter(null, reader)
        executor.runPostInstallScript(file, tmplQualifiers, projectDir, templateDir, m)
    }

    /**
     * <p>Prints a message asking for a property value.  If a value for the property already exists in
     * the binding of the script, it is used instead of asking the question.  If the user just presses
     * &lt;return&gt; the default value is returned.</p>
     * <p>This method also saves the value in the script's {@link #parentParams} map against the
     * <code>propertyName</code> key.</p>
     *
     * @param message The message to display to the user requesting some information.
     * @param defaultValue If the user doesn't provide a value, return this.
     * @param propertyName The name of the property in the binding whose value will
     * be used instead of prompting the user for input if that property exists.
     * @return The required value based on whether the message was displayed and
     * whether the user entered a value.
     * @since 0.4
     */
    String ask(String message, String defaultValue, String propertyName) {
        def val = propertyName && binding.hasVariable(propertyName) ?
                binding.getVariable(propertyName) :
                ask(message, defaultValue)

        parentParams[propertyName] = val
        return val
    }

    /**
     * Been deprecated as of lazybones 0.5, please use
     * {@link LazybonesScript#processTemplates(java.lang.String, java.util.Map)}
     *
     * @deprecated
     * @param filePattern
     * @param substitutionVariables
     * @since 0.4
     */
    def filterFiles(String filePattern, Map substitutionVariables) {
        String warningMessage = "The template you are using depends on a deprecated part of the API, [filterFiles], " +
                "which will be removed in Lazybones 1.0. Use a version of Lazybones prior to 0.5 with this template."
        log.warning(warningMessage)
        processTemplates(filePattern, substitutionVariables)
    }

    /**
     * @param filePattern classic ant pattern matcher
     * @param substitutionVariables model for processing the template
     * @return
     * @since 0.5
     */
    def processTemplates(String filePattern, Map substitutionVariables) {
        if (projectDir == null) throw new IllegalStateException("projectDir has not been set")
        if (templateDir == null) throw new IllegalStateException("templateDir has not been set")

        boolean atLeastOneFileFiltered = false

        if (templateEngine) {
            log.fine "Processing files matching the pattern ${filePattern} using the default template engine"
            atLeastOneFileFiltered |= processTemplatesWithEngine(
                    findFilesByPattern(filePattern),
                    substitutionVariables,
                    templateEngine,
                    true)
        }

        for (entry in registeredEngines) {
            log.fine "Processing files matching the pattern ${filePattern} using the template engine for '${entry.key}'"
            atLeastOneFileFiltered |= processTemplatesWithEngine(
                    findFilesByPattern(filePattern + '.' + entry.key),
                    substitutionVariables,
                    entry.value,
                    false)
        }

        if (!atLeastOneFileFiltered) {
            log.warning "No files filtered with file pattern [$filePattern] " +
                    "and template directory [${templateDir.path}]"
        }

        return this
    }

    /**
     * Returns a flat list of files in the target directory that match the
     * given Ant path pattern. The pattern should use forward slashes rather
     * than the platform file separator.
     */
    private List<File> findFilesByPattern(String pattern) {
        def filesToFilter = []
        def filePatternWithUserDir = new File(templateDir.canonicalFile, pattern).path

        templateDir.eachFileRecurse(FileType.FILES) { File file ->
            if (antPathMatcher.match(filePatternWithUserDir, file.canonicalPath)) {
                filesToFilter << file
            }
        }

        return filesToFilter
    }

    /**
     * Applies a specific template engine to a set of files. The files should be
     * templates of the appropriate type.
     * @param file The template files to process.
     * @param properties The model (variables and their values) for the templates.
     * @param engine The template engine to use for processing.
     * @param replace If {@code true}, replaces each source file with the text
     * generated by the processing. Otherwise, a new file is created with the same
     * name as the original, minus its final suffix (assumed to be a template-specific
     * suffix).
     * @throws IllegalArgumentException if any of the template file don't exist.
     */
    protected boolean processTemplatesWithEngine(
            Iterable<File> files,
            Map properties,
            TemplateEngine engine,
            boolean replace) {
        boolean atLeastOneFileFiltered = false

        //have to use for instead of each, closure causes issues when script is used as base script
        for (file in files) {
            processTemplateWithEngine(file, properties, engine, replace)
            atLeastOneFileFiltered = true
        }

        return atLeastOneFileFiltered
    }

    /**
     * Applies a specific template engine to a file. The file should be a
     * template of the appropriate type.
     * @param file The template file to process.
     * @param properties The model (variables and their values) for the template.
     * @param engine The template engine to use for processing.
     * @param replace If {@code true}, replaces the file with the text generated
     * by the processing. Otherwise, a new file is created with the same name as
     * the original, minus its final suffix (assumed to be a template-specific suffix).
     * @throws IllegalArgumentException if the template file doesn't exist.
     */
    protected void processTemplateWithEngine(File file, Map properties, TemplateEngine engine, boolean replace) {
        if (!file.exists()) {
            throw new IllegalArgumentException("File ${file} does not exist")
        }
        log.fine "Filtering file ${file}${replace ? ' (replacing)' : ''}"

        def template = makeTemplate(engine, file, properties)

        def targetFile = replace ? file : new File(file.parentFile, FilenameUtils.getBaseName(file.path))
        targetFile.withWriter(fileEncoding) { writer ->
            template.writeTo(writer)
        }

        if (!replace) file.delete()
    }

    @Override
    Object run() {
        throw new UnsupportedOperationException("${this.getClass().name} is not meant to be used directly. " +
                "It should instead be used as a base script")
    }

    /**
     * Determines whether the version of Lazybones loading the post-installation
     * script supports a particular feature. Current features include "ask" and
     * processTemplates for example.
     * @param featureName The name of the feature you want to check for. This should
     * be the name of a method on `LazybonesScript`.
     * @since 0.4
     */
    boolean hasFeature(String featureName) {
        return this.getClass().methods.any { Method method -> method.name == featureName }
    }

    /**
     * Returns the target project directory as a string. Only kept for backwards
     * compatibility and post-install scripts should switch to using {@link #projectDir}
     * as soon as possible.
     * @deprecated Will be removed before Lazybones 1.0
     */
    String getTargetDir() {
        log.warning "The targetDir property is deprecated and should no longer be used by post-install scripts. " +
                "Use `projectDir` instead."
        return projectDir.path
    }

    /**
     * Read-only access to the path matcher. This method seems to be required
     * for {@link #processTemplates(java.lang.String, java.util.Map)} to work
     * properly.
     */
    protected AntPathMatcher getAntPathMatcher() { return this.antPathMatcher }

    /**
     * Creates a new template instance from a file.
     * @param engine The template engine to use for parsing the file contents.
     * @param file The file to use as the content of the template.
     * @param properties The properties to populate the template with. Each key
     * in the map should correspond to a variable in the template.
     * @return The new template object
     */
    protected Writable makeTemplate(TemplateEngine engine, File file, Map properties) {
        file.withReader(fileEncoding) { Reader reader ->
            return engine.createTemplate(reader).make(properties)
        }
    }
}
