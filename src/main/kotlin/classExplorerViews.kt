import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithDeclaration
import com.github.javaparser.ast.observer.ObservableProperty
import javassist.Loader.Simple
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.widgets.*

interface CustomComposite{
    fun getComposite():Composite

}

internal class listView(val model:FileManager,parent:Composite):CustomComposite,listElement,IObservable<(listView.listEntityEvent,clazz:ClassOrInterfaceDeclaration?,method:MethodDeclaration?)->Unit> {

    //var mainComposite=Composite(parent,SWT.BORDER)
    var tree:Composite?=null
    private var root:TreeItem?=null
    val registeredElements= mutableMapOf<TreeItem,listElement>()
    enum class listEntityEvent {
        METHODCLICKED,
        CLASSCLICKED

    }

    override val observers: MutableList<(listEntityEvent, ClassOrInterfaceDeclaration?, MethodDeclaration?) -> Unit> = mutableListOf()

    init {
        tree =Tree(parent,SWT.BORDER)
        root=TreeItem(tree as Tree,SWT.DROP_DOWN)

        model.addObserver { event, fileManager, fileparser ->
            if (event == EventType.PROJECTLOADED) {
                buildTree(FilterManager("", mutableListOf<FilterOptions>()))
            }
        }
    }

    public fun addFilter(filter: String, filteredOut: List<FilterOptions>){
        val filterManager=FilterManager(filter,filteredOut)
        buildTree(filterManager)
    }

    internal fun getPckgs():List<Pckg>{
        return registeredElements.values.filter { it is PckageListElement }.map { (it as PckageListElement).pckg }
    }

    private fun buildTree(filterManager: FilterManager){
        clearTree()
        registeredElements.clear()

        (tree as Tree).setRedraw(false)


        model.javaPackages.filter {filterManager.pckgObeysFilter(it)}.forEach {
            val pckgElement=PckageListElement(it,this,filterManager)
        }
        root!!.expanded=true
        (tree as Tree).setRedraw(true)

        tree!!.addMouseListener(object : MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {
            }
            override fun mouseDown(p0: MouseEvent?) {
                val item = (tree as Tree).selection
                item.forEach {
                    val element=registeredElements.get(it)
                    if (element is listMethodElement){
                        //println(element.toStringDebug())

                        notifyObservers { it(listEntityEvent.METHODCLICKED,null,element.body.asMethodDeclaration()) }
                    }
                    if (element is listTypeElement){
                        println("class clicked")
                        notifyObservers { it(listEntityEvent.CLASSCLICKED,element.type.toClassOrInterfaceDeclaration().get(),null) }
                    }
                    if (element is PckageListElement){
                        //todo
                    }
                }
            }
            override fun mouseUp(p0: MouseEvent?) {}
        })
    }

    private fun clearTree(){
        for (item in registeredElements){
            registeredElements[item.key]!!.getTree()?.dispose()
            item.key.dispose()
        }
        registeredElements.clear()
    }

    override fun getComposite(): Composite {
        return tree!!
    }

    override fun getparent(): TreeItem? {
        return null
    }

    override fun getTree(): TreeItem? {
       return root
    }

    override fun registerInTree(element: listElement) {
        registeredElements[element.getTree()!!]=element
    }
}

interface listElement{

    fun getparent():TreeItem?
    fun getTree():TreeItem?
    fun registerInTree(element:listElement)

}

class listTypeElement(val type:TypeDeclaration<*>,val parent:listElement,filterManager: FilterManager):listElement{
    val treeItem:TreeItem

    override fun getparent():TreeItem? {
        return parent.getTree()
    }

    override fun getTree(): TreeItem {
        return treeItem
    }

    override fun registerInTree(element: listElement) {
        parent.registerInTree(element)
    }

    init {
        treeItem=TreeItem(parent.getTree(),0)

        treeItem.text=type.name.toString()
        treeItem.font= Font(Display.getDefault(), FontData("Courier New",10,SWT.BOLD))
        type.observeProperty<Name>(ObservableProperty.NAME){
            treeItem.text=it.toString()
        }
        type.methods.filter { filterManager.mthdObeysFilter(it)}.forEach {
            val method=listMethodElement(it,this)
        }
        treeItem.expanded=true
        registerInTree(this)


    }


}

class listMethodElement(val body:BodyDeclaration<*>,val parent:listElement):listElement {
    val treeItem:TreeItem
    override fun getparent(): TreeItem? {
        return parent.getTree()
    }

    override fun getTree(): TreeItem {
        return treeItem
    }

    override fun registerInTree(element: listElement) {
        parent.registerInTree(element)
    }

    fun toStringDebug():String{
        return body.toString()
    }

    init {
        treeItem=TreeItem(parent.getTree(),0)
        val declaration=DeclarationStringManager(body.asMethodDeclaration())
        treeItem.text=declaration.lightToString()
        treeItem.text
        treeItem.font= Font(Display.getDefault(), FontData("Courier New",10,SWT.BOLD))
        body.observeProperty<SimpleName>(ObservableProperty.NAME){
            it?.let {declaration.updateName(it) }
            treeItem.text=declaration.lightToString()
        }
        treeItem.expanded=true
        registerInTree(this)
    }
}

class PckageListElement(val pckg:Pckg,val parent:listElement,val filterManager: FilterManager):listElement{
    val treeItem:TreeItem

    override fun getparent():TreeItem? {
        return parent.getTree()
    }

    override fun getTree(): TreeItem {
        return treeItem
    }

    override fun registerInTree(element: listElement) {
        parent.registerInTree(element)
    }
    init {
        treeItem=TreeItem(parent.getTree(),0)

        treeItem.text=pckg.name
        treeItem.font= Font(Display.getDefault(), FontData("Courier New",10,SWT.BOLD))
        pckg.getTypeDeclaration().filter { filterManager.clssgObeysFilter(it) }.forEach {
            val clazz=listTypeElement(it,this, filterManager)
        }

        treeItem.expanded=true
        registerInTree(this)
    }

}

class FilterManager(val filter: String,val filteredOut: List<FilterOptions>){

        fun pckgObeysFilter(pckg: Pckg):Boolean{
            return pckg.name.uppercase().contains(filter.uppercase()) or pckgContainsFilter(pckg)
        }
        private fun pckgContainsFilter(pckg: Pckg):Boolean{
            return pckg.getTypeDeclaration().filter { clssgObeysFilter(it) }.isNotEmpty()
        }
        fun clssgObeysFilter(type: TypeDeclaration<*>):Boolean{
            return type.nameAsString.uppercase().contains(filter.uppercase()) or clssContainsFilter(type)
        }
        private fun clssContainsFilter(type: TypeDeclaration<*>):Boolean{
            return type.methods.filter{ mthdObeysFilter(it) }.isNotEmpty()
        }
        fun mthdObeysFilter(method: MethodDeclaration):Boolean{
            return method.nameAsString.uppercase().contains(filter.uppercase()) and !(filteredOut.map { it.filterFun(method) }.contains(true))
        }
}