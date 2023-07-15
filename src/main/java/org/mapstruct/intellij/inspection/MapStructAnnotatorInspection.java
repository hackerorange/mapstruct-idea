package org.mapstruct.intellij.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.intellij.MapStructBundle;
import org.mapstruct.intellij.quickfix.MapStructMapperGenerateAndUseConvertMethodQuickFix;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MapStructAnnotatorInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new MyJavaElementVisitor(holder);
    }

    static class MyJavaElementVisitor extends JavaElementVisitor {

        private static final Set<String> IGNORE_CLASS_LIST = new HashSet<>();

        static {


            IGNORE_CLASS_LIST.add("java.math.BigDecimal");
            IGNORE_CLASS_LIST.add("java.math.BigInteger");
            IGNORE_CLASS_LIST.add("java.lang.AbstractMethodError");
            IGNORE_CLASS_LIST.add("java.lang.AbstractStringBuilder");
            IGNORE_CLASS_LIST.add("java.lang.Appendable");
            IGNORE_CLASS_LIST.add("java.lang.ApplicationShutdownHooks");
            IGNORE_CLASS_LIST.add("java.lang.ArithmeticException");
            IGNORE_CLASS_LIST.add("java.lang.ArrayIndexOutOfBoundsException");
            IGNORE_CLASS_LIST.add("java.lang.ArrayStoreException");
            IGNORE_CLASS_LIST.add("java.lang.AssertionError");
            IGNORE_CLASS_LIST.add("java.lang.AssertionStatusDirectives");
            IGNORE_CLASS_LIST.add("java.lang.AutoCloseable");
            IGNORE_CLASS_LIST.add("java.lang.Boolean");
            IGNORE_CLASS_LIST.add("java.lang.BootstrapMethodError");
            IGNORE_CLASS_LIST.add("java.lang.Byte");
            IGNORE_CLASS_LIST.add("java.lang.Character");
            IGNORE_CLASS_LIST.add("java.lang.CharacterData");
            IGNORE_CLASS_LIST.add("java.lang.CharacterData0E");
            IGNORE_CLASS_LIST.add("java.lang.CharacterData00");
            IGNORE_CLASS_LIST.add("java.lang.CharacterData01");
            IGNORE_CLASS_LIST.add("java.lang.CharacterData02");
            IGNORE_CLASS_LIST.add("java.lang.CharacterDataLatin1");
            IGNORE_CLASS_LIST.add("java.lang.CharacterDataPrivateUse");
            IGNORE_CLASS_LIST.add("java.lang.CharacterDataUndefined");
            IGNORE_CLASS_LIST.add("java.lang.CharacterName");
            IGNORE_CLASS_LIST.add("java.lang.CharSequence");
            IGNORE_CLASS_LIST.add("java.lang.Class");
            IGNORE_CLASS_LIST.add("java.lang.ClassCastException");
            IGNORE_CLASS_LIST.add("java.lang.ClassCircularityError");
            IGNORE_CLASS_LIST.add("java.lang.ClassFormatError");
            IGNORE_CLASS_LIST.add("java.lang.ClassLoader");
            IGNORE_CLASS_LIST.add("java.lang.ClassLoaderHelper");
            IGNORE_CLASS_LIST.add("java.lang.ClassNotFoundException");
            IGNORE_CLASS_LIST.add("java.lang.ClassValue");
            IGNORE_CLASS_LIST.add("java.lang.Cloneable");
            IGNORE_CLASS_LIST.add("java.lang.CloneNotSupportedException");
            IGNORE_CLASS_LIST.add("java.lang.Comparable");
            IGNORE_CLASS_LIST.add("java.lang.Compiler");
            IGNORE_CLASS_LIST.add("java.lang.ConditionalSpecialCasing");
            IGNORE_CLASS_LIST.add("java.lang.Deprecated");
            IGNORE_CLASS_LIST.add("java.lang.Double");
            IGNORE_CLASS_LIST.add("java.lang.Enum");
            IGNORE_CLASS_LIST.add("java.lang.EnumConstantNotPresentException");
            IGNORE_CLASS_LIST.add("java.lang.Error");
            IGNORE_CLASS_LIST.add("java.lang.Exception");
            IGNORE_CLASS_LIST.add("java.lang.ExceptionInInitializerError");
            IGNORE_CLASS_LIST.add("java.lang.Float");
            IGNORE_CLASS_LIST.add("java.lang.FunctionalInterface");
            IGNORE_CLASS_LIST.add("java.lang.IllegalAccessError");
            IGNORE_CLASS_LIST.add("java.lang.IllegalAccessException");
            IGNORE_CLASS_LIST.add("java.lang.IllegalArgumentException");
            IGNORE_CLASS_LIST.add("java.lang.IllegalMonitorStateException");
            IGNORE_CLASS_LIST.add("java.lang.IllegalStateException");
            IGNORE_CLASS_LIST.add("java.lang.IllegalThreadStateException");
            IGNORE_CLASS_LIST.add("java.lang.IncompatibleClassChangeError");
            IGNORE_CLASS_LIST.add("java.lang.IndexOutOfBoundsException");
            IGNORE_CLASS_LIST.add("java.lang.InheritableThreadLocal");
            IGNORE_CLASS_LIST.add("java.lang.InstantiationError");
            IGNORE_CLASS_LIST.add("java.lang.InstantiationException");
            IGNORE_CLASS_LIST.add("java.lang.Integer");
            IGNORE_CLASS_LIST.add("java.lang.InternalError");
            IGNORE_CLASS_LIST.add("java.lang.InterruptedException");
            IGNORE_CLASS_LIST.add("java.lang.Iterable");
            IGNORE_CLASS_LIST.add("java.lang.LinkageError");
            IGNORE_CLASS_LIST.add("java.lang.Long");
            IGNORE_CLASS_LIST.add("java.lang.Math");
            IGNORE_CLASS_LIST.add("java.lang.NegativeArraySizeException");
            IGNORE_CLASS_LIST.add("java.lang.NoClassDefFoundError");
            IGNORE_CLASS_LIST.add("java.lang.NoSuchFieldError");
            IGNORE_CLASS_LIST.add("java.lang.NoSuchFieldException");
            IGNORE_CLASS_LIST.add("java.lang.NoSuchMethodError");
            IGNORE_CLASS_LIST.add("java.lang.NoSuchMethodException");
            IGNORE_CLASS_LIST.add("java.lang.NullPointerException");
            IGNORE_CLASS_LIST.add("java.lang.Number");
            IGNORE_CLASS_LIST.add("java.lang.NumberFormatException");
            IGNORE_CLASS_LIST.add("java.lang.Object");
            IGNORE_CLASS_LIST.add("java.lang.OutOfMemoryError");
            IGNORE_CLASS_LIST.add("java.lang.Override");
            IGNORE_CLASS_LIST.add("java.lang.Package");
            IGNORE_CLASS_LIST.add("java.lang.Process");
            IGNORE_CLASS_LIST.add("java.lang.ProcessBuilder");
            IGNORE_CLASS_LIST.add("java.lang.ProcessEnvironment");
            IGNORE_CLASS_LIST.add("java.lang.ProcessImpl");
            IGNORE_CLASS_LIST.add("java.lang.Readable");
            IGNORE_CLASS_LIST.add("java.lang.ReflectiveOperationException");
            IGNORE_CLASS_LIST.add("java.lang.Runnable");
            IGNORE_CLASS_LIST.add("java.lang.Runtime");
            IGNORE_CLASS_LIST.add("java.lang.RuntimeException");
            IGNORE_CLASS_LIST.add("java.lang.RuntimePermission");
            IGNORE_CLASS_LIST.add("java.lang.SafeVarargs");
            IGNORE_CLASS_LIST.add("java.lang.SecurityException");
            IGNORE_CLASS_LIST.add("java.lang.SecurityManager");
            IGNORE_CLASS_LIST.add("java.lang.Short");
            IGNORE_CLASS_LIST.add("java.lang.Shutdown");
            IGNORE_CLASS_LIST.add("java.lang.StackOverflowError");
            IGNORE_CLASS_LIST.add("java.lang.StackTraceElement");
            IGNORE_CLASS_LIST.add("java.lang.StrictMath");
            IGNORE_CLASS_LIST.add("java.lang.String");
            IGNORE_CLASS_LIST.add("java.lang.StringBuffer");
            IGNORE_CLASS_LIST.add("java.lang.StringBuilder");
            IGNORE_CLASS_LIST.add("java.lang.StringCoding");
            IGNORE_CLASS_LIST.add("java.lang.StringIndexOutOfBoundsException");
            IGNORE_CLASS_LIST.add("java.lang.SuppressWarnings");
            IGNORE_CLASS_LIST.add("java.lang.System");
            IGNORE_CLASS_LIST.add("java.lang.SystemClassLoaderAction");
            IGNORE_CLASS_LIST.add("java.lang.Terminator");
            IGNORE_CLASS_LIST.add("java.lang.Thread");
            IGNORE_CLASS_LIST.add("java.lang.ThreadDeath");
            IGNORE_CLASS_LIST.add("java.lang.ThreadGroup");
            IGNORE_CLASS_LIST.add("java.lang.ThreadLocal");
            IGNORE_CLASS_LIST.add("java.lang.Throwable");
            IGNORE_CLASS_LIST.add("java.lang.TypeNotPresentException");
            IGNORE_CLASS_LIST.add("java.lang.UNIXProcess");
            IGNORE_CLASS_LIST.add("java.lang.UnknownError");
            IGNORE_CLASS_LIST.add("java.lang.UnsatisfiedLinkError");
            IGNORE_CLASS_LIST.add("java.lang.UnsupportedClassVersionError");
            IGNORE_CLASS_LIST.add("java.lang.UnsupportedOperationException");
            IGNORE_CLASS_LIST.add("java.lang.VerifyError");
            IGNORE_CLASS_LIST.add("java.lang.VirtualMachineError");
            IGNORE_CLASS_LIST.add("java.lang.Void");
        }

        private final @NotNull ProblemsHolder holder;


        public MyJavaElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }


        @Override
        public void visitReturnStatement(PsiReturnStatement returnStatement) {


            PsiExpression sourceReturnValue = returnStatement.getReturnValue();
            if (sourceReturnValue == null) {
                return;
            }

            PsiType sourceType = sourceReturnValue.getType();
            if (!(sourceType instanceof PsiClassType)) {
                return;
            }

            Project project = returnStatement.getProject();

            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);

            PsiClass iterableClass = javaPsiFacade.findClass("java.lang.Iterable", GlobalSearchScope.allScope(project));

            if (PsiJavaPatterns.psiReturnStatement().inside(PsiLambdaExpression.class).accepts(returnStatement)) {
                // lambda 表达式中不进行处理
                return;

            } else if (PsiJavaPatterns.psiReturnStatement().inside(PsiMethod.class).accepts(returnStatement)) {

                PsiMethod psiMethod = PsiTreeUtil.getParentOfType(returnStatement, PsiMethod.class);
                assert psiMethod != null;

                PsiType targetType = psiMethod.getReturnType();

                if (!(targetType instanceof PsiClassType)) {
                    return;
                }

                if (needFix(iterableClass, (PsiClassType) sourceType, (PsiClassType) targetType)) {
                    holder.registerProblem(
                            returnStatement,
                            MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.descriptor"),
                            new MapStructMapperGenerateAndUseConvertMethodQuickFix((PsiElement) sourceReturnValue, (PsiClassType) sourceType, (PsiClassType) targetType)
                    );
                }
            }

            super.visitReturnStatement(returnStatement);
        }

        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement declarationStatement) {
            super.visitDeclarationStatement(declarationStatement);

            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();

            Project project = declarationStatement.getProject();

            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
            PsiClass iterableClass = javaPsiFacade.findClass("java.lang.Iterable", GlobalSearchScope.allScope(project));


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

                    if (needFix(iterableClass, sourceClassType, targetClassType)) {
                        holder.registerProblem(
                                declaredElement,
                                MapStructBundle.message("inspection.generate_mapstruct_class_and_use_it.problem.descriptor"),
                                new MapStructMapperGenerateAndUseConvertMethodQuickFix(initializer, sourceClassType, targetClassType)
                        );
                    }

                }


            }


        }

        private boolean needFix(PsiClass iterableClass, PsiClassType sourceClassType, PsiClassType targetClassType) {
            if (sourceClassType == null || targetClassType == null) {
                return false;
            }

            PsiClass sourceClass = sourceClassType.resolveGenerics().getElement();
            if (sourceClass == null) {
                return false;
            }

            PsiClass targetClass = targetClassType.resolveGenerics().getElement();
            if (targetClass == null) {
                return false;
            }


            boolean isSourceTypeIterable = sourceClass.isInheritor(iterableClass, true);
            boolean isTargetTypeIterable = targetClass.isInheritor(iterableClass, true);

            // 如果 source 和 target 中，一个是集合，另外一个不是集合的话，则无法转化
            if (isSourceTypeIterable != isTargetTypeIterable) {
                return false;
            }

            // 如果 source 和 target都是集合的话
            if (isSourceTypeIterable) {
                PsiType innerSourceType = PsiUtil.extractIterableTypeParameter(sourceClassType, false);
                PsiType innerTargetType = PsiUtil.extractIterableTypeParameter(targetClassType, false);
                if (!(innerSourceType instanceof PsiClassType)) {
                    return false;
                }
                if (!(innerTargetType instanceof PsiClassType)) {
                    return false;
                }
                return needFix(iterableClass, (PsiClassType) innerSourceType, (PsiClassType) innerTargetType);
            }

            if (sourceClass.isInheritor(targetClass, true)) {
                return false;
            }

            if (sourceClassType.getCanonicalText().equals(targetClassType.getCanonicalText())) {
                return false;
            }

            PsiClass resolvedTargetType = targetClassType.resolve();

            if (resolvedTargetType == null) {
                return false;
            }

            if (IGNORE_CLASS_LIST.contains(resolvedTargetType.getQualifiedName())) {
                return false;
            }
            PsiClass resolvedSourceType = sourceClassType.resolve();
            if (resolvedSourceType == null) {
                return false;
            }

            if (Objects.equals(resolvedTargetType.getQualifiedName(), resolvedSourceType.getQualifiedName())) {
                return false;
            }

            return true;
        }
    }
}
