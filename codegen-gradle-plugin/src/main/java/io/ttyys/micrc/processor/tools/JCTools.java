package io.ttyys.micrc.processor.tools;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import javax.lang.model.element.Element;
import java.util.ArrayList;

public class JCTools {
    private final JavacTrees trees;
    private final TreeMaker maker;
    private final Name.Table nameTable;

    private JCTools(JavacTrees trees, TreeMaker maker, Name.Table nameTable) {
        this.trees = trees;
        this.maker = maker;
        this.nameTable = nameTable;
    }

    public static JCTools newInstance(JavacTrees trees, TreeMaker maker, Name.Table nameTable) {
        return new JCTools(trees, maker, nameTable);
    }

    public void importClass(Element element, String packageName, String className) {
        TreePath treePath = this.trees.getPath(element);
        JCTree.JCIdent packages = maker.Ident(nameTable.fromString(packageName));
        JCTree.JCImport jcImport = maker.Import(maker.Select(packages, nameTable.fromString(className)), false);
        JCTree.JCCompilationUnit jcCompilationUnit = (JCTree.JCCompilationUnit) treePath.getCompilationUnit();
        java.util.List<JCTree> trs = new ArrayList<>(jcCompilationUnit.defs);
        trs.add(jcImport);
        jcCompilationUnit.defs = List.from(trs);
    }

    public JCTree.JCVariableDecl createInstanceVar(JCTree.JCModifiers modifiers, String name, String type,
                                                   JCTree.JCNewClass init) {
        return maker.VarDef(
                modifiers,
                nameTable.fromString(name),
                maker.Ident(nameTable.fromString(type)),
                init);
    }

    public JCTree.JCVariableDecl createInstanceVar(JCTree.JCModifiers modifiers, String name, String type,
                                                   String newType,
                                                   List<JCTree.JCExpression> newParamTypes,
                                                   List<JCTree.JCExpression> newParamValues) {
        return maker.VarDef(
                modifiers,
                nameTable.fromString(name),
                maker.Ident(nameTable.fromString(type)),
                maker.NewClass(null,
                        newParamTypes,
                        maker.Ident(nameTable.fromString(newType)),
                        newParamValues,
                        null));
    }
}
