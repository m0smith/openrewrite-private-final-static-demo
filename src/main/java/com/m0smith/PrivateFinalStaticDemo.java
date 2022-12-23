/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m0smith;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Incubating(since = "7.0.0")
public class PrivateFinalStaticDemo extends Recipe { 

    @Override
    public String getDisplayName() {
        return "Private Final Static Demo";
    }

    @Override
    public String getDescription() {
        return "Non-overridable methods (`private` or `final`) that don’t access instance  data can be static to prevent any misunderstanding about the contract  of the method.";
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
	    @Override
	    public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext executionContext) {
		J.Identifier i = super.visitIdentifier(identifier, executionContext);

		// When a field access is not static, add a message so the method declaration knows it can skip it
		JavaType.Variable ft = i.getFieldType();
		// if (ft != null) {
		//     System.out.println("FT: " + ft);
		//     System.out.println("    OWNER:" + ft.getOwner());
		//     System.out.println("        CLASS:" + ft.getOwner().getClass().getName());
		//     // System.out.println(ft.getType());
		//     System.out.println("    CLASS:" + ft.getClass());
		//     System.out.println("    FLAGS:" + ft.getFlags());
		// }
		
		if(ft != null &&
		   !ft.hasFlags(Flag.Static) &&
		   // This should look something like TypeUtils.isOfType(ft.getOwner(), JavaType.Class)
		   // Except that doesn't work
		   "org.openrewrite.java.tree.JavaType$Class".equals(ft.getOwner().getClass().getName()))
		    {
			// System.out.println("NONSTATIC: " + ft);
			getCursor().putMessageOnFirstEnclosing(J.MethodDeclaration.class, "nonstatic", "true");
		    }		
		return i;
	    }
	    @Override
	    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ec) {
		J.MethodDeclaration m = super.visitMethodDeclaration(method, ec);

		// if this already has "static", we don't need to bother going any further; we're done
                if (m.hasModifier(J.Modifier.Type.Static)) {
                    return m;
                }


		// If it isn't final or private, ignore it; we are done
                if (!m.hasModifier(J.Modifier.Type.Private) && !m.hasModifier(J.Modifier.Type.Final)) {
                    return m;
                }
		String message = getCursor().getMessage("nonstatic");
		System.out.println("MESSAGE:" + message);
		if( "true".equals(message)) {
		    System.out.println("BAIL");
		    return m;
		}
		// // ignore instance fields
		// J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ec);
                // if (mv.getVariables().stream().anyMatch(v -> v.isField(getCursor()))) {
                //     return m;
                // }


		J.Modifier modifier =  new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList());
		m = autoFormat( m.withModifiers( ListUtils.concat(m.getModifiers(), modifier)), ec);
		return m;
	    }	
        };
    }
    
    // private boolean isDeclaredInForLoopControl(Cursor cursor) {
    //     return cursor.getParentTreeCursor()
    // 	    .getValue() instanceof J.ForLoop.Control;
    // }
    

}
