import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import java.awt.Point
import kotlin.reflect.KClass


internal class WorkSpaceViewerManager(parent:Composite,val shell:Shell,val quintalDJV2: QuintalDJV2):CustomComposite{

    private val tabFolder:Composite
    private val addTab:CTabItem
    private var counter=0
    private val registeredWorkSpaces= mutableMapOf<CTabItem,WorkSpace>()
    lateinit var solver:CombinedTypeSolver

    private var workSpaceOptions=mapOf<String,Boolean>("Default" to false,"Class Unfold" to true)
    private var selectedOption:Boolean = false


    override fun getComposite(): Composite {
        return tabFolder
    }

    init {

        tabFolder = CTabFolder(parent, SWT.BORDER)
        //val layout=FillLayout()
        //tabFolder.layout=layout
        addTab= CTabItem(tabFolder,SWT.NULL)
        addTab.text="+"
        openWorkSpace()
        quintalDJV2.initWorkSpaceMenuFromMap(workSpaceOptions,this)

        tabFolder.addMouseListener(object : org.eclipse.swt.events.MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {}
            override fun mouseDown(p0: MouseEvent?) {
                val point = Point(p0?.x ?: 0, p0?.y ?: 0)
                if (point.x >= addTab.bounds.x && point.y >= addTab.bounds.y && point.x <= addTab.bounds.width + addTab.bounds.x && point.y <= addTab.bounds.y + addTab.bounds.height) {
                    openWorkSpace()
                }
            }
            override fun mouseUp(p0: MouseEvent?) {}
        })
    }

    internal fun workSpaceOptionSelected(option:Boolean){
        selectedOption=option
        openWorkSpace()
    }

    fun openWorkSpace(){

        val ws=WorkSpace(this@WorkSpaceViewerManager, shell,counter,selectedOption)
        if(this@WorkSpaceViewerManager::solver.isInitialized){
            ws.addSolver(solver)
        }
        counter++
        (tabFolder as CTabFolder).setSelection((tabFolder as CTabFolder).items.size - 1)
        quintalDJV2.initToggles(this)
    }

    public fun addSolver(solver:CombinedTypeSolver){
        this.solver=solver
        registeredWorkSpaces.values.forEach { it.addSolver(solver) }
    }
    fun registerWorkSpace(workSpace:WorkSpace){
        registeredWorkSpaces[workSpace.getTab()]=workSpace
    }
    fun removeFromRegister(workSpace: WorkSpace){
        registeredWorkSpaces.remove(workSpace.getTab())
    }
    fun currentWorkSpace():WorkSpace?{
        val currenttab=(tabFolder as CTabFolder).selection
        return registeredWorkSpaces[currenttab]

    }

}

internal class WorkSpace(parent:WorkSpaceViewerManager, val shell:Shell,counter:Int,val option: Boolean):CustomComposite,LeveledStructure{

    private val tabItem:CTabItem
    val workSpace:Composite
    val workSpaceModel:WorkSpaceModel = WorkSpaceModel()
    val classViwers= mutableListOf<ClassView>()
    var javardise=false
    var allowDuplicate=true
        set(value) {
            value
            field = value
            workSpaceModel.allowDuplicates =value

        }
    lateinit var solver:CombinedTypeSolver


    override val levels= mutableListOf<MutableList<CustomComposite>>()
    override val topComponent=null

    fun methodSelected(method:MethodDeclaration){
        if (option){
            workSpaceModel.unfoldClass(method.parentNode.get() as ClassOrInterfaceDeclaration)
        }else {
            workSpaceModel.addMethod(method)
        }
    }

    override fun getComposite(): Composite {
       return  workSpace
    }
    fun getTab():CTabItem{
        return tabItem
    }

    fun addSolver(solver:CombinedTypeSolver){
        this.solver=solver
        workSpaceModel.addSolver(solver)
    }

    fun getSelectedView():KClass<MethodView> {
        if (javardise){
            return JavardiseView::class as KClass<MethodView>
        }else{
            return plainTextView::class as KClass<MethodView>
        }
    }

    fun clearFocus(classView: ClassView) {
        classViwers.filter { it!=classView }.forEach {
            it.isFocused=false
            it.clearFocus(null)
        }
    }

    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "WorkSpace $counter"
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }


        workSpace = Composite(parent.getComposite(),SWT.BORDER )
        val formLayout=FormLayout()
        workSpace.layout=formLayout
//        val gridLayout = GridLayout()
//        gridLayout.numColumns = 1
//        workSpace.layout = gridLayout

        tabItem.control = workSpace
        parent.registerWorkSpace(this)

        workSpaceModel.addObserver{event,clazz,method->
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD){

                val viewer:ClassView=DefaultClassView(this,clazz)
                addToLevel(viewer,workSpaceModel.depthOf(clazz))
                classViwers.add(viewer)
                viewer.addMethod(method)


            }
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.addMethod(method)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.addMethodAndReorganize(method)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHOD){
                classViwers.firstOrNull { it.clazz.equals(clazz) }
                println("removed")
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHODANDREORGANIZED){
                classViwers.firstOrNull { it.clazz.equals(clazz) }
                println("removed and stuff")
            }
        }

    }
}

interface LeveledStructure{

    val levels:MutableList<MutableList<CustomComposite>>
    val topComponent:Scrollable?

    fun addToLevel(viewToAdd:CustomComposite, depth:Int){
        levelFunctions.addToLevel(viewToAdd, depth, levels,topComponent)

    }
    companion object levelFunctions{
        private fun addToLevel(viewToAdd:CustomComposite, depth:Int, viewMatrix:MutableList<MutableList<CustomComposite>>, topComponent:Scrollable?){
            if(depth-1<= viewMatrix.lastIndex){
                val elementsOfLevel= viewMatrix[depth-1]
                val data1 = FormData()
                if(depth-1==0){
                    data1.left = FormAttachment(0, 5)
                }else{
                    val lastLevel=viewMatrix[depth-2]
                    if(lastLevel.isNotEmpty()){
                        data1.left = FormAttachment(lastLevel.maxByOrNull { it.getComposite().bounds.width }!!.getComposite(), 5)
                    }else{
                        println("algo de errado nao esta certo 2")
                        data1.left = FormAttachment(0, 5)
                    }
                }
                if(elementsOfLevel.isNotEmpty()){
                    data1.top = FormAttachment(elementsOfLevel.lastOrNull()!!.getComposite(), 5)
                }else{
                    data1.top = FormAttachment(0, 5)
                    topComponent?.let { data1.top = FormAttachment(topComponent, 5) }
                    println("algo de errado nao esta certo 1")
                }
                viewToAdd.getComposite().layoutData=data1
                elementsOfLevel.add(viewToAdd)
            }
            else{
                val newLevel=mutableListOf<CustomComposite>()
                newLevel.add(viewToAdd)
                viewMatrix.add(newLevel)
                val data1 = FormData()
                data1.top=FormAttachment(0,5)
                topComponent?.let { data1.top=FormAttachment(it,5) }
                if (depth-1==0){
                    data1.left=FormAttachment(0,5)
                }else{
                    val lastLevel=viewMatrix.getOrNull(depth-2)
                    if(lastLevel==null){
                        println("algo de errado nao esta certo 3")
                        data1.left=FormAttachment(0,5)
                    }else{
                        if(lastLevel.isEmpty()){
                            println("algo de errado nao esta certo 4")
                            data1.left=FormAttachment(0,5)
                        }else {
                            data1.left = FormAttachment(lastLevel.maxByOrNull { it.getComposite().bounds.width }!!.getComposite(), 5)
                        }
                    }
                }
                viewToAdd.getComposite().layoutData=data1
            }
        }

        private fun reorganizeLevels(viewToAdd:CustomComposite, depth:Int, viewMatrix:MutableList<MutableList<CustomComposite>>, topComponent:Scrollable?){

        }
    }
}

