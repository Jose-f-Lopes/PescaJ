import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.List
import org.eclipse.swt.widgets.Shell
import kotlin.reflect.KClass


abstract class MyDialog(parent:Shell,val method: MethodDeclaration): Dialog(parent) {

    override fun createDialogArea(parent: Composite?): Control {
        val container = super.createDialogArea(parent) as Composite
        initMethod(container,method)
        return container
    }

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = "Code"
    }

    override fun getInitialSize(): Point {
        return Point(450, 300)
    }

    override fun isResizable(): Boolean {
        return true
    }

    abstract fun initMethod(parent:Composite,method:MethodDeclaration)
}

internal class MyDialogDoc(parent:Shell, method: MethodDeclaration, override val controller: WorkSpace): MyDialog(parent,method),MethodContainer {
    override fun initMethod(parent: Composite, method: MethodDeclaration) {
        val doc=docView(parent,method,this)
    }

    override val methodViews= mutableListOf<MethodView>()
    override fun closeMethod(method: MethodDeclaration) {

    }


}

internal class MyDialogCode(parent:Shell, method: MethodDeclaration, override val controller: WorkSpace): MyDialog(parent,method),MethodContainer {
    override fun initMethod(parent: Composite, method: MethodDeclaration) {
        val code=JavardiseView(parent,method,this)
    }
    override val methodViews= mutableListOf<MethodView>()
    override fun closeMethod(method: MethodDeclaration) {
    }

}

internal class OpenOptions(parent:Shell,val method: MethodDeclaration,val controller:WorkSpaceViewerManager):Dialog(parent) {

    lateinit var list:List
    override fun createDialogArea(parent: Composite?): Control {
        val container = super.createDialogArea(parent) as Composite
        list = List(container, SWT.BORDER)
        list.add("New WorkSpace")
        for (workspace in controller.workspaces) {
            if (workspace is CodeExplorerWorkSpace) {
                list.add(workspace.name)
            }
        }

        return container
    }

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = "Choose WorkSpace"
    }

    override fun getInitialSize(): Point {
        return Point(450, 300)
    }

    override fun okPressed() {
        if (list.selection.isNotEmpty()){
            if (list.selection.first()=="New WorkSpace"){
                val ws=controller.openWorkSpace((CodeExplorerWorkSpace::class as KClass<WorkSpace>))
                ws.methodSelected(method)
            }
            val selectedWorkSpace=controller.workspaces.first{it.name==list.selection.first()}
            selectedWorkSpace.methodSelected(method)
            controller.setSelection(selectedWorkSpace.getTab())
            this.close()
        }
    }
}