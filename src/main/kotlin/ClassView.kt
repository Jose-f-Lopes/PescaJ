import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.external.focusGained
import pt.iscte.javardise.external.focusLost
import pt.iscte.javardise.widgets.members.FieldWidget
import java.lang.IllegalArgumentException

internal interface ClassView:CustomComposite{

    var isFocused:Boolean
    var focusedMethod:MethodView?
    val methodViews:MutableList<MethodView>

    val clazz:WorkSpaceModel.WorkSpaceClass
    fun addMethod(method: WorkSpaceModel.WorkSpaceMethod)
    fun addMethodAndReorganize(method:WorkSpaceModel.WorkSpaceMethod)

    fun clearFocus(methodView: MethodView?)

}

internal class DebugClassView(val parent:WorkSpace,override val clazz: WorkSpaceModel.WorkSpaceClass):ClassView{

    val mycomposite:Composite
    override var isFocused=false
    override var focusedMethod:MethodView?=null
    override val methodViews= mutableListOf<MethodView>()

    init {
        mycomposite= Composite(parent.getComposite(),SWT.BORDER)
        val layout=GridLayout()
        layout.numColumns=1
        mycomposite.layout=layout

        val text=Text(mycomposite,SWT.BORDER)
        text.text=clazz.thisClass.nameAsString

        println("DEBUG added class to viewer")

        mycomposite.pack()
        println(mycomposite.bounds)


    }
    override fun addMethod(method: WorkSpaceModel.WorkSpaceMethod) {
        //val widget=plainTextView(mycomposite,method.thisMethod)
    }

    override fun addMethodAndReorganize(method: WorkSpaceModel.WorkSpaceMethod) {

    }

    override fun clearFocus(methodView: MethodView?) {

    }

    override fun getComposite(): Composite {
        return mycomposite
    }

}


internal class DefaultClassView(val parent:WorkSpace, override val clazz:WorkSpaceModel.WorkSpaceClass):ClassView{

    val focusedColor= Color(173,216,230)
    val unfocusedColor= Color(240,240,240)
    private val classComposite: Composite
    val topComponent:Scrollable
    val levels:VerticalLevelStructure<WorkSpaceModel.WorkSpaceMethod>
    override val methodViews= mutableListOf<MethodView>()
    override var focusedMethod:MethodView?=null
    override var isFocused=false
        set(value){
            if(value!=field){
                if (value==true){
                    parent.clearFocus(this@DefaultClassView)
                    classComposite.background=focusedColor
                }else{
                    classComposite.background=unfocusedColor
                }
            }
            field=value
        }



    init {
        classComposite= Composite(parent.getComposite(), SWT.BORDER)
        val formLayout= FormLayout()
        classComposite.layout=formLayout

        classComposite.addMouseListener(object :MouseListener{
            override fun mouseDoubleClick(p0: MouseEvent?) {
            }
            override fun mouseDown(p0: MouseEvent?) {
                this@DefaultClassView.isFocused=true
            }
            override fun mouseUp(p0: MouseEvent?) {
            }

        })


        val text= Text(classComposite, SWT.BORDER)
        text.text=clazz.thisClass.nameAsString
        val formData= FormData()
        formData.top= FormAttachment(0,5)
        text.layoutData=formData
        topComponent=text

//        val widget=FieldWidget(classComposite,clazz.thisClass.fields.first())
//        val formData3= FormData()
//        formData3.top= FormAttachment(topComponent,5)
//        widget.layoutData=formData3



        val classDisplay=Composite(classComposite,SWT.NONE)
        val formData2= FormData()
        formData2.top= FormAttachment(topComponent,5)
        formData2.left=FormAttachment(0,0)
        classDisplay.layoutData=formData2
        levels=VerticalLevelStructure<WorkSpaceModel.WorkSpaceMethod>(classDisplay)




        //classComposite.pack()


    }

    override fun addMethod(method: WorkSpaceModel.WorkSpaceMethod){

        isFocused=true
        levels.addWidget(method,method.inClassDepth(),::initWidget)
        classComposite.update()
        classComposite.pack()

    }

    override fun addMethodAndReorganize(method: WorkSpaceModel.WorkSpaceMethod) {
        levels.addAndReorganize(method,method.inClassDepth(),::initWidget,::getCalled,::getDepth)

    }

    override fun clearFocus(methodView: MethodView?) {
        methodViews.filter { it!=methodView }.forEach { it.isFocused=false }
    }

    private fun getCalled(method:WorkSpaceModel.WorkSpaceMethod):List<WorkSpaceModel.WorkSpaceMethod> {
        return (method as WorkSpaceModel.WorkSpaceMethod).calledMethods
    }

    private fun getDepth(method:WorkSpaceModel.WorkSpaceMethod):Int {
        return (method as WorkSpaceModel.WorkSpaceMethod).inClassDepth()
    }

    private fun initWidget(methodModel: WorkSpaceModel.WorkSpaceMethod,parent: Composite):Composite{
        val method=methodModel.thisMethod
        val methodView=this.parent.getSelectedView()
        val widget=methodView.constructors.first().call(parent,method,this)
        widget.addObserver{event,methodclicked->
            if(event==MethodView.EventType.METHODCLICKED){
                val methodDeclaration=javaParserUtil.expressionToDeclaration(methodclicked,this.parent.solver)
                methodDeclaration?.let {this.parent.workSpaceModel.addMethod(methodDeclaration) }
            }
        }
        return widget.getComposite()
    }


    override fun getComposite(): Composite {
        return classComposite
    }
}

internal class VerticalLevelStructure<T>(val parent:Composite){
    private val levels= mutableListOf<VerticalLevel<T>>()
    val gridLayout:GridLayout
    var deepestLevel=-1


    init {
        gridLayout= GridLayout()
        gridLayout.numColumns = 0
        parent.layout = gridLayout


//        val composite=Text(parent,SWT.BORDER)
//        composite.text="hello"


    }

    fun addLevel(){
        deepestLevel++
        gridLayout.numColumns=deepestLevel+1
        parent.layout=gridLayout
        val newLevel=VerticalLevel<T>(parent,deepestLevel)
        levels.add(newLevel)
        parent.update()
        parent.pack()

    }


    internal fun addWidget(any:T,depth:Int,builderFunction: (T, Composite) -> Composite){
        val level=getLevel(depth)
        level.addWidget(builderFunction,any)
    }

    private fun getLevel(depth:Int):VerticalLevel<T>{
        if (depth<1){
            throw IllegalArgumentException("depth is negative or 0")
        }
        if(depth>deepestLevel+1){
            addLevel()
            return getLevel(depth)
        }else{
            return levels[depth-1]
        }
    }

    fun addAndReorganize(any:T, inClassDepth: Int, initFun: (T,Composite)->Composite,called:(T)->List<T>, depthFun:(T)->Int) {
        val calledThings=called(any)
        for(thing in calledThings){
            levels.first{it.isInLevel(thing)}.removeWidget(thing)
        }
        addWidget(any,inClassDepth,initFun)
        for(thing in calledThings){
            addAndReorganize(thing,depthFun(thing),initFun,called,depthFun)
        }

    }


    private class VerticalLevel<T>(parent:Composite,val depth:Int){
        val level:Composite
        val listOfWidgets= mutableMapOf<T,Composite>()

        init {
            level=Composite(parent,SWT.BORDER or SWT.V_SCROLL)
            val formLayout= FormLayout()
            level.layout=formLayout

        }

        fun addWidget(builderFunction:(T,Composite)->Composite,thing:T){
            val composite=builderFunction(thing,level)
            val data=FormData()
            data.left= FormAttachment(0,0)
            if(listOfWidgets.isEmpty()){
                data.top=FormAttachment(0,0)
            }else{
                data.top=FormAttachment(listOfWidgets.values.last(),0)
            }
            composite.layoutData=data
            listOfWidgets[thing]=composite
            level.pack()
        }

        fun removeWidget(id:T){
            listOfWidgets[id]?.dispose()
            listOfWidgets.remove(id)

        }

        fun isInLevel(id:T):Boolean{
            return listOfWidgets.keys.contains(id)
        }
    }

}

