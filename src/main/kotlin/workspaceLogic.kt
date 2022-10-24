import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.MethodDeclaration

class WorkSpaceModel(){

    val grid= mutableListOf<MutableList<BodyDeclaration<*>>>()

    init {
        val level0= mutableListOf<BodyDeclaration<*>>()
        grid.add(level0)
    }

    fun addMethod(method:BodyDeclaration<*>){
        grid[0].add(method)
        if(grid[0].size!=0){
            println(javaParserUtil.contains(method as MethodDeclaration,grid[0][0]))
        }
    }

}