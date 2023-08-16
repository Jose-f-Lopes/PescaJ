import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc
import com.github.javaparser.ast.observer.ObservableProperty
import com.github.javaparser.javadoc.Javadoc
import com.github.javaparser.javadoc.JavadocBlockTag
import com.github.javaparser.javadoc.description.JavadocDescription
import com.github.javaparser.javadoc.description.JavadocDescriptionElement
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import pt.iscte.javardise.DefaultConfiguration
import pt.iscte.javardise.external.getOrNull
import pt.iscte.javardise.widgets.members.FieldWidget
import javax.net.ssl.CertPathTrustManagerParameters

internal class DefaultClassHeader(parent:Composite, clazz:ClassOrInterfaceDeclaration):CustomComposite {
    val myComposite:Composite
    init {
        myComposite= Composite(parent, SWT.NONE)
        myComposite.layout=GridLayout()

        val text= Text(myComposite, SWT.BORDER or SWT.READ_ONLY)
        text.text=clazz.nameAsString
        text.font= Font(Display.getDefault(),FontData("Courier New",10,SWT.BOLD))
        for (field in clazz.fields){
            val fieldwiget= FieldWidget(myComposite,field,configuration = object : DefaultConfiguration(){
                override val fontSize: Int
                    get() = 12
                override val tabLength: Int
                    get() = 2
                override val fontFace: String
                    get() = "Courier New"

            })
        }
    }
    override fun getComposite(): Composite {
        return myComposite
    }
}


internal class DocumentationClassHeader(parent:Composite, clazz:ClassOrInterfaceDeclaration):CustomComposite,IObservable<(List<FilterOptions>)->Unit> {
    val myComposite:Composite
    init {
        myComposite= Composite(parent, SWT.NONE)
        myComposite.layout=GridLayout()
        myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)

        val headerComposite=Composite(myComposite,SWT.NONE)
        val layout=GridLayout()
        layout.numColumns=2
        headerComposite.layout=layout


        val text= Text(headerComposite, SWT.BORDER or SWT.READ_ONLY)
        text.text=clazz.nameAsString
        text.font= Font(Display.getDefault(),FontData("Courier New",10,SWT.BOLD))

        val options=Button(headerComposite,SWT.BORDER)
        val pontosImage= Image(Display.getDefault(),"ImagesAndIcons\\pontos.png")
        options.image=pontosImage
        val optionsMenu= Menu(headerComposite)
        options.menu=optionsMenu
        options.addSelectionListener(object :SelectionListener{
            override fun widgetSelected(p0: SelectionEvent?) {
                options.menu.visible=true
            }
            override fun widgetDefaultSelected(p0: SelectionEvent?) {
            }
        })
        val filterOut= mutableListOf<FilterOptions>()
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
                    notifyObservers { it(filterOut) }
                }
                override fun widgetDefaultSelected(p0: SelectionEvent?) {
                }
            })
        }

        ClazzDocFragment(myComposite,clazz)
    }
    override fun getComposite(): Composite {
        return myComposite
    }
    override val observers= mutableListOf<(List<FilterOptions>)->Unit>()

    internal class ClazzDocFragment(parent:Composite,val clazz:ClassOrInterfaceDeclaration):JavaDocController{

        init {
            if (clazz.javadoc.getOrNull==null){
                clazz.setJavadocComment(initJavaDoc())
            }

            val myComposite=Composite(parent,SWT.NONE)
            myComposite.layout=GridLayout()
            myComposite.layoutData=GridData(GridData.FILL_HORIZONTAL)
            val descriptionElement=docView.descriptionWidget(myComposite,clazz.javadoc.get().description,this,true)
            //TODO fix
//            clazz.observeProperty<Comment>(ObservableProperty.COMMENT){
//                if(it is JavadocComment){
//                    descriptionElement.update((it as JavadocComment).parse().description)
//                }
//            }
        }

        fun initJavaDoc():Javadoc{
            val newJDoc= Javadoc(
                JavadocDescription(
                    mutableListOf(JavadocDescriptionElement{"New Description"})
                )
            )
            return newJDoc
        }


        override fun descriptionEdited(description: JavadocDescription) {
            classEdited(clazz,description=description)
        }

        override fun tagsEdited(tags: MutableList<JavadocBlockTag>) {
            classEdited(clazz, tags = tags)
        }
    }

}
