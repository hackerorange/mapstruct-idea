package org.mapstruct.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.MapStructBundle;
import org.mapstruct.intellij.domain.MapStructMapperGenerator;

import java.util.Objects;


public class MapStructMapperGenerateAndUseConvertMethodQuickFix implements LocalQuickFix {

    private static final String CONVERTER_PACKAGE = "com.glodon.cloudt.converters";

    @SafeFieldForPreview
    private final PsiClassType sourceType;
    @SafeFieldForPreview
    private final PsiClassType targetType;

    public MapStructMapperGenerateAndUseConvertMethodQuickFix(PsiClassType sourceType, PsiClassType targetType) {
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.quickfix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement psiElement = descriptor.getPsiElement();

        PsiFile containingFile = psiElement.getContainingFile();

        PsiDirectory converterDirectory = getConverterDirectory(psiElement);

        if (converterDirectory == null) {
            return;
        }

        MapStructMapperGenerator mapStructMapperGenerator = new MapStructMapperGenerator(project, converterDirectory);

        PsiMethod mapperMethod = mapStructMapperGenerator.generateMapperMethod(sourceType, targetType);

        if (mapperMethod != null) {
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();

            PsiExpression expressionFromText = elementFactory.createExpressionFromText(
                    Objects.requireNonNull(mapperMethod.getContainingClass()).getQualifiedName() + ".INSTANCE." + mapperMethod.getName() + "(" + psiElement.getText() + ")",
                    psiElement);

            if (expressionFromText instanceof PsiMethodCallExpression) {
                psiElement.replace(expressionFromText);
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(containingFile);
            }
        }
    }

    private PsiDirectory getConverterDirectory(PsiElement psiElement) {

        /* ************************************************************************
         *
         * create converters package
         *
         * ************************************************************************ */
        PsiDirectory containingDirectory = psiElement.getContainingFile().getContainingDirectory();

        while (containingDirectory != null && !isRootSourceDirectory(containingDirectory)) {
            containingDirectory = containingDirectory.getParentDirectory();
        }
        if (containingDirectory == null) {
            return null;
        }
        for (String it : CONVERTER_PACKAGE.split("\\.")) {
            PsiDirectory subdirectory = containingDirectory.findSubdirectory(it);
            containingDirectory = subdirectory == null ? containingDirectory.createSubdirectory(it) : subdirectory;
        }

        return containingDirectory;
    }

    private boolean isRootSourceDirectory(PsiDirectory containingDirectory) {
        if (containingDirectory.getName().equals("java")) {
            VirtualFile virtualFile = containingDirectory.getVirtualFile();
            return virtualFile.getPath().endsWith("src/main/java");
        }
        return false;
    }


}
