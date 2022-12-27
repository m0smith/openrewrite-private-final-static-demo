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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Incubating(since = "7.0.0")
public class PrivateFinalStaticDemo extends Recipe {

    private static final List<String> SERIALIZABLE_METHODS_LIST = Arrays.asList("readObjectNoData", "readObject", "writeObject");
    private static final Set<String> SERIALIZABLE_METHODS = new HashSet(SERIALIZABLE_METHODS_LIST);

    @Override
    public String getDisplayName() {
        return "Private Final Static Demo";
    }

    @Override
    public String getDescription() {
        return "Non-overridable methods (`private` or `final`) that don’t access instance  data can be static to prevent any misunderstanding about the contract  of the method.";
    }

    /**
       Return true is `md` is declared in a Serializable class.
    */
    private boolean isInSerializableClass(Cursor cursor){
	J.ClassDeclaration cd = cursor.firstEnclosing(J.ClassDeclaration.class);
	if( cd == null ) {
	    return false;
	}
	List<TypeTree> impls = cd.getImplements();
	if( impls == null) {
	    return false;
	}
	for(TypeTree impl : impls){
	    // TODO: This is code smell.  Should be comparing the class not the class name
	    if( "java.io.Serializable".equals(impl.getType().toString())) {
		return true;
	    }
	}
	return false;
    }

    /**
       Return true is `md` declares one of the Serializable methods.
       @see https://docs.oracle.com/javase/7/docs/api/java/io/ObjectInputStream.html
     */
    private boolean isSerializableMethod(J.MethodDeclaration md) {
	String name = md.getName().getSimpleName();
	return SERIALIZABLE_METHODS.contains(name);
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
	    /**
	       Visit every identifier looking for instance varaibles.
	       If any are found, send a message to the enclosing
	       method declaration that it references a non-static
	       varable.
	     */ 
	    @Override
	    public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext executionContext) {
		J.Identifier i = super.visitIdentifier(identifier, executionContext);

		// When a field access is not static, send a message
		// so the method declaration knows it can skip it
		JavaType.Variable ft = i.getFieldType();
		if(ft != null &&
		   !ft.hasFlags(Flag.Static) &&
		   // TODO: The following should look something like TypeUtils.isOfType(ft.getOwner(), JavaType.Class)
		   // Except that doesn't work due to scoping.  I am sure there is something better than this.
		   "org.openrewrite.java.tree.JavaType$Class".equals(ft.getOwner().getClass().getName()))
		    {
			getCursor().putMessageOnFirstEnclosing(J.MethodDeclaration.class, "nonstatic", "true");
		    }		
		return i;
	    }
	    /**
	       Look for non-static methods that are private or final.
	       If it hasn't recevied a message that it references to
	       non-static variable, mark it as static.
	     */
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

		Cursor cursor = getCursor();
		
		// If the method is a member of the Serialiazable related methods, don't make it static
		if(isInSerializableClass(cursor) && isSerializableMethod(m)){
		    System.out.println("SKIPPNG SERIALIZABLE:" + m);
		    return m;
		}

		// If a "nonstatic" message was received, we are done.
		String message = cursor.getMessage("nonstatic");
		if( "true".equals(message)) {
		    return m;
		}

		// Looks good, mark it static.
		J.Modifier modifier =  new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList());
		m = autoFormat( m.withModifiers( ListUtils.concat(m.getModifiers(), modifier)), ec);
		return m;
	    }	
        };
    }
}
