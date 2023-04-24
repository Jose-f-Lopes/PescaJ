import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import org.eclipse.swt.SWT
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.external.column
import pt.iscte.javardise.external.scrollable
import pt.iscte.javardise.widgets.members.MethodWidget
import java.awt.Point

internal interface MethodView:IObservable<(MethodView.EventType,MethodCallExpr)->Unit>,CustomComposite{
    val parent:Composite
    val model:MethodDeclaration
    override val observers: MutableList<(EventType, MethodCallExpr) -> Unit>
    var isFocused:Boolean


    //call this whenever a method is clicked on the widget
    fun methodClicked(method:MethodCallExpr){
        notifyObservers { it(EventType.METHODCLICKED,method) }
    }

    enum class EventType{
        METHODCLICKED
    }



}

internal class JavardiseView(override val parent: Composite, override val model: MethodDeclaration,val classView: ClassView):MethodView{

    override val observers: MutableList<(MethodView.EventType, MethodCallExpr) -> Unit> = mutableListOf()
    val focusedColor=Color(173,216,230)
    val unfocusedColor= Color(240,240,240)
    val parentComposite:Composite= Composite(parent,SWT.NONE)
    override var isFocused=false
        set(value){
            if(value!=field) {
                if (value == true) {
                    if (!classView.isFocused) {
                        classView.isFocused = true
                    }
                    classView.clearFocus(this@JavardiseView)
                    parentComposite.background = focusedColor
                } else {
                    parentComposite.background = unfocusedColor
                }
            }
            field=value
        }

    lateinit var myComposite:MethodWidget
    override fun getComposite(): Composite {
        return parentComposite
    }

    init {
        val gridLayout = GridLayout()
        gridLayout.numColumns = 1
        parentComposite.layout=gridLayout

        classView.methodViews.add(this)


        val methodWidget = parentComposite.column {
            layout = FillLayout()
            myComposite = scrollable {
                MethodWidget(it, model)
            }
            myComposite.enabled = true

            display.addFilter(SWT.KeyDown) {
                if(it.keyCode == SWT.HOME) {
                    val n: Node? = myComposite.getChildNodeOnFocus()
                    if(n is MethodCallExpr) {
                        methodClicked(n)
                    }
                }
            }
            myComposite.pack()
            parent.pack()

//            display.addFilter(SWT.MouseDown){
//                val point = Point(it.x , it.y )
//                if (point.x >= parentComposite.bounds.x && point.y >= parentComposite.bounds.y && point.x <= parentComposite.bounds.width + parentComposite.bounds.x && point.y <= parentComposite.bounds.y + parentComposite.bounds.height) {
//                    this@JavardiseView.setFocused(true)
//                }
//            }
        }
        parentComposite.addDisposeListener {
            classView.methodViews.remove(this@JavardiseView)
            isFocused=false
            methodWidget.dispose()
        }
        parentComposite.addMouseListener(object : org.eclipse.swt.events.MouseListener {
            override fun mouseDoubleClick(p0: MouseEvent?) {}
            override fun mouseDown(p0: MouseEvent?) {

                this@JavardiseView.isFocused=true
            }
            override fun mouseUp(p0: MouseEvent?) {}
        })
        this@JavardiseView.isFocused=true
    }


}

internal class plainTextView(override val parent: Composite, override val model: MethodDeclaration,val classView: ClassView):MethodView{

    override val observers: MutableList<(MethodView.EventType, MethodCallExpr) -> Unit> = mutableListOf()
    override var isFocused=false


    val myComposite=Composite(parent,SWT.BORDER)
    override fun getComposite(): Composite {
        return myComposite
    }

    init {
        val text:Text= Text(myComposite, SWT.V_SCROLL)
        text.text=model.body.toString()
        text.layoutData = RowData(250, 80)
        text.pack()
    }

}