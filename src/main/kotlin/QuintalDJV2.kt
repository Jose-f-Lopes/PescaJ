import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass


fun main(){
    val window = QuintalDJV2()
    window.open()
}

class QuintalDJV2 {

    private val display = Display()
    val shell = Shell(display)
    private val model:FileManager
    internal val viewer:WorkSpaceViewerManager
    val javardiseToggle:Button?
    val allowDuplicates:Button?
    val workSpaceMenu:Menu
    init {
        shell.text = "QuintalDJ"
        val layout= GridLayout()
        layout.numColumns=1
        shell.layout =layout


        // nao esquecer layouts nos paineis (caso contrario nao se ve nada)


        val buttonBar=Composite(shell,SWT.PUSH or SWT.BORDER )
        val gridData=GridData(GridData.FILL_HORIZONTAL)
        buttonBar.layoutData=gridData
        buttonBar.layout=RowLayout()

        val menuBar= Menu(shell,SWT.BAR)
        val fileMenuHeader=MenuItem(menuBar,SWT.CASCADE)
        fileMenuHeader.text = "&Project"

        val fileMenu=Menu(shell,SWT.DROP_DOWN)
        fileMenuHeader.menu=fileMenu

        val loadProject=MenuItem(fileMenu,SWT.PUSH)
        loadProject.text="&Load Project"

        val workSpaceMenuHeader=MenuItem (menuBar,SWT.CASCADE)
        workSpaceMenuHeader.text="&Add WorkSpace"
        workSpaceMenu=Menu(shell,SWT.DROP_DOWN)
        workSpaceMenuHeader.menu=workSpaceMenu

        shell.menuBar=menuBar

        val mainPanel=SashForm(shell, SWT.HORIZONTAL or SWT.BORDER)
        mainPanel.layoutData=GridData(GridData.FILL_BOTH)

        model=FileManager()
        val solver=model.solver
        val list=listView(model,mainPanel,this)
        viewer=WorkSpaceViewerManager(mainPanel,shell,this)

        val debugButton=Button(buttonBar,SWT.BORDER)
        loadProject.addSelectionListener(loadProjectListener(shell,model,viewer))

        //DEBUG BUTTON START
        debugButton.text="debug"
        debugButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                println("DEBUG BUTTON ACTIVATED")

                println("DEBUG BUTTON COMPLETED")
            }
        })
        //DEBUG BUTTON END

        //TOGGLES START
        javardiseToggle=Button(buttonBar,SWT.CHECK)
        javardiseToggle.selection=true
        javardiseToggle.text="Javardise"
        javardiseToggle.addSelectionListener(object : SelectionAdapter(){
            override fun widgetSelected(e: SelectionEvent?) {
                viewer.currentWorkSpace()?.javardise=javardiseToggle.selection
            }
        })

        allowDuplicates=Button(buttonBar,SWT.CHECK)
        allowDuplicates.selection=false
        allowDuplicates.text="Allow Duplicates"
        allowDuplicates.addSelectionListener(object : SelectionAdapter(){
            override fun widgetSelected(e: SelectionEvent?) {
                viewer.currentWorkSpace()?.allowDuplicate=allowDuplicates.selection
            }
        })
        //TOGGLES END

        mainPanel.setWeights(*listOf<Int>(1,4).toIntArray())
        shell.maximized=true

    }
    internal fun initToggles(viewer: WorkSpaceViewerManager){
        viewer.currentWorkSpace()?.javardise=javardiseToggle?.selection?:true
        viewer.currentWorkSpace()?.allowDuplicate=allowDuplicates?.selection?:false
    }

    internal fun initWorkSpaceMenuFromMap(workSpaceOptions: kotlin.collections.Map<String, Boolean>,workSpaceViewerManager: WorkSpaceViewerManager){
        for (s in workSpaceOptions){
            val item=MenuItem(workSpaceMenu,SWT.PUSH)
            item.text=s.key
            item.addSelectionListener(workSpaceOptionSelected(workSpaceViewerManager,s.value))
        }
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
        viewer.currentWorkSpace()?.methodSelected(model as MethodDeclaration)
    }

    internal class loadProjectListener(val shell:Shell,val fileManager:FileManager,val viewer: WorkSpaceViewerManager):SelectionListener{
        override fun widgetSelected(p0: SelectionEvent?) {
            val dialog = DirectoryDialog(shell, SWT.NULL);
            val path = dialog.open();
            if (path != null) {
                println(path)
                try {
                    fileManager.setDirectory(path)
                    if(fileManager.solver!=null) {
                        viewer.addSolver(fileManager.solver!!)
                    }
                } catch (e: IllegalArgumentException) {
                    println(e.message)
                }
            }
            else{
                println("nope")
            }
        }
        override fun widgetDefaultSelected(p0: SelectionEvent?) {}
    }

    internal class workSpaceOptionSelected(val view:WorkSpaceViewerManager,val option:Boolean):SelectionListener{
        override fun widgetSelected(p0: SelectionEvent?) {
            view.workSpaceOptionSelected(option)
        }
        override fun widgetDefaultSelected(p0: SelectionEvent?) {}
    }
}


