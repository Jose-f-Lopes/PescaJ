import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
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
    val workSpaceMenu:Menu
    private val list:listView
    init {
        shell.text = "Pesca-J"
        shell.layout=FillLayout()
        shell.image= Image(display,"ImagesAndIcons\\icon.png")


        // nao esquecer layouts nos paineis (caso contrario nao se ve nada)



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

        val holder=Composite(shell,SWT.NONE)
        holder.layout=GridLayout()



        val mainPanel=SashForm(holder, SWT.HORIZONTAL)
        mainPanel.layoutData=GridData(GridData.FILL_BOTH)
        mainPanel.layout=GridLayout()
        val leftSection=Composite(mainPanel,SWT.NONE or SWT.BORDER)
        val layout=GridLayout()
        leftSection.layout=layout
        val searchBar=SearchBar(leftSection)
        searchBar.myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)

        model=FileManager()
        val solver=model.solver
        list=listView(model,leftSection)
        list.getComposite().layoutData=GridData(GridData.FILL_BOTH)

        searchBar.addObserver { searchEvent,searchString,filteredOut ->
            if (searchEvent==SearchBar.searchEvent.NEWSEARCH){
                list.addFilter(searchString!!,filteredOut)
            }

        }


        viewer=WorkSpaceViewerManager(mainPanel,this)
        list.addObserver { listEntityEvent,clazz,method ->
            if (listEntityEvent==listView.listEntityEvent.METHODCLICKED){
                method?.let {viewer.currentWorkSpace()?.methodSelected(method)}

            }
            if (listEntityEvent==listView.listEntityEvent.CLASSCLICKED){
                clazz?.let {viewer.currentWorkSpace()?.classSelected(clazz)}

            }
        }


        loadProject.addSelectionListener(loadProjectListener(shell,model,viewer))


        mainPanel.setWeights(*listOf<Int>(1,5).toIntArray())
        shell.maximized=true

    }
    internal fun initToggles(viewer: WorkSpaceViewerManager){
        //viewer.currentWorkSpace()?.isDocumentation=documentationButton?.selection?:false
        //viewer.currentWorkSpace()?.allowDuplicate=allowDuplicates?.selection?:false

    }

    internal fun initWorkSpaceMenuFromMap(workSpaceOptions: kotlin.collections.Map<String, KClass<WorkSpace>>,workSpaceViewerManager: WorkSpaceViewerManager){
        for (s in workSpaceOptions){
            val item=MenuItem(workSpaceMenu,SWT.PUSH)
            item.text=s.key
            item.addSelectionListener(object :SelectionListener{
                override fun widgetSelected(p0: SelectionEvent?) {
                    val ws=workSpaceViewerManager.openWorkSpace(s.value)
                    ws.projSelected(list.getPckgs())
                }
                override fun widgetDefaultSelected(p0: SelectionEvent?) {
                }
            })
        }
    }
    fun open() {
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
        model.stopThreads()
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

}


