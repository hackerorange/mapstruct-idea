package org.mapstruct.intellij.domain;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiUtil;

import java.util.Arrays;
import java.util.Objects;

import static org.mapstruct.intellij.util.MapstructUtil.MAPPER_ANNOTATION_FQN;

public class MapStructMapperGenerator {

    private final Project project;
    private final PsiDirectory converterDirectory;
    private final JavaPsiFacade javaPsiFacade;

    private final PsiClass iterableClass;
    private final PsiElementFactory elementFactory;


    public MapStructMapperGenerator(Project project, PsiDirectory converterDirectory) {
        this.project = project;
        this.converterDirectory = converterDirectory;
        this.javaPsiFacade = JavaPsiFacade.getInstance(project);
        this.elementFactory = javaPsiFacade.getElementFactory();
        this.iterableClass = javaPsiFacade.findClass("java.lang.Iterable", GlobalSearchScope.allScope(project));
    }

    public PsiMethod generateMapperMethod(PsiClassType sourceType, PsiClassType targetType) {

        // 如果目标类型是一个集合类型,找到目标类型 Iterable 的泛型类,使用泛型类,生成一个 Mapper
        PsiClass targetTypeClass = targetType.resolveGenerics().getElement();
        if (targetTypeClass == null) {
            return null;
        }


        PsiClass iterableClass = this.iterableClass;
        if ((targetTypeClass).isInheritor(iterableClass, true)) {
            PsiType innerTargetType = PsiUtil.extractIterableTypeParameter(targetType, false);
            if (innerTargetType instanceof PsiClassType) {
                return generateMapperMethod(sourceType, (PsiClassType) innerTargetType);
            }
            return null;
        }

        // 如果目标类型是一个集合类型，找到目标类型 Iterable 的泛型类，使用泛型类，生成一个 Mapper
        PsiClass sourceTypeClass = sourceType.resolveGenerics().getElement();
        if (sourceTypeClass == null) {
            return null;
        }

        // 如果来源类型是集合类型,获取来源类型的泛型类,生成 mapList 方法
        if ((sourceTypeClass).isInheritor(iterableClass, true)) {
            PsiType typeParameter = PsiUtil.extractIterableTypeParameter(sourceType, false);

            if (typeParameter instanceof PsiClassType) {

                sourceType = (PsiClassType) typeParameter;

                PsiClass mapperClass = createClassIfNotExists(converterDirectory, targetType.getClassName() + "Assembler");

                // create INSTANCE field
                createInstanceFieldIfNotExists(mapperClass);

                // 创建对象映射方法
                generateConvertMethod(mapperClass, sourceType, targetType);

                // 创建对象映射方法（default）
//                generateConvertOrDefaultMethod(mapperClass, sourceType, targetType);
                generateConvertOrNewInstanceMethod(mapperClass, sourceType, targetType);

                // 创建 list 映射方法
                return generateConvertListMethod(mapperClass, sourceType, targetType);
            }

            return null;
        } else {
            PsiClass mapperClass = createClassIfNotExists(converterDirectory, targetType.getClassName() + "Assembler");

            // create INSTANCE field
            createInstanceFieldIfNotExists(mapperClass);

            // 创建对象映射方法（default）
//            generateConvertOrDefaultMethod(mapperClass, sourceType, targetType);
            generateConvertOrNewInstanceMethod(mapperClass, sourceType, targetType);

            // 如果来源类型不是集合类型,直接生成对象 convert 方法
            return generateConvertMethod(mapperClass, sourceType, targetType);
        }


    }

    private PsiMethod generateConvertMethod(PsiClass mapperClass, PsiClassType sourceClassType, PsiClassType targetClassType) {

        String methodName = "convert";
        String sourceParamName = sourceClassType.getClassName().substring(0, 1).toLowerCase() + sourceClassType.getClassName().substring(1);
        String defaultConverterName = "convert_from_" + sourceClassType.getClassName() + "_to_" + targetClassType.getClassName();

        /* ********************************************************
         *
         * 如果当前类中，已经存在了此方法，直接将现有的方法返回出去
         *
         ******************************************************** */
        // TODO:后续添加多个参数校验
        for (PsiMethod method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (method.getParameterList().getParametersCount() == 1) {
                    PsiParameter parameter = method.getParameterList().getParameter(0);
                    if (parameter != null) {
                        PsiType type = parameter.getType();
                        if (type instanceof PsiClassType) {
                            if (type.equals(sourceClassType)) {
                                if (method.getDocComment() == null) {
                                    addOrReplaceDocCommentForSingleObjectConvertMethod(sourceClassType, targetClassType, method, sourceParamName);
                                }
                                if (!method.hasAnnotation("org.springframework.lang.Nullable")) {
                                    method.getModifierList().addAnnotation("org.springframework.lang.Nullable");
                                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(method.getContainingFile());
                                }
                                if (!parameter.hasAnnotation("org.springframework.lang.Nullable")) {
                                    PsiModifierList modifierList = parameter.getModifierList();
                                    if (modifierList != null) {
                                        modifierList.addAnnotation("org.springframework.lang.Nullable");
                                    }
                                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(method.getContainingFile());
                                }
                                return method;
                            }
                        }
                    }
                    if (!method.hasAnnotation("org.mapstruct.Named")) {
                        JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText("@org.mapstruct.Named(\"" + defaultConverterName + "\")", null);
                        method.getModifierList().addAnnotation("org.mapstruct.Named");

                    }
                }
            }
        }

        /* ********************************************************
         *
         * 如果当前类中，不存在了此方法，创建新的方法
         *
         ******************************************************** */

        //noinspection ConcatenationWithEmptyString
        String methodContent = "" +
                "@org.springframework.lang.Nullable\n" +
                "@org.mapstruct.Named(\"" + defaultConverterName + "\")\n" +
                targetClassType.getCanonicalText() + " " + methodName + "(@org.springframework.lang.Nullable " + sourceClassType.getCanonicalText() + " " + sourceParamName + ");";
        PsiMethod resultMethod = (PsiMethod) mapperClass.add(elementFactory.createMethodFromText(methodContent, mapperClass));

        addOrReplaceDocCommentForSingleObjectConvertMethod(sourceClassType, targetClassType, resultMethod, sourceParamName);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(resultMethod.getContainingFile());
        return resultMethod;
    }

    private PsiMethod generateConvertOrNewInstanceMethod(PsiClass mapperClass, PsiClassType sourceClassType, PsiClassType targetClassType) {
        PsiClass psiClass = targetClassType.resolve();
        if (psiClass == null) {
            return null;
        }
        boolean existsNoneArgsConstructor = Arrays.stream(psiClass.getConstructors()).anyMatch(constructor -> constructor.getParameterList().getParametersCount() == 0);
        if (!existsNoneArgsConstructor) {
            return null;
        }
        String methodName = "convertOrNewInstance";
        String sourceParamName = sourceClassType.getClassName().substring(0, 1).toLowerCase() + sourceClassType.getClassName().substring(1);

        /* ********************************************************
         *
         * 如果当前类中，已经存在了此方法，直接将现有的方法返回出去
         *
         ******************************************************** */
        // TODO:后续添加多个参数校验
        for (PsiMethod method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (method.getParameterList().getParametersCount() == 1) {
                    PsiParameter parameter = method.getParameterList().getParameter(0);
                    if (parameter != null) {
                        PsiType type = parameter.getType();
                        if (type instanceof PsiClassType) {
                            if (type.equals(sourceClassType)) {
                                if (method.getDocComment() == null) {
                                    addOrReplaceDocCommentForSingleObjectConvertMethod(sourceClassType, targetClassType, method, sourceParamName);
                                }
                                if (!method.hasAnnotation("org.springframework.lang.NonNull")) {
                                    method.getModifierList().addAnnotation("org.springframework.lang.NonNull");
                                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(method.getContainingFile());
                                }
                                if (!parameter.hasAnnotation("org.springframework.lang.Nullable")) {
                                    PsiModifierList modifierList = parameter.getModifierList();
                                    if (modifierList != null) {
                                        modifierList.addAnnotation("org.springframework.lang.Nullable");
                                    }
                                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(method.getContainingFile());
                                }
                                return method;
                            }
                        }
                    }
                }
            }
        }

        /* ********************************************************
         *
         * 如果当前类中，不存在了此方法，创建新的方法
         *
         ******************************************************** */

        //noinspection ConcatenationWithEmptyString
        String methodContent = "" +
                "@org.springframework.lang.NonNull\n" +
                "default " + targetClassType.getCanonicalText() + " " + methodName + "(@org.springframework.lang.Nullable " + sourceClassType.getCanonicalText() + " " + sourceParamName + ") {" +
                "    return java.util.Optional.ofNullable(convert(" + sourceParamName + ")).orElse(new " + targetClassType.getCanonicalText() + "());" +
                "}";
        PsiMethod methodFromText = elementFactory.createMethodFromText(methodContent, mapperClass);
        PsiMethod resultMethod = (PsiMethod) mapperClass.add(methodFromText);

        addOrReplaceDocCommentForSingleObjectConvertMethod(sourceClassType, targetClassType, resultMethod, sourceParamName);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(resultMethod.getContainingFile());
        return resultMethod;
    }

    private PsiMethod generateConvertListMethod(PsiClass mapperClass, PsiClassType sourceClassType, PsiClassType targetClassType) {

        String methodName = "convertFrom" + sourceClassType.getClassName() + "List";
        String defaultConverterName = "convert_from_" + sourceClassType.getClassName() + "_to_" + targetClassType.getClassName();
        String sourceParamName = sourceClassType.getClassName().substring(0, 1).toLowerCase() + sourceClassType.getClassName().substring(1) + "List";

        /* ********************************************************
         *
         * 如果当前类中，已经存在了此方法，直接将现有的方法返回出去
         *
         ******************************************************** */
        // TODO:后续添加多个参数校验
        for (PsiMethod method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {

                if (method.getDocComment() == null) {
                    addOrReplaceDocCommentForListConvertMethod(sourceClassType, targetClassType, method, sourceParamName);
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(method.getContainingFile());
                }


                return method;
            }
        }

        /* ********************************************************
         *
         * 如果当前类中，不存在了此方法，创建新的方法
         *
         ******************************************************** */

        String methodContent = "java.util.List<" + targetClassType.getCanonicalText() + "> " + methodName + "(java.util.List<" + sourceClassType.getCanonicalText() + "> " + sourceParamName + ");";

        PsiMethod psiMethod = elementFactory.createMethodFromText(methodContent, mapperClass);

        psiMethod.getModifierList().addAnnotation("org.mapstruct.IterableMapping(nullValueMappingStrategy = org.mapstruct.NullValueMappingStrategy.RETURN_DEFAULT,qualifiedByName = \"" + defaultConverterName + "\")");
//        psiMethod.getModifierList().addAnnotation("org.springframework.lang.NonNull");

        PsiMethod generatedNewMethod = (PsiMethod) mapperClass.add(psiMethod);
        addOrReplaceDocCommentForListConvertMethod(sourceClassType, targetClassType, generatedNewMethod, sourceParamName);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(generatedNewMethod.getContainingFile());
        return generatedNewMethod;
    }

    /**
     * 为列表转换方法添加或替换文档注释
     *
     * @param sourceClassType source class type
     * @param targetClassType target class type
     * @param method          method
     * @param sourceParamName source param name
     * @apiNote add or replace doc comment for list convert method
     */
    private void addOrReplaceDocCommentForListConvertMethod(PsiClassType sourceClassType, PsiClassType targetClassType, PsiMethod method, String sourceParamName) {
        //noinspection ConcatenationWithEmptyString
        String docComment = ""
                + "/** \n"
                + " * 将 [" + sourceClassType.getClassName() + "] 列表转换为 [" + targetClassType.getClassName() + "] 列表\n"
                + " * @param " + sourceParamName + " [ required ] 要转换的对象列表\n"
                + " * @return " + targetClassType.getCanonicalText() + " <UNK>\n"
                + " * @return 转换结果列表\n"
                + " * \n"
                + " */";
        PsiDocComment docCommentFromText = elementFactory.createDocCommentFromText(docComment);

        if (method.getDocComment() != null) {
            CodeStyleManager.getInstance(method.getProject()).reformat(method.getDocComment().replace(docCommentFromText));
        } else {
            CodeStyleManager.getInstance(method.getProject()).reformat(method.addAfter(docCommentFromText, null));
        }

    }


    /**
     * 为列表转换方法添加或替换文档注释
     *
     * @param sourceClassType source class type
     * @param targetClassType target class type
     * @param method          method
     * @param sourceParamName source param name
     * @apiNote add or replace doc comment for list convert method
     */
    private void addOrReplaceDocCommentForSingleObjectConvertMethod(PsiClassType sourceClassType, PsiClassType targetClassType, PsiMethod method, String sourceParamName) {
        //noinspection ConcatenationWithEmptyString
        String docComment = ""
                + "/** \n"
                + " * 将 [" + sourceClassType.getClassName() + "] 对象转换为 [" + targetClassType.getClassName() + "] 对象\n"
                + " * @param " + sourceParamName + " [ required ] 要转换的对象\n"
                + " * @return 转换结果对象\n"
                + " * \n"
                + " */";
        PsiDocComment docCommentFromText = elementFactory.createDocCommentFromText(docComment);

        if (method.getDocComment() != null) {
            CodeStyleManager.getInstance(method.getProject()).reformat(method.getDocComment().replace(docCommentFromText));
        } else {
            CodeStyleManager.getInstance(method.getProject()).reformat(method.addAfter(docCommentFromText, null));
        }

    }

    private PsiField createInstanceFieldIfNotExists(PsiClass mapperClass) {

        /* ********************************************************************************
         *
         * find field "INSTANCE" in the class, if field exists, use it
         *
         ********************************************************************************* */
        for (PsiField psiField : mapperClass.getFields()) {
            if ("INSTANCE".equals(psiField.getName())) {
                return psiField;
            }
        }

        /* ********************************************************************************
         *
         * create "INSTANCE" field if not exists
         *
         ********************************************************************************* */
        PsiClassType classType = elementFactory.createType(mapperClass);

        // create field "INSTANCE"
        PsiField instanceField = elementFactory.createField("INSTANCE", classType);

        // initialize the field "INSTANCE"
        String fieldInitExpression = "org.mapstruct.factory.Mappers.getMapper(" + mapperClass.getName() + ".class)";
        instanceField.setInitializer(elementFactory.createExpressionFromText(fieldInitExpression, mapperClass));

        // modify field visibility to PUBLIC
        PsiUtil.setModifierProperty(instanceField, PsiModifier.PUBLIC, true);

        // add "INSTANCE" field to the class
        return (PsiField) mapperClass.add(instanceField);
    }

    private PsiClass createClassIfNotExists(PsiDirectory converterDirectory, String mapperClass) {

        // 根据类名，查找是否存在同名的类，如果是同名的类
        PsiClass[] classesByName = PsiShortNamesCache.getInstance(converterDirectory.getProject()).getClassesByName(mapperClass, GlobalSearchScope.allScope(converterDirectory.getProject()));

        for (PsiClass psiClass : classesByName) {
            // 如果是内部类，暂不考虑
            if (PsiJavaPatterns.psiClass().inside(PsiClass.class).accepts(psiClass)) {
                continue;
            }
            // 如果不可写，不考虑
            if (!psiClass.isWritable()) {
                continue;
            }
            // 如果没有 @Mapper 注解，不考虑
            PsiModifierList modifierList = psiClass.getModifierList();
            if (modifierList == null) {
                continue;
            }
            if (modifierList.hasAnnotation(MAPPER_ANNOTATION_FQN)) {
                return psiClass;
            }
        }

        /* ********************************************************************************
         *
         * find class by name in the directory, if class exists, use it
         *
         ********************************************************************************* */
        for (PsiFile psiFile : converterDirectory.getFiles()) {
            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                for (PsiClass psiClass : psiJavaFile.getClasses()) {
                    if (Objects.equals(psiClass.getName(), mapperClass)) {
                        return psiClass;
                    }
                }

            }
        }

        /* ********************************************************************************
         *
         * create new class by name in the directory
         *
         ********************************************************************************* */
        PsiClass mapperInterface = (PsiClass) converterDirectory.add(elementFactory.createInterface(mapperClass));

        addMapperAnnotationToTheInterface(mapperInterface);
        return mapperInterface;
    }

    /**
     * add mapper annotation to the interface <br/>
     * 向 Interface 添加 @Mapper 注解
     *
     * @param mapperInterface mapper interface
     */
    private void addMapperAnnotationToTheInterface(PsiClass mapperInterface) {
        // add @Mapper annotation to the class
        PsiModifierList interfaceModifierList = mapperInterface.getModifierList();
        if (interfaceModifierList != null) {
            interfaceModifierList.addAnnotation(MAPPER_ANNOTATION_FQN);
        }
    }


}
