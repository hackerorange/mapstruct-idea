package org.mapstruct.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.MapStructBundle;
import org.mapstruct.intellij.domain.MapStructMapperGenerator;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public class MapStructMapperGenerateAndUseConvertMethodQuickFix implements LocalQuickFix {

//    private static final String CONVERTER_PACKAGE = "com.glodon.cloudt.converters";

    @SafeFieldForPreview
    private final PsiElement psiElement;
    @SafeFieldForPreview
    private final PsiClassType sourceType;
    @SafeFieldForPreview
    private final PsiClassType targetType;

    public MapStructMapperGenerateAndUseConvertMethodQuickFix(PsiElement psiElement, PsiClassType sourceType, PsiClassType targetType) {
        this.psiElement = psiElement;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.quickfix");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {

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
        VirtualFile virtualFile = containingDirectory.getVirtualFile();

        AtomicReference<PsiDirectory> baseDirectory = new AtomicReference<>();

        ApplicationManager
                .getApplication()
                .runReadAction(() -> {
                    // 遍历文件夹下的所有文件，找到java文件下的所有 request mapping
                    VfsUtilCore.iterateChildrenRecursively(virtualFile, VirtualFileFilter.ALL, it -> {
                        PsiDirectory psiDirectory = baseDirectory.get();

                        // 基准文件夹已经找到，返回 false 不继续查找
                        if (psiDirectory != null) {
                            return false;
                        }
                        // 不是有效的文件，继续后面的文件
                        if (!it.isValid()) {
                            return true;
                        }
                        // 是文件夹，继续后面的文件
                        if (it.isDirectory()) {
                            return true;
                        }
                        // 不是 java 文件，继续后面的文件
                        if (it.getName().endsWith(".java")) {
                            PsiFile psiFile = PsiUtilBase.getPsiFile(psiElement.getProject(), it);
                            if (psiFile instanceof PsiJavaFile) {
                                PsiDirectory directory = psiElement.getContainingFile().getContainingDirectory();
                                baseDirectory.set(directory);
                                return false;
                            }
                        }
                        return true;
                    });
                });

        PsiDirectory sourceBaseDirectory = baseDirectory.get();

        if (sourceBaseDirectory == null) {
            return null;
        }
        PsiDirectory subdirectory = sourceBaseDirectory.findSubdirectory("assemblers");
        return subdirectory != null ? subdirectory : sourceBaseDirectory.createSubdirectory("assemblers");
    }

    private boolean isRootSourceDirectory(PsiDirectory containingDirectory) {
        if (containingDirectory.getName().equals("java")) {
            VirtualFile virtualFile = containingDirectory.getVirtualFile();
            return virtualFile.getPath().endsWith("src/main/java") || virtualFile.getPath().endsWith("src/test/java");
        }
        return false;
    }


}
