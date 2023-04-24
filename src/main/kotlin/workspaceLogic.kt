import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.checkerframework.checker.builder.qual.CalledMethods
import java.lang.IllegalStateException
import java.util.Objects

internal class WorkSpaceModel():IObservable<(WorkSpaceModel.workSpaceModelEvents,WorkSpaceModel.WorkSpaceClass,WorkSpaceModel.WorkSpaceMethod)->Unit>{

    var rootClasses= mutableListOf<WorkSpaceClass>()
    lateinit var solver:CombinedTypeSolver
    var allowDuplicates=true

    override val observers: MutableList<(workSpaceModelEvents, WorkSpaceClass, WorkSpaceMethod) -> Unit> = mutableListOf()


    enum class workSpaceModelEvents{
        ADDEDCLASSANDMETHOD,
        ADDEDMETHOD,
        ADDEDMETHODANDREORGANIZED,
        REMOVEDMETHOD,
        REMOVEDMETHODANDREORGANIZED

    }

    fun addSolver(solver:CombinedTypeSolver){
        this.solver=solver
    }

    fun addMethod(method:MethodDeclaration){
        if(allowDuplicates or (!allowDuplicates && !hasMethod(method).first)){
            if(!this::solver.isInitialized){
                println("solver is null at workSpaceModel")
                return
            }
            val parentClass=(method.parentNode.get() as ClassOrInterfaceDeclaration)

            val (existsClass,clazz)=this.hasClass(parentClass)
            val (isCalled,callingMethod)=this.contained(method,solver)


            if(!existsClass && !isCalled){
                val newClass=WorkSpaceClass(parentClass,null)
                val newMethod=WorkSpaceMethod(method,null, newClass)
                rootClasses.add(newClass)
                notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod) }
                //println("1")

            }
            if(existsClass && !isCalled){
                val (isCalling,calling)=clazz!!.isCalling(method,solver)
                if(!isCalling){
                    val newMethod=WorkSpaceMethod(method,null,clazz)
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD,clazz,newMethod) }
                    //println("2")
                }else{
                    val newMethod=WorkSpaceMethod(method,null,clazz)
                    calling!!.parent=newMethod
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod) }
                    //println(7)
                }

            }
            if (!existsClass && isCalled){
                val newClass=WorkSpaceClass(parentClass,callingMethod!!.clazz)
                val newMethod=WorkSpaceMethod(method,callingMethod!!,newClass)
                notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod) }
                //println("3")

            }
            if(existsClass && isCalled){
                val (isCalling,calling)=clazz!!.isCalling(method,solver)

                if(callingMethod!!.clazz.thisClass.equals(parentClass)){
                    val newMethod=WorkSpaceMethod(method,callingMethod!!,callingMethod!!.clazz)
                    if(!isCalling) {
                        notifyObservers {
                            it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD, callingMethod!!.clazz, newMethod)
                        }
                        //println("6")
                    }else{
                        calling!!.parent=newMethod
                        notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod) }
                    }
                }else{
                    val classInParent=callingMethod!!.clazz.children.firstOrNull { it.thisClass.equals(parentClass) }
                    if(classInParent!=null){
                        val newMethod=WorkSpaceMethod(method,callingMethod!!,classInParent)
                        if(!isCalling){
                            notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD,classInParent,newMethod) }
                            //println("4")
                        }else{
                            calling!!.parent=newMethod
                            notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod) }
                        }

                    }else{
                        val newClass=WorkSpaceClass(parentClass,callingMethod!!.clazz)
                        val newMethod=WorkSpaceMethod(method,callingMethod!!,newClass)
                        notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod) }
                        //println("5")
                    }
                }
            }
        }
    }

    fun removeMethod(method:MethodDeclaration){
        if(!this::solver.isInitialized){
            throw IllegalStateException("solver is null at workSpaceModel")
        }
        val parentClass=(method.parentNode.get() as ClassOrInterfaceDeclaration)
        val (existsClass,clazz)=this.hasClass(parentClass)
        val (isCalled,callingMethod)=this.contained(method,solver)
        if(!existsClass){
            throw IllegalStateException("Algo de errado nao está certo-> tried to delete method and there was no class")
        }else{
            val (isCalling,calling)=clazz!!.isCalling(method,solver)
            val (hasMethod,foundMethod)=hasMethod(method)
            if(!hasMethod){
                throw IllegalStateException("nao encontrei o meteodo que queres apagar, muito estranho")
            }else{
                if(!isCalling){
                    foundMethod!!.remove()
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHOD,clazz,foundMethod) }
                }else{
                    calling!!.parent=null
                    foundMethod!!.remove()
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHODANDREORGANIZED,clazz,foundMethod) }
                }
                if(clazz.isEmpty()){
                    //REMOVE CLASS
                }
            }
        }
    }

    fun accept(v:Visitor){
        if(v.visit(this)){
            rootClasses.forEach { it.accept(v) }
        }
        v.endVisit(this)
    }

    fun unfoldClass(clazz:ClassOrInterfaceDeclaration){
        clazz.methods.filter {!javaParserUtil.isCalledInClass(it,clazz,solver) }.forEach {
            addMethod(it)
            javaParserUtil.allCalledMethods(it,solver).forEach { addMethod(it) }
        }
    }

    fun unfoldClass2(clazz:ClassOrInterfaceDeclaration){
        clazz.methods.filter {!javaParserUtil.isCalledInClass(it,clazz,solver) }.forEach {
            addMethod(it)
        }
    }



    class WorkSpaceClass(val thisClass:ClassOrInterfaceDeclaration,parent:WorkSpaceClass?) {

        val containedMethods= mutableListOf<WorkSpaceMethod>()
        val children= mutableListOf<WorkSpaceClass>()

        init {
            parent?.addChild(this)
        }

        internal fun addChild(myclass:WorkSpaceClass){
            children.add(myclass)
        }

        fun addMethod(workSpaceMethod: WorkSpaceModel.WorkSpaceMethod) {
            containedMethods.add(workSpaceMethod)
        }

        fun accept(v:Visitor){
            if(v.visit(this)) {
                containedMethods.forEach { it.accept(v) }
                children.forEach { it.accept(v) }
            }
            v.endvisit(this)
        }

        fun removeChildMethod(workSpaceMethod: WorkSpaceModel.WorkSpaceMethod) {
            containedMethods.remove(workSpaceMethod)
        }
    }

    class WorkSpaceMethod(val thisMethod:MethodDeclaration, parent:WorkSpaceMethod?, val clazz:WorkSpaceClass){
        var parent=parent
            set(value:WorkSpaceMethod?){
                value?.addChild(this)
                field = value
            }

        val calledMethods=mutableListOf <WorkSpaceMethod>()
        init {
            clazz.addMethod(this)
            parent?.addChild(this)
        }

        private fun addChild(workSpaceMethod: WorkSpaceModel.WorkSpaceMethod) {
            calledMethods.add(workSpaceMethod)
        }

        fun accept(v: Visitor) {
            if(v.visit(this)){
                calledMethods.forEach { it.accept(v) }
            }
            v.endvisit(this)
        }

        fun remove(){
            clazz.removeChildMethod(this)
            parent?.removeChild(this)
        }

        private fun removeChild(workSpaceMethod: WorkSpaceModel.WorkSpaceMethod) {
            calledMethods.remove(workSpaceMethod)
        }

    }


}

internal interface Visitor{
    fun visit(w:WorkSpaceModel):Boolean=true
    fun endVisit(w:WorkSpaceModel){}

    fun visit(c:WorkSpaceModel.WorkSpaceClass):Boolean=true
    fun endvisit(c:WorkSpaceModel.WorkSpaceClass){}

    fun visit(m:WorkSpaceModel.WorkSpaceMethod):Boolean=true
    fun endvisit(m:WorkSpaceModel.WorkSpaceMethod){}

}