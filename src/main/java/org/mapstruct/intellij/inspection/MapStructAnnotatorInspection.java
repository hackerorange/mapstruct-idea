package org.mapstruct.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.MapStructBundle;
import org.mapstruct.intellij.quickfix.MapStructMapperGenerateAndUseConvertMethodQuickFix;
import org.mapstruct.intellij.quickfix.MapStructMapperGenerateAndUseConvertMethodQuickFix2;

import java.util.Objects;

public class MapStructAnnotatorInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(holder.getProject());

        PsiClass iterableClass = javaPsiFacade.findClass("java.lang.Iterable", GlobalSearchScope.allScope(holder.getProject()));

        return new JavaElementVisitor() {

            @Override
            public void visitReturnStatement(PsiReturnStatement returnStatement) {
                super.visitReturnStatement(returnStatement);

                PsiExpression sourceReturnValue = returnStatement.getReturnValue();
                if (sourceReturnValue == null) {
                    return;
                }

                PsiType sourceType = sourceReturnValue.getType();
                if (!(sourceType instanceof PsiClassType)) {
                    return;
                }

                if (PsiJavaPatterns.psiReturnStatement().inside(PsiLambdaExpression.class).accepts(returnStatement)) {
                    // lambda 表达式中不进行处理
                    return;

                }
                if (PsiJavaPatterns.psiReturnStatement().inside(PsiMethod.class).accepts(returnStatement)) {

                    PsiMethod psiMethod = PsiTreeUtil.getParentOfType(returnStatement, PsiMethod.class);
                    assert psiMethod != null;

                    PsiType targetType = psiMethod.getReturnType();

                    if (!(targetType instanceof PsiClassType)) {
                        return;
                    }

                    if (needFix((PsiClassType) sourceType, (PsiClassType) targetType)) {
                        holder.registerProblem(
                                returnStatement,
                                MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.descriptor"),
                                new MapStructMapperGenerateAndUseConvertMethodQuickFix(sourceReturnValue, (PsiClassType) sourceType, (PsiClassType) targetType)
                        );

                        holder.registerProblem(
                                returnStatement,
                                "[MapStruct] generate mapper class and use mapper method in this class",
                                new MapStructMapperGenerateAndUseConvertMethodQuickFix2(sourceReturnValue, (PsiClassType) sourceType, (PsiClassType) targetType)
                        );

                    }
                }

            }

            @Override
            public void visitDeclarationStatement(PsiDeclarationStatement declarationStatement) {
                super.visitDeclarationStatement(declarationStatement);

                PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

                for (PsiElement declaredElement : declaredElements) {
                    if (declaredElement instanceof PsiLocalVariable) {
                        PsiLocalVariable it = (PsiLocalVariable) declaredElement;
                        PsiExpression initializer = it.getInitializer();

                        if (initializer == null) {
                            continue;
                        }

                        if (initializer instanceof PsiLiteralExpression) {
                            continue;
                        }

                        PsiType sourceType = initializer.getType();
                        if (sourceType == null) {
                            continue;
                        }

                        PsiTypeElement typeElement = it.getTypeElement();

                        PsiType targetType = typeElement.getType();

                        if (!(sourceType instanceof PsiClassType)) {
                            continue;
                        }

                        if (!(targetType instanceof PsiClassType)) {
                            continue;
                        }
                        PsiClassType sourceClassType = (PsiClassType) sourceType;
                        PsiClassType targetClassType = (PsiClassType) targetType;

                        if (needFix(sourceClassType, targetClassType)) {
                            holder.registerProblem(
                                    declaredElement,
                                    MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.descriptor"),
                                    new MapStructMapperGenerateAndUseConvertMethodQuickFix(initializer, sourceClassType, targetClassType)
                            );
                            holder.registerProblem(
                                    declaredElement,
                                    "[MapStruct] generate mapper class and use mapper method in this class",
                                    new MapStructMapperGenerateAndUseConvertMethodQuickFix2(initializer, (PsiClassType) sourceType, (PsiClassType) targetType)
                            );

                        }

                    }


                }


            }

            private boolean needFix(PsiClassType sourceClassType, PsiClassType targetClassType) {
                if (sourceClassType == null || targetClassType == null) {
                    return Boolean.FALSE;
                }

                PsiClass sourceClass = sourceClassType.resolveGenerics().getElement();
                if (sourceClass == null) {
                    return Boolean.FALSE;
                }

                PsiClass targetClass = targetClassType.resolveGenerics().getElement();
                if (targetClass == null) {
                    return Boolean.FALSE;
                }


                boolean isSourceTypeIterable = iterableClass != null && sourceClass.isInheritor(iterableClass, true);
                boolean isTargetTypeIterable = iterableClass != null && targetClass.isInheritor(iterableClass, true);

                // 如果 source 和 target 中，一个是集合，另外一个不是集合的话，则无法转化
                if (isSourceTypeIterable != isTargetTypeIterable) {
                    return Boolean.FALSE;
                }

                // 如果 source 和 target都是集合的话
                if (isSourceTypeIterable) {
                    PsiType innerSourceType = PsiUtil.extractIterableTypeParameter(sourceClassType, false);
                    PsiType innerTargetType = PsiUtil.extractIterableTypeParameter(targetClassType, false);
                    if (!(innerSourceType instanceof PsiClassType)) {
                        return Boolean.FALSE;
                    }
                    if (!(innerTargetType instanceof PsiClassType)) {
                        return Boolean.FALSE;
                    }
                    return needFix((PsiClassType) innerSourceType, (PsiClassType) innerTargetType);
                }

                if (sourceClass.isInheritor(targetClass, true)) {
                    return Boolean.FALSE;
                }

                if (sourceClassType.getCanonicalText().equals(targetClassType.getCanonicalText())) {
                    return Boolean.FALSE;
                }

                PsiClass resolvedTargetType = targetClassType.resolve();

                if (resolvedTargetType == null) {
                    return Boolean.FALSE;
                }
                if (resolvedTargetType.getQualifiedName() == null) {
                    return Boolean.FALSE;
                }
                if (resolvedTargetType.getQualifiedName().startsWith("java.math.")) {
                    return Boolean.FALSE;
                }
                if (resolvedTargetType.getQualifiedName().startsWith("java.lang.")) {
                    return Boolean.FALSE;
                }

                PsiClass resolvedSourceType = sourceClassType.resolve();
                if (resolvedSourceType == null) {
                    return Boolean.FALSE;
                }

                if (Objects.equals(resolvedTargetType.getQualifiedName(), resolvedSourceType.getQualifiedName())) {
                    return Boolean.FALSE;
                }

                return Boolean.TRUE;
            }

        };
    }

}
