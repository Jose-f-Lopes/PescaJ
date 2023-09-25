import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite

internal class DocClassView(parent:Composite, clazz:ClassOrInterfaceDeclaration, override val controller:WorkSpace):CustomComposite,MethodContainer {
    val myComposite:Composite
    override val methodViews= mutableListOf<MethodView>()
    override fun closeMethod(method: MethodDeclaration) {

    }

    init {
        myComposite=Composite(parent, SWT.NONE)
        myComposite.layout=GridLayout()
        myComposite.layoutData=GridData(GridData.FILL_BOTH)
        val headerComposite=Composite(myComposite,SWT.NONE)
        headerComposite.layout=GridLayout()
        headerComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)

        val bodyComposite=Composite(myComposite,SWT.NONE)
        val layout=GridLayout()
        layout.numColumns=1
        bodyComposite.layout=layout
        bodyComposite.layoutData=GridData(GridData.FILL_BOTH)

        val header=DocumentationClassHeader(headerComposite,clazz)
        header.addObserver {event->
            val iterator=methodViews.iterator()
            while (iterator.hasNext()){
                val item=iterator.next()
                iterator.remove()
                item.getComposite().dispose()
            }
            clazz.methods.filter { method->!(event.map { it.filterFun(method) }.contains(true)) }.sortedBy { it.nameAsString }.forEach {
                val methodView=docView(bodyComposite,it,this)
            }
        }
        clazz.methods.sortedBy { it.nameAsString }.forEach {
            //val methodView=docView(bodyComposite,it,this)
            methodDocListView(bodyComposite,it,this)
        }
        myComposite.requestLayout()

    }

    override fun getComposite(): Composite {
       return myComposite
    }

}