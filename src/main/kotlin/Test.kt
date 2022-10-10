import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.SimpleName
import java.io.File

fun main() {
    val cu = StaticJavaParser.parse("class A {}")
    val a = cu.getClassByName("A")
    a.get().addField("int", "i")

    println(a.get())

    val src = StaticJavaParser.parse(File("src/main/kotlin/TestExample.java"))
    val dec : ClassOrInterfaceDeclaration = src.getClassByName(src.primaryTypeName.get()).get()

    val m = dec.addMethod("m3", Modifier.Keyword.PRIVATE)
    m.createBody().addStatement("return 3;")

    m.name = SimpleName("m4")
    dec.methods.forEach {
        println(it)

    }
}
