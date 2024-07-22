package com.homeofthewizard;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Mojo( name = "generate", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.TEST)
public class ProviderGeneratorMojo extends AbstractMojo {

    private static final String GENERATION_PATH = "${project.build.directory}/generated-sources";

    private MavenProject project;
    private MavenSession session;
    private String[] componentClassesFilter = new String[0];
    private String[] contextConfigClasses;
    private File applicationPropertiesFile;
    private File outputDir;
    private final BuildPluginManager pluginManager;
    private final SpringHelper springHelper;
    private final ProviderGenerator generator;

    @Inject
    public ProviderGeneratorMojo(SpringHelper springHelper, BuildPluginManager buildPluginManager, ProviderGenerator providerGenerator){
        this.pluginManager = buildPluginManager;
        this.springHelper = springHelper;
        this.generator = providerGenerator;
    }

    @Parameter(property = "project", readonly = true)
    public void setProject(MavenProject project){
        this.project=project;
    }

    @Parameter(property = "session", readonly = true)
    public void setSession(MavenSession session){
        this.session=session;
    }

    @Parameter(property = "componentClassesFilter")
    public void setComponentClassesFilter(String[] componentClassesFilter){
        this.componentClassesFilter=componentClassesFilter;
    }

    @Parameter(required = true, property = "contextConfigClasses")
    public void setContextConfigClasses(String[] contextConfigClasses){
        this.contextConfigClasses = contextConfigClasses;
    }

    @Parameter(property = "applicationPropertiesFile", defaultValue = "${project.basedir}/src/main/resources/application.properties")
    public void setApplicationPropertiesFile(File applicationPropertiesFile){
        this.applicationPropertiesFile=applicationPropertiesFile;
    }

    @Parameter(defaultValue = GENERATION_PATH, required = true)
    public void setOutputDir(File outputDir){
        this.outputDir = outputDir;
    }

    @Override
    public void execute() throws MojoExecutionException{
        setClassPath();

        var basePackages = getSpringLibsBasePackages();
        var configClasses = parseClassNameArray(contextConfigClasses);
        var beanClassesFilter = parseClassNameArray(componentClassesFilter);

        var beanClasses = springHelper.buildSpringContextAndGetBeanClasses(configClasses, beanClassesFilter, basePackages, applicationPropertiesFile);

        generateClasses(configClasses, beanClasses);
        compileGeneratedClasses();
    }

    private void setClassPath() throws MojoExecutionException {
        try {
            var urls = new HashSet<URL>();
            List<String> elements = project.getRuntimeClasspathElements();
            elements.addAll(project.getCompileClasspathElements());
            elements.addAll(project.getTestClasspathElements());

            for (String element : elements) {
                urls.add(new File(element).toURI().toURL());
            }

            ClassLoader contextClassLoader = URLClassLoader.newInstance(
                    urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader());

            Thread.currentThread().setContextClassLoader(contextClassLoader);

        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
            throw new MojoExecutionException("Could not merge classpath of plugin and project", e);
        }
    }

    private List<String> getSpringLibsBasePackages() {
        return project.getDependencies().stream().map(Dependency::getGroupId).collect(Collectors.toList());
    }

    private List<? extends Class<?>> parseClassNameArray(String[] classes) {
        return Arrays.stream(classes).map(this::getClassFromName).collect(Collectors.toList());
    }

    private void generateClasses(List<? extends Class<?>> configClasses, List<Class<?>> beanClasses) {
        for(var bean : beanClasses){
            generator.generateBeansProviders(outputDir.toPath(), project.getGroupId(), bean);
        }
        generator.generateSpringConfig(outputDir.toPath(), project.getGroupId(), configClasses, applicationPropertiesFile);
        generator.generateSpringContextProvider(outputDir.toPath(), project.getGroupId(), configClasses);
    }

    private Class<?> getClassFromName(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void compileGeneratedClasses() throws MojoExecutionException {
        getLog().info("compiling project via mvn embedder");
        MavenCli cli = new MavenCli();
        cli.doMain(new String[] {"clean","compile"}, project.getBasedir().getAbsolutePath(), System.out, System.err);
        getLog().info("compiling project ended");
    }
}
