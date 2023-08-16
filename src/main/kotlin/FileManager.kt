import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import java.io.File

internal class FileManager() :IObservable<(EventType, FileManager, CompilationUnit?) -> Unit> {


    override val observers: MutableList<(EventType, FileManager, CompilationUnit?) -> Unit> = mutableListOf()
    var javaPackages= mutableListOf<Pckg>()
    var solver:CombinedTypeSolver?=null
    val registedThreads= mutableListOf<Thread>()


    private fun clearProject(){
        javaPackages.clear()
        solver=null
        stopThreads()
    }

    internal fun setDirectory(path: String){
        clearProject()
        val (javaFiles,solver)=parserFromFile(path,this)
        this.solver=solver
        if(javaFiles.size==0){
            throw IllegalArgumentException("not a java project :(")
        }else{
            this.javaPackages.addAll(javaFiles)
            this.solver=solver
        }
        notifyObservers { it(EventType.PROJECTLOADED,this,null) }
    }

//    fun addJavaFile(path:String){
//        println("adding")
//        val fileparser=StaticJavaParser.parse(File(path))
//        javaPackages.add(fileparser)
//        notifyObservers { it(EventType.FILEADDED,this,fileparser) }
//    }

    public fun stopThreads(){
        for(thread in registedThreads){
            thread.interrupt()
        }
    }



}