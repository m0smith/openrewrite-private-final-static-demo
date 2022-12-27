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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.java.Assertions.java;

class PrivateFinalStaticDemoTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PrivateFinalStaticDemo())
            .parser(JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(true)
                .classpath("guava"));
    }

    @Issue("https://rules.sonarsource.com/java/RSPEC-2325")
    @Test
    void methodsAccessingOnlyStaticsAreMadeStatic() {
        rewriteRun(
            java("""
                 class Utilities {
                     private static String magicWord = "magic";

                     private String getMagicWord() {
                         return magicWord;
                     }
                     
                     private void setMagicWord(String value) {
                         magicWord = value;
                     }
                     
                 }
                 """,
                 """
                 class Utilities {
                     private static String magicWord = "magic";

                     private static String getMagicWord() {
                         return magicWord;
                     }
                     
                     private static void setMagicWord(String value) {
                         magicWord = value;
                     }
                     
                 }
                 """)
        );
    }


    @Test
    void ignoreSerializableMethods() {
        rewriteRun(
            java("""
                 import java.io.*;
                 class Utilities implements Serializable {
                     private static String magicWord = "magic";

                     private String getMagicWord() {
                         return magicWord;
                     }
                     
                     private void setMagicWord(String value) {
                         magicWord = value;
                     }
                     
                     private void writeObject(java.io.ObjectOutputStream out) throws IOException {}
                     private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {}
                     private void readObjectNoData() throws ObjectStreamException {}
                 }
                 """,
                 """
                 import java.io.*;
                 class Utilities implements Serializable {
                     private static String magicWord = "magic";

                     private static String getMagicWord() {
                         return magicWord;
                     }
                     
                     private static void setMagicWord(String value) {
                         magicWord = value;
                     }
                     
                     private void writeObject(java.io.ObjectOutputStream out) throws IOException {}
                     private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {}
                     private void readObjectNoData() throws ObjectStreamException {}                 
                 }
                 """)
        );
    }
        
    @Test
    void methodsAccessingOnlyStaticsAreMadeStaticOuterClass() {
        rewriteRun(
            java("""
                 class Other {
                     static String magicWord = "magic";
                 }
                 class Util {
                     private String getMagicWord() {
                         return Outer::magicWord;
                     }

                     private void setMagicWord(String value) {
                         Outer::magicWord = value;
                     }
                 }  
                 """,
                 """
                 class Other {
                     static String magicWord = "magic";
                 }
                 class Util {
                     private static String getMagicWord() {
                         return Outer::magicWord;
                     }

                     private static void setMagicWord(String value) {
                         Outer::magicWord = value;
                     }
                 }               
                 """)
        );
    }

    @Test
    void ignoreMethodsAccessingInstanceVariable() {
        rewriteRun(
            java("""
                 class Utilities {
                     private String instanceVariable = "instance";

                     private String getInstanceVariable() {
                         return instanceVariable;
                     }
                     
                     private void setInstanceVariable(String value) {
                         instanceVariable = value;
                     }
                     
                 }
                 """)
        );
    }
    @Test
    void ignoreNonPrivateNonFinalMethods() {
        rewriteRun(
            java("""
                 class Utilities {
                     private static String magic = "magic";

                     String getInstanceVariable() {
                         return magic;
                     }
                     
                     public void setInstanceVariable(String value) {
                         magic = value;
                     }
                     
                 }
                 """)
        );
    }       


}
