import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.GenericVisitor

class javaParserUtil {


    companion object{
        //TODO fazer isto funcionar para os ifs e etc (recurssivo)
       fun contains(method:MethodDeclaration,contained:BodyDeclaration<*>):Boolean{

           if(!contained.isMethodDeclaration){
               return false
           }else{
               var contains=false
               val containedMethod=contained.asMethodDeclaration()
               //println("contained: "+containedMethod.name)
               method.body.get().statements.forEach {
                   if(it.isExpressionStmt){
                       if(it.asExpressionStmt().expression.isMethodCallExpr){
                           val expr=it.asExpressionStmt().expression.asMethodCallExpr()
                           contains= contains || compareMethod(expr,containedMethod)
                       }
                       else if (it.asExpressionStmt().expression.isAssignExpr){
                           val expr= it.asExpressionStmt().expression.asAssignExpr().value
                           if(expr.isMethodCallExpr){
                                contains= contains || compareMethod(expr.asMethodCallExpr(),containedMethod)
                           }
                           else if(expr.isBinaryExpr){
                               contains= contains || (binaryAux(expr.asBinaryExpr(),containedMethod))
                           }
                       }
                   }
               }
               return contains
           }
       }

        private fun binaryAux(bin:BinaryExpr,method:MethodDeclaration):Boolean{
            val left=bin.asBinaryExpr().left
            val right=bin.asBinaryExpr().right
            if(left.isMethodCallExpr){
                if(compareMethod(left.asMethodCallExpr(),method)){
                    return true
                }
            }else if(left.isBinaryExpr){ return binaryAux(left.asBinaryExpr(),method)}

            if(right.isMethodCallExpr){
                if(compareMethod(right.asMethodCallExpr(),method)){
                    return true
                }
            }else if(right.isBinaryExpr){ return binaryAux(right.asBinaryExpr(),method)}
            return false
        }

        private fun compareMethod(methodCall:MethodCallExpr,method:MethodDeclaration):Boolean{
            return(methodCall.name==method.name)
        }
    }
}