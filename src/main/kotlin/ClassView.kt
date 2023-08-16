import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import kotlin.reflect.KClass

internal class ClassView(val parent:CodeExplorerWorkSpace, val clazz:WorkSpaceModel.WorkSpaceClass, val parentComposite: Composite,
                         override val controller: WorkSpace):CustomComposite,MethodContainer{

    val focusedColor= Color(173,216,230)
    val unfocusedColor= Color(240,240,240)
    private val classComposite: Composite
    val topComponent:Scrollable
    internal val levels:VerticalLevelStructure<WorkSpaceModel.WorkSpaceMethod>
    override val methodViews= mutableListOf<MethodView>()
    override fun closeMethod(method: MethodDeclaration) {
        parent.workSpaceModel.removeMethod(method)
    }

    val classDisplay:Composite

    private val viewMode= JavardiseView::class as KClass<MethodView>



    init {
        classComposite= Composite(parentComposite, SWT.BORDER)
        val gridLayout=GridLayout()
        gridLayout.marginWidth=0
        gridLayout.marginHeight=0
        classComposite.layout=gridLayout

        val header=DefaultClassHeader(classComposite,clazz.thisClass)
        topComponent=header.getComposite()

        classDisplay=Composite(classComposite,SWT.NONE)
        levels=VerticalLevelStructure<WorkSpaceModel.WorkSpaceMethod>(classDisplay,false)

        classComposite.addDisposeListener {
            parent.classViwers.remove(this)
            //classDisplay.dispose()
            //header.getComposite().dispose()
        }
        ContextMenu(classComposite)
    }

    private fun ContextMenu(composite:Composite){
        val menu = Menu(composite)
        val close = MenuItem(menu, SWT.PUSH)
        close.text = "Close Class"
        close.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                parent.closeClass(clazz.thisClass)
            }
        })
        val openDoc = MenuItem(menu, SWT.PUSH)
        openDoc.text = "Open Documentation"
        openDoc.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                val ws=parent.parent.openWorkSpace((methodDocWorkSpace::class as KClass<WorkSpace>))
                ws.classSelected(clazz.thisClass)
            }
        })
        composite.menu = menu
    }



    fun addMethod(method: WorkSpaceModel.WorkSpaceMethod){
        levels.addWidget(method,method.inClassDepth(),::initWidget)
        //levels.addWidget(method,method.inClassDepth(),::debugInitWidget)
    }

    fun addMethodAndReorganize(method: WorkSpaceModel.WorkSpaceMethod) {
        levels.addAndReorganize(method,method.inClassDepth(),::initWidget,{it.calledMethods},{it.inClassDepth()})
    }


    fun removeMethod(method: WorkSpaceModel.WorkSpaceMethod) {
        levels.removeWidget(method)
    }
    fun removeMethodAndReorganize(method: WorkSpaceModel.WorkSpaceMethod) {
        levels.removeAndReoganize(method,::initWidget,{it.calledMethods},{it.inClassDepth()})
    }
    fun reorderLevelInClass(method: WorkSpaceModel.WorkSpaceMethod,placeInLevel:Int) {
        levels.reorderLevel(placeInLevel,method,method.inClassDepth()-1)
    }


    private fun initWidget(methodModel: WorkSpaceModel.WorkSpaceMethod,parent: Composite):Composite {
        val method = methodModel.thisMethod
        val methodView = viewMode
        return try {
            val widget = methodView.constructors.first().call(parent, method, this)
            widget.addObserver { event, methodclicked ->
                if (event == MethodView.EventType.METHODCLICKED) {
                    val methodDeclaration = javaParserUtil.expressionToDeclaration(methodclicked, this.parent.solver)
                    methodDeclaration?.let { this.parent.workSpaceModel.addMethod(methodDeclaration) }
                }
            }
            widget.getComposite()
        }catch (e:Exception){
            println("nao consegui abrir esse metodo :(")
            println(e.printStackTrace())
            Composite(parent,SWT.BORDER)
        }
    }

    override fun getComposite(): Composite {
        return classComposite
    }

}


internal interface MethodContainer{
    val methodViews:MutableList<MethodView>
    val controller:WorkSpace

    //fun closeClass(clazz: ClassOrInterfaceDeclaration)
    fun closeMethod(method: MethodDeclaration)

}



