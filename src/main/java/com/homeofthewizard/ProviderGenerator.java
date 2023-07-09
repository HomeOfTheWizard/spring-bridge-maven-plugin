package com.homeofthewizard;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.classes.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ProviderGenerator {

    public static void generateBeansProviders(Path sourcePath, String packageName, Class beanClass) {
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

        var componentContainer = ComponentContainer.getInstance();
        var classFactory = componentContainer.getClassFactory();
        classFactory.loadOrBuildAndDefine(unitSG);

//        JavaMemoryCompiler javaMemoryCompiler = componentContainer.getJavaMemoryCompiler();
//        QueuedTaskExecutor.ProducerTask<JavaMemoryCompiler.Compilation.Result> compilationTask = javaMemoryCompiler.compile(
//                JavaMemoryCompiler.Compilation.Config.forUnitSourceGenerator(
//                                unitSG
//                        )
//                        .storeCompiledClassesTo(
//                                buildPath.toAbsolutePath().toString()
//                        )
//        );
//        JavaMemoryCompiler.Compilation.Result compilattionResult = compilationTask.join();
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
                                .addBodyCodeLine("return new AnnotationConfigApplicationContext(" +
                                        componentClasses.stream()
                                                .map(Class::getSimpleName)
                                                .map(name -> name + ".class")
                                                .collect(Collectors.joining(",")) + ");"
                                )
                                .useType(AnnotationConfigApplicationContext.class)
                                .useType(componentClasses.toArray(new Class[0]))
                        )
        );
        unitSG.storeToClassPath(sourcePath.toAbsolutePath().toString());

        var componentContainer = ComponentContainer.getInstance();
        var classFactory = componentContainer.getClassFactory();
        classFactory.loadOrBuildAndDefine(unitSG);
    }
}
