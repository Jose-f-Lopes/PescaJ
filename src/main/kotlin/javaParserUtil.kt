import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import pt.iscte.javardise.external.getOrNull
import java.lang.RuntimeException

class javaParserUtil {


    companion object{

        //BUG sometimes this doesn't work if the file has a package header (?????)
        fun contains(method: MethodDeclaration,contained: BodyDeclaration<*>,solver:CombinedTypeSolver):Boolean{
            var contains=false


            method.findAll(MethodCallExpr::class.java).forEach {
                try {
                    val jpf = JavaParserFacade.get(solver).solve(it)
                    //println(jpf.correspondingDeclaration.javaClass)
                    if (jpf.isSolved && jpf.correspondingDeclaration is JavaParserMethodDeclaration) {
                        val methodDecl = (jpf.correspondingDeclaration as JavaParserMethodDeclaration)
                        val methodDeclNode = methodDecl.wrappedNode
                        if(methodDeclNode==contained){
                            contains=true
                        }
                        //val calledMethodclassOrInterface = methodDeclNode.parentNode.get() as TypeDeclaration<*>
                    }
                } catch (e: UnsolvedSymbolException) {
                    println("contains catch 1")
                    println(e.message)
                } catch (e2: RuntimeException) {
                    println("contains catch 2")
                    println(e2.message)
                }
            }
            println("contains method: "+contains)
            return contains
        }

        fun expressionToDeclaration(expr:MethodCallExpr,solver: CombinedTypeSolver):MethodDeclaration?{
            //expr.
            try {
                val jpf = JavaParserFacade.get(solver).solve(expr)
                if (jpf.isSolved && jpf.correspondingDeclaration is JavaParserMethodDeclaration) {
                    val methodDecl = (jpf.correspondingDeclaration as JavaParserMethodDeclaration)
                    return methodDecl.wrappedNode
                }
            } catch (e: UnsolvedSymbolException) {
                println(e.message)
            } catch (e2: RuntimeException) {
                println(e2.message)
            }
            return null
        }

        fun isCalledInClass(method:MethodDeclaration,clazz:ClassOrInterfaceDeclaration,solver:CombinedTypeSolver):Boolean{
            clazz.methods.forEach {
                if (contains(it,method,solver)){
                    return true
                }
            }
            return false
        }

        fun allCalledMethods(method:MethodDeclaration,solver:CombinedTypeSolver):List<MethodDeclaration>{
            val result= mutableListOf<MethodDeclaration>()
            method.findAll(MethodCallExpr::class.java).forEach {
                try {
                    val jpf = JavaParserFacade.get(solver).solve(it)
                    //println(jpf.correspondingDeclaration.javaClass)
                    if (jpf.isSolved && jpf.correspondingDeclaration is JavaParserMethodDeclaration) {
                        val methodDecl = (jpf.correspondingDeclaration as JavaParserMethodDeclaration)
                        val methodDeclNode = methodDecl.wrappedNode
                        if(!result.contains(methodDeclNode)){
                            result.add(methodDeclNode)
                        }
                        //val calledMethodclassOrInterface = methodDeclNode.parentNode.get() as TypeDeclaration<*>
                    }
                } catch (e: UnsolvedSymbolException) {
                    println(e.message)
                } catch (e2: RuntimeException) {
                    println(e2.message)
                }
            }

            return result
        }

        fun nonDocumentedAPI(clazz:ClassOrInterfaceDeclaration):List<MethodDeclaration>{
            val result= mutableListOf<MethodDeclaration>()
            clazz.methods.filter{it.isPublic}.forEach {
                it.javadoc.getOrNull?:result.add(it)
            }
            return result
        }

        fun nonDocumentedPrivate(clazz:ClassOrInterfaceDeclaration):List<MethodDeclaration>{
            val result= mutableListOf<MethodDeclaration>()
            clazz.methods.filter{!it.isPublic}.forEach {
                it.javadoc.getOrNull?:result.add(it)
            }
            return result
        }
    }
}