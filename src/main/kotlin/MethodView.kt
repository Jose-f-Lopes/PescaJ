import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag
import com.github.javaparser.javadoc.description.JavadocDescription
import com.github.javaparser.javadoc.description.JavadocDescriptionElement
import com.github.javaparser.javadoc.description.JavadocSnippet
import org.eclipse.jface.wizard.WizardDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.browser.Browser
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.members.MethodWidget
import kotlin.reflect.KClass

interface MethodView:IObservable<(MethodView.EventType,MethodCallExpr)->Unit>,CustomComposite{
    val parent:Composite
    val model:MethodDeclaration
    override val observers: MutableList<(EventType, MethodCallExpr) -> Unit>


    //call this whenever a method is clicked on the widget
    fun methodClicked(method:MethodCallExpr){
        notifyObservers { it(EventType.METHODCLICKED,method) }
    }

    enum class EventType{
        METHODCLICKED
    }



}

internal class JavardiseView(override val parent: Composite, override val model: MethodDeclaration,val classView: MethodContainer):MethodView{

    override val observers: MutableList<(MethodView.EventType, MethodCallExpr) -> Unit> = mutableListOf()
    val parentComposite:Composite= Composite(parent,SWT.NONE)

    lateinit var myComposite:MethodWidget
    override fun getComposite(): Composite {
        return parentComposite
    }

    init {
        val gridLayout = GridLayout()
        gridLayout.numColumns = 1
        gridLayout.marginWidth=0
        gridLayout.marginHeight=0
        parentComposite.layout=gridLayout
        parentComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
        ContextMenu(parentComposite)
        classView.methodViews.add(this)


        val methodWidget = parentComposite.column {
            layout = FillLayout()
            myComposite = scrollable {
                MethodWidget(it, model, configuration = object :DefaultConfiguration(){
                    override val fontSize: Int
                        get() = 12
                    override val tabLength: Int
                        get() = 2

                    override val fontFace: String
                        get() = "Courier New"

                })
            }
            myComposite.enabled = true

            display.addFilter(SWT.KeyDown) {
                if(it.keyCode == SWT.HOME) {
                    val n: Node? = myComposite.getChildNodeOnFocus()
                    if(n is MethodCallExpr) {
                        println("name: "+n.nameAsString)
                        methodClicked(n)
                    }
                }
            }
            (myComposite as Scrollable)

            //TODO
//            display.addFilter(SWT.MouseDown){
//                println(it.widget)
//                val point = Point(it.x , it.y )
//                if (point.x >= parentComposite.bounds.x && point.y >= parentComposite.bounds.y && point.x <= parentComposite.bounds.width + parentComposite.bounds.x && point.y <= parentComposite.bounds.y + parentComposite.bounds.height) {
//
//                //this@JavardiseView.setFocused(true)
//                }
//            }
        }
        methodWidget.layoutData=GridData(GridData.FILL_HORIZONTAL)

        parentComposite.addDisposeListener {
            classView.methodViews.remove(this@JavardiseView)
            //methodWidget.dispose()
        }
    }

    private fun ContextMenu(parent:Composite){
        val menu = Menu(parent)
        val close = MenuItem(menu, SWT.PUSH)
        close.text = "Close Method"
        close.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                classView.closeMethod(model)
            }
        })
        val showDoc = MenuItem(menu, SWT.PUSH)
        showDoc.text = "Show Documentation"
        showDoc.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                //val modal=MyDialog(Display.getDefault().activeShell)
                val wizard = MyPage(Display.getDefault(),model,classView.controller)
            }

        })
        parent.menu = menu
    }


}


internal class plainTextView(override val parent: Composite, override val model: MethodDeclaration):MethodView{

    override val observers: MutableList<(MethodView.EventType, MethodCallExpr) -> Unit> = mutableListOf()



    val myComposite=Composite(parent,SWT.BORDER)
    override fun getComposite(): Composite {
        return myComposite
    }

    init {
        val text:Text= Text(myComposite, SWT.V_SCROLL)
        text.text=model.body.toString()
        text.layoutData = RowData(250, 80)

    }

}

internal class methodDocListView(val parent: Composite, val model: MethodDeclaration,val classView: DocClassView):JavaDocController{
    val myComposite=Composite(parent,SWT.BORDER)

    init {
        val layout=GridLayout()
        layout.numColumns=2
        myComposite.layout = layout
        myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
        val name = Link(myComposite, SWT.NONE)
        val declaration=DeclarationStringManager(model)
        name.text = "<a>" +declaration.lightToString()+ "</a>"
        val data = GridData()
        data.widthHint = 125
        name.layoutData = data
        val description:docView.descriptionWidget
        if (model.hasJavaDocComment()){
            description=docView.descriptionWidget(myComposite,model.javadoc?.get()?.description,this)
        }else{
            description=docView.descriptionWidget(myComposite,null,this)
        }


        //description.layoutData=GridData(GridData.FILL_HORIZONTAL)
//        if(model.hasJavaDocComment()){
//            description.text=model.javadoc?.get()?.description?.toText()
//        }
        name.addSelectionListener(object : SelectionListener {
            override fun widgetSelected(p0: SelectionEvent?) {
                name.dispose()
                description.getComposite().dispose()
                val methodView=docView(myComposite,model,classView)
            }
            override fun widgetDefaultSelected(p0: SelectionEvent?) {}
        })
    }

    override fun descriptionEdited(description: JavadocDescription) {
    }
    override fun tagsEdited(tags: MutableList<JavadocBlockTag>) {
    }
}

internal class docView(
    override val parent: Composite,
    override val model: MethodDeclaration,
    val classView: MethodContainer

):MethodView,JavaDocController{

    val myComposite:Composite
    override val observers: MutableList<(MethodView.EventType, MethodCallExpr) -> Unit> = mutableListOf()
    val descriptionView:descriptionWidget

    init {
        classView.methodViews.add(this)

        myComposite = Composite(parent, SWT.BORDER)
        myComposite.layout = GridLayout()
        myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
        val headerComposite=Composite(myComposite,SWT.NONE)
        val layout=GridLayout()
        layout.numColumns=2
        headerComposite.layout = layout
        headerComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
        val bodyComposite=Composite(myComposite,SWT.NONE)
        bodyComposite.layout = GridLayout()
        bodyComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
        ContextMenu(myComposite)

        val text = Text(headerComposite, SWT.NONE)
        val declaration=DeclarationStringManager(model)
        text.text = declaration.toString()
        text.font= Font(Display.getDefault(), FontData("Courier New",10,SWT.BOLD))

        val tagView:JavaDocTagController
        if (!model.hasJavaDocComment()) {
            val newJDoc = initNewJavaDoc(model)
            model.setJavadocComment(newJDoc)
        }
        descriptionView=descriptionWidget(bodyComposite,model.javadoc.get().description,this)
        tagView=JavaDocTagController(bodyComposite,model.javadoc.get(),this)
        docViewContextMenu(tagView.myComposite, tagView)




        myComposite.addDisposeListener {
            classView.methodViews.remove(this@docView)

        }

        model.observeProperty<Comment>(ObservableProperty.COMMENT){
            if(it is JavadocComment){
                descriptionView.update((it as JavadocComment).parse().description)
                tagView.update(it.parse().blockTags)
            }
        }
        model.observeProperty<SimpleName>(ObservableProperty.NAME){
            it?.let {declaration.updateName(it)}
            text.text=declaration.toString()
            text.requestLayout()
        }
        model.observeProperty<Modifier>(ObservableProperty.MODIFIERS){
            println(it.toString())
        }

    }

    private fun ContextMenu(parent:Composite){
        val menu = Menu(parent)
        val close = MenuItem(menu, SWT.PUSH)
        close.text = "Close Method"
        close.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                classView.closeMethod(model)
            }
        })
        val showCode = MenuItem(menu, SWT.PUSH)
        showCode.text = "Show Code"
        showCode.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                //MyDialogCode(Display.getDefault().activeShell,model,classView.controller).open()
            }
        })
        val openInNew=MenuItem(menu,SWT.PUSH)
        openInNew.text="Open in new WorkSpace"
        openInNew.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                val ws=classView.controller.parent.openWorkSpace((CodeExplorerWorkSpace::class as KClass<WorkSpace>))
                ws.methodSelected(model)
            }
        })
        val openInExisting=MenuItem(menu,SWT.PUSH)
        openInExisting.text="Open in existing WorkSpace"
        openInExisting.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                //val dialog=OpenOptions(Display.getDefault().activeShell,model,classView.controller.parent)
                //dialog.open()

            }
        })
        parent.menu = menu
    }

    private fun initNewJavaDoc(model:MethodDeclaration):Javadoc{
        val newJDoc=Javadoc(
            JavadocDescription(
                mutableListOf(JavadocDescriptionElement{"New Description"})
            ))
        model.parameters.forEach {
            newJDoc.addBlockTag(
                JavadocBlockTag(
                    JavadocBlockTag.Type.PARAM,
                    it.nameAsString+" Insert parameter description"
                ))
        }
        if (model.typeAsString!="void"){
            newJDoc.addBlockTag(
                JavadocBlockTag(
                    JavadocBlockTag.Type.RETURN,
                    "Insert return description"
                )
            )
        }
        return newJDoc
    }

    override fun getComposite(): Composite {
        return myComposite
    }

    override fun descriptionEdited(description: JavadocDescription) {
        methodEdited(model,description=description)
    }
    override fun tagsEdited(tags: MutableList<JavadocBlockTag>) {
        methodEdited(model, tags = tags)
    }

    internal class JavaDocTagController(parent:Composite,doc:Javadoc,val controler:JavaDocController){
        val blockTagList= mutableListOf<blockTagCluster>()
        val myComposite:Composite

        init {
            myComposite=Composite(parent,SWT.NONE)
            myComposite.layout=GridLayout()
            myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)

            doc.blockTags.forEach {
                initBlockTag(it)
            }
        }

        private fun initBlockTag(tag:JavadocBlockTag){
            var cluster=blockTagList.firstOrNull{it.type==tag.type}
            if (cluster==null){
                cluster=blockTagCluster(tag.type,myComposite,this)
                blockTagList.add(cluster)
            }
            cluster.addTag(tag)
        }

        fun addEmptyTag(type:JavadocBlockTag.Type){
            var cluster=blockTagList.firstOrNull{it.type==type}
            if (cluster==null){
                cluster=blockTagCluster(type,myComposite,this)
                blockTagList.add(cluster)
            }
            cluster.addEmptyTag(type)
        }

        fun removeTag(tag:blockTagWidget){
            var cluster=blockTagList.firstOrNull{it.type==tag.myType}
            cluster!!.tagsInCluster.remove(tag)
            if (cluster.tagsInCluster.isEmpty()){
                cluster.myComposite.dispose()
                blockTagList.remove(cluster)
                myComposite.requestLayout()
            }else{
                cluster.myComposite.requestLayout()
            }
        }

        private fun toTagList():MutableList<JavadocBlockTag>{
            val result= mutableListOf<JavadocBlockTag>()
            blockTagList.forEach { result.addAll(it.toTagList()) }
            return result
        }
        internal fun tagsEdited(){
            controler.tagsEdited(toTagList())
        }
         internal fun update(tags: MutableList<JavadocBlockTag>){
             blockTagList.forEach { it.clear() }
             tags.forEach {initBlockTag(it) }
             blockTagList.forEach {
                 if (it.tagsInCluster.isEmpty()){
                 it.myComposite.dispose()
                 blockTagList.remove(it)
                 }
             }
         }
    }
    internal class blockTagCluster(val type:JavadocBlockTag.Type,parent: Composite,val controler:JavaDocTagController ):CustomComposite{
        val myComposite=Composite(parent,SWT.NONE)
        val tagsInCluster= mutableListOf<blockTagWidget>()
        private val title:Label

        init {
            myComposite.layout=GridLayout()
            myComposite.background= Color(230,230,230)
            myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
            title=Label(myComposite,SWT.NONE)
            title.text=blockTagCluster.tagTypesNames.values()[type.ordinal].stringName

        }
        override fun getComposite(): Composite {
            return myComposite
        }
         internal fun addTag(tag:JavadocBlockTag) {
             val newTag=blockTagWidget(tag,myComposite,controler)
             tagsInCluster.add(newTag)
         }

        internal fun addEmptyTag(type:JavadocBlockTag.Type){
            val newTag:JavadocBlockTag
            newTag = if (tagTypesNames.values()[type.ordinal].hasName){
                JavadocBlockTag(
                    type,
                    "Name Add Description"
                )
            } else{
                JavadocBlockTag(
                    type,
                    "Add Description"
                )
            }
            addTag(newTag)
        }

        internal fun toTagList():MutableList<JavadocBlockTag>{
            val result= mutableListOf<JavadocBlockTag>()
            tagsInCluster.forEach { result.add(it.toTag())}
            return result
        }

        internal fun clear(){
            tagsInCluster.forEach { it.myComposite.dispose() }
            tagsInCluster.clear()
        }
        internal enum class tagTypesNames(val stringName:String,val optional:Boolean,val hasName:Boolean){
            AUTHOR("Author(s)",true,false),
            DEPRECATED("Deprecated",true,false),
            EXCEPTION("Exception",true,true),
            PARAM("Parameter(s)",false,true),
            RETURN("Return",false,false),
            SEE("See/References",true,false),
            SERIAL("Serial",true,false),
            SERIAL_DATA("SERIAL_DATA",true,false),
            SINCE("Since",true,false),
            THROWS("Throws",true,false),
            VERSION("Versions",true,true),
            UNKNOWN("Unknown",true,false)
        }
    }

    internal class blockTagWidget(tag:JavadocBlockTag,parent: Composite,val controler:JavaDocTagController):CustomComposite{

        val myComposite=Composite(parent,SWT.NONE)
        val myType:JavadocBlockTag.Type=tag.type
        var name:Text?=null
        val description:Text
        private var modified=false
        private var modifiedName=false

        init {
            val layout=GridLayout()
            layout.numColumns=1
            if (blockTagCluster.tagTypesNames.values()[tag.type.ordinal].hasName){
                layout.numColumns++
                name=Text(myComposite,SWT.NONE)
                name!!.text=tag.name.getOrNull?:"\t"
                name!!.addFocusListener(object : FocusAdapter(){
                    override fun focusLost(e: FocusEvent?) {
                        if (modifiedName){
                            controler.tagsEdited()
                            modifiedName=false
                        }
                    }
                })
                name!!.addModifyListener(object :ModifyListener{
                    override fun modifyText(p0: ModifyEvent?) {
                        modifiedName=true
                    }
                })
            }
            myComposite.layout=layout
            myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
            description=Text(myComposite,SWT.MULTI)
            description.text=tag.content.toText()
            description.addFocusListener(object : FocusAdapter(){
                override fun focusLost(e: FocusEvent?) {
                    if (modified){
                        controler.tagsEdited()
                        modified=false
                    }
                }
            })
            description.addModifyListener(object :ModifyListener{
                override fun modifyText(p0: ModifyEvent?) {
                    modified=true
                }
            })
            description.layoutData=GridData(GridData.FILL_HORIZONTAL)
            if (blockTagCluster.tagTypesNames.values()[tag.type.ordinal].optional){
                layout.numColumns++
                val removeButton=Button(myComposite,SWT.NONE)
                removeButton.text="X"
                removeButton.addSelectionListener(object : SelectionListener {
                    override fun widgetSelected(p0: SelectionEvent?) {
                        this@blockTagWidget.remove()
                    }
                    override fun widgetDefaultSelected(p0: SelectionEvent?) {
                    }
                })
            }

            myComposite.requestLayout()

        }

        override fun getComposite(): Composite {
            return myComposite
        }

        internal fun remove(){
            this.myComposite.dispose()
            controler.removeTag(this)
        }

        internal fun toTag():JavadocBlockTag{
            if (blockTagCluster.tagTypesNames.values()[myType.ordinal].hasName){
                return JavadocBlockTag(myType,name!!.text+" "+description.text)
            }else{
                return JavadocBlockTag(myType,description.text)
            }
        }


    }



    internal class descriptionWidget(parent:Composite,var description:JavadocDescription?,val controler:JavaDocController,limitedSize:Boolean=false):CustomComposite{

        val myComposite=Composite(parent,SWT.NONE)
        var content:Text
        private var modified=false
        init {
            val layout=GridLayout()
            layout.numColumns=1
            myComposite.layout=layout
            myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
            content=Text(myComposite,SWT.MULTI)
            content.layoutData=GridData(GridData.FILL_BOTH)
            if (limitedSize){
                val data=GridData(GridData.FILL_HORIZONTAL)
                data.heightHint=150
                content.layoutData=data
            }

            content.addFocusListener(object : FocusAdapter(){
                override fun focusLost(e: FocusEvent?) {
                    if (modified){
                        descriptionEdited()
                        modified=false
                    }

                }
            })
            content.addModifyListener(object :ModifyListener{
                override fun modifyText(p0: ModifyEvent?) {
                    modified=true
                }

            })
            if (description==null){
                description=JavadocDescription(mutableListOf(JavadocDescriptionElement { "emptyDescription" }))
            }
            update(description!!)

        }


        fun update(description: JavadocDescription){
            content.text=description.toText()
        }
        override fun getComposite(): Composite {
            return myComposite
        }

        fun descriptionEdited() {
            val description=toDescription()
            controler.descriptionEdited(description=description)
        }

        fun toDescription():JavadocDescription{
            val result= mutableListOf<JavadocDescriptionElement>()
            result.add(JavadocDescriptionElement { content.text })
            val newDescription=JavadocDescription(
                result
            )
            return newDescription
        }
    }

}

internal class docViewContextMenu(parent:Control, tagController: docView.JavaDocTagController){
    init {
        val menu=Menu(parent)

        for (type in docView.blockTagCluster.tagTypesNames.values()){
            if (type.optional){
                val createTag=MenuItem(menu,SWT.PUSH)
                createTag.text="Add "+type.stringName
                createTag.addSelectionListener(object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent?) {
                        tagController.addEmptyTag(JavadocBlockTag.Type.values()[type.ordinal])
                    }
                })
            }
        }
        parent.menu=menu
    }
}

internal class MethodViewContextMenu(parent:Control,controller:MethodView){
    init {
        val menu = Menu(parent)
        val close = MenuItem(menu, SWT.PUSH)
        close.text = "Close Method"
        close.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                println("close")
            }
        })
        parent.menu = menu
    }

}

interface JavaDocController{

    fun methodEdited(model:MethodDeclaration,description: JavadocDescription?=null,tags: MutableList<JavadocBlockTag> = mutableListOf()){
        var newDescription=model.javadoc.getOrNull?.description
        if (description!=null){newDescription=description}
        if (newDescription==null){
            //should de unreachable
            newDescription= JavadocDescription(mutableListOf(JavadocDescriptionElement { "emptyDescription" }))
        }
        var newTags=model.javadoc.getOrNull?.blockTags
        if (tags.isNotEmpty() or (newTags==null)){newTags=tags}

        val newJDoc=Javadoc(
            newDescription
        )
        newTags!!.forEach{ newJDoc.addBlockTag(it)}

        model.setJavadocComment(newJDoc)

    }
    fun classEdited(model:ClassOrInterfaceDeclaration,description: JavadocDescription?=null,tags: MutableList<JavadocBlockTag> = mutableListOf()){
        var newDescription=model.javadoc.getOrNull?.description
        if (description!=null){newDescription=description}
        if (newDescription==null){
            //should de unreachable
            newDescription= JavadocDescription(mutableListOf(JavadocDescriptionElement { "emptyDescription" }))
        }
        var newTags=model.javadoc.getOrNull?.blockTags
        if (tags.isNotEmpty() or (newTags==null)){newTags=tags}

        val newJDoc=Javadoc(
            newDescription)
        newTags!!.forEach{ newJDoc.addBlockTag(it)}
        model.setJavadocComment(newJDoc)
    }

    fun descriptionEdited(description: JavadocDescription)
    fun tagsEdited(tags: MutableList<JavadocBlockTag>)
}