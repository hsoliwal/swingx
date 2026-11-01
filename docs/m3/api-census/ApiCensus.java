/*
 * Copyright (c) 2026 Synexia contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * API-CENSUS v1 - parse-only declaration census of the public/protected Java API.
 *
 * Usage:  java ApiCensus.java <root-dir> <file-list>   (file-list: one path per line, relative to root)
 * Output: TSV on stdout, sorted, one row per exported declaration:
 *           fqcn <TAB> key <TAB> detail <TAB> path <TAB> generic
 *         key     = TYPE | M name(p1,p2) | F name        (constructors are named <init>)
 *         detail  = kind or modifiers + erased return/field type
 *         generic = the declaration as written (type parameters, generic return/parameter/field types)
 *
 * Erasure rules (source level, no attribution): type arguments dropped, type variables replaced by the
 * erasure of their first bound (Object when unbounded), qualified names reduced to the simple name,
 * varargs reported as arrays. A member is exported when it is public or protected (implicitly public in
 * interfaces and annotation types) and every enclosing type is exported. Classes without an explicit
 * constructor report the implicit default constructor with the class visibility.
 * Requires JDK 17+ (javac Tree API, parse only: no classpath, no attribution).
 */

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ApiCensus {

    private static final Set<String> ROWS = new TreeSet<>();

    private record Scope(String fqcn, boolean exported, boolean iface, Map<String, String> typeVars, String path) {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        List<Path> files = Files.readAllLines(Path.of(args[1])).stream()
                .map(String::strip).filter(s -> !s.isEmpty()).map(root::resolve).toList();
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = javac.getStandardFileManager(null, null, StandardCharsets.ISO_8859_1);
        Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(files);
        List<String> errors = new java.util.ArrayList<>();
        JavacTask task = (JavacTask) javac.getTask(null, fm, d -> {
            if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                errors.add(d.getSource() + ":" + d.getLineNumber() + ": " + d.getMessage(null));
            }
        }, List.of("-proc:none"), null, units);
        task.parse().forEach(cu -> unit(cu, root));
        ROWS.forEach(System.out::println);
        errors.forEach(e -> System.err.println("parse-error\t" + e));
        System.err.println("api-census\tfiles=" + files.size() + "\trows=" + ROWS.size() + "\tparse_errors=" + errors.size());
    }

    private static void unit(CompilationUnitTree cu, Path root) {
        String pkg = cu.getPackageName() == null ? "" : cu.getPackageName() + ".";
        String path = root.relativize(Path.of(cu.getSourceFile().toUri())).toString().replace('\\', '/');
        Scope top = new Scope(pkg, true, false, Map.of(), path);
        cu.getTypeDecls().stream().filter(ClassTree.class::isInstance)
                .forEach(t -> type((ClassTree) t, top, true));
    }

    private static void type(ClassTree ct, Scope outer, boolean topLevel) {
        String fqcn = topLevel ? outer.fqcn() + ct.getSimpleName() : outer.fqcn() + "$" + ct.getSimpleName();
        boolean exported = outer.exported() && visible(ct.getModifiers(), outer.iface());
        boolean iface = ct.getKind() == Tree.Kind.INTERFACE || ct.getKind() == Tree.Kind.ANNOTATION_TYPE;
        Scope scope = new Scope(fqcn, exported, iface, bind(ct.getTypeParameters(), outer.typeVars()), outer.path());
        if (exported) {
            String supers = "extends " + ct.getExtendsClause() + " implements " + ct.getImplementsClause();
            emit(scope, "TYPE", ct.getKind() + " " + flags(ct.getModifiers()), ct.getTypeParameters() + " " + supers);
        }
        ct.getMembers().forEach(m -> member(m, scope, ct));
        implicitConstructor(ct, scope);
    }

    private static void member(Tree m, Scope scope, ClassTree owner) {
        if (m instanceof ClassTree nested) {
            type(nested, scope, false);
        } else if (m instanceof MethodTree mt && scope.exported() && visible(mt.getModifiers(), scope.iface())) {
            method(mt, scope);
        } else if (m instanceof VariableTree vt && scope.exported()
                && (enumConstant(vt, owner) || visible(vt.getModifiers(), scope.iface()))) {
            String detail = enumConstant(vt, owner) ? "enum-constant" : flags(vt.getModifiers()) + " "
                    + erase(vt.getType(), scope.typeVars());
            emit(scope, "F " + vt.getName(), detail, enumConstant(vt, owner) ? "" : String.valueOf(vt.getType()));
        }
    }

    private static void method(MethodTree mt, Scope scope) {
        Map<String, String> tv = bind(mt.getTypeParameters(), scope.typeVars());
        List<? extends VariableTree> params = mt.getParameters().stream()
                .filter(p -> !"this".contentEquals(p.getName())).toList();
        String erased = params.stream().map(p -> erase(p.getType(), tv)).collect(Collectors.joining(","));
        String written = params.stream().map(p -> String.valueOf(p.getType())).collect(Collectors.joining(","));
        String ret = mt.getReturnType() == null ? "<ctor>" : erase(mt.getReturnType(), tv);
        String generic = mt.getTypeParameters() + " " + mt.getReturnType() + " (" + written + ")";
        emit(scope, "M " + mt.getName() + "(" + erased + ")", flags(mt.getModifiers()) + " " + ret, generic);
    }

    private static void implicitConstructor(ClassTree ct, Scope scope) {
        boolean hasCtor = ct.getMembers().stream()
                .anyMatch(m -> m instanceof MethodTree mt && "<init>".contentEquals(mt.getName()));
        if (scope.exported() && ct.getKind() == Tree.Kind.CLASS && !hasCtor) {
            emit(scope, "M <init>()", "implicit <ctor>", "");
        }
    }

    private static boolean visible(ModifiersTree mods, boolean inInterface) {
        Set<Modifier> f = mods.getFlags();
        return f.contains(Modifier.PUBLIC) || f.contains(Modifier.PROTECTED)
                || (inInterface && !f.contains(Modifier.PRIVATE));
    }

    private static boolean enumConstant(VariableTree vt, ClassTree owner) {
        return owner.getKind() == Tree.Kind.ENUM && vt.getModifiers().getFlags().isEmpty()
                && vt.getInitializer() instanceof NewClassTree nc
                && owner.getSimpleName().contentEquals(nc.getIdentifier().toString());
    }

    private static Map<String, String> bind(List<? extends TypeParameterTree> params, Map<String, String> outer) {
        Map<String, String> tv = new HashMap<>(outer);
        params.forEach(p -> tv.put(p.getName().toString(), "Object"));
        params.forEach(p -> tv.put(p.getName().toString(),
                p.getBounds().isEmpty() ? "Object" : erase(p.getBounds().get(0), tv)));
        return tv;
    }

    private static String erase(Tree t, Map<String, String> tv) {
        if (t == null) {
            return "void";
        } else if (t instanceof ArrayTypeTree a) {
            return erase(a.getType(), tv) + "[]";
        } else if (t instanceof ParameterizedTypeTree p) {
            return erase(p.getType(), tv);
        } else if (t instanceof AnnotatedTypeTree a) {
            return erase(a.getUnderlyingType(), tv);
        } else if (t instanceof IdentifierTree i) {
            return tv.getOrDefault(i.getName().toString(), i.getName().toString());
        }
        return t instanceof MemberSelectTree s ? s.getIdentifier().toString() : t.toString();
    }

    private static String flags(ModifiersTree mods) {
        String names = mods.getFlags().stream().sorted().map(Modifier::toString).collect(Collectors.joining(" "));
        return names.isEmpty() ? "package" : names;
    }

    private static void emit(Scope scope, String key, String detail, String generic) {
        String flat = generic.replaceAll("\\s+", " ").strip();
        ROWS.add(scope.fqcn() + "\t" + key + "\t" + detail + "\t" + scope.path() + "\t" + flat);
    }
}
