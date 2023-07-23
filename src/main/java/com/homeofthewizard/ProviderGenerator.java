package com.homeofthewizard;

import org.burningwave.core.classes.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ProviderGenerator {

    private static final String SPRING_CONFIG_CLASSNAME = "GeneratedSpringConfig";

    public static void generateBeansProviders(Path sourcePath, String packageName, Class<?> beanClass) {
        var unitSG = UnitSourceGenerator.create(packageName);
        unitSG.addClass(
            ClassSourceGenerator.create(
                    TypeDeclarationSourceGenerator.create(beanClass.getSimpleName() + "Provider"))
                    .addModifier(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSourceGenerator.create(Named.class))
                    .addConcretizedType(TypeDeclarationSourceGenerator.create(Provider.class).addGeneric(GenericSourceGenerator.create(beanClass)))

                    .addField(VariableSourceGenerator.create(ApplicationContext.class, "applicationContext")
                            .addModifier(Modifier.PRIVATE)
                            .addModifier(Modifier.FINAL))

                    .addConstructor(FunctionSourceGenerator.create(beanClass.getSimpleName() + "Provider")
                            .addAnnotation(AnnotationSourceGenerator.create(Inject.class))
                            .addParameter(VariableSourceGenerator.create(ApplicationContext.class, "applicationContext"))
                            .addBodyCodeLine("this.applicationContext = applicationContext;"))

                    .addMethod(FunctionSourceGenerator.create("get")
                            .setReturnType(
                                    TypeDeclarationSourceGenerator.create(beanClass.getSimpleName())
                            )
                            .addModifier(Modifier.PUBLIC)
                            .addBodyCodeLine("return applicationContext.getBean(" + beanClass.getSimpleName() + ".class);")
                            .useType(beanClass)
                    )
        );
        unitSG.storeToClassPath(sourcePath.toAbsolutePath().toString());
    }

    public static void generateSpringContextProvider(Path sourcePath, String packageName, List<? extends Class<?>> componentClasses){
        var unitSG = UnitSourceGenerator.create(packageName);
        unitSG.addClass(
                ClassSourceGenerator.create(TypeDeclarationSourceGenerator.create("SpringContextProvider"))
                        .addModifier(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSourceGenerator.create(Named.class))
                        .addConcretizedType(TypeDeclarationSourceGenerator.create(Provider.class).addGeneric(GenericSourceGenerator.create(ApplicationContext.class)))
                        .addMethod(FunctionSourceGenerator.create("get")
                                .setReturnType(
                                        TypeDeclarationSourceGenerator.create(ApplicationContext.class)
                                )
                                .addModifier(Modifier.PUBLIC)
                                .addBodyCodeLine("return new AnnotationConfigApplicationContext( "+SPRING_CONFIG_CLASSNAME+".class );"
                                )
                                .useType(AnnotationConfigApplicationContext.class)
                                .useType(componentClasses.toArray(new Class[0]))
                        )
        );
        unitSG.storeToClassPath(sourcePath.toAbsolutePath().toString());
    }

    public static void generateSpringConfig(Path sourcePath, String packageName, List<? extends Class<?>> configClasses, File applicationPropertiesFile) {
        var unitSG = UnitSourceGenerator.create(packageName);
        unitSG.addClass(
                ClassSourceGenerator.create(TypeDeclarationSourceGenerator.create(SPRING_CONFIG_CLASSNAME))
                        .addModifier(Modifier.PUBLIC)
                        .addAnnotation(AnnotationSourceGenerator.create(Configuration.class))
                        .addAnnotation(AnnotationSourceGenerator.create(PropertySource.class)
                                .addParameter("value", true, VariableSourceGenerator.create("\"" + applicationPropertiesFile.getName() + "\""))
                                .addParameter("ignoreResourceNotFound", false, VariableSourceGenerator.create("true"))
                        )
                        .addAnnotation(AnnotationSourceGenerator.create(Import.class).addParameter("value", true,
                                VariableSourceGenerator.create(
                                        getSimpleClassesNamesString(configClasses) )
                        ))
        ).addImport(configClasses.toArray(new Class<?>[0]));
        unitSG.storeToClassPath(sourcePath.toAbsolutePath().toString());
    }

    private static String getSimpleClassesNamesString(List<? extends Class<?>> componentClasses) {
        return componentClasses.stream()
                .map(Class::getSimpleName)
                .map(name -> name + ".class")
                .collect(Collectors.joining(","));
    }
}
