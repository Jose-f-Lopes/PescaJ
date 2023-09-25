import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag
import com.github.javaparser.javadoc.description.JavadocDescription
import com.github.javaparser.javadoc.description.JavadocDescriptionElement
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.external.getOrNull
import java.awt.Point
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


internal class WorkSpaceViewerManager(parent:Composite,val quintalDJV2: QuintalDJV2):CustomComposite{

    private val tabFolder:Composite
    private val addTab:CTabItem
    private var counter=0
    private val registeredWorkSpaces= mutableMapOf<CTabItem,WorkSpace>()
    lateinit var solver:CombinedTypeSolver

    internal val workspaces get()=registeredWorkSpaces.values

    private var workSpaceOptions=mapOf<String,KClass<WorkSpace>>("Default" to (CodeExplorerWorkSpace::class as KClass<WorkSpace>),"Package Explorer" to (PackageDocWorkSpace::class as KClass<WorkSpace>))
    private var selectedOption:Boolean = false


    override fun getComposite(): Composite {
        return tabFolder
    }

    init {

        tabFolder = CTabFolder(parent, SWT.BORDER)
        tabFolder.layout=GridLayout()
        tabFolder.layoutData= GridData(GridData.FILL_BOTH)
        addTab= CTabItem(tabFolder,SWT.NULL)
        addTab.text="+"
        openWorkSpace((CodeExplorerWorkSpace::class as KClass<WorkSpace>))
        quintalDJV2.initWorkSpaceMenuFromMap(workSpaceOptions,this)

        tabFolder.addMouseListener(object : org.eclipse.swt.events.MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {}
            override fun mouseDown(p0: MouseEvent?) {
                val point = Point(p0?.x ?: 0, p0?.y ?: 0)
                if (point.x >= addTab.bounds.x && point.y >= addTab.bounds.y && point.x <= addTab.bounds.width + addTab.bounds.x && point.y <= addTab.bounds.y + addTab.bounds.height) {
                    openWorkSpace((CodeExplorerWorkSpace::class as KClass<WorkSpace>))
                }
            }
            override fun mouseUp(p0: MouseEvent?) {}
        })

        Display.getDefault().addFilter(SWT.MenuDetect){
            openParentMenu(it.widget)
        }
    }

    fun openParentMenu(wdgt:Widget){
        if((wdgt is Control)){
            if(wdgt.menu==null){
                if (wdgt.parent!=null){
                    openParentMenu(wdgt.parent)
                }
            }else{
                wdgt.menu.visible=true
            }
        }
    }

    fun openWorkSpace(option:KClass<WorkSpace>):WorkSpace{

        //val ws=CodeExplorerWorkSpace(this@WorkSpaceViewerManager,counter)
        val ws=option.primaryConstructor!!.call(this@WorkSpaceViewerManager,counter)
        if(this@WorkSpaceViewerManager::solver.isInitialized){
            ws.addSolver(solver)
        }
        counter++
        setSelection(ws.getTab())
        quintalDJV2.initToggles(this)
        return ws
    }

    fun setSelection(cTabItem: CTabItem){
        val index=(tabFolder as CTabFolder).items.indexOf(cTabItem)
        (tabFolder as CTabFolder).setSelection(index)
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

internal class CodeExplorerWorkSpace(override val parent:WorkSpaceViewerManager, val counter:Int):CustomComposite,WorkSpace{

    private val tabItem:CTabItem
    val workSpace:Composite
    val workSpaceModel:WorkSpaceModel = WorkSpaceModel()
    val classViwers= mutableListOf<ClassView>()
    var javardise=true

    val isCurrentWorkSpace:Boolean
        get() {
            return parent.currentWorkSpace()==this
        }
    lateinit var solver:CombinedTypeSolver
    val levels:VerticalLevelStructure<WorkSpaceModel.WorkSpaceClass>
    var focusedClass:ClassView?=null

    override fun methodSelected(method:MethodDeclaration){
        workSpaceModel.addMethod(method)
    }

    override fun classSelected(clazz:ClassOrInterfaceDeclaration){
        workSpaceModel.unfoldClass(clazz)
    }


    override fun getComposite(): Composite {
       return  workSpace
    }
    override fun getTab():CTabItem{
        return tabItem
    }

    override fun addSolver(solver:CombinedTypeSolver){
        this.solver=solver
        workSpaceModel.addSolver(solver)
    }



    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "Workspace $counter"
        tabItem.image=Image(Display.getDefault(),"ImagesAndIcons\\codeIcon.png")
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }


//        workSpace=parent.getComposite().scrollable {
//            Composite(parent.getComposite(), SWT.NONE)
//        }
        workSpace = Composite(parent.getComposite(),SWT.NONE)
        workSpace.layoutData=GridData(GridData.FILL_BOTH)

        tabItem.control = workSpace
        parent.registerWorkSpace(this)

        levels=VerticalLevelStructure(workSpace,false)

        workSpaceModel.addObserver{event,clazz,method,placeInLevel->
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDCLASSANDMETHOD){
                addClass(clazz)
                classViwers.lastOrNull{it.clazz==clazz}?.addMethod(method!!)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDMETHOD){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.addMethod(method!!)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.ADDEDMETHODANDREORGANIZED){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.addMethodAndReorganize(method!!)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHOD){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.removeMethod(method!!)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.REMOVEDMETHODANDREORGANIZED){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.removeMethodAndReorganize(method!!)
            }
            if(event==WorkSpaceModel.workSpaceModelEvents.REMOVEDCLASS){
                removeClass(clazz)
            }
            if (event==WorkSpaceModel.workSpaceModelEvents.LEVELREORDERED){
                classViwers.firstOrNull { it.clazz.equals(clazz) }?.reorderLevelInClass(method!!,placeInLevel!!)
            }
        }
    }

    fun closeClass(clazz:ClassOrInterfaceDeclaration){
        workSpaceModel.removeClass(clazz)
    }
    fun closeMethod(method:MethodDeclaration){
        workSpaceModel.removeMethod(method)
    }

    fun removeClass(clazz: WorkSpaceModel.WorkSpaceClass){
        levels.removeWidget(clazz)
    }

    fun addClass(clazz:WorkSpaceModel.WorkSpaceClass){
        levels.addWidget(clazz,workSpaceModel.depthOf(clazz),::clazzBuilderFunction)
    }

    fun clazzBuilderFunction(clazz:WorkSpaceModel.WorkSpaceClass,parent:Composite):Composite{
        val viewer=ClassView(this,clazz,parent,this)
        classViwers.add(viewer)
        return viewer.getComposite()
    }
}

internal class PackageDocWorkSpace(override val parent:WorkSpaceViewerManager, val counter:Int):CustomComposite,WorkSpace{
    val tabItem:CTabItem
    val workSpace:Composite
    val greyCheck= Image(Display.getDefault(),"ImagesAndIcons\\grey check mark.png")
    val redWarning= Image(Display.getDefault(),"ImagesAndIcons\\warning red.png")
    lateinit var warningAPI:Label

    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "Workspace $counter"
        tabItem.image=Image(Display.getDefault(),"ImagesAndIcons\\bars2.png")
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }

        workSpace = Composite(parent.getComposite(),SWT.NONE )
        workSpace.layout=GridLayout()
        workSpace.layoutData=GridData(GridData.FILL_BOTH)

        tabItem.control = workSpace
        parent.registerWorkSpace(this)
    }


    override fun getTab():CTabItem {
        return tabItem
    }
    override fun projSelected(pckgs: List<Pckg>) {
        tabItem.text="Package Documentation"
        for(pckg in pckgs){
            val pckgcomposite=Composite(workSpace,SWT.BORDER)
            pckgcomposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
            val layout=GridLayout()
            layout.numColumns=3
            pckgcomposite.layout=layout
            val name= Link(pckgcomposite,SWT.NONE)
            name.text="<a>"+pckg.name+"</a>"
            val data=GridData()
            data.widthHint=300
            name.layoutData=data
            name.addSelectionListener(object :SelectionListener{
                override fun widgetSelected(p0: SelectionEvent?) {
                    val ws=parent.openWorkSpace((clazzDocWorkSpace::class) as KClass<WorkSpace>)
                    ws.pckgSelected(pckg)
                }
                override fun widgetDefaultSelected(p0: SelectionEvent?) {}
            })

            warningAPI=Label(pckgcomposite,SWT.NONE)
            updateWarnings(pckg)
            pckg.getTypeDeclaration().forEach { it.methods.forEach{
                it.observeProperty<Comment>(ObservableProperty.COMMENT){updateWarnings(pckg)}
            } }

            val text=Text(pckgcomposite,SWT.NONE)
            text.layoutData=GridData(GridData.FILL_HORIZONTAL)
            val doc=pckg.docComUnit
            if (doc==null){

            }else{
                text.text=doc.comment.getOrNull?.content.toString()
                text.addFocusListener(object :FocusAdapter(){
                    override fun focusLost(e: FocusEvent?) {
                        val newComment=JavadocComment(text.text)
                        doc.setComment(newComment)
                    }
                })
                doc.observeProperty<Comment>(ObservableProperty.COMMENT){
                    text.text=it?.content
                    text.requestLayout()
                }
            }
            text.requestLayout()
            pckgcomposite.requestLayout()
        }
    }
    private fun updateWarnings(pckg:Pckg){
        val undocumentedPublic= mutableListOf<ClassOrInterfaceDeclaration>()
        pckg.getTypeDeclaration().forEach {
            if((javaParserUtil.nonDocumentedAPI(it as ClassOrInterfaceDeclaration)).isNotEmpty()){
                undocumentedPublic.add(it)
            }
        }
        if (undocumentedPublic.isNotEmpty()){
            warningAPI.image=redWarning
            var text="Undocumented API Methods in "+undocumentedPublic.size.toString()+" Class"
            if (undocumentedPublic.size>1){ text += "es" }
            warningAPI.toolTipText=text

        }else{
            warningAPI.image=greyCheck
            warningAPI.toolTipText="All API Methods Documented"
        }
    }
    override fun getComposite(): Composite {
        return workSpace
    }
}

internal class clazzDocWorkSpace(override val parent:WorkSpaceViewerManager, val counter:Int):WorkSpace,CustomComposite{
    val tabItem:CTabItem
    val workSpace:Composite


    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "Workspace $counter"
        tabItem.image=Image(Display.getDefault(),"ImagesAndIcons\\bars2.png")
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }

        workSpace = Composite(parent.getComposite(),SWT.BORDER )
        workSpace.layout=GridLayout()
        workSpace.layoutData=GridData(GridData.FILL_BOTH)

        tabItem.control = workSpace
        parent.registerWorkSpace(this)

    }
    override fun getTab(): CTabItem {
        return tabItem
    }
    override fun getComposite(): Composite {
        return workSpace
    }

    override fun pckgSelected(pckg: Pckg) {
        tabItem.text=pckg.treeName+" Documentation"
        for(clazz in pckg.getTypeDeclaration()){
            ClazzListElement(workSpace,clazz as ClassOrInterfaceDeclaration,parent)
        }
    }

    internal class ClazzListElement(workspace:Composite,val clazz: ClassOrInterfaceDeclaration,parent:WorkSpaceViewerManager):JavaDocController{
        val greyCheck= Image(Display.getDefault(),"ImagesAndIcons\\grey check mark.png")
        val redWarning= Image(Display.getDefault(),"ImagesAndIcons\\warning red.png")
        val warningAPI:Label

        init {
            val clazzcomposite = Composite(workspace, SWT.BORDER)
            clazzcomposite.layoutData = GridData(GridData.FILL_HORIZONTAL)
            val layout = GridLayout()
            layout.numColumns = 4
            clazzcomposite.layout = layout
            val name = Link(clazzcomposite, SWT.NONE)
            name.text = "<a>" + clazz.nameAsString + "</a>"
            val data = GridData()
            data.widthHint = 100
            name.layoutData = data
            name.addSelectionListener(object : SelectionListener {
                override fun widgetSelected(p0: SelectionEvent?) {
                    val ws = parent.openWorkSpace((methodDocWorkSpace::class) as KClass<WorkSpace>)
                    ws.classSelected(clazz)
                }
                override fun widgetDefaultSelected(p0: SelectionEvent?) {}
            })
            warningAPI = Label(clazzcomposite, SWT.NONE)
            updateWarnings()
            clazz.methods.forEach { it.observeProperty<Comment>(ObservableProperty.COMMENT){updateWarnings()} }
            val doc = clazz.javadoc.getOrNull
            if (doc == null) {
                val newJDoc = Javadoc(
                    JavadocDescription(
                        mutableListOf(JavadocDescriptionElement { "New Description" })
                    )
                )
                clazz.setJavadocComment(newJDoc)
            }
            val description =
                docView.descriptionWidget(clazzcomposite,clazz.javadoc.getOrNull?.description!!,  this,true)
            clazzcomposite.requestLayout()
        }

        private fun updateWarnings(){
            val undocumentedPublic = javaParserUtil.nonDocumentedAPI(clazz)
            if (undocumentedPublic.isNotEmpty()) {
                warningAPI.image = redWarning
                var text = undocumentedPublic.size.toString() + " Undocumented API Method"
                if (undocumentedPublic.size > 1) {
                    text += "s"
                }
                warningAPI.toolTipText = text

            } else {
                warningAPI.image = greyCheck
                warningAPI.toolTipText = "All API Methods Documented"
            }

            val undocumentedPriv = javaParserUtil.nonDocumentedPrivate(clazz as ClassOrInterfaceDeclaration)
            if (undocumentedPriv.isNotEmpty()) {
                var textPriv = undocumentedPriv.size.toString() + " Undocumented Non-Public Method"
                if (undocumentedPriv.size > 1) {
                    textPriv += "s"
                }
                warningAPI.toolTipText += "\n\n" + textPriv
            } else {
                warningAPI.toolTipText += "\n\nAll Non-Public Methods Documented"
            }
        }

        override fun descriptionEdited(description: JavadocDescription) {
            classEdited(clazz,description=description)
        }

        override fun tagsEdited(tags: MutableList<JavadocBlockTag>) {
            classEdited(clazz, tags = tags)
        }
    }



}

internal class methodDocWorkSpace(override val parent:WorkSpaceViewerManager, val counter:Int):WorkSpace{
    val tabItem:CTabItem
    val workSpace:Composite

    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "WorkSpace $counter"
        tabItem.image=Image(Display.getDefault(),"ImagesAndIcons\\bars2.png")
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }
        workSpace = Composite(parent.getComposite(),SWT.NONE)
        workSpace.layout=GridLayout()
        workSpace.layoutData=GridData(GridData.FILL_BOTH)
        tabItem.control = workSpace
        parent.registerWorkSpace(this)


    }



    override fun getTab(): CTabItem {
        return tabItem
    }

    override fun classSelected(clazz: ClassOrInterfaceDeclaration) {
        tabItem.text="Class: "+clazz.nameAsString
        val view=DocClassView(workSpace,clazz,this)
    }



}

internal interface WorkSpace{
    val parent:WorkSpaceViewerManager
    val name get()=getTab().text

    fun addSolver(solver:CombinedTypeSolver){}
    fun methodSelected(method:MethodDeclaration){}
    fun getTab():CTabItem
    fun projSelected(pckgs:List<Pckg>){}
    fun pckgSelected(pckg: Pckg){}
    fun classSelected(clazz:ClassOrInterfaceDeclaration){}
    fun setSelected(){
        parent.setSelection(getTab())
    }
}


