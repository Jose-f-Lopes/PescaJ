import com.github.javaparser.ast.body.BodyDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import java.lang.IllegalArgumentException


fun main(){
    val window = QuintalDJV2()
    window.open()
}

class QuintalDJV2 {

    private val display = Display()
    private val shell = Shell(display)
    private val model:FileManager
    internal val viewer:WorkSpaceViewer

    init {
        shell.text = "QuintalDJ"
        val layout= GridLayout()
        layout.numColumns=1
        shell.layout =layout


        // nao esquecer layouts nos paineis (caso contrario nao se ve nada)


        val menuBar=Composite(shell,SWT.PUSH or SWT.BORDER )
        menuBar.layoutData=GridData(GridData.FILL_HORIZONTAL)
        menuBar.layout=RowLayout()
//        val fileMenu=Menu(shell,SWT.DROP_DOWN)
//        val fileMenuHeader=MenuItem(fileMenu,SWT.CASCADE)
//        fileMenuHeader.text="File"




        val mainPanel=SashForm(shell, SWT.HORIZONTAL or SWT.BORDER)
        mainPanel.layoutData=GridData(GridData.FILL_BOTH)

        model=FileManager()
        val button=browserComponent(model,menuBar)
        val list=listView(model,mainPanel,this)
        viewer=WorkSpaceViewer(mainPanel)


        mainPanel.weights= listOf<Int>(1,4).toIntArray()


    }
    fun open() {
        //shell.pack()
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
        model.stopThreads()
    }

    fun methodSelected(model:BodyDeclaration<*>){
        //println(viewer.currentWorkSpace())
        viewer.currentWorkSpace()?.addMethod(model)
    }
}

internal class browserComponent(var model: FileManager, parent: Composite) {

    init {
        val button = Button(parent, SWT.PUSH)
        button.text
        button.setText("LoadProject")
        button.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val dialog = DirectoryDialog(parent.shell, SWT.NULL);
                val path = dialog.open();
                if (path != null) {
                    println(path)
                    try {
                        model.setDirectory(path)
                    } catch (e: IllegalArgumentException) {
                        println(e.message)
                    }
                }
                else{
                    println("nope")
                }

            }
        });
    }
}

