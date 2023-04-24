import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.widgets.*

interface CustomComposite{
    fun getComposite():Composite

}

internal class listView(model:FileManager,parent:Composite,controler:QuintalDJV2):CustomComposite,listElement,IObservable<listView.listEntityEvent> {

    val tree:Composite
    private val root:TreeItem
    val registeredElements= mutableMapOf<TreeItem,listElement>()
    interface listEntityEvent {

    }

    override val observers: MutableList<listEntityEvent> = mutableListOf()

    init {
        tree =Tree(parent,SWT.BORDER)
        root=TreeItem(tree,SWT.DROP_DOWN)


        model.addObserver { event, parent, fileparser ->
            if (event == EventType.PROJECTLOADED) {
                tree.setRedraw(false)
                println("PROJECTLOADED")
                model.getTypeDeclaration().forEach {
                    val classElement = listTypeElement(it, this)
                }
                root.expanded=true
                tree.setRedraw(true)



            } else if (event == EventType.FILEADDED) {
                println("FILEADDED")
                //TODO
            }
        }

        tree.addMouseListener(object : MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {
                val item = (tree as Tree).selection
                item.forEach {
                    val element=registeredElements.get(it)
                    if (element is listMethodElement){
                        //println(element.toStringDebug())
                        controler.methodSelected(element.body)
                    }
                }
            }

            override fun mouseDown(p0: MouseEvent?) {}

            override fun mouseUp(p0: MouseEvent?) {}

        })
    }

    override fun getComposite(): Composite {
        return tree
    }

    override fun getparent(): TreeItem? {
        return null
    }

    override fun getTree(): TreeItem {
       return root
    }

    override fun registerInTree(element: listElement) {
        registeredElements[element.getTree()]=element
    }
}

interface listElement{

    fun getparent():TreeItem?
    fun getTree():TreeItem
    fun registerInTree(element:listElement)

}

class listTypeElement(type:TypeDeclaration<*>,val parent:listElement):listElement{
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
        type.members.filter { it.isMethodDeclaration }.forEach {
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
        treeItem.text=body.asMethodDeclaration().declarationAsString
        treeItem.expanded=true
        registerInTree(this)
    }



}