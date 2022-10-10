import com.github.javaparser.ast.body.BodyDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CLabel
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.widgets.*
import java.awt.Point
import java.awt.event.MouseListener


internal class WorkSpaceViewer(parent:Composite):customComposite{

    private val tabFolder:Composite
    private val addTab:CTabItem
    private var counter=0
    private val registeredWorkSpaces= mutableMapOf<CTabItem,WorkSpace>()

    override fun getComposite(): Composite {
        return tabFolder
    }

    init {
        tabFolder = CTabFolder(parent, SWT.BORDER)
        addTab= CTabItem(tabFolder,SWT.NULL)
        addTab.text="+"

        tabFolder.addMouseListener(object : org.eclipse.swt.events.MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {}
            override fun mouseDown(p0: MouseEvent?) {
                val point=Point(p0?.x ?:0, p0?.y ?:0)
                if(point.x>=addTab.bounds.x && point.y>=addTab.bounds.y && point.x<=addTab.bounds.width+addTab.bounds.x && point.y<=addTab.bounds.y+addTab.bounds.height){
                    WorkSpace(this@WorkSpaceViewer,counter)
                    counter++
                    (tabFolder as CTabFolder).setSelection((tabFolder as CTabFolder).items.size-1)

                }
            }
            override fun mouseUp(p0: MouseEvent?) {}
        })
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

internal class WorkSpace(parent:WorkSpaceViewer,counter:Int):customComposite{

    private val tabItem:CTabItem
    private val workSpace:Composite
    override fun getComposite(): Composite {
       return  workSpace
    }
    fun getTab():CTabItem{
        return tabItem
    }
    init {
        tabItem = CTabItem(parent.getComposite() as CTabFolder, SWT.NULL or SWT.CLOSE)
        tabItem.text = "WorkSpace $counter"
        tabItem.addDisposeListener {
            parent.removeFromRegister(this)
        }

        workSpace = SashForm(parent.getComposite(),SWT.BORDER)
        tabItem.control = workSpace
        parent.registerWorkSpace(this)

    }
    fun addMethod(model:BodyDeclaration<*>){
        println(model.toString())
        val text=Text(workSpace,SWT.BORDER)
        text.text=model.toString()
        workSpace.pack()

    }

}