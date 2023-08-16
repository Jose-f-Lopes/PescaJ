import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag
import com.github.javaparser.javadoc.description.JavadocDescription
import com.github.javaparser.javadoc.description.JavadocDescriptionElement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.external.getOrNull
import java.util.logging.Filter

class SearchBar(parent:Composite):CustomComposite,IObservable<(SearchBar.searchEvent,String?,List<FilterOptions>)->Unit> {
    val myComposite:Composite
    override val observers: MutableList<(searchEvent,String?,List<FilterOptions>) -> Unit> = mutableListOf()
    val filterOut= mutableListOf<FilterOptions>()

    enum class searchEvent{
        NEWSEARCH
    }
    init {
        myComposite=Composite(parent, SWT.NONE)
        val layout=GridLayout()
        layout.numColumns=4
        myComposite.layout=layout

        val searchText= Text(myComposite,SWT.BORDER or SWT.CENTER)
        searchText.layoutData=GridData(GridData.FILL_BOTH)


//        searchText.addModifyListener(object :ModifyListener{
//            override fun modifyText(p0: ModifyEvent?) {
//               notifyObservers { searchEvent.NEWSEARCH }
//            }
//
//        })

        val searchGo=Button(myComposite,SWT.BORDER)
        val lupaImage= Image(Display.getDefault(),"ImagesAndIcons\\lupa.png")
        searchGo.image=lupaImage
        searchGo.addSelectionListener(object :SelectionListener{
            override fun widgetSelected(p0: SelectionEvent?) {
                notifyObservers { it(searchEvent.NEWSEARCH,searchText.text,filterOut) }
            }
            override fun widgetDefaultSelected(p0: SelectionEvent?) {
            }

        })
        val options=Button(myComposite,SWT.BORDER)
        val pontosImage= Image(Display.getDefault(),"ImagesAndIcons\\pontos.png")
        options.image=pontosImage

        val optionsMenu= Menu(myComposite)
        options.menu=optionsMenu
        options.addSelectionListener(object :SelectionListener{
            override fun widgetSelected(p0: SelectionEvent?) {
                options.menu.visible=true
            }
            override fun widgetDefaultSelected(p0: SelectionEvent?) {
            }
        })
        FilterOptions.values().forEach {filter->
            val option= MenuItem(optionsMenu,SWT.CHECK)
            option.text=filter.nameString
            option.selection=true
            option.addSelectionListener(object :SelectionListener{
                override fun widgetSelected(p0: SelectionEvent?) {
                    if(option.selection and filterOut.contains(filter)){
                        filterOut.remove(filter)
                    }
                    if (!option.selection and !filterOut.contains(filter)){
                        filterOut.add(filter)
                    }
                    notifyObservers { it(searchEvent.NEWSEARCH,searchText.text,filterOut) }
                }
                override fun widgetDefaultSelected(p0: SelectionEvent?) {
                }
            })
        }

        val clear=Button(myComposite,SWT.BORDER)
        val lixoImage= Image(Display.getDefault(),"ImagesAndIcons\\garbage.png")
        clear.image=lixoImage
        clear.addSelectionListener(object :SelectionListener{
            override fun widgetSelected(p0: SelectionEvent?) {
                searchText.text=""
                optionsMenu.items.forEach { it.selection=true }
                filterOut.clear()
                notifyObservers { it(searchEvent.NEWSEARCH,"",filterOut) }
            }
            override fun widgetDefaultSelected(p0: SelectionEvent?) {
            }

        })



    }
    override fun getComposite(): Composite {
        return myComposite
    }

}

enum class FilterOptions(val filterFun:(MethodDeclaration)->Boolean,val nameString:String){
    PUBLIC({method->method.isPublic},"Public"),
    PRIVATE({method->method.isPrivate},"Private"),
    PROTECTED({method->method.isProtected},"Protected"),
    DOCUMENTED({method->method.hasJavaDocComment()},"Is Documented"),
    NOTDOCUMENTED({ method->!method.hasJavaDocComment() },"Not Documented");

}