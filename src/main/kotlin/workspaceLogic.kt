import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.checkerframework.checker.builder.qual.CalledMethods
import pt.iscte.javardise.external.getOrNull
import java.lang.IllegalStateException
import java.util.Objects
import kotlin.math.min

internal class WorkSpaceModel():IObservable<(WorkSpaceModel.workSpaceModelEvents,WorkSpaceModel.WorkSpaceClass,WorkSpaceModel.WorkSpaceMethod?,Int?)->Unit>{

    var rootClasses= mutableListOf<WorkSpaceClass>()
    lateinit var solver:CombinedTypeSolver
    var allowDuplicates=false

    override val observers: MutableList<(workSpaceModelEvents, WorkSpaceClass, WorkSpaceMethod?, Int?) -> Unit> = mutableListOf()


    enum class workSpaceModelEvents{
        ADDEDCLASSANDMETHOD,
        ADDEDMETHOD,
        ADDEDMETHODANDREORGANIZED,
        REMOVEDMETHOD,
        REMOVEDMETHODANDREORGANIZED,
        REMOVEDCLASS,
        LEVELREORDERED
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
            //println("1")
            val parentClass=(method.parentNode.get() as ClassOrInterfaceDeclaration)
            //println("2")
            val (existsClass,clazz)=this.hasClass(parentClass)
            //println("3")
            val (isCalled,callingMethod)=this.contained(method,solver)
            //println("4")


            if(!existsClass && !isCalled){
                val newClass=WorkSpaceClass(parentClass,null,rootClasses,solver,this)
                val newMethod=WorkSpaceMethod(method,null, newClass)
                notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod,null) }
                //println("1")

            }
            if(existsClass && !isCalled){
                val (isCalling,calling)=clazz!!.isCalling(method,solver)
                if(!isCalling){
                    val newMethod=WorkSpaceMethod(method,null,clazz)
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD,clazz,newMethod,null) }
                    //println("2")
                }else{
                    val newMethod=WorkSpaceMethod(method,null,clazz)
                    calling.forEach { it.parent=newMethod }
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod,null) }
                    //println(7)
                }
                clazz.checkOrderOfMethods()

            }
            if (!existsClass && isCalled){
                val newClass=WorkSpaceClass(parentClass,callingMethod!!.clazz,rootClasses,solver,this)
                val newMethod=WorkSpaceMethod(method,callingMethod!!,newClass)
                notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod,null) }
                //println("3")

            }
            if(existsClass && isCalled){
                val (isCalling,calling)=clazz!!.isCalling(method,solver)

                if(callingMethod!!.clazz.thisClass.equals(parentClass)){
                    val newMethod=WorkSpaceMethod(method,callingMethod!!,callingMethod!!.clazz)
                    if(!isCalling) {
                        notifyObservers {
                            it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD, callingMethod!!.clazz, newMethod,null)
                        }
                        //println("6")
                    }else{
                        calling.forEach { it.parent=newMethod }
                        notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod,null) }
                    }
                }else{
                    val classInParent=callingMethod!!.clazz.children.firstOrNull { it.thisClass.equals(parentClass) }
                    if(classInParent!=null){
                        val newMethod=WorkSpaceMethod(method,callingMethod!!,classInParent)
                        if(!isCalling){
                            notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD,classInParent,newMethod,null) }
                            //println("4")
                        }else{
                            calling.forEach { it.parent=newMethod }
                            notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED,clazz,newMethod,null) }
                        }

                    }else{
                        val newClass=WorkSpaceClass(parentClass,callingMethod!!.clazz,rootClasses,solver,this)
                        val newMethod=WorkSpaceMethod(method,callingMethod!!,newClass)
                        notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD,newClass,newMethod,null) }
                        //println("5")
                    }
                }
                clazz.checkOrderOfMethods()
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
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHOD,clazz,foundMethod,null) }
                }else{
                    foundMethod!!.remove()
                    notifyObservers { it(WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHODANDREORGANIZED,clazz,foundMethod,null) }
                }
                if(clazz.isEmpty()){
                    clazz.remove()
                    notifyObservers {  it(workSpaceModelEvents.REMOVEDCLASS,clazz,foundMethod,null)}
                }
            }
            clazz.checkOrderOfMethods()
        }
    }

    fun removeClass(clazz:ClassOrInterfaceDeclaration){
        val (existsClass,clazzModel)=this.hasClass(clazz)
        if(!existsClass){
            throw IllegalStateException("nao encontrei essa classe, uhoh")
        }else{
            clazzModel!!.remove()
            notifyObservers {  it(workSpaceModelEvents.REMOVEDCLASS,clazzModel,null,null)}
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




    class WorkSpaceClass(val thisClass:ClassOrInterfaceDeclaration,val parent:WorkSpaceClass?,val rootClasses:MutableList<WorkSpaceClass>,val solver: CombinedTypeSolver,val model:WorkSpaceModel) {

        val containedMethods= mutableListOf<WorkSpaceMethod>()
        val children= mutableListOf<WorkSpaceClass>()
        var orderOfMethods= mutableListOf<MutableList<WorkSpaceMethod>>()

        init {
            parent?.addChild(this)
            if (parent==null) {
                rootClasses.add(this)
            }
        }

        internal fun addChild(myclass:WorkSpaceClass){
            children.add(myclass)
        }

        fun addMethod(workSpaceMethod: WorkSpaceModel.WorkSpaceMethod) {
            containedMethods.add(workSpaceMethod)
        }

        private fun printOrder(){
            orderOfMethods.forEachIndexed {index,it->
                println("LEVEL $index")
                it.forEach {
                    println("\t"+it.thisMethod.nameAsString)
                }
            }
        }

        internal fun checkOrderOfMethods(){
            val newOrder=this.methodOrderByLevel()
            var i=0
            var found=false
            while ((i< min( newOrder.size , orderOfMethods.size)) and !found){
                var j=0
                while ((j<min( newOrder[i].size , orderOfMethods[i].size)) and !found){
                    if (newOrder[i][j].thisMethod!=orderOfMethods[i][j].thisMethod){
                        model.notifyObservers { it(workSpaceModelEvents.LEVELREORDERED,this,newOrder[i][j],j)}
                        found=true
                    }
                    j++
                }
                i++
            }
            orderOfMethods=newOrder
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

        fun remove(){
            if (parent==null){
                rootClasses.remove(this)
                rootClasses.addAll(children)
            }else{
                parent.removeChildClass(this)
                children.forEach { parent.addChild(it) }
            }
            containedMethods.forEach { it.safeRemove() }
            containedMethods.clear()
        }

        fun removeChildClass(workSpaceClass: WorkSpaceClass){
            this.children.remove(workSpaceClass)
        }
    }

    class WorkSpaceMethod(val thisMethod:MethodDeclaration, parent:WorkSpaceMethod?, val clazz:WorkSpaceClass){
        var parent=parent
            set(value:WorkSpaceMethod?){
                if(value==null){
                    //field?.calledMethods?.remove(this)
                }
                else {
                    value.addChild(this)
                }
                field = value

            }

        val calledMethods=mutableListOf <WorkSpaceMethod>()
        init {
            parent?.addChild(this)
            clazz.addMethod(this)


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

        internal fun safeRemove(){
            parent?.removeChild(this)
            calledMethods.forEach { it.parent=null }
            calledMethods.clear()
        }

        fun remove(){
            clazz.removeChildMethod(this)
            safeRemove()
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