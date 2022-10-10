import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*

fun main() {
    val window = Window()
    window.open()
}

class Window {

    private val display = Display()
    private val shell = Shell(display)

    private lateinit var textArea: Text
    private lateinit var table: Table

    init {
        shell.text = "hello"
        shell.layout = FillLayout() // nao esquecer layouts nos paineis (caso contrario nao se ve nada)
        val sashForm = SashForm(shell, SWT.HORIZONTAL)
        initTree(sashForm)
        initSplit(sashForm)
    }

    fun initTree(parent: Composite) {
        val comp = SashForm(parent, SWT.VERTICAL)

        val tree = Tree(comp, SWT.CHECK or SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL)
        val a = TreeItem(tree, 0)
        a.text = "A"
        val b = TreeItem(tree, 0)
        b.text = "B"

        val c = TreeItem(b, 0)
        c.text = "C"

        val d = TreeItem(c, 0)
        d.text = "D"

        val button = Button(comp, SWT.PUSH)
        button.text = "show"
        button.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                if (tree.selection.isEmpty()) {
                    val box = MessageBox(shell, SWT.CANCEL or SWT.OK or SWT.ICON_INFORMATION)
                    box.message = "select in tree"
                    box.open()
                } else {
                    textArea.text = tree.selection[0].text
                    addTableItem(tree.selection[0].text, "la", "le", "li")
                }
            }
        })
    }

    fun initSplit(parent: Composite) {
        val sashForm = SashForm(parent, SWT.VERTICAL)
        textArea = Text(sashForm, SWT.MULTI)
        initTable(sashForm)
    }

    fun initTable(parent: Composite) {
        table = Table(parent, SWT.CHECK or SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL)
        table.headerVisible = true
        val titles = arrayOf("Col 1", "Col 2", "Col 3", "Col 4")

        for (loopIndex in titles.indices) {
            val column = TableColumn(table, SWT.NULL)
            column.text = titles[loopIndex]
        }

        for (loopIndex in 0..5) {
            addTableItem("Item $loopIndex", "Yes", "No")
        }

        for (loopIndex in titles.indices) {
            table.getColumn(loopIndex).pack()
        }
    }

    fun addTableItem(vararg text: String) {
        val item = TableItem(table, SWT.NULL)
        text.forEachIndexed { index, s ->
            item.setText(index, s)
        }
    }

    fun open() {
        //shell.pack()
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        display.dispose()
    }
}