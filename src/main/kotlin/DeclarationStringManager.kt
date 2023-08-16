import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.SimpleName

internal class DeclarationStringManager(val method:MethodDeclaration) {
    val modifiers= mutableListOf<String>()
    var name:String
    var returnType:String
    val parameters= mutableMapOf<String,String>()

    init {
        method.modifiers.forEach{modifiers.add(it.toString())}
        name=method.nameAsString
        returnType=method.typeAsString
        method.parameters.forEach { parameters[it.nameAsString] = it.typeAsString }
    }

    override fun toString():String{
        var result=""
        modifiers.forEach { result+= "$it" }
        result+="$returnType"
        result+=" $name"
        result+="("
        var first=true
        parameters.forEach{
            if (!first){
                result+=", "
            }
            result+="${it.value} ${it.key}"
            first=false
        }
        result+=")"
        return result
    }

    fun lightToString():String{
        var result=""
        result+=" $name"
        result+="("
        var first=true
        parameters.forEach{
            if (!first){
                result+=", "
            }
            result+="${it.value}"
            first=false
        }
        result+=")"
        result+=": $returnType"
        return result
    }
    fun updateName(name:SimpleName){
        this.name=name.toString()
    }
}