import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.BorderData
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Scrollable
import pt.iscte.javardise.external.scrollable
import javax.swing.border.Border

internal class VerticalLevelStructure<T>(val parent: Composite, val scroll:Boolean){
    private val levels= mutableListOf<VerticalLevel<T>>()
    val gridLayout: GridLayout
    var deepestLevel=-1


    init {
        gridLayout= GridLayout()
        gridLayout.numColumns = 0
        gridLayout.marginWidth=0
        gridLayout.marginHeight=0
        parent.layout = gridLayout
        parent.layoutData=GridData(GridData.FILL_VERTICAL)

    }

    fun removeLevel(depth:Int){
        for (i in depth..deepestLevel){
            levels[i].depth--
        }
        levels.removeAt(depth)
        deepestLevel--
        parent.requestLayout()
    }

    fun addLevel(){
        deepestLevel++
        gridLayout.numColumns=deepestLevel+1
        parent.layout=gridLayout
        val newLevel=VerticalLevel<T>(parent,deepestLevel,this)
        levels.add(newLevel)
    }

    fun removeWidget(id:T){
        levels.firstOrNull { it.isInLevel(id) }?.removeWidget(id)

    }


    internal fun addWidget(any:T,depth:Int,builderFunction: (T, Composite) -> Composite){
        val level=getLevel(depth)
        level.addWidget(builderFunction,any)

    }

    private fun getLevel(depth:Int):VerticalLevel<T>{
        if (depth<1){
            throw IllegalArgumentException("depth is negative or 0")
        }
        if(depth>deepestLevel+1){
            addLevel()
            return getLevel(depth)
        }else{
            return levels[depth-1]
        }
    }

    fun addAndReorganize(any:T, inClassDepth: Int, initFun: (T, Composite)-> Composite, called:(T)->List<T>, depthFun:(T)->Int) {
        val calledThings=called(any)
        for(thing in calledThings){
            levels.first{it.isInLevel(thing)}.removeWidget(thing)
        }
        addWidget(any,inClassDepth,initFun)
        for(thing in calledThings){
            addAndReorganize(thing,depthFun(thing),initFun,called,depthFun)
        }

    }

    fun removeAndReoganize(any: T, initFun: (T, Composite)-> Composite, called: (T)->List<T>, depthFun:(T)->Int) {
        val calledThings=called(any)
        for(thing in calledThings){
            levels.firstOrNull{it.isInLevel(thing)}?.removeWidget(thing)
        }
        removeWidget(any)
        for(thing in calledThings){
            addAndReorganize(thing,depthFun(thing),initFun,called,depthFun)
        }
    }

    fun reorderLevel(placeInLevel:Int,any:T,depth:Int){
        val levelOfDepth=levels.getOrNull(depth)
        if (levelOfDepth!=null){
            levelOfDepth.moveWidget(any,placeInLevel)
        }else {
            println("null level")
        }
    }


    private class VerticalLevel<T>(parent: Composite, var depth:Int, val levelManager:VerticalLevelStructure<T>){
        val level: Composite
        val listOfWidgets= mutableListOf<Pair<T, Composite>>()

        init {

            if (levelManager.scroll){
                level=parent.scrollable {
                    Composite(it, SWT.NONE)
                }
                level.background= Color(255,255,0)
//                val levelScroll= ScrolledComposite(parent,  SWT.V_SCROLL )
//                levelScroll.expandHorizontal=true
//                levelScroll.expandVertical=true
//                levelScroll.minWidth=350
//                levelScroll.minHeight=350
//                level= Composite(levelScroll, SWT.NONE)
//                levelScroll.content=level

            }else{
                level= Composite(parent, SWT.NONE)
                //level.background=Color(255,255,0)
            }

            val gridLayout=GridLayout()
            gridLayout.marginWidth=0
            gridLayout.marginHeight=0
            level.layout=gridLayout


            level.addDisposeListener {
                listOfWidgets.forEach { it.second.dispose() }
                listOfWidgets.clear()
            }
            level.requestLayout()
        }

        fun addWidget(builderFunction:(T, Composite)-> Composite, thing:T){
            val composite=builderFunction(thing,level)
            val data= GridData(GridData.FILL_HORIZONTAL)
            composite.layoutData=data
            listOfWidgets.add(Pair(thing,composite))

            composite.requestLayout()
        }

        fun removeWidget(id:T){
            val pair=listOfWidgets.first { it.first==id }
            pair.second.dispose()
            listOfWidgets.remove(pair)
            if(!checkLevel()){level.requestLayout()}
        }

        fun isInLevel(id:T):Boolean{
            return listOfWidgets.firstOrNull { it.first==id }!=null
        }

        private fun checkLevel():Boolean{
            if (listOfWidgets.isEmpty()){
                level.dispose()
                levelManager.removeLevel(depth)
                levelManager.parent.requestLayout()
                return true
            }
            return false
        }

        fun moveWidget(any:T,index:Int){
            val pair=listOfWidgets.firstOrNull { it.first==any }
            if (pair==null){
                println("move widget error")
            }else {
                listOfWidgets.remove(pair)
                listOfWidgets.add(index, pair)
                val nextWidget = listOfWidgets.getOrNull(index + 1)
                if (nextWidget != null) {
                    pair.second.moveAbove(nextWidget.second)
                }
            }
        }

    }

}