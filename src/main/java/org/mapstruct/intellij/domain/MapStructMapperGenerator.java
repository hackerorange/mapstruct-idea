package org.mapstruct.intellij.domain;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;

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

        PsiClass mapperClass = createClassIfNotExists(converterDirectory, targetType.getClassName() + "Mapper");

        // create INSTANCE field
        PsiField instanceFieldIfNotExists = createInstanceFieldIfNotExists(mapperClass);

        // 如果目标类型是一个集合类型，找到目标类型 Iterable 的泛型类，使用泛型类，生成一个 Mapper
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
                return createListMethod(mapperClass, (PsiClassType) typeParameter, targetType);
            }

            return null;
        }
        // 如果来源类型不是集合类型,直接生成对象 convert 方法
        return generateConvertMethod(sourceType, targetType, mapperClass);

    }

    private PsiMethod generateConvertMethod(PsiClassType sourceClassType, PsiClassType targetClassType, PsiClass mapperClass) {

        String methodName = "convertFrom" + sourceClassType.getClassName();

        /* ********************************************************
         *
         * 如果当前类中，已经存在了此方法，直接将现有的方法返回出去
         *
         ******************************************************** */
        // TODO:后续添加多个参数校验
        for (PsiMethod method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        /* ********************************************************
         *
         * 如果当前类中，不存在了此方法，创建新的方法
         *
         ******************************************************** */
        String sourceParamName = sourceClassType.getClassName().substring(0, 0).toLowerCase() + sourceClassType.getClassName().substring(1);

        String methodContent = targetClassType.getCanonicalText() + " " + methodName + "(" + sourceClassType.getCanonicalText() + " " + sourceParamName + ");";
        PsiMethod resultMethod = (PsiMethod) mapperClass.add(elementFactory.createMethodFromText(methodContent, mapperClass));

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(resultMethod.getContainingFile());
        return resultMethod;
    }

    private PsiMethod createListMethod(PsiClass mapperClass, PsiClassType sourceClassType, PsiClassType targetClassType) {

        String methodName = "convertFrom" + sourceClassType.getClassName() + "List";

        /* ********************************************************
         *
         * 如果当前类中，已经存在了此方法，直接将现有的方法返回出去
         *
         ******************************************************** */
        // TODO:后续添加多个参数校验
        for (PsiMethod method : mapperClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        /* ********************************************************
         *
         * 如果当前类中，不存在了此方法，创建新的方法
         *
         ******************************************************** */
        String sourceParamName = sourceClassType.getClassName().substring(0, 0).toLowerCase() + sourceClassType.getClassName().substring(1) + "List";

        String methodContent = "java.util.List<" + targetClassType.getCanonicalText() + "> " + methodName + "(java.util.List<" + sourceClassType.getCanonicalText() + "> " + sourceParamName + ");";

        PsiMethod psiMethod = elementFactory.createMethodFromText(methodContent, mapperClass);

        psiMethod.getModifierList().addAnnotation("org.mapstruct.IterableMapping(nullValueMappingStrategy = org.mapstruct.NullValueMappingStrategy.RETURN_DEFAULT)");
        psiMethod.getModifierList().addAnnotation("org.springframework.lang.NonNull");

        PsiMethod generatedNewMethod = (PsiMethod) mapperClass.add(psiMethod);
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(generatedNewMethod.getContainingFile());
        return generatedNewMethod;
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
