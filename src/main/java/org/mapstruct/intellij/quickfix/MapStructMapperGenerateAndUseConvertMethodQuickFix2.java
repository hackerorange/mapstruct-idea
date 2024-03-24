package org.mapstruct.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.domain.MapStructMapperGeneratorForClass;

import java.util.Objects;


public class MapStructMapperGenerateAndUseConvertMethodQuickFix2 implements LocalQuickFix {

//    private static final String CONVERTER_PACKAGE = "com.glodon.cloudt.converters";

    @SafeFieldForPreview
    private final PsiElement psiElement;
    @SafeFieldForPreview
    private final PsiClassType sourceType;
    @SafeFieldForPreview
    private final PsiClassType targetType;

    public MapStructMapperGenerateAndUseConvertMethodQuickFix2(PsiElement psiElement, PsiClassType sourceType, PsiClassType targetType) {
        this.psiElement = psiElement;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "[MapStruct] generate mapper class and use mapper method in current class";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {

        PsiFile containingFile = psiElement.getContainingFile();


        PsiClass parentOfType = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);


        if (parentOfType == null) {
            return;
        }

        MapStructMapperGeneratorForClass mapStructMapperGenerator = new MapStructMapperGeneratorForClass(project, parentOfType);

        PsiMethod mapperMethod = mapStructMapperGenerator.generateMapperMethod(sourceType, targetType);

        if (mapperMethod != null) {
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
            // 表达式
            String expression = Objects.requireNonNull(mapperMethod.getContainingClass()).getQualifiedName() + ".INSTANCE." + mapperMethod.getName() + "(" + psiElement.getText() + ")";
            // 生成表达式
            PsiExpression expressionFromText = elementFactory.createExpressionFromText(expression, psiElement);

            if (expressionFromText instanceof PsiMethodCallExpression) {
                psiElement.replace(expressionFromText);
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(containingFile);
            }
        }
    }


}
