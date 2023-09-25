import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

internal class MyPage(parent:Display,model:MethodDeclaration,override val controller:WorkSpace):MethodContainer {
    private var shell=Shell(parent)
    override val methodViews= mutableListOf<MethodView>()

    init {
        shell.layout = GridLayout(1, false)
        val composite=Composite(shell, SWT.NONE)
        composite.layout=GridLayout(1, false)
        docView(composite,model,this)

        shell.pack();
        shell.open();
    }



    override fun closeMethod(method: MethodDeclaration) {
    }
}
