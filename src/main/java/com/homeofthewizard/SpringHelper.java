package com.homeofthewizard;

import jakarta.annotation.Nonnull;
import org.apache.maven.plugin.MojoExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class SpringHelper {

    public List<? extends Class<?>> buildSpringContextAndGetBeanClasses(@Nonnull List<? extends Class<?>> configClasses, @Nonnull List<? extends Class<?>> beanClassesFilter, @Nonnull List<String> basePackages, @Nonnull File applicationPropertiesFile) throws MojoExecutionException {
        Assert.notEmpty(basePackages, """
                Base package name cannot be left empty.
                Please provide a package name to be used filtering the beans.
                This is necessary for not creating unnecessary code for objects used only internally in spring context.
                \s""");

        var applicationContext = buildSpringContext(configClasses, applicationPropertiesFile);

        return Arrays.stream(applicationContext.getBeanDefinitionNames())
                .map(applicationContext::getBean)
                .map(Object::getClass)
                .filter(beanClass -> beanClassesFilter.isEmpty() || beanClassesFilter.contains(beanClass))
                .filter(beanClass -> isPublicBean(beanClass))
                .filter(beanClass -> isPartOfLibrary(beanClass, basePackages))
                .toList();
    }

    private ApplicationContext buildSpringContext(List<? extends Class<?>> configClasses, File applicationPropertiesFile) throws MojoExecutionException {
        var applicationContext = new AnnotationConfigApplicationContext();
        if(applicationPropertiesFile.exists()) {
            var resourcePropertyFile = createResourceFromPropertyFile(applicationPropertiesFile);
            applicationContext.getEnvironment().getPropertySources().addFirst(resourcePropertyFile);
        }
        applicationContext.register(configClasses.toArray(new Class[0]));
        applicationContext.refresh();
        return applicationContext;
    }

    private ResourcePropertySource createResourceFromPropertyFile(File applicationPropertiesFile) throws MojoExecutionException {
        try {
            return new ResourcePropertySource( new FileSystemResource( applicationPropertiesFile ) );
        } catch (IOException e) {
            throw new MojoExecutionException("Application property file could not be opened",e);
        }
    }

    private boolean isPublicBean(Class<?> beanClass) {
        if(!Modifier.isPublic(beanClass.getModifiers())) return false;
        if(beanClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) return false;
        return true;
    }

    private boolean isPartOfLibrary(Class<?> beanClass, List<String> basePackages) {
        return basePackages.stream().anyMatch(bPackage -> beanClass.getPackage().toString().contains(bPackage));
    }

}
