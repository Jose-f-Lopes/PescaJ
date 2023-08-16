import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver

internal fun WorkSpaceModel.mytoString():String{
    val model=this
    val visitor=object: Visitor{
        var result=""
        var currentDepth=0

        override fun visit(w: WorkSpaceModel): Boolean {
            result+="LOADED CLASSES AND METHODS:\n"
            return true
        }

        override fun visit(c: WorkSpaceModel.WorkSpaceClass): Boolean {
            result+="\t".repeat(currentDepth)+"CLASS-"+c.thisClass.nameAsString+"\n"
            currentDepth++
            return true
        }

        override fun endvisit(c: WorkSpaceModel.WorkSpaceClass) {
            currentDepth--
        }

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            result+="\t".repeat(currentDepth)+"METHOD-"+m.thisMethod.nameAsString+"\n"
            currentDepth++
            return true
        }

        override fun endvisit(m: WorkSpaceModel.WorkSpaceMethod) {
            currentDepth--
        }

        override fun endVisit(w: WorkSpaceModel) {}
    }

    model.accept(visitor)
    return visitor.result
}

internal fun WorkSpaceModel.hasClass(classToFind:ClassOrInterfaceDeclaration):Pair<Boolean,WorkSpaceModel.WorkSpaceClass?>{
    val model=this
    val visitor=object:Visitor{
        var hasClass=false
        var theClass:WorkSpaceModel.WorkSpaceClass?=null

        override fun visit(c: WorkSpaceModel.WorkSpaceClass): Boolean {
            if(c.thisClass.equals(classToFind)){
                hasClass=true
                theClass=c
                return false
            }else{
                return true
            }
        }
    }
    model.accept(visitor)
    return Pair(visitor.hasClass,visitor.theClass)
}

internal fun WorkSpaceModel.hasMethod(methodToFind:MethodDeclaration):Pair<Boolean,WorkSpaceModel.WorkSpaceMethod?>{
    val model=this
    val visitor=object:Visitor{
        var hasMethod=false
        var theMethod:WorkSpaceModel.WorkSpaceMethod?=null

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            if(m.thisMethod.equals(methodToFind)){
                hasMethod=true
                theMethod=m
                return false
            }else{
                return true
            }
        }
    }
    model.accept(visitor)
    return Pair(visitor.hasMethod,visitor.theMethod)
}



internal fun WorkSpaceModel.contained(methodToFind:MethodDeclaration,solver:CombinedTypeSolver):Pair<Boolean,WorkSpaceModel.WorkSpaceMethod?>{
    val model=this
    val visitor=object:Visitor{
        var isCalled=false
        var callingMethod:WorkSpaceModel.WorkSpaceMethod?=null

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            if (javaParserUtil.contains(m.thisMethod,methodToFind,solver)){
                isCalled=true
                callingMethod=m
                return false
            }
            return true
        }
    }
    model.accept(visitor)
    println("is called: "+ visitor.isCalled)
    println("calling: "+visitor.callingMethod?.thisMethod?.nameAsString)
    return Pair(visitor.isCalled,visitor.callingMethod)
}

internal fun WorkSpaceModel.WorkSpaceClass.contained(methodToFind:MethodDeclaration,solver:CombinedTypeSolver):Pair<Boolean,WorkSpaceModel.WorkSpaceMethod?>{
    val model=this
    val visitor=object:Visitor{
        var isCalled=false
        var callingMethod:WorkSpaceModel.WorkSpaceMethod?=null

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            if (javaParserUtil.contains(m.thisMethod,methodToFind,solver)){
                isCalled=true
                callingMethod=m
                return false
            }
            return true
        }
    }
    model.accept(visitor)
    return Pair(visitor.isCalled,visitor.callingMethod)
}

internal fun WorkSpaceModel.depthOf(clazz:WorkSpaceModel.WorkSpaceClass):Int{
    val model=this
    val visitor=object:Visitor{
        var depth=0
        var result=0
        override fun visit(c: WorkSpaceModel.WorkSpaceClass): Boolean {
            depth++
            if(c.equals(clazz)){
                result=depth
                return false
            }
            return true
        }

        override fun endvisit(c: WorkSpaceModel.WorkSpaceClass) {
            depth--
        }
    }
    model.accept(visitor)
    return visitor.result
}

internal fun WorkSpaceModel.WorkSpaceMethod.inClassDepth():Int{
    val clazz=this.clazz
    val method=this
    val visitor=object :Visitor{
        var depth=0
        var result=0

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            depth++
            if(m.equals(method) && result<depth){
                result=depth
            }
            return true
        }

        override fun endvisit(m: WorkSpaceModel.WorkSpaceMethod) {
            depth--
        }
    }

    clazz.accept(visitor)
    return visitor.result
}

internal fun WorkSpaceModel.WorkSpaceClass.methodOrderByLevel(): MutableList<MutableList<WorkSpaceModel.WorkSpaceMethod>>{
    val clazz=this
    val visitor=object :Visitor{
        var depth=0
        var result= mutableListOf<MutableList<WorkSpaceModel.WorkSpaceMethod>>()

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            val realDepth = m.inClassDepth()
            depth++
            if (realDepth == depth) {
                val level = result.getOrNull(depth - 1)
                if (level == null) {
                    val newLevel = mutableListOf<WorkSpaceModel.WorkSpaceMethod>()
                    newLevel.add(m)
                    result.add(newLevel)
                } else {
                    level.add(m)
                }
            }
            return true
        }

        override fun endvisit(m: WorkSpaceModel.WorkSpaceMethod) {
            depth--
        }
    }

    clazz.accept(visitor)
    return visitor.result
}

internal fun WorkSpaceModel.WorkSpaceClass.isCalling(method:MethodDeclaration,solver:CombinedTypeSolver):Pair<Boolean,List<WorkSpaceModel.WorkSpaceMethod>>{
    val clazz=this
    val visitor=object :Visitor{

        val calledMethods=javaParserUtil.allCalledMethods(method,solver)
        var isCalling=false
        var calling= mutableListOf<WorkSpaceModel.WorkSpaceMethod>()

        override fun visit(m: WorkSpaceModel.WorkSpaceMethod): Boolean {
            if(calledMethods.contains(m.thisMethod)){
                isCalling=true
                calling.add(m)
            }
            return false
        }
    }
    clazz.accept(visitor)
    return Pair(visitor.isCalling,visitor.calling)
}

internal fun WorkSpaceModel.WorkSpaceClass.methodInDepth(depth:Int):List<WorkSpaceModel.WorkSpaceMethod>{
    val clazz=this
    return this.containedMethods.filter { it.inClassDepth()==depth }

}

internal fun WorkSpaceModel.WorkSpaceClass.isEmpty():Boolean{
    val clazz=this
    return this.containedMethods.isEmpty()
}