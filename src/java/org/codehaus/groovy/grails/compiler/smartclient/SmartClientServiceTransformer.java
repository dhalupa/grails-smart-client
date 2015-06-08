package org.codehaus.groovy.grails.compiler.smartclient;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.compiler.injection.AllArtefactClassInjector;
import org.codehaus.groovy.grails.compiler.injection.AstTransformer;
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.smartclient.annotation.NamedParam;
import org.grails.plugins.smartclient.annotation.Remote;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * AST transformer which injects {@link NamedParam} parameter annotation for the purpose of preserving parameter name
 *
 * @author Denis Halupa
 */
@AstTransformer
public class SmartClientServiceTransformer implements AllArtefactClassInjector {

    private static final AnnotationNode REMOTE_ANNOTATION_NODE = new AnnotationNode(new ClassNode(Remote.class));
    public static Pattern SERVICE_PATTERN = Pattern.compile(".+/" +
            GrailsResourceUtils.GRAILS_APP_DIR + "/services/(.+)Service\\.groovy");


    public void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode) {
        for (MethodNode method : classNode.getMethods()) {
            if (!method.isStatic() && method.isPublic() && !method.getAnnotations(REMOTE_ANNOTATION_NODE.getClassNode()).isEmpty()) {
                for (Parameter p : method.getParameters()) {
                    AnnotationNode rn = new AnnotationNode(new ClassNode(NamedParam.class));
                    rn.setMember("value", new ConstantExpression(p.getName()));
                    p.addAnnotation(rn);
                }
            }
        }
    }


    public void performInjection(SourceUnit source, ClassNode classNode) {
        performInjection(source, null, classNode);
    }

    public void performInjectionOnAnnotatedClass(SourceUnit sourceUnit, ClassNode classNode) {

    }

    public boolean shouldInject(URL url) {
        return url != null && SERVICE_PATTERN.matcher(url.getFile()).find();
    }
}
